#include "vex_basebanddata.h"

std::ostream& operator << (std::ostream &os, const VexBasebandData &x)
{
	os << "Baseband(" << x.filename << ", " << (const Interval&)x << ")";

	return os;
}

