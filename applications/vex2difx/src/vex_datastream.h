#ifndef __VEX_DATASTREAM_H__
#define __VEX_DATASTREAM_H__

#include <iostream>
#include <string>
#include <vector>
#include <difxio.h>
#include "interval.h"
#include "vex_basebanddata.h"

class VexDatastream
{
public:
	VexDatastream() : dataSource(DataSourceModule), windowSize(0) {}
	bool hasData(const Interval &timerange) const;
	bool hasFormat() const { return !format.empty(); }

	std::string name;
	enum DataSource	dataSource;
	std::vector<VexBasebandData> vsns;
	std::vector<VexBasebandData> files;

	std::string format;		// If set, overrides format specifier in VexSetup, for _all_ setups!
					// Should be the full format descrition for the data being read by this datastream
	enum SamplingType dataSampling;	// If set, overrides sampling type in VexSetup (real by default) for _all_ setups!

	std::string networkPort;	// For eVLBI : port for this antenna.  A non-number indicates raw mode attached to an  ethernet interface
	int windowSize;			// For eVLBI : TCP window size

	std::vector<int> chanMap;	// Map from this datastream's record channel to the channel in the antenna setup.
					// Note 1: for single datastream use keep this empty, which implies the trivial map.
};

std::ostream& operator << (std::ostream &os, const VexDatastream &x);

#endif
