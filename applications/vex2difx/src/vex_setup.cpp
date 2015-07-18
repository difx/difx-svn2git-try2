#include "vex_setup.h"

int VexSetup::phaseCalIntervalMHz() const
{
	int p;
	int pc = 0;

	for(std::map<std::string,VexIF>::const_iterator it = ifs.begin(); it != ifs.end(); ++it)
	{
		p = it->second.phaseCalIntervalMHz;
		if(p > 0 && (p < pc || pc == 0))
		{
			pc = p;
		}
	}

	return pc;
}

const VexIF *VexSetup::getIF(const std::string &ifName) const
{
	for(std::map<std::string,VexIF>::const_iterator it = ifs.begin(); it != ifs.end(); ++it)
	{
		if(it->second.name == ifName)
		{
			return &it->second;
		}
	}

	return 0;
}

double VexSetup::firstTuningForIF(const std::string &ifName) const	// return Hz
{
	double tune = 0.0;	// [Hz]
	std::string chanName;

	for(std::vector<VexChannel>::const_iterator ch=channels.begin(); ch != channels.end(); ++ch)
	{
		if(ch->ifName == ifName && (chanName == "" || ch->name < chanName))
		{
			chanName = ch->name;
			tune = ch->bbcFreq;
		}
	}

	return tune;
}

double VexSetup::dataRateMbps() const
{
	double rate = 0;	// [Mbps]

	for(std::vector<VexStream>::const_iterator it = streams.begin(); it != streams.end(); ++it)
	{
		rate += it->dataRateMbps();
	}

	return rate;
}

void VexSetup::setPhaseCalInterval(int phaseCalIntervalMHz)
{
	// change IF phase cal values
	for(std::map<std::string,VexIF>::iterator it = ifs.begin(); it != ifs.end(); ++it)
	{
		it->second.phaseCalIntervalMHz = phaseCalIntervalMHz;
	}

	// weed out unwanted tones
	for(std::vector<VexChannel>::iterator it = channels.begin(); it != channels.end(); ++it)
	{
		if(phaseCalIntervalMHz <= 0)
		{
			it->tones.clear();
		}
		else
		{
			for(std::vector<int>::iterator tit = it->tones.begin(); tit != it->tones.end(); )
			{
				if(*tit % phaseCalIntervalMHz != 0)
				{
					tit = it->tones.erase(tit);
				}
				else
				{
					++tit;
				}
			}
		}
	}
}

void VexSetup::selectTones(enum ToneSelection selection, double guardBandMHz)
{
	for(std::vector<VexChannel>::iterator it = channels.begin(); it != channels.end(); ++it)
	{
		const VexIF *vif = getIF(it->ifName);
		it->selectTones(vif->phaseCalIntervalMHz, selection, guardBandMHz);
	}
}

int VexSetup::nRecordChan() const
{
	int rc = 0;

	for(std::vector<VexStream>::const_iterator it = streams.begin(); it != streams.end(); ++it)
	{
		rc += it->nRecordChan;
	}

	return rc;
}

std::ostream& operator << (std::ostream &os, const VexSetup &x)
{
	os << "   Setup:" << std::endl;
	for(std::vector<VexChannel>::const_iterator it = x.channels.begin(); it != x.channels.end(); ++it)
	{
		os << "    Channel: " << *it << std::endl;
	}
	for(std::map<std::string,VexIF>::const_iterator it = x.ifs.begin(); it != x.ifs.end(); ++it)
	{
		os << "    IF: " << it->first << " " << it->second << std::endl;
	}
	for(std::vector<VexStream>::const_iterator it = x.streams.begin(); it != x.streams.end(); ++it)
	{
		os << "    Datastream: " << *it << std::endl;
	}

	return os;
}
