#include "event.h"

// Note: the ordering here is crucial!
const char Event::eventName[][20] =
{
	"None",
	"Ant Stop",
	"Ant Off-source",
	"Scan Stop",
	"Job Stop",
	"Observe Stop",
	"Record Stop",
	"Clock Break",
	"Leap Second",
	"Manual Break",
	"Record Start",
	"Observe Start",
	"Job Start",
	"Scan Start",
	"Ant On-source",
	"Ant Start"
};

bool operator<(const Event &a, const Event &b)
{
	if(a.mjd < b.mjd - 0.000001)
	{
		return true;
	}
	else if(a.mjd > b.mjd + 0.000001)
	{
		return false;
	}
	if(a.eventType < b.eventType)
	{
		return true;
	}
	else if(a.eventType > b.eventType)
	{
		return false;
	}

	return a.name < b.name;
}

void addEvent(std::vector<Event> &events, double mjd, Event::EventType eventType, const std::string &name)
{
	events.push_back(Event(mjd, eventType, name));
}

void addEvent(std::vector<Event> &events, double mjd, Event::EventType eventType, const std::string &name, const std::string &scan)
{
	events.push_back(Event(mjd, eventType, name, scan));
}

std::ostream& operator << (std::ostream &os, const Event &x)
{
	int d, s;

	d = static_cast<int>(x.mjd);
	s = static_cast<int>((x.mjd - d)*86400.0 + 0.5);

	os << "mjd=" << d << " sec=" << s << " : " << Event::eventName[x.eventType] << " " << x.name;

	return os;
}
