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

#include <fstream>
#include <sstream>
#include <iostream>
#include <algorithm>
#include <cmath>
#include <regex.h>
#include "vextables.h"
#include "util.h"


int VexData::sanityCheck()
{
	int nWarn = 0;

	if(eops.size() < 5)
	{
		std::cerr << "Warning: Fewer than 5 EOPs specified" << std::endl;
		++nWarn;
	}

	for(std::vector<VexAntenna>::const_iterator it = antennas.begin(); it != antennas.end(); ++it)
	{
		if(it->clocks.empty())
		{
			std::cerr << "Warning: no clock values for antenna " << it->name << " ." << std::endl;
			++nWarn;
		}
	}

	for(std::vector<VexAntenna>::const_iterator it = antennas.begin(); it != antennas.end(); ++it)
	{
#if 0
FIXME: this functionality needs to be put somewhere else
		if(it->dataSource == DataSourceFile && it->basebandFiles.empty())
		{
			std::cerr << "Warning: file based correlation desired but no files provided for antenna " << it->name << " ." << std::endl;
			++nWarn;
		}
		if(it->dataSource == DataSourceModule && it->basebandFiles.empty())
		{
			std::cerr << "Warning: module based correlation desired but no media specified for antenna " << it->name << " ." << std::endl;
			++nWarn;
		}
		if(it->dataSource == DataSourceNone)
		{
			std::cerr << "Warning: data source is NONE for antenna " << it->name << " ." << std::endl;
			++nWarn;
		}
#endif
	}

	return nWarn;
}

VexSource *VexData::newSource()
{
	sources.push_back(VexSource());

	return &sources.back();
}

const VexSource *VexData::getSource(unsigned int num) const
{
	if(num >= nSource())
	{
		return 0;
	}

	return &sources[num];
}

int VexData::getSourceIdByDefName(const std::string &defName) const
{
	for(std::vector<VexSource>::const_iterator it = sources.begin(); it != sources.end(); ++it)
	{
		if(it->defName == defName)
		{
			return it - sources.begin();
		}
	}

	return -1;
}

const VexSource *VexData::getSourceByDefName(const std::string &defName) const
{
	for(std::vector<VexSource>::const_iterator it = sources.begin(); it != sources.end(); ++it)
	{
		if(it->defName == defName)
		{
			return &(*it);
		}
	}

	return 0;
}

const VexSource *VexData::getSourceBySourceName(const std::string &name) const
{
	for(std::vector<VexSource>::const_iterator it = sources.begin(); it != sources.end(); ++it)
	{
		if(find(it->sourceNames.begin(), it->sourceNames.end(), name) != it->sourceNames.end())
		{
			return &(*it);
		}
	}

	return 0;
}


VexScan *VexData::newScan()
{
	scans.push_back(VexScan());

	return &scans.back();
}


void VexJob::assignAntennas(const VexData &V)
{
	jobAntennas.clear();

	for(std::vector<std::string>::const_iterator s = scans.begin(); s != scans.end(); ++s)
	{
		const VexScan* S = V.getScanByDefName(*s);
		for(std::map<std::string,Interval>::const_iterator a = S->stations.begin(); a != S->stations.end(); ++a)
		{
			if(find(jobAntennas.begin(), jobAntennas.end(), a->first) == jobAntennas.end())
			{
				const VexAntenna *A = V.getAntenna(a->first);

				if(A->hasData(*S))
				{
					jobAntennas.push_back(a->first);
				}
			}
		}
	}
	sort(jobAntennas.begin(), jobAntennas.end());
}

/* Modified from http://www-graphics.stanford.edu/~seander/bithacks.html */
static int intlog2(unsigned int v)
{
	const unsigned int b[] = {0x2, 0xC, 0xF0, 0xFF00, 0xFFFF0000};
	const unsigned int S[] = {1, 2, 4, 8, 16};
	unsigned int r = 0; // result of log2(v) will go here

	for(int i = 4; i >= 0; --i) 
	{
		if(v & b[i])
		{
			v >>= S[i];
			r |= S[i];
		} 
	}

	return r;
}

double VexJob::calcOps(const VexData *V, int fftSize, bool doPolar) const
{
	double ops = 0.0;
	int nAnt, nPol, nSubband;
	double sampRate, seconds;
	const VexMode *M;
	char pols[8];
	double opsPerSample;

	for(std::vector<std::string>::const_iterator si = scans.begin(); si != scans.end(); ++si)
	{
		const VexScan *S = V->getScanByDefName(*si);
		M = V->getModeByDefName(S->modeDefName);
		if(!M)
		{
			return 0.0;
		}
		
		sampRate = M->getAverageSampleRate();
		nPol = M->getPols(pols);
		if(nPol > 1 && doPolar)
		{
			nPol = 2;
		}
		else
		{
			nPol = 1;
		}

		seconds = S->duration_seconds();
		nAnt = S->stations.size();
		nSubband = M->subbands.size();

		// Estimate number of operations based on VLBA Sensitivity Upgrade Memo 16
		// Note: this assumes all polarizations are matched
		opsPerSample = 16.0 + 5.0*intlog2(fftSize) + 2.5*nAnt*nPol;
		ops += opsPerSample*seconds*sampRate*nSubband*nAnt;
	}

	return ops;
}

double VexJob::calcSize(const VexData *V) const
{
	double size = 0.0;

	for(std::vector<std::string>::const_iterator it = scans.begin(); it != scans.end(); ++it)
	{
		size += V->getScanByDefName(*it)->size;
	}

	return size;
}

bool VexJobGroup::hasScan(const std::string &scanName) const
{
	return find(scans.begin(), scans.end(), scanName) != scans.end();
}

void VexJobGroup::genEvents(const std::list<Event> &eventList)
{
	for(std::list<Event>::const_iterator it = eventList.begin(); it != eventList.end(); ++it)
	{
		if(it->eventType == Event::SCAN_START ||
		   it->eventType == Event::SCAN_STOP ||
		   it->eventType == Event::ANT_SCAN_START ||
		   it->eventType == Event::ANT_SCAN_STOP)
		{
			if(hasScan(it->scan))
			{
				events.push_back(*it);
			}
		}
		else
		{
			events.push_back(*it);
		}
	}

	// Now remove any module changes that don't occur within scans

	std::map<std::string,bool> inScan;
	std::map<std::string,bool> inScanNow;

	// initialize inScan

	for(std::list<Event>::const_iterator it = events.begin(); it != events.end(); ++it)
	{
		if(it->eventType == Event::RECORD_START)
		{
			inScan[it->name] = false;
			inScanNow[it->name] = false;
		}
	}

	std::list<Event>::iterator rstart, rstop;
	for(rstart = events.begin(); rstart != events.end();)
	{
		if(rstart->eventType == Event::ANT_SCAN_START)
		{
			inScan[rstart->name] = true;
			inScanNow[rstart->name] = true;
		}
		else if(rstart->eventType == Event::ANT_SCAN_STOP)
		{
			inScanNow[rstart->name] = false;
		}
		if(rstart->eventType == Event::RECORD_START && !inScanNow[rstart->name])
		{
			inScan[rstart->name] = inScanNow[rstart->name];
			for(rstop = rstart, ++rstop; rstop != events.end(); ++rstop)
			{
				if(rstart->name != rstop->name)
				{
					continue;
				}

				if(rstop->eventType == Event::ANT_SCAN_START)
				{
					inScan[rstart->name] = true;
				}

				if(rstop->eventType == Event::RECORD_STOP)
				{
					if(!inScan[rstop->name])
					{
						inScan[rstop->name] = inScanNow[rstop->name];
						events.erase(rstop);
						rstart = events.erase(rstart);
					}
					else
					{
						++rstart;
					}

					break;
				}
			}
		}
		else
		{
			++rstart;
		}
	}
}

bool VexJob::hasScan(const std::string &scanName) const
{
	// if find returns .end(), then it was not found
	return find(scans.begin(), scans.end(), scanName) != scans.end();
}

int VexJob::generateFlagFile(const VexData &V, const char *fileName, unsigned int invalidMask) const
{
	std::vector<VexJobFlag> flags;
	std::map<std::string,int> antIds;
	unsigned int nAnt = 0;
	std::ofstream of;
	const std::list<Event> &eventList = *V.getEvents();

	for(std::vector<std::string>::const_iterator a = jobAntennas.begin(); a != jobAntennas.end(); ++a)
	{
		antIds[*a] = nAnt;
		++nAnt;
	}

	// Assume all flags from the start.  
	std::vector<unsigned int> flagMask(nAnt, 
		VexJobFlag::JOB_FLAG_RECORD | 
		VexJobFlag::JOB_FLAG_POINT | 
		VexJobFlag::JOB_FLAG_TIME | 
		VexJobFlag::JOB_FLAG_SCAN);
	std::vector<double> flagStart(nAnt, mjdStart);

	// Except if not a Mark5 Module case, don't assume RECORD flag is on
	for(unsigned int antId = 0; antId < nAnt; ++antId)
	{
		const VexAntenna *ant = V.getAntenna(jobAntennas[antId]);

		if(!ant)
		{
			std::cerr << "Developer error: generateFlagFile: antenna " << jobAntennas[antId] << " not found in antenna table." << std::endl;

			exit(EXIT_FAILURE);
		}

		if(ant->dataSource() != DataSourceModule)
		{
			// Aha! not module based so unflag JOB_FLAG_RECORD
			flagMask[antId] &= ~VexJobFlag::JOB_FLAG_RECORD;
		}
	}

	// Then go through each event, adjusting current flag state.  
	for(std::list<Event>::const_iterator e = eventList.begin(); e != eventList.end(); ++e)
	{
		if(e->eventType == Event::RECORD_START)
		{
			if(antIds.count(e->name) > 0)
			{
				flagMask[antIds[e->name]] &= ~VexJobFlag::JOB_FLAG_RECORD;
			}
		}
		else if(e->eventType == Event::RECORD_STOP)
		{
			if(antIds.count(e->name) > 0)
			{
				flagMask[antIds[e->name]] |= VexJobFlag::JOB_FLAG_RECORD;
			}
		}
		else if(e->eventType == Event::SCAN_START)
		{
			if(hasScan(e->scan))
			{
				const VexScan *scan = V.getScanByDefName(e->scan);

				if(!scan)
				{
					std::cerr << "Developer error: generateFlagFile: SCAN_START, scan=0" << std::endl;

					exit(EXIT_FAILURE);
				}
				for(std::map<std::string,Interval>::const_iterator sa = scan->stations.begin(); sa != scan->stations.end(); ++sa)
				{
					if(antIds.count(sa->first) == 0)
					{
						continue;
					}
					flagMask[antIds[sa->first]] &= ~VexJobFlag::JOB_FLAG_SCAN;
				}
			}
		}
		else if(e->eventType == Event::SCAN_STOP)
		{
			if(hasScan(e->scan))
			{
				const VexScan *scan = V.getScanByDefName(e->scan);

				if(!scan)
				{
					std::cerr << "Developer error! generateFlagFile: SCAN_STOP, scan=0" << std::endl;

					exit(EXIT_FAILURE);
				}
				for(std::map<std::string,Interval>::const_iterator sa = scan->stations.begin(); sa != scan->stations.end(); ++sa)
				{
					if(antIds.count(sa->first) == 0)
					{
						continue;
					}
					flagMask[antIds[sa->first]] |= VexJobFlag::JOB_FLAG_SCAN;
				}
			}
		}
		else if(e->eventType == Event::ANT_SCAN_START)
		{
			if(hasScan(e->scan) && antIds.count(e->name) > 0)
			{
				flagMask[antIds[e->name]] &= ~VexJobFlag::JOB_FLAG_POINT;
			}
		}
		else if(e->eventType == Event::ANT_SCAN_STOP)
		{
			if(hasScan(e->scan) && antIds.count(e->name) > 0)
			{
				flagMask[antIds[e->name]] |= VexJobFlag::JOB_FLAG_POINT;
			}
		}
		else if(e->eventType == Event::JOB_START)
		{
			if(fabs(e->mjd - mjdStart) < 0.5/86400.0)
			{
				for(unsigned int antId = 0; antId < nAnt; ++antId)
				{
					flagMask[antId] &= ~VexJobFlag::JOB_FLAG_TIME;
				}
			}
		}
		else if(e->eventType == Event::JOB_STOP)
		{
			if(fabs(e->mjd - mjdStart) < 0.5/86400.0)
			{
				for(unsigned int antId = 0; antId < nAnt; ++antId)
				{
					flagMask[antId] |= VexJobFlag::JOB_FLAG_TIME;
				}
			}
		}

		for(unsigned int antId = 0; antId < nAnt; ++antId)
		{
			if( (flagMask[antId] & invalidMask) == 0)
			{
				if(flagStart[antId] > 0)
				{
					if(e->mjd - flagStart[antId] > 0.5/86400.0)
					{
						VexJobFlag f(flagStart[antId], e->mjd, antId);
						// only add flag if it overlaps in time with this job
						if(overlap(f))
						{
							flags.push_back(f);
						}
					}
					flagStart[antId] = -1;
				}
			}
			else
			{
				if(flagStart[antId] <= 0)
				{
					flagStart[antId] = e->mjd;
				}
			}
		}
	}

	// At end of loop see if any flag->unflag (or vice-versa) occurs.
	for(unsigned int antId = 0; antId < nAnt; ++antId)
	{
		if( (flagMask[antId] & invalidMask) != 0)
		{
			if(mjdStop - flagStart[antId] > 0.5/86400.0)
			{
				VexJobFlag f(flagStart[antId], mjdStop, antId);
				// only add flag if it overlaps in time with this job
				if(overlap(f))
				{
					flags.push_back(f);
				}
			}
		}
	}

	// write data to file
	of.open(fileName);
	of << flags.size() << std::endl;
	for(std::vector<VexJobFlag>::const_iterator it = flags.begin(); it != flags.end(); ++it)
	{
		of << "  " << *it << std::endl;
	}
	of.close();

	return flags.size();
}

void VexJobGroup::createJobs(std::vector<VexJob> &jobs, Interval &jobTimeRange, const VexData *V, double maxLength, double maxSize) const
{
	std::list<Event>::const_iterator s, e;
	jobs.push_back(VexJob());
	VexJob *J = &jobs.back();
	double totalTime, scanTime = 0.0;
	double size = 0.0;
	std::string id("");

	// note these are backwards now; will set these to minimum range covering scans
	J->setTimeRange(jobTimeRange.mjdStop, jobTimeRange.mjdStart);

	for(e = events.begin(); e != events.end(); ++e)
	{
		if(e->eventType == Event::SCAN_START)
		{
			s = e;
			id = e->name;
		}
		if(e->eventType == Event::SCAN_STOP)
		{
			if(id != e->name)
			{
				std::cerr << "Programming error: createJobs: id != e->name  (" << id << " != " << e->name << ")" << std::endl;
				std::cerr << "Contact developer" << std::endl;

				exit(EXIT_FAILURE);
			}
			Interval scanTimeRange(s->mjd, e->mjd);
			scanTimeRange.logicalAnd(jobTimeRange);
			if(scanTimeRange.duration() > 0.0)
			{
				J->scans.push_back(e->name);
				J->logicalOr(scanTimeRange);
				scanTime += scanTimeRange.duration();

				// Work in progress: calculate correlated size of scan
				size += V->getScanByDefName(id)->size;

				/* start a new job at scan boundary if maxLength exceeded */
				if(J->duration() > maxLength || size > maxSize)
				{
					totalTime = J->duration();
					J->dutyCycle = scanTime / totalTime;
					J->dataSize = size;
					jobs.push_back(VexJob());
					J = &jobs.back();
					scanTime = 0.0;
					size = 0.0;
					J->setTimeRange(jobTimeRange.mjdStop, jobTimeRange.mjdStart);
				}
			}
		}
	}

	totalTime = J->duration();
	
	if(totalTime <= 0.0)
	{
		jobs.pop_back();
	}
	else
	{
		J->dutyCycle = scanTime / totalTime;
		J->dataSize = size;
	}
}

const VexScan *VexData::getScan(unsigned int num) const
{
	if(num >= nScan())
	{
		return 0;
	}

	return &scans[num];
}

const VexScan *VexData::getScanByDefName(const std::string &defName) const
{
	for(std::vector<VexScan>::const_iterator it = scans.begin(); it != scans.end(); ++it)
	{
		if(it->defName == defName)
		{
			return &(*it);
		}
	}

	return 0;
}

const VexScan *VexData::getScanByAntennaTime(const std::string &antName, double mjd) const
{
	for(std::vector<VexScan>::const_iterator it = scans.begin(); it != scans.end(); ++it)
	{
		const Interval *interval = it->getAntennaInterval(antName);
		if(interval)
		{
			if(interval->contains(mjd))
			{
				return &(*it);
			}
		}
	}

	return 0;
}

static Interval adjustTimeRange(std::map<std::string, double> &antStart, std::map<std::string, double> &antStop, unsigned int minSubarraySize)
{
	std::list<double> start;
	std::list<double> stop;
	double mjdStart, mjdStop;

	if(minSubarraySize < 1)
	{
		std::cerr << "Developer error: adjustTimeRange: minSubarraySize = " << minSubarraySize << " is < 1" << std::endl;

		exit(EXIT_FAILURE);
	}

	if(antStart.size() != antStop.size())
	{
		std::cerr << "Developer error: adjustTimeRange: size mismatch" << std::endl;

		exit(EXIT_FAILURE);
	}

	if(antStart.size() < minSubarraySize)
	{
		// Return an acausal interval
		return Interval(1, 0);
	}

	for(std::map<std::string, double>::iterator it = antStart.begin(); it != antStart.end(); ++it)
	{
		start.push_back(it->second);
	}
	start.sort();
	// Now the start times are sorted chronologically

	for(std::map<std::string, double>::iterator it = antStop.begin(); it != antStop.end(); ++it)
	{
		stop.push_back(it->second);
	}
	stop.sort();
	// Now stop times are sorted chronologically

	// Pick off times where min subarray condition is met
	// If these are in the wrong order (i.e., no such interval exists)
	// Then these will form an acausal interval which will be caught by
	// the caller.
	for(unsigned int i = 0; i < minSubarraySize-1; ++i)
	{
		start.pop_front();
		stop.pop_back();
	}
	mjdStart = start.front();
	mjdStop = stop.back();

	// Adjust start times where needed
	for(std::map<std::string, double>::iterator it = antStart.begin(); it != antStart.end(); ++it)
	{
		if(it->second < mjdStart)
		{
			it->second = mjdStart;
		}
	}

	for(std::map<std::string, double>::iterator it = antStop.begin(); it != antStop.end(); ++it)
	{
		if(it->second > mjdStop)
		{
			it->second = mjdStop;
		}
	}

	return Interval(mjdStart, mjdStop);
}

// this removes scans out of time range or with fewer than minSubarraySize antennas
// the final time ranges (both of scans and antennas within) are truncated to the time window when the subarray size condition is met.
void VexData::reduceScans(int minSubarraySize, const Interval &timerange)
{
	std::list<std::string> antsToRemove;
	std::map<std::string, double> antStart, antStop;

// FIXME: maybe print some statistics such as number of scans dropped due to minsubarraysize and timerange

	for(std::vector<VexScan>::iterator it = scans.begin(); it != scans.end(); )
	{
		antStart.clear();
		antStop.clear();
		antsToRemove.clear();

		for(std::map<std::string,Interval>::iterator sit = it->stations.begin(); sit != it->stations.end(); ++sit)
		{
			if(sit->second.overlap(timerange) <= 0.0)
			{
				antsToRemove.push_back(sit->first);
			}
			else
			{
				sit->second.logicalAnd(timerange);
				antStart[sit->first] = sit->second.mjdStart;
				antStop[sit->first]  = sit->second.mjdStop;
			}
		}
		for(std::list<std::string>::const_iterator ait = antsToRemove.begin(); ait != antsToRemove.end(); ++ait)
		{
			it->stations.erase(*ait);
		}

		Interval antennaTimeRange = adjustTimeRange(antStart, antStop, minSubarraySize);

		if(!antennaTimeRange.isCausal() || it->overlap(antennaTimeRange) <= 0.5/86400.0)
		{
			it = scans.erase(it);
			continue;
		}

		for(std::map<std::string,Interval>::iterator sit = it->stations.begin(); sit != it->stations.end(); ++sit)
		{
			sit->second.logicalAnd(antennaTimeRange);
		}

		it->logicalAnd(antennaTimeRange);
		++it;
	}
}

void VexData::setScanSize(unsigned int num, double size)
{
	if(num >= nScan())
	{
		return;
	}
	
	scans[num].size = size;
}

void VexData::getScanList(std::list<std::string> &scanList) const
{
	for(std::vector<VexScan>::const_iterator it = scans.begin(); it != scans.end(); ++it)
	{
		scanList.push_back(it->defName);
	}
}

unsigned int VexData::nAntennasWithRecordedData(const VexScan &scan) const
{
	unsigned int nAnt = 0;

	const VexMode *M = getModeByDefName(scan.modeDefName);
	if(!M)
	{
		return 0;
	}

	for(std::map<std::string,Interval>::const_iterator it = scan.stations.begin(); it != scan.stations.end(); ++it)
	{
		std::map<std::string,VexSetup>::const_iterator S = M->setups.find(it->first);
		if(S != M->setups.end() && S->second.nRecordChan > 0 && S->second.formatName != "NONE" && scan.getRecordEnable(it->first) == true)
		{
			++nAnt;
		}
	}

	return nAnt;
}

VexAntenna *VexData::newAntenna()
{
	antennas.push_back(VexAntenna());

	return &antennas.back();
}

const VexAntenna *VexData::getAntenna(unsigned int num) const
{
	if(num >= nAntenna())
	{
		return 0;
	}

	return &antennas[num];
}

const VexAntenna *VexData::getAntenna(const std::string &name) const
{
	for(std::vector<VexAntenna>::const_iterator it = antennas.begin(); it != antennas.end(); ++it)
	{
		if(it->name == name)
		{
			return &(*it);
		}
	}

	return 0;
}

double VexData::getAntennaStartMJD(const std::string &name) const
{
	for(std::list<Event>::const_iterator e = events.begin(); e != events.end(); ++e)
	{
		if(e->eventType == Event::ANTENNA_START && e->name == name)
		{
			return e->mjd;
		}
	}

	return exper.mjdStart - 1.0;
}

double VexData::getAntennaStopMJD(const std::string &name) const
{
	for(std::list<Event>::const_iterator e = events.begin(); e != events.end(); ++e)
	{
		if(e->eventType == Event::ANTENNA_STOP && e->name == name)
		{
			return e->mjd;
		}
	}

	return exper.mjdStop + 1.0;
}


VexMode *VexData::newMode()
{
	modes.push_back(VexMode());

	return &modes.back();
}

const VexMode *VexData::getMode(unsigned int num) const
{
	if(num >= nMode())
	{
		return 0;
	}

	return &modes[num];
}

const VexMode *VexData::getModeByDefName(const std::string &defName) const
{
	for(std::vector<VexMode>::const_iterator it = modes.begin(); it != modes.end(); ++it)
	{
		if(it->defName == defName)
		{
			return &(*it);
		}
	}

	return 0;
}

unsigned int VexData::nRecordChan(const VexMode &mode, const std::string &antName) const
{
	unsigned int nRecChan = 0;

	std::map<std::string,VexSetup>::const_iterator it = mode.setups.find(antName);
	if(it != mode.setups.end())
	{
		nRecChan = it->second.nRecordChan;
	}
	else
	{
		std::cerr << "Warning: Ant " << antName << " not found in mode " << mode.defName << std::endl;
	}

	return nRecChan;
}

int VexData::getAntennaIdByName(const std::string &antName) const
{
	for(std::vector<VexAntenna>::const_iterator it = antennas.begin(); it != antennas.end(); ++it)
	{
		if(it->name == antName)
		{
			return it - antennas.begin();
		}
	}

	return -1;
}

int VexData::getAntennaIdByDefName(const std::string &antName) const
{
	for(std::vector<VexAntenna>::const_iterator it = antennas.begin(); it != antennas.end(); ++it)
	{
		if(it->defName == antName)
		{
			return it - antennas.begin();
		}
	}

	return -1;
}

// returns < 0 if number of channels varies with mode
int VexData::getNumAntennaRecChans(const std::string &antName) const
{
	int nRecChan = 0;
	
	for(std::vector<VexMode>::const_iterator it = modes.begin(); it != modes.end(); ++it)
	{
		int n;

		n = nRecordChan(*it, antName);
		if(n > 0)
		{
			if(nRecChan == 0)
			{
				nRecChan = n;
			}
			if(nRecChan != n)
			{
				return -1;	// two scans with different numbers of record chans found.
			}
		}
	}

	return nRecChan;
}

// returns false if antenna was not there.  Otherwise true
// this function essentially wipes out any record of this antenna being part of observing
bool VexData::removeAntenna(const std::string &name)
{
	bool rv = false;

	// remove VexAntenna
	for(std::vector<VexAntenna>::iterator it = antennas.begin(); it != antennas.end(); )
	{
		if(it->name == name)
		{
			it = antennas.erase(it);
			rv = true;
		}
		else
		{
			++it;
		}
	}

	// remove antenna from scans
	for(std::vector<VexScan>::iterator it = scans.begin(); it != scans.end(); )
	{
		it->stations.erase(name);

		if(it->stations.empty())
		{
			it = scans.erase(it);
		}
		else
		{
			++it;
		}
	}

	// remove antenna from modes
	for(std::vector<VexMode>::iterator it = modes.begin(); it != modes.end(); )
	{
		it->setups.erase(name);

		if(it->setups.empty())
		{
			it = modes.erase(it);
		}
		else
		{
			++it;
		}
	}

	return rv;
}


int VexData::getModeIdByDefName(const std::string &defName) const
{
	for(std::vector<VexMode>::const_iterator it = modes.begin(); it != modes.end(); ++it)
	{
		if(it->defName == defName)
		{
			return it - modes.begin();
		}
	}

	return -1;
}


VexEOP *VexData::newEOP()
{
	eops.push_back(VexEOP());

	return &eops.back();
}

const VexEOP *VexData::getEOP(unsigned int num) const
{
	if(num > nEOP())
	{
		return 0;
	}

	return &eops[num];
}

void VexData::addEOP(const VexEOP &e)
{
	VexEOP *E;

	E = newEOP();
	*E = e;
}

bool VexData::usesAntenna(const std::string &antennaName) const
{
	unsigned int n = nAntenna();

	for(unsigned int i = 0; i < n; ++i)
	{
		if(getAntenna(i)->name == antennaName)
		{
			return true;
		}
	}

	return false;
}

bool VexData::usesMode(const std::string &modeDefName) const
{
	unsigned int n = nScan();

	for(unsigned int i = 0; i < n; ++i)
	{
		if(getScan(i)->modeDefName == modeDefName)
		{
			return true;
		}
	}

	return false;
}

/*
unsigned int VexData::nVSN(const std::string &antName) const
{
	const VexAntenna *A;

	A = getAntenna(antName);
	if(!A)
	{
		return 0;
	}
	else
	{
		return A->vsns.size();
	}
}
*/

void VexData::addVSN(const std::string &antName, unsigned int datastreamId, const std::string &vsn, const Interval &timeRange)
{
	for(std::vector<VexAntenna>::iterator it = antennas.begin(); it != antennas.end(); ++it)
	{
		if(it->name == antName)
		{
			if(datastreamId < it->nDatastream())
			{
				it->datastreams[datastreamId].vsns.push_back(VexBasebandData(vsn, timeRange));
				it->datastreams[datastreamId].dataSource = DataSourceModule;
			}
			else
			{
				std::cerr << "Error: trying to add VSN " << vsn << " to antenna " << antName << " datastream " << datastreamId << " but the highest datastream there is " << (it->nDatastream()-1) << std::endl;
			}
		}
	}
}

/*
std::string VexData::getVSN(const std::string &antName, const Interval &timeRange) const
{
	const VexAntenna *A;
	double best = 0.0;
	std::string bestVSN("None");

	A = getAntenna(antName);
	if(!A)
	{
		return bestVSN;
	}

	if(A->dataSource != DataSourceModule)
	{
		return bestVSN;
	}

	for(std::vector<VexBasebandData>::const_iterator v = A->vsns.begin(); v != A->vsns.end(); ++v)
	{
		double timeOverlap = timeRange.overlap(*v);
		if(timeOverlap > best)
		{
			best = timeOverlap;
			bestVSN = v->filename;
		}
	}

	return bestVSN;
}
*/

void VexData::setExper(const std::string &name, const Interval &experTimeRange)
{
	double a=1.0e7, b=0.0;

	for(std::list<Event>::const_iterator it = events.begin(); it != events.end(); ++it)
	{
		if(it->mjd < a && it->eventType != Event::CLOCK_BREAK)
		{
			a = it->mjd;
		}
		if(it->mjd > b && it->eventType != Event::CLOCK_BREAK)
		{
			b = it->mjd;
		}
	}

	exper.name = name;
	exper.setTimeRange(experTimeRange);
	if(exper.mjdStart < 10000)
	{
		exper.mjdStart = a;
	}
	if(exper.mjdStop < 10000)
	{
		exper.mjdStop = b;
	}
}

void VexData::findLeapSeconds()
{
	int n = eops.size();

	if(n < 2)
	{
		return;
	}

	for(int i = 1; i < n; ++i)
	{
		if(eops[i-1].tai_utc != eops[i].tai_utc)
		{
			addEvent(eops[i].mjd, Event::LEAP_SECOND, "Leap second");
			std::cout << "Leap second detected at day " << eops[i].mjd << std::endl;
		}
	}
}

void VexData::addBreaks(const std::vector<double> &breaks)
{
	for(std::vector<double>::const_iterator t = breaks.begin(); t != breaks.end(); ++t)
	{
		if(exper.contains(*t))
		{
			addEvent(*t, Event::MANUAL_BREAK, "");
		}
	}
}

void VexData::swapPolarization(const std::string &antName)
{
	for(std::vector<VexMode>::iterator it = modes.begin(); it != modes.end(); ++it)
	{
		it->swapPolarization(antName);
	}
}

void VexData::setPhaseCalInterval(const std::string &antName, int phaseCalIntervalMHz)
{
	for(std::vector<VexMode>::iterator it = modes.begin(); it != modes.end(); ++it)
	{
		it->setPhaseCalInterval(antName, phaseCalIntervalMHz);
	}
}

void VexData::selectTones(const std::string &antName, enum ToneSelection selection, double guardBandMHz)
{
	for(std::vector<VexMode>::iterator it = modes.begin(); it != modes.end(); ++it)
	{
		it->selectTones(antName, selection, guardBandMHz);
	}
}

void VexData::setClock(const std::string &antName, const VexClock &clock)
{
	for(std::vector<VexAntenna>::iterator it = antennas.begin(); it != antennas.end(); ++it)
	{
		if(it->name == antName)
		{
			it->clocks.clear();
			it->clocks.push_back(clock);
		}
	}
}

void VexData::setTcalFrequency(const std::string &antName, int tcalFrequency)
{
	for(std::vector<VexAntenna>::iterator it = antennas.begin(); it != antennas.end(); ++it)
	{
		if(it->name == antName)
		{
			it->tcalFrequency = tcalFrequency;
		}
	}
}

void VexData::setAntennaPosition(const std::string &antName, double X, double Y, double Z)
{
	for(std::vector<VexAntenna>::iterator it = antennas.begin(); it != antennas.end(); ++it)
	{
		if(it->name == antName)
		{
			it->x = X;
			it->y = Y;
			it->z = Z;
			it->dx = 0.0;
			it->dy = 0.0;
			it->dz = 0.0;
		}
	}
}

void VexData::setAntennaAxisOffset(const std::string &antName, double axisOffset)
{
	for(std::vector<VexAntenna>::iterator it = antennas.begin(); it != antennas.end(); ++it)
	{
		if(it->name == antName)
		{
			it->axisOffset = axisOffset;
		}
	}
}

void VexData::addExperEvents(std::vector<Event> &events) const
{
	addEvent(events, exper.mjdStart, Event::OBSERVE_START, exper.name); 
	addEvent(events, exper.mjdStop, Event::OBSERVE_STOP, exper.name); 
}

void VexData::addClockEvents(std::vector<Event> &events) const
{
	for(std::vector<VexAntenna>::const_iterator it = antennas.begin; it != antenna.end; ++it)
	{
		for(std::vector<VexClock>::const_iterator cit = it->clocks.begin(); cit != it->clocks.end(); ++it)
		{
			addEvent(events, cit->mjdStart, Event::CLOCK_BREAK, it->name);
		}
	}
}

void VexData::addScanEvents(std::vector<Event> &events) const
{
	for(std::vector<VexScan>::const_iterator it = scans.begin(); it != scans.end(); ++it)
	{
		addEvent(events, it->mjdStart, Event::SCAN_START, it->defName, it->defName);
		addEvent(events, it->mjdStop,  Event::SCAN_STOP,  it->defName, it->defName);
		for(std::map<std::string,Interval>::const_iterator sit = it->stations.begin(); sit != it->stations.end(); ++sit)
		{
			addEvent(events, std::max(sit->second.mjdStart, it->mjdStart), Event::ANT_SCAN_START, sit->first, it->defName);
			addEvent(events, std::min(sit->second.mjdStop,  it->mjdStop),  Event::ANT_SCAN_STOP,  sit->first, it->defName);
		}
	}

}

void VexData::addVSNEvents(std::vector<Event> &events) const
{
	for(std::vector<VexAntenna>::iterator it = antennas.begin(); it != antennas.end(); ++it)
	{
		for(std::vector<VexDatastream>::const_iterator dit = it->datastreams.begin(); dit != it->datastreams.end(); ++dit)
		{
			if(dit->dataSource == DataSourceModule)
			{
				for(std::vector<VexBasebandData>::const_iterator vit = dit->vsns.begin(); vit != dit->vsns.end(); ++vit)
				{
					addEvent(events, vit->mjdStart, Event::RECORD_START, it->defName);
					addEvent(events vit->mjdStop,  Event::RECORD_STOP,  it->defName);
				}
			}
		}
		
	}
}

void VexData::generateEvents(std::vector<Event> &events) const;
{
	events.clear();

	addExperEvents(events);
	addClockEvents(events);
	addScanEvents(events);
	addVSNEvents(events);

	sort(events.begin(), events.end());
}

std::ostream& operator << (std::ostream &os, const VexJob &x)
{
	int p = os.precision();
	
	os.precision(12);
	os << "Job " << x.jobSeries << "_" << x.jobId << std::endl;
	os << "  " << (const Interval&)x << std::endl;
	os << "  duty cycle = " << x.dutyCycle << std::endl;
	os << "  scans =";
	for(std::vector<std::string>::const_iterator s = x.scans.begin(); s != x.scans.end(); ++s)
	{
		os << " " << *s;
	}
	os << std::endl;
	os << "  Antenna list:";
	for(std::vector<std::string>::const_iterator a = x.jobAntennas.begin(); a != x.jobAntennas.end(); ++a)
	{
		os << " " << *a;
	}
	os << std::endl;
	os << "  size = " << x.dataSize << " bytes" << std::endl;

	os.precision(p);

	return os;
}

std::ostream& operator << (std::ostream &os, const VexJobGroup &x)
{
	int p = os.precision();
	
	os.precision(12);
	os << "Group: scans " << x.scans.front() << " - " << x.scans.back() << " = " << (const Interval &)x << std::endl;
	os.precision(p);
	
	return os;
}

std::ostream& operator << (std::ostream &os, const VexJobFlag &x)
{
	int p = os.precision();

	os.precision(12);

	os << x.mjdStart << " " << x.mjdStop << " " << x.antId;

	os.precision(p);

	return os;
}

std::ostream& operator << (std::ostream &os, const VexData &x)
{
	int n = x.nSource();

	os << "Vex:" << std::endl;
	os << n << " sources:" << std::endl;
	for(int i = 0; i < n; ++i)
	{
		os << *x.getSource(i);
	}

	n = x.nScan();
	os << n << " scans:" << std::endl;
	for(int i = 0; i < n; ++i)
	{
		os << *x.getScan(i);
	}

	n = x.nAntenna();
	os << n << " antennas:" << std::endl;
	for(int i = 0; i < n; ++i)
	{
		os << *x.getAntenna(i);
	}

	n = x.nMode();
	os << n << " modes:" << std::endl;
	for(int i = 0; i < n; ++i)
	{
		os << *x.getMode(i);
	}

	n = x.nEOP();
	os << n << " eops:" << std::endl;
	for(int i = 0; i < n; ++i)
	{
		os << "   " << *x.getEOP(i) << std::endl;
	}

	return os;
}
