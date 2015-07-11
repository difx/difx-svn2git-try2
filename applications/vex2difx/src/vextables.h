/***************************************************************************
 *   Copyright (C) 2009-2015 by Walter Brisken & Adam Deller               *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 3 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 ***************************************************************************/
/*===========================================================================
 * SVN properties (DO NOT CHANGE)
 *
 * $Id$
 * $HeadURL$
 * $LastChangedRevision$
 * $Author$
 * $LastChangedDate$
 *
 *==========================================================================*/

#ifndef __VEXTABLES_H__
#define __VEXTABLES_H__

#include <vector>
#include <list>
#include <string>
#include <iostream>
#include <map>
#include <difxio.h>
#include "interval.h"
#include "event.h"
#include "vex_exper.h"
#include "vex_basebanddata.h"
#include "vex_clock.h"
#include "vex_datastream.h"
#include "vex_antenna.h"
#include "vex_scan.h"
#include "vex_source.h"
#include "vex_subband.h"
#include "vex_channel.h"
#include "vex_if.h"
#include "vex_setup.h"
#include "vex_mode.h"
#include "vex_eop.h"


class VexData
{
private:
	VexExper exper;
	std::vector<VexSource> sources;
	std::vector<VexScan> scans;
	std::vector<VexMode> modes;
	std::vector<VexAntenna> antennas;
	std::vector<VexEOP> eops;

	std::list<Event> events;	// eventually move this out of VexData
	std::string directory;

public:
	char vexStartTime[50];		// FIXME: figure out why these are needed and are not in VexExper
	char vexStopTime[50];

	int sanityCheck();

	VexSource *newSource();
	VexScan *newScan();
	VexMode *newMode();
	VexAntenna *newAntenna();
	VexEOP *newEOP();
	void findLeapSeconds();
	void addBreaks(const std::vector<double> &breaks);
	void swapPolarization(const std::string &antName);
	void setPhaseCalInterval(const std::string &antName, int phaseCalIntervalMHz);
	void selectTones(const std::string &antName, enum ToneSelection selection, double guardBandMHz);
	void setClock(const std::string &antName, const VexClock &clock);
	void setTcalFrequency(const std::string &antName, int tcalFrequency);
	void setAntennaPosition(const std::string &antName, double X, double Y, double Z);
	void setAntennaAxisOffset(const std::string &antName, double axisOffset);
	void addClockEvents(std::vector<Event> &events) const;
	void addScanEvents(std::vector<Event> &events) const;
	void addVSNEvents(std::vector<Event> &events) const;
	void generateEvents(std::vector<Event> &events) const;

	double obsStart() const { return events.front().mjd; }
	double obsStop() const { return events.back().mjd; }


	const std::string &getDirectory() const { return directory; }
	void setDirectory(const std::string &dir) { directory = dir; }

	unsigned int nSource() const { return sources.size(); }
	int getSourceIdByDefName(const std::string &defName) const;
	const VexSource *getSource(unsigned int num) const;
	const VexSource *getSourceByDefName(const std::string &defName) const;
	const VexSource *getSourceBySourceName(const std::string &name) const;

	unsigned int nScan() const { return scans.size(); }
	const VexScan *getScan(unsigned int num) const;
	const VexScan *getScanByDefName(const std::string &defName) const;
	const VexScan *getScanByAntennaTime(const std::string &antName, double mjd) const;
	void reduceScans(int minSubarraySize, const Interval &timerange);
	void setScanSize(unsigned int num, double size);
	void getScanList(std::list<std::string> &scans) const;
	unsigned int nAntennasWithRecordedData(const VexScan &scan) const;

	unsigned int nAntenna() const { return antennas.size(); }
	int getAntennaIdByName(const std::string &antName) const;
	int getAntennaIdByDefName(const std::string &antName) const;
	const VexAntenna *getAntenna(unsigned int num) const;
	const VexAntenna *getAntenna(const std::string &name) const;
	double getAntennaStartMJD(const std::string &name) const;
	double getAntennaStopMJD(const std::string &name) const;
	int getNumAntennaRecChans(const std::string &name) const;
	bool removeAntenna(const std::string &name);

	unsigned int nMode() const { return modes.size(); }
	int getModeIdByDefName(const std::string &defName) const;
	const VexMode *getMode(unsigned int num) const;
	const VexMode *getModeByDefName(const std::string &defName) const;
	unsigned int nRecordChan(const VexMode &mode, const std::string &antName) const;

	unsigned int nEOP() const { return eops.size(); }
	void addEOP(const VexEOP &e);
	const VexEOP *getEOP(unsigned int num) const;
	const std::vector<VexEOP> &getEOPs() const { return eops; }

	bool usesAntenna(const std::string &antennaName) const;
	bool usesMode(const std::string &modeDefName) const;

//	unsigned int nVSN(const std::string &antName) const;
	void addVSN(const std::string &antName, unsigned int datastreamId, const std::string &vsn, const Interval &timeRange);
//	std::string getVSN(const std::string &antName, const Interval &timeRange) const;

	unsigned int nEvent() const { return events.size(); }
	const std::list<Event> *getEvents() const;
	void addEvent(double mjd, Event::EventType eventType, const std::string &name);
	void addEvent(double mjd, Event::EventType eventType, const std::string &name, const std::string &scanName);
	void sortEvents();

	const VexExper *getExper() const { return &exper; }
	void setExper(const std::string &name, const Interval &experTimeRange);
};

class VexJob : public Interval
{
public:
	VexJob() : Interval(0.0, 1000000.0), jobSeries("Bogus"), jobId(-1), dutyCycle(1.0), dataSize(0.0) {}

//	void assignVSNs(const VexData &V);
//	std::string getVSN(const std::string &antName) const;
	void assignAntennas(const VexData &V);
	bool hasScan(const std::string &scanName) const;
	int generateFlagFile(const VexData &V, const char *fileName, unsigned int invalidMask=0xFFFFFFFF) const;

	// return the approximate number of Operations required to compute this scan
	double calcOps(const VexData *V, int fftSize, bool doPolar) const;
	double calcSize(const VexData *V) const;

	std::string jobSeries;
	int jobId;
	std::vector<std::string> scans;
//std::map<std::string,std::string> vsns;	// vsn, indexed by antenna name
	std::vector<std::string> jobAntennas;	// vector of antennas used in this job
	double dutyCycle;		// fraction of job spent in scans
	double dataSize;		// [bytes] estimate of data output size
};

class VexJobFlag : public Interval
{
public:
	static const unsigned int JOB_FLAG_RECORD = 1 << 0;
	static const unsigned int JOB_FLAG_POINT  = 1 << 1;
	static const unsigned int JOB_FLAG_TIME   = 1 << 2;
	static const unsigned int JOB_FLAG_SCAN   = 1 << 3;
	VexJobFlag() : antId(-1) {}
	VexJobFlag(double start, double stop, int ant) : Interval(start, stop), antId(ant) {}

	int antId;
};

class VexJobGroup : public Interval
{
public:
	std::vector<std::string> scans;
	std::list<Event> events;

	bool hasScan(const std::string &scanName) const;
	void genEvents(const std::list<Event> &eventList);
	void createJobs(std::vector<VexJob> &jobs, Interval &jobTimeRange, const VexData *V, double maxLength, double maxSize) const;
};

std::ostream& operator << (std::ostream &os, const VexJob &x);
std::ostream& operator << (std::ostream &os, const VexJobGroup &x);
std::ostream& operator << (std::ostream &os, const VexJobFlag &x);
std::ostream& operator << (std::ostream &os, const VexData &x);

#endif
