/***************************************************************************
 *   Copyright (C) 2006 by Adam Deller                                     *
 *                                                                         *
 *   This program is free for non-commercial use: see the license file     *
 *   at http://astronomy.swin.edu.au:~adeller/software/difx/ for more      *
 *   details.                                                              *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                  *
 ***************************************************************************/
#include <mpi.h>
#include "mk5.h"

#include <errno.h>
#include <sys/types.h>
#include <sys/socket.h>

#define MAXPACKETSIZE 10000
#define MARK5FILL 0x11223344;

static int genFormatName(Configuration::dataformat format, int nchan, double bw, int nbits, int framebytes, char *formatname)
{
  int fanout=1, mbps;

  mbps = int(2*nchan*bw*nbits + 0.5);

  switch(format)
  {
    case Configuration::MKIV:
      fanout = framebytes*8/(20000*nbits*nchan);
      if(fanout*20000*nbits*nchan != framebytes*8)
      {
        cerr << "genFormatName : MKIV format : framebytes = " << framebytes << " is not allowed\n";
        exit(1);
      }
      sprintf(formatname, "MKIV1_%d-%d-%d-%d", fanout, mbps, nchan, nbits);
      break;
    case Configuration::VLBA:
      fanout = framebytes*8/(20160*nbits*nchan);
      if(fanout*20160*nbits*nchan != framebytes*8)
      {
        cerr << "genFormatName : VLBA format : framebytes = " << framebytes << " is not allowed\n";
        exit(1);
      }
      sprintf(formatname, "VLBA1_%d-%d-%d-%d", fanout, mbps, nchan, nbits);
      break;
    case Configuration::MARK5B:
      sprintf(formatname, "Mark5B-%d-%d-%d", mbps, nchan, nbits);
      break;
    default:
      cerr << "genFormatName : unsupported format encountered\n" << endl;
      exit(1);
  }

  return fanout;
}

/// Mk5DataMode ---------------------------------------------------------


Mk5Mode::Mk5Mode(Configuration * conf, int confindex, int dsindex, int nchan, int bpersend, int gblocks, int nfreqs, double bw, double * freqclkoffsets, int ninputbands, int noutputbands, int nbits, bool fbank, bool postffringe, bool quaddelayinterp, bool cacorrs, int framebytes, int framesamples, Configuration::dataformat format)
 : Mode(conf, confindex, dsindex, nchan, bpersend, gblocks, nfreqs, bw, freqclkoffsets, ninputbands, noutputbands, nbits, nchan*2+4, fbank, postffringe, quaddelayinterp, cacorrs, bw*2)
{
  char formatname[64];

  fanout = genFormatName(format, ninputbands, bw, nbits, framebytes, formatname);

  // since we allocated the max amount of space needed above, we need to change
  // this to the number actually needed.
  unpacksamples = nchan*2;

  samplestounpack = nchan*2;
  if(fanout > 1)
    samplestounpack += fanout;

  //create the mark5_stream used for unpacking
  mark5stream = new_mark5_stream(
      new_mark5_stream_unpacker(0),
      new_mark5_format_generic_from_string(formatname) );

  if(mark5stream == 0)
  {
    cerr << "Mk5Mode::Mk5Mode : mark5stream is null " << endl;
  }

  if(framesamples != mark5stream->framesamples)
  {
    cerr << "Mk5Mode::Mk5Mode : framesamples inconsistent (" << framesamples 
	 << "/" << mark5stream->framesamples << ")" << endl;
    exit(1);
  }

}

Mk5Mode::~Mk5Mode()
{
  delete_mark5_stream(mark5stream);
}

float Mk5Mode::unpack(int sampleoffset)
{
  int framesin, goodsamples;

  //work out where to start from
  framesin = (sampleoffset/framesamples);
  unpackstartsamples = sampleoffset - (sampleoffset % fanout);

  //unpack one frame plus one FFT size worth of samples
  goodsamples = mark5_unpack_with_offset(mark5stream, data, unpackstartsamples, unpackedarrays, samplestounpack);
  if(fanout > 1)
  {
    for(int i = 0; i < sampleoffset % fanout; i++)
      if(unpackedarrays[0][i] != 0.0)
        goodsamples--;
    for(int i = unpacksamples + sampleoffset % fanout; i < samplestounpack; i++)
      if(unpackedarrays[0][i] != 0.0)
        goodsamples--;
  }
    
  if(goodsamples < 0)
  {
    cerr << "Error trying to unpack Mark5 format data at sampleoffset " << sampleoffset << " from buffer seconds " << bufferseconds << " plus " << buffermicroseconds << " microseconds!!!" << endl;
    goodsamples = 0;
  }

  return goodsamples/(float)unpacksamples;
}



/// Mk5DataStream -------------------------------------------------------


Mk5DataStream::Mk5DataStream(Configuration * conf, int snum, int id, int ncores, int * cids, int bufferfactor, int numsegments)
 : DataStream(conf, snum, id, ncores, cids, bufferfactor, numsegments)
{
  //each data buffer segment contains an integer number of frames, because thats the way config determines max bytes
  lastconfig = -1;
}

Mk5DataStream::~Mk5DataStream()
{}

void Mk5DataStream::initialise()
{
  DataStream::initialise();

  if (!readfromfile && !tcp) {
    if (sizeof(long long)!=8) {
      cerr << "Warning: DataStream assumes long long is 8 bytes, it is " << sizeof(long long) << " bytes" << endl;
      // Should exit here
    }
    udpsize = abs(tcpwindowsizebytes)-20-2*4-sizeof(long long); // IP header is 20 bytes, UDP is 4x2 bytes
    if (udpsize<=0) {
      cerr << "Datastream " << mpiid << ":" << " Warning implied UDP packet size is negative!! " << endl;
      // Should exit here
    }
    udpsize &= ~ 0x7;  // Ensures udpsize multiple of 64 bits
    udp_offset = 0;
    packet_framestart = 0;
    packet_head = 0;
    udp_buf = new char[MAXPACKETSIZE];
    packets_arrived.resize(readbytes/udpsize+2);
    
    invalid_buf = new char[udpsize];
        unsigned long *tmp = (unsigned long*)invalid_buf;
    for (int i=0; i<udpsize/4; i++) {
      tmp[i] = MARK5FILL;
    }

    // UDP statistics
    packet_drop = 0;
    packet_oo = 0;
    packet_duplicate = 0;
    npacket = 0;
    packet_sum = 0;
  }

}


int Mk5DataStream::calculateControlParams(int offsetsec, int offsetsamples)
{
  int bufferindex, framesin, vlbaoffset;
  
  bufferindex = DataStream::calculateControlParams(offsetsec, offsetsamples);

  if(bufferinfo[atsegment].controlbuffer[bufferinfo[atsegment].numsent][0] < 0.0)
    return 0;

  //do the necessary correction to start from a frame boundary - work out the offset from the start of this segment
  vlbaoffset = bufferindex - atsegment*readbytes;

  if(vlbaoffset < 0)
  {
    cout << "ERROR Mk5DataStream::calculateControlParams : vlbaoffset=" << vlbaoffset << endl;
  }

  // bufferindex was previously computed assuming no framing overhead
  framesin = vlbaoffset/payloadbytes;

  // Note here a time is needed, so we only count payloadbytes
  bufferinfo[atsegment].controlbuffer[bufferinfo[atsegment].numsent][0] = bufferinfo[atsegment].seconds + ((double)(bufferinfo[atsegment].nanoseconds) + ((double)framesin*framens))/1000000000.0;

  //go back to nearest frame -- here the total number of bytes matters
  bufferindex = atsegment*readbytes + framesin*framebytes;
  if(bufferindex >= bufferbytes)
  {
    cout << "Mk5DataStream::calculateControlParams : bufferindex=" << bufferindex << " >= bufferbytes=" << bufferbytes << endl;
    bufferinfo[atsegment].controlbuffer[bufferinfo[atsegment].numsent][0] = -1.0;
    return 0;
  }
  return bufferindex;
}

void Mk5DataStream::updateConfig(int segmentindex)
{
  int sec; // dummy for now

  //run the default update config, then add additional information specific to Mk5
  DataStream::updateConfig(segmentindex);
  if(bufferinfo[segmentindex].configindex < 0) //If the config < 0 we can skip this scan
    return;

  framebytes = config->getFrameBytes(bufferinfo[segmentindex].configindex, streamnum);
  payloadbytes = config->getFramePayloadBytes(bufferinfo[segmentindex].configindex, streamnum);
  config->getFrameInc(bufferinfo[segmentindex].configindex, streamnum, sec, framens);

  //correct the nsinc - should be number of frames*frame time
  bufferinfo[segmentindex].nsinc = int(((bufferbytes/numdatasegments)/framebytes)*framens);

  //take care of the case where an integral number of frames is not an integral number of blockspersend - ensure sendbytes is long enough

  //note below, the math should produce a pure integer, but add 0.5 to make sure that the fuzziness of floats doesn't cause an off-by-one error
  bufferinfo[segmentindex].sendbytes = int(((((double)bufferinfo[segmentindex].sendbytes)* ((double)config->getBlocksPerSend(bufferinfo[segmentindex].configindex)))/(config->getBlocksPerSend(bufferinfo[segmentindex].configindex) + config->getGuardBlocks(bufferinfo[segmentindex].configindex)) + 0.5));
}

void Mk5DataStream::initialiseFile(int configindex, int fileindex)
{
  int offset;
  char formatname[64];
  struct mark5_stream *mark5stream;
  int nbits, ninputbands, framebytes;
  Configuration::dataformat format;
  double bw;

  format = config->getDataFormat(configindex, streamnum);
  nbits = config->getDNumBits(configindex, streamnum);
  ninputbands = config->getDNumInputBands(configindex, streamnum);
  framebytes = config->getFrameBytes(configindex, streamnum);
  bw = config->getConfigBandwidth(configindex);

  genFormatName(format, ninputbands, bw, nbits, framebytes, formatname);

  mark5stream = new_mark5_stream(
    new_mark5_stream_file(datafilenames[configindex][fileindex].c_str(), 0),
    new_mark5_format_generic_from_string(formatname) );
  if(mark5stream->nchan != config->getDNumInputBands(configindex, streamnum))
  {
    cerr << "Error - number of input bands for datastream " << streamnum << " (" << ninputbands << ") does not match with MkV file " << datafilenames[configindex][fileindex] << " (" << mark5stream->nchan << "), will be ignored!!!" << endl;
  }

  // resolve any day ambiguities
  mark5_stream_fix_mjd(mark5stream, corrstartday);

  mark5_stream_print(mark5stream);

  offset = mark5stream->frameoffset;

  readseconds = 86400*(mark5stream->mjd-corrstartday) + mark5stream->sec-corrstartseconds + intclockseconds;
  readnanoseconds = mark5stream->ns;
  cout << "The frame start day is " << mark5stream->mjd << ", the frame start seconds is " << mark5stream->sec << ", the frame start ns is " << mark5stream->ns << ", readseconds is " << readseconds << ", readnanoseconds is " << readnanoseconds << endl;

  //close mark5stream
  delete_mark5_stream(mark5stream);

  cout << "About to seek to byte " << offset << " to get to the first frame" << endl;

  input.seekg(offset);
}

void Mk5DataStream::networkToMemory(int buffersegment, int & framebytesremaining)
{
  DataStream::networkToMemory(buffersegment, framebytesremaining);

  // This deadreckons readseconds from the last frame. This will not initially be set, and we really should 
  // resync occasionally, so..
  initialiseNetwork(0, buffersegment);

  readnanoseconds += bufferinfo[buffersegment].nsinc;
  readseconds += readnanoseconds/1000000000;
  readnanoseconds %= 1000000000;
}



/****
 ASSUMPTIONS

 udp_offset is left containing number of bytes still to be consumned from last UDP packet. If
 first packet from next frame is missing then udp_offset points to END of initial data. It is
 assumed only udpsize bytes are available though.

*****/


int Mk5DataStream::readnetwork(int sock, char* ptr, int bytestoread, int* nread)
{
  ssize_t nr;

  cout << "DataStream " << mpiid << ": Mk5DataStream::readnetwork" << endl;

  if (tcp) {
    DataStream::readnetwork(sock, ptr, bytestoread, nread);
  } else { // UDP
    bool done;
    long long packet_index, packet_frameend;
    unsigned long long sequence;
    struct msghdr msg;
    struct iovec iov[2];

    memset(&msg, 0, sizeof(msg));
    iov[0].iov_base = &sequence;
    iov[0].iov_len = sizeof(sequence);
    iov[1].iov_base = udp_buf;
    iov[1].iov_len = MAXPACKETSIZE;

    packet_frameend = packet_framestart+(bytestoread-(udp_offset%udpsize)-1)/udpsize+1;
    cout << "****** readnetwork will read packets " << packet_framestart << " to " << packet_frameend << endl;
    cout << "****** udp_offset = " << udp_offset << endl;

    if (packet_frameend-packet_framestart+1>packets_arrived.size()) {
      cerr << "Mk5DataStream::readnetwork  bytestoreed too large (" << bytestoread << ")" << endl;
      exit(1);
    } 
    
    for (int i=0; i<=packet_frameend-packet_framestart; i++) {
      packets_arrived[i] = false;
    }
    *nread = -1;
    done = 0;

    // First copy left over bytes from last packet
    if (udp_offset>0) {
      packet_index = (udp_offset-1)/udpsize;
      udp_offset = (udp_offset-1)%udpsize+1;
      cout "*** DataStream " << mpiid << ": packet_index=" << packet_index << " udp_offset=" << udp_offset << endl;
      if (packet_index<=packet_frameend) // Check not out of range
	packets_arrived[packet_index] = true;

      if (bytestoread<udp_offset+packet_index*udpsize) {
	if (packet_index==0) {
	  memcpy(ptr, udp_buf, bytestoread);
	  packet_sum += bytestoread;
	  npacket++;
	  udp_offset -= bytestoread;
	  memmove(udp_buf, udp_buf+bytestoread, udp_offset);
	} else if (packet_index>packet_frameend) {
	  // NEED TO SETUP FRAMEHEAD AND OFFSET ETC
	  done = 1;
	  cerr << "CORNER UDP CASE NOT IMPLEMENTED YET!!!!! " << endl;
	} else {
	  int bytes = (bytestoread-udp_offset)%udpsize;
	  memcpy(ptr+udp_offset+(packet_index-1)*udpsize, udp_buf, bytes);  
	  packet_sum += bytes;
	  npacket++;
	  memmove(udp_buf,udp_buf+bytes, udpsize-bytes);
	  udp_offset = udpsize-bytes;
	  packet_framestart = packet_head;  // CHECK THIS IS CORRECT
	}
	done = 1;

      } else {
	if (packet_index==0) { // Partial packet
	  memcpy(ptr, udp_buf, udp_offset);
	  packet_sum += udp_offset;
	  npacket++;
	} else {
	  memcpy(ptr+udp_offset+(packet_index-1)*udpsize, udp_buf, udpsize);
	  npacket++;
	  packet_sum += udpsize;
	}
      } 
      udp_offset %= udp_size;
    }      

    while (!done) {
      nr = recvmsg(sock,&msg,MSG_WAITALL);
      if (nr==-1) { // Error reading socket
	if (errno == EINTR) continue;
	return(nr);
      } else if (nr==0) {  // Socket closed remotely
	return(nr);
      } else if (nr!=udpsize+sizeof(long long)) {
	cerr << "DataStream " << mpiid << ": Expected " << udpsize+sizeof(long long) << " bytes, got " << nr << "bytes. Quiting" << endl;
	exit(1);
      } else {
	// Check this is a sensible packet
	packet_index = sequence-packet_framestart;
	if (packet_index<0) {
	  // If this was much smaller we really should assume the sequence has got screwed up and 
	  // Resync
	  packet_oo++;  // This could be duplicate but we cannot tell
	} else if (packet_index==packet_frameend) { 
	  int bytes = (bytestoread-udp_offset-1)%udpsize+1;
	  // Consistence check
	  if (bytes<0) {
	    cerr << "Datastream " << mpiid << ": Error read too many UDP packets!!" << endl;
	    exit(1);
	  } else if (bytes > udpsize) {
	    cerr << "Datastream " << mpiid << ": Error have not read enough UDP packets!!" << endl;
	    exit(1);
	  }
	  memcpy(ptr+udp_offset+(packet_index-1)*udpsize,udp_buf,bytes);
	  packet_sum += bytes;
	  memmove(udp_buf,udp_buf+bytes, udpsize-bytes);
	  udp_offset = udpsize-bytes;
	  if (udp_offset==0) {
	    packet_framestart = sequence; 
	    npacket++;
	  } else
	    packet_framestart = sequence+1; 
	  packet_head = sequence;
	  *nread = bytestoread;
	  done = 1;
	} else if (packet_index>packet_frameend) { 
	  udp_offset = udpsize-(bytestoread-udp_offset)%udpsize+udpsize*(packet_index-packet_frameend);
	  if (udp_offset==0) 
	    packet_framestart = packet_frameend+1;
	  else
	    packet_framestart = packet_frameend;
	  packet_head = sequence;
	    *nread = bytestoread;
	    done = 1;
	} else if (packets_arrived[packet_index]) {
	  packet_duplicate++;
	} else {
	  // This must be good data finally!
	  packets_arrived[packet_index] = true;
	  memcpy(ptr+udp_offset+(packet_index-1)*udpsize,udp_buf,udpsize);
	  packet_sum += udpsize;
	  nread += udpsize;
	  
	  // Do packet statistics
	  if (sequence<packet_head) { // Got a packet earlier than the head
	    packet_oo++;
	    } else {
	    packet_head = sequence;
	    }
	  npacket++;
	}
      }
    }
    // Replace missing packets with fill data
    for (int i=0; i<=packet_frameend-packet_framestart; i++) {
      if (!packets_arrived[i]) {
	packet_drop++; // Will generally count dropped first packet twice
	if (i==0) {
	  memcpy(ptr, invalid_buf, udp_offset); // CHECK INITIAL FULL OR EMPTY BUFFER
	} else if (i=packet_frameend-packet_framestart) {
	  memcpy(ptr+udp_offset+(i-1)*udpsize,invalid_buf,(bytestoread-udp_offset)%udpsize);	  
	} else {
	  memcpy(ptr+udp_offset+(i-1)*udpsize,invalid_buf,udpsize);	  
	}
      }
    }
  }
  return(1);
}

// This is almost identical to initialiseFile. The two should maybe be combined
void Mk5DataStream::initialiseNetwork(int configindex, int buffersegment)
{
  int offset;
  char formatname[64];
  char *ptr;
  struct mark5_stream *mark5stream;
  int nbits, ninputbands, framebytes;
  Configuration::dataformat format;
  double bw;


  format = config->getDataFormat(configindex, streamnum);
  nbits = config->getDNumBits(configindex, streamnum);
  ninputbands = config->getDNumInputBands(configindex, streamnum);
  framebytes = config->getFrameBytes(configindex, streamnum);
  bw = config->getConfigBandwidth(configindex);

  genFormatName(format, ninputbands, bw, nbits, framebytes, formatname);

  mark5stream = new_mark5_stream(
    new_mark5_stream_memory(&databuffer[buffersegment*(bufferbytes/numdatasegments)], 
			    bufferinfo[buffersegment].validbytes),
    new_mark5_format_generic_from_string(formatname) );
  if(mark5stream->nchan != config->getDNumInputBands(configindex, streamnum))
  {
    cerr << "Error - number of input bands for datastream " << streamnum << " (" << ninputbands << ") does not match with MkV data " << " (" << mark5stream->nchan << "), will be ignored!!!" << endl;
  }

  // resolve any day ambiguities
  mark5_stream_fix_mjd(mark5stream, corrstartday);

  if (configindex != lastconfig) {
    mark5_stream_print(mark5stream);
    lastconfig = configindex;
  }

  offset = mark5stream->frameoffset;

  // If offset is non-zero we need to shuffle the data in memory to align on a frame
  // boundary. This is the equivalent of a seek in the file case.
  if (offset>0) { 
    ptr = (char*)&databuffer[buffersegment*(bufferbytes/numdatasegments)];

    if (offset>bufferinfo[buffersegment].validbytes) {
      cerr << "Datastream " << mpiid << ": ERROR Mark5 offset > segment size!!!" << endl;
      keepreading=false;
    } else {
      int nread, status;
      cout << "************Datastream " << mpiid << ": Seeking " << offset << " bytes into stream. " << endl;
      memmove(ptr, ptr+offset, bufferinfo[buffersegment].validbytes-offset);
      status = readnetwork(socketnumber, ptr+bufferinfo[buffersegment].validbytes-offset, offset, &nread);

      // We *should not* update framebytes remaining (or validbyes). 

      if (status==-1) { // Error reading socket
	cerr << "Datastream " << mpiid << ": Error reading socket" << endl;
	keepreading=false;
      } else if (status==0) {  // Socket closed remotely
	keepreading=false;
      } else if (nread!=offset) {
	cerr << "Datastream " << mpiid << ": Did not read enough bytes" << endl;
	keepreading=false;
      }
    }
  }

  readseconds = 86400*(mark5stream->mjd-corrstartday) + mark5stream->sec-corrstartseconds + intclockseconds;
  readnanoseconds = mark5stream->ns;
  //cout << "The frame start day is " << mark5stream->mjd << ", the frame start seconds is " << mark5stream->sec << ", the frame start ns is " << mark5stream->ns << ", readseconds is " << readseconds << ", readnanoseconds is " << readnanoseconds << endl;

  delete_mark5_stream(mark5stream);

}

double tim(void) {
  struct timeval tv;
  double t;

  gettimeofday(&tv, NULL);
  t = (double)tv.tv_sec + (double)tv.tv_usec/1000000.0;

  return t;
}

int Mk5DataStream::openframe()
{
  // The number of segments per "frame" is arbitrary. Just set it to ~ 1sec
  int nsegment;
  nsegment = (int)(1.0e9/bufferinfo[0].nsinc+0.1);

  if (!tcp) {
    // Print statistics. 
    double delta, t2;
    float dropped, oo, dup, rate;

    t2 = tim();
    delta = t2-t1;
    t1 = t2;
    
    dropped = (float)packet_drop*100.0/(float)npacket;
    oo = (float)packet_oo*100.0/(float)npacket;
    dup = (float)packet_duplicate*100.0/(float)npacket;
    rate = packet_sum/delta*8/1e6;
    
    cout << fixed << setprecision(2);
    cout << "Datastream " << mpiid << ": Packets=" << npacket << "  Dropped=" << dropped
	 << "  Duplicated=" << dup << "  Out-of-order=" << oo << "  Rate=" << rate << " Mbps" << endl;
    cout << "test " << 1 << 1.1 << endl;

    packet_drop = 0;
    packet_oo = 0;
    packet_duplicate = 0;
    npacket = 0;
    packet_sum = 0;
  }


  return readbytes*nsegment;  
}
