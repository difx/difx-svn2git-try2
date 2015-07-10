
#ifndef __VEX_ANTENNA_H__
#define __VEX_ANTENNA_H__

#include <iostream>
#include <string>
#include <vector>
#include <difxio.h>
#include "interval.h"
#include "vex_clock.h"
#include "vex_datastream.h"

class VexAntenna
{
public:
	VexAntenna() : x(0.0), y(0.0), z(0.0), dx(0.0), dy(0.0), dz(0.0), posEpoch(0.0), axisOffset(0.0), tcalFrequency(0) {}

	double getVexClocks(double mjd, double * coeffs) const;
	int nDatastream() const { return datastreams.size(); }
	bool hasData(const Interval &timerange) const;
	int nDatastreamWithData(const Interval &timerange) const;
	enum DataSource dataSource() const;

	std::string name;
	std::string defName;	// Sometimes names get changed

	double x, y, z;		// (m) antenna position in ITRF
	double dx, dy, dz;	// (m/sec) antenna velocity
	double posEpoch;	// mjd
	std::string axisType;
	double axisOffset;	// (m)
	std::vector<VexClock> clocks;
	int tcalFrequency;	// Hz
	std::vector<VexDatastream> datastreams;
};

std::ostream& operator << (std::ostream &os, const VexAntenna &x);

#endif
