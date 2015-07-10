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
	VexDatastream() : dataSource(DataSourceModule) {}
	bool hasData(const Interval &timerange) const;

	std::string name;
	enum DataSource	dataSource;
	std::vector<VexBasebandData> vsns;
	std::vector<VexBasebandData> files;
};

std::ostream& operator << (std::ostream &os, const VexDatastream &x);

#endif
