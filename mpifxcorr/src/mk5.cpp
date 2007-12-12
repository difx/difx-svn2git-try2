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

Mk5Mode::Mk5Mode(Configuration * conf, int confindex, int dsindex, int fanout, int nchan, int bpersend, int gblocks, int nfreqs, double bw, double * freqclkoffsets, int ninputbands, int noutputbands, int nbits, bool fbank, bool pbin, bool pscrunch, bool postffringe, bool quaddelayinterp, bool cacorrs, int fbytes)
 : Mode(conf, confindex, dsindex, nchan, bpersend, gblocks, nfreqs, bw, freqclkoffsets, ninputbands, noutputbands, nbits, PAYLOADSIZE*fanout+nchan*2, fbank, pbin, pscrunch, postffringe, quaddelayinterp, cacorrs, bw*2)
{
  int format;
  
  format = conf->getMkVFormat(confindex, dsindex);
  
  //create the mark5_stream used for unpacking
  switch(format)
  {
  case MK5_FORMAT_VLBA:
    vf = new_mark5_stream(
      new_mark5_stream_unpacker(0),
      new_mark5_format_vlba(0, numinputbands, numbits, fanout) );
    break;
  case MK5_FORMAT_MARK4:
    vf = new_mark5_stream(
      new_mark5_stream_unpacker(0),
      new_mark5_format_mark4(0, numinputbands, numbits, fanout) );
    break;
  case MK5_FORMAT_MARK5B:
    vf = new_mark5_stream(
      new_mark5_stream_unpacker(0),
      new_mark5_format_mark5b(0, numinputbands, numbits) );
    break;
  }

  framesamples = vf->framesamples;
  framebytes   = vf->framebytes;
}


Mk5Mode::~Mk5Mode()
{
  delete_mark5_stream(vf);
}

void Mk5Mode::unpack(int sampleoffset)
{
  int status, framesin;

  //work out where to start from
  framesin = (sampleoffset/framesamples);
  unpackstartsamples = framesin*framesamples;

  //unpack one frame plus one FFT size worth of samples
  status = mark5_unpack(vf, data + framesin*framebytes, unpackedarrays, unpacksamples);
  if(status < 0)
    cerr << "Error trying to unpack MkV format data at sampleoffset " << sampleoffset << " from buffer seconds " << bufferseconds << " plus " << buffermicroseconds << " microseconds!!!" << endl;
}


Mk5DataStream::Mk5DataStream(Configuration * conf, int snum, int id, int ncores, int * cids, int bufferfactor, int numsegments)
 : DataStream(conf, snum, id, ncores, cids, bufferfactor, numsegments)
{
  //each data buffer segment contains an integer number of frames, because thats the way config determines max bytes
}

Mk5DataStream::~Mk5DataStream()
{}

int Mk5DataStream::calculateControlParams(int offsetsec, int offsetsamples)
{
  int bufferindex, framesin, vlbaoffset, payloadbytesperframe;
  
  bufferindex = DataStream::calculateControlParams(offsetsec, offsetsamples);
  
  //do the necessary correction to start from a frame boundary - work out the offset from the start of this segment
  vlbaoffset = bufferindex - atsegment*readbytes;
  payloadbytesperframe = (PAYLOADSIZE*fanout*bufferinfo[atsegment].bytespersamplenum)/bufferinfo[atsegment].bytespersampledenom;
  framesin = vlbaoffset/payloadbytesperframe;

  bufferinfo[atsegment].controlbuffer[bufferinfo[atsegment].numsent][0] = bufferinfo[atsegment].seconds + (double(bufferinfo[atsegment].nanoseconds) + double(((framesin*(framebytes-headerbytes))*bufferinfo[atsegment].bytespersampledenom)/bufferinfo[atsegment].bytespersamplenum)* bufferinfo[atsegment].sampletimens)/1000000000.0;

  //go back to nearest frame
  return atsegment*readbytes + framesin*framebytes;
}

void Mk5DataStream::updateConfig(int segmentindex)
{
  //run the default update config, then add additional information specific to Mk5
  DataStream::updateConfig(segmentindex);
  if(bufferinfo[segmentindex].configindex < 0) //If the config < 0 we can skip this scan
    return;

  framebytes = config->getFrameBytes(bufferinfo[segmentindex].configindex, streamnum);
  headerbytes = config->getHeaderBytes(bufferinfo[segmentindex].configindex, streamnum);
  fanout = config->getFanout(bufferinfo[segmentindex].configindex, streamnum);
  numbits = config->getDNumBits(bufferinfo[segmentindex].configindex, streamnum);

  //correct the nsinc - should be number of frames*frame time
  bufferinfo[segmentindex].nsinc = int(((bufferbytes/numdatasegments)/framebytes)*bufferinfo[segmentindex].sampletimens*PAYLOADSIZE*fanout);

  //take care of the case where an integral number of frames is not an integral number of blockspersend - ensure sendbytes is long enough
  bufferinfo[segmentindex].sendbytes = int(((((double)bufferinfo[segmentindex].sendbytes)* ((double)config->getBlocksPerSend(bufferinfo[segmentindex].configindex)))/(config->getBlocksPerSend(bufferinfo[segmentindex].configindex) + config->getGuardBlocks(bufferinfo[segmentindex].configindex)) + 0.99));

}

void Mk5DataStream::initialiseFile(int configindex, int fileindex)
{
  int offset;

  vs = mark5_stream_open(datafilenames[configindex][fileindex].c_str(), numbits, fanout, 0);
  if(vs->nchan != config->getDNumInputBands(configindex, streamnum))
  {
    cerr << "Error - number of input bands for datastream " << streamnum << " (" << config->getDNumInputBands(configindex, streamnum) << ") does not match with MkV file " << datafilenames[configindex][fileindex] << " (" << vs->nchan << "), will be ignored!!!" << endl;
  }

  // resolve any day ambiguities
  mark5_stream_fix_mjd(vs, corrstartday);

  mark5_stream_print(vs);

  offset = vs->frameoffset;

  readseconds = 86400*(vs->mjd-corrstartday) + vs->sec-corrstartseconds + intclockseconds;
  readnanoseconds = vs->ns;
  cout << "The frame start day is " << vs->mjd << ", the frame start seconds is " << vs->sec << ", the frame start ns is " << vs->ns << ", readseconds is " << readseconds << ", readnanoseconds is " << readnanoseconds << endl;

  //close mark5stream
  delete_mark5_stream(vs);

  cout << "About to seek to byte " << offset << " to get to the first frame" << endl;

  input.seekg(offset);

  //update all the configs - to ensure that the nsincs and headerbytes are correct
  for(int i=0;i<numdatasegments;i++)
    updateConfig(i);
}
