#ifndef __VEX_SETUP_H__
#define __VEX_SETUP_H__

#include <iostream>
#include <string>
#include <vector>
#include <map>
#include <difxio.h>
#include "vex_if.h"
#include "vex_channel.h"

class VexSetup	// Container for all antenna-specific settings
{
public:
	VexSetup() : sampRate(0.0), nBit(0), nRecordChan(0), dataSampling(SamplingReal) {}
	int phaseCalIntervalMHz() const;
	const VexIF *getIF(const std::string &ifName) const;
	double firstTuningForIF(const std::string &ifName) const;	// returns Hz
	double dataRateMbps() const { return sampRate*nBit*nRecordChan/1000000.0; }
	void setPhaseCalInterval(int phaseCalIntervalMHz);
	void selectTones(enum ToneSelection selection, double guardBandMHz);

	std::map<std::string,VexIF> ifs;		// Indexed by name in the vex file, such as IF_A
	std::vector<VexChannel> channels;

	double sampRate;		// [Hz]
	unsigned int nBit;
	unsigned int nRecordChan;	// number of recorded channels
	std::string formatName;		// e.g., VLBA, MKIV, Mk5B, VDIF, LBA, K5, ...
	enum SamplingType dataSampling;	// Real or Complex
};

std::ostream& operator << (std::ostream &os, const VexSetup &x);

#endif
