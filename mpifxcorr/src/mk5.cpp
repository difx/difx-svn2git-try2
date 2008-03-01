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


static void genFormatName(Configuration::dataformat format, int nchan, double bw, int nbits, int framebytes, char *formatname)
{
  int fanout, mbps;

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
}

/// Mk5DataMode ---------------------------------------------------------


Mk5Mode::Mk5Mode(Configuration * conf, int confindex, int dsindex, int nchan, int bpersend, int gblocks, int nfreqs, double bw, double * freqclkoffsets, int ninputbands, int noutputbands, int nbits, bool fbank, bool postffringe, bool quaddelayinterp, bool cacorrs, int framebytes, int framesamples, Configuration::dataformat format)
 : Mode(conf, confindex, dsindex, nchan, bpersend, gblocks, nfreqs, bw, freqclkoffsets, ninputbands, noutputbands, nbits, framesamples+nchan*2, fbank, postffringe, quaddelayinterp, cacorrs, bw*2)
{
  char formatname[64];

  genFormatName(format, ninputbands, bw, nbits, framebytes, formatname);

  //create the mark5_stream used for unpacking
  mark5stream = new_mark5_stream(
      new_mark5_stream_unpacker(0),
      new_mark5_format_generic_from_string(formatname) );

  if(framesamples != mark5stream->framesamples)
  {
    cerr << "Mk5Mode::Mk5Mode : framesamples inconsistent" << endl;
    exit(1);
  }

}

Mk5Mode::~Mk5Mode()
{
  delete_mark5_stream(mark5stream);
}

void Mk5Mode::unpack(int sampleoffset)
{
  int status, framesin;

  //work out where to start from
  framesin = (sampleoffset/framesamples);
  unpackstartsamples = framesin*framesamples;

  //unpack one frame plus one FFT size worth of samples
  status = mark5_unpack(mark5stream, data + framesin*framebytes, unpackedarrays, unpacksamples);
  if(status < 0)
    cerr << "Error trying to unpack Mark5 format data at sampleoffset " << sampleoffset << " from buffer seconds " << bufferseconds << " plus " << buffermicroseconds << " microseconds!!!" << endl;
}



/// Mk5DataStream -------------------------------------------------------


Mk5DataStream::Mk5DataStream(Configuration * conf, int snum, int id, int ncores, int * cids, int bufferfactor, int numsegments)
 : DataStream(conf, snum, id, ncores, cids, bufferfactor, numsegments)
{
  //each data buffer segment contains an integer number of frames, because thats the way config determines max bytes
}

Mk5DataStream::~Mk5DataStream()
{}

int Mk5DataStream::calculateControlParams(int offsetsec, int offsetsamples)
{
  int bufferindex, framesin, vlbaoffset, framens;
  
  bufferindex = DataStream::calculateControlParams(offsetsec, offsetsamples);
  
  //do the necessary correction to start from a frame boundary - work out the offset from the start of this segment
  vlbaoffset = bufferindex - atsegment*readbytes;

  framesin = vlbaoffset/payloadbytes;

  bufferinfo[atsegment].controlbuffer[bufferinfo[atsegment].numsent][0] = bufferinfo[atsegment].seconds + (double(bufferinfo[atsegment].nanoseconds) + double(((framesin*payloadbytes)*bufferinfo[atsegment].bytespersampledenom)/bufferinfo[atsegment].bytespersamplenum)* bufferinfo[atsegment].sampletimens)/1000000000.0;

  //go back to nearest frame
  return atsegment*readbytes + framesin*framebytes;
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
  bufferinfo[segmentindex].sendbytes = int(((((double)bufferinfo[segmentindex].sendbytes)* ((double)config->getBlocksPerSend(bufferinfo[segmentindex].configindex)))/(config->getBlocksPerSend(bufferinfo[segmentindex].configindex) + config->getGuardBlocks(bufferinfo[segmentindex].configindex)) + 0.99));

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
    cerr << "Error - number of input bands for datastream " << streamnum << " (" << config->getDNumInputBands(configindex, streamnum) << ") does not match with MkV file " << datafilenames[configindex][fileindex] << " (" << mark5stream->nchan << "), will be ignored!!!" << endl;
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

  //update all the configs - to ensure that the nsincs and payloadbytes are correct
  for(int i=0;i<numdatasegments;i++)
    updateConfig(i);
}
