#include "vex_datastream.h"

bool VexDatastream::hasData(const Interval &timerange) const
{
	bool rv = false;

	switch(dataSource)
	{
	case DataSourceNone:
		rv = false;
		break;
	case DataSourceNetwork:
		rv = true;
		break;
	case DataSourceFake:
		rv = true;
		break;
	case DataSourceFile:
		for(std::vector<VexBasebandData>::const_iterator it = files.begin(); it != files.end(); ++it)
		{
			if(it->overlap(timerange) > 0.0)
			{
				rv = true;
				break;
			}
		}
		break;
	case DataSourceModule:
		for(std::vector<VexBasebandData>::const_iterator it = vsns.begin(); it != vsns.end(); ++it)
		{
			if(it->overlap(timerange) > 0.0)
			{
				rv = true;
				break;
			}
		}
		break;
	case NumDataSources:
		// Should never come up.  print error?
		break;
	}

	return rv;
}

std::ostream& operator << (std::ostream &os, const VexDatastream &x)
{
	os << "Datastream " << x.name << std::endl;
	os << "    dataSource=" << dataSourceNames[x.dataSource] << std::endl;
	if(x.dataSource == DataSourceFile)
	{
		for(std::vector<VexBasebandData>::const_iterator it = x.files.begin(); it != x.files.end(); ++it)
		{
			os << "      Files = " << *it << std::endl;
		}
	}
	else if(x.dataSource == DataSourceModule)
	{
		for(std::vector<VexBasebandData>::const_iterator it = x.vsns.begin(); it != x.vsns.end(); ++it)
		{
			os << "      VSN = " << *it << std::endl;
		}
	}

	return os;
}
