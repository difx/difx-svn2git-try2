#include "vex_antenna.h"

// get the clock epoch as a MJD value (with fractional component), negative 
// means not found.  Also fills in the first two coeffs, returned in seconds
double VexAntenna::getVexClocks(double mjd, double *coeffs) const
{
	double epoch = -1.0;

	for(std::vector<VexClock>::const_iterator it = clocks.begin(); it != clocks.end(); ++it)
	{
		if(it->mjdStart <= mjd)
		{
			epoch = it->offset_epoch;
			coeffs[0] = it->offset;
			coeffs[1] = it->rate;
		}
	}

	return epoch;
}

bool VexAntenna::hasData(const Interval &timerange) const
{
	for(std::vector<VexDatastream>::const_iterator it = datastreams.begin(); it != datastreams.end(); ++it)
	{
		if(it->hasData(timerange))
		{
			return true;
		}
	}

	return false;
}

int VexAntenna::nDatastreamWithData(const Interval &timerange) const
{
	int n = 0;

	for(std::vector<VexDatastream>::const_iterator it = datastreams.begin(); it != datastreams.end(); ++it)
	{
		if(it->hasData(timerange))
		{
			++n;
		}
	}

	return n;
}

enum DataSource VexAntenna::dataSource() const
{
	enum DataSource ds;

	if(datastreams.size() == 0)
	{
		return DataSourceNone;
	}

	ds = datastreams[0].dataSource;
	
	for(std::vector<VexDatastream>::const_iterator it = datastreams.begin(); it != datastreams.end(); ++it)
	{
		if(ds == DataSourceNone)
		{
			ds = it->dataSource;
		}
		else if(ds != it->dataSource && it->dataSource != DataSourceNone)
		{
			return NumDataSources;	// an error
		}
	}

	return ds;
}

std::ostream& operator << (std::ostream &os, const VexAntenna &x)
{
	os << "Antenna " << x.name <<
		"\n  x=" << x.x << "  dx/dt=" << x.dx <<
		"\n  y=" << x.y << "  dy/dt=" << x.dy <<
		"\n  z=" << x.z << "  dz/dt=" << x.dz <<
		"\n  posEpoch=" << x.posEpoch <<
		"\n  axisType=" << x.axisType <<
		"\n  axisOffset=" << x.axisOffset <<
		"\n  tcalFrequency=" << x.tcalFrequency << std::endl;

	for(std::vector<VexDatastream>::const_iterator it = x.datastreams.begin(); it != x.datastreams.end(); ++it)
	{
		os << "  " << *it << std::endl;
	}

	for(std::vector<VexClock>::const_iterator it = x.clocks.begin(); it != x.clocks.end(); ++it)
	{
		os << "  " << *it << std::endl;
	}

	return os;
}
