#ifndef __VEX_BASEBANDDATA_H__
#define __VEX_BASEBANDDATA_H__

#include <iostream>
#include <string>
#include "interval.h"

class VexBasebandData : public Interval
{
	public:
	std::string filename;	// or VSN, ...

	VexBasebandData(const std::string &name, const Interval &timeRange) : Interval(timeRange), filename(name) {} 
	VexBasebandData(const std::string &name, double start=-1.0e9, double stop=1.0e9) : Interval(start, stop), filename(name) {}
};

std::ostream& operator << (std::ostream &os, const VexBasebandData &x);

#endif
