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
 * $Id: vex2difx.cpp 6854 2015-07-18 16:46:05Z WalterBrisken $
 * $HeadURL: https://svn.atnf.csiro.au/difx/applications/vex2difx/branches/multidatastream_refactor/src/vex2difx.cpp $
 * $LastChangedRevision: 6854 $
 * $Author: WalterBrisken $
 * $LastChangedDate: 2015-07-18 10:46:05 -0600 (Sat, 18 Jul 2015) $
 *
 *==========================================================================*/

#include <vector>
#include <set>
#include <sstream>
#include <cmath>
#include <cstdlib>
#include <cstring>
#include <fstream>
#include <algorithm>
#include <sys/time.h>
#include <sys/stat.h>
#include <difxio/difx_input.h>
#include <difxmessage.h>
#include "vex_data.h"
#include "event.h"
#include "corrparams.h"
#include "vexload.h"
#include "util.h"
#include "timeutils.h"
#include "../config.h"

using namespace std;


int applyCorrParams(VexData *V, const CorrParams &params)
{
	int nWarn = 0;

	// merge sets of EOPs from vex and corr params file
	if(!params.eops.empty())
	{
		if(V->nEOP() > 0)
		{
			std::cerr << "Warning: Mixing EOP values from vex and v2d files.  Your mileage may vary!" << std::endl;
			++nWarn;
		}

		for(std::vector<VexEOP>::const_iterator e = params.eops.begin(); e != params.eops.end(); ++e)
		{
			V->addEOP(*e);
		}
	}

	// remove unwanted antennas
	for(unsigned int a = 0; a < V->nAntenna(); )
	{
		const VexAntenna *A;

		A = V->getAntenna(a);
		if(!A)
		{
			std::cerr << "Developer error: mergeCorrParams: Antenna number " << a << " cannot be gotten even though nAntenna() reports " << V->nAntenna() << std::endl;

			exit(EXIT_FAILURE);
		}
		if(!params.useAntenna(A->defName))
		{
			V->removeAntenna(A->defName);
		}
		else
		{
			++a;
		}
	}

	// remove scans with too few antennas or with scans outside the specified time range
	V->reduceScans(params.minSubarraySize, params);

	// swap antenna polarizations
	for(unsigned int a = 0; a < V->nAntenna(); ++a)
	{
		const VexAntenna *A;

		A = V->getAntenna(a);
		if(!A)
		{
			std::cerr << "Developer error: applyCorrParams: Antenna number " << a << " cannot be gotten even though nAntenna() reports " << V->nAntenna() << std::endl;

			exit(EXIT_FAILURE);
		}

		if(params.swapPol(A->defName))
		{
			V->swapPolarization(A->defName);
		}
	}

	// Data and data source
	for(unsigned int a = 0; a < V->nAntenna(); ++a)
	{
		const VexAntenna *A = V->getAntenna(a);
		if(!A)
		{
			std::cerr << "Developer error: applyCorrParams: Antenna " << a << " cannot be gotten" << std::endl;

			exit(EXIT_FAILURE);
		}

		const AntennaSetup *as = params.getAntennaSetup(A->defName);
		if(!as)
		{
			// No antenna setup here, so continue...
			continue;
		}

		int nDatastreamSetup = as->datastreamSetups.size();
		if(nDatastreamSetup <= 0)
		{
			// nothing provided
			continue;
		}

		for(int i = 0; i < nDatastreamSetup; ++i)
		{
			// Here just directly copy updated values from v2d into existing structure
			const DatastreamSetup &dss = as->datastreamSetups[i];
			
			switch(dss.dataSource)
			{
			case DataSourceFile:
				V->setFiles(a, i, dss.basebandFiles);
				break;
			case DataSourceModule:
				V->setModule(a, i, dss.vsn);
				break;
			case DataSourceNetwork:
				V->setNetworkParameters(a, i, dss.networkPort, dss.windowSize);
				break;
			case DataSourceFake:
				V->setFake(a);
				break;
			default:
				break;
			}
		}
	}

	// MODES / SETUPS / formats

	for(unsigned int m = 0; m < V->nMode(); ++m)
	{
		const VexMode *M = V->getMode(m);
		if(!M)
		{
			std::cerr << "Developer error: applyCorrParams: Mode number " << m << " cannot be gotten even though nMode() reports " << V->nMode() << std::endl;

			exit(EXIT_FAILURE);
		}
		for(std::map<std::string,VexSetup>::const_iterator it = M->setups.begin(); it != M->setups.end(); ++it)
		{
		}
	}


	// datastream merging: change formats, channel selection, ...
	/* mode->setup[ant] = antSetup->getFormat() 
	
		but maybe much more interesting -- set threads, streams, ... here too?

	*/


	// Tones
	for(unsigned int a = 0; a < V->nAntenna(); ++a)
	{
		const VexAntenna *A;

		A = V->getAntenna(a);
		if(!A)
		{
			std::cerr << "Developer error: mergeCorrParams: Antenna number " << a << " cannot be gotten even though nAntenna() reports " << V->nAntenna() << std::endl;

			exit(EXIT_FAILURE);
		}

		const AntennaSetup *as = params.getAntennaSetup(A->defName);
		if(!as)
		{
			// No antenna setup implies doing "smart" tone extraction (-1.0 implies 1/8 band guard)
			V->selectTones(A->defName, ToneSelectionSmart, -1.0);

			continue;
		}

		if(as->toneSelection == ToneSelectionNone)
		{
			// change to having no injected tones
			V->setPhaseCalInterval(A->defName, -1);

			continue;
		}

		if(as->phaseCalIntervalMHz >= 0)
		{
			// this sets phase cal interval and removes tones that are not multiples of it
			// interval = 0 implies no pulse cal
			V->setPhaseCalInterval(A->defName, as->phaseCalIntervalMHz);
		}

		if(as->toneSelection != ToneSelectionVex)
		{
			V->selectTones(A->defName, as->toneSelection, as->toneGuardMHz);
		}
		
	}

	// Override clocks and other antenna parameters
	for(unsigned int a = 0; a < V->nAntenna(); ++a)
	{
		const VexAntenna *A;

		A = V->getAntenna(a);
		if(!A)
		{
			std::cerr << "Developer error: mergeCorrParams: Antenna number " << a << " cannot be gotten even though nAntenna() reports " << V->nAntenna() << std::endl;

			exit(EXIT_FAILURE);
		}

		const AntennaSetup *as = params.getAntennaSetup(A->defName);
		if(!as)
		{
			continue;
		}

		const VexClock *paramClock = params.getAntennaClock(A->defName);
		if(paramClock)
		{
			V->setClock(A->defName, *paramClock);
		}

		if(as->X != 0.0 || as->Y != 0.0 || as->Z != 0.0)
		{
			V->setAntennaPosition(A->defName, as->X, as->Y, as->Z);
		}

		if(as->tcalFrequency != 0)
		{
			V->setTcalFrequency(A->defName, as->tcalFrequency);
		}

		if(as->axisOffset != 0.0)
		{
			V->setAntennaAxisOffset(A->defName, as->axisOffset);
		}
	}


	return nWarn;
}

int main(int argc, char **argv)
{
	CorrParams *P;
	VexData *V;
	const VexScan * S;
	const SourceSetup * sourceSetup;
	list<Event> events;
	string shelfFile;
	string missingDataFile;	// created if file-based and no files for a particular antenna/job are found
	string v2dFile;
	string command;
	int verbose = 0;
	int ok;
	bool writeParams = 0;
	bool deleteOld = 0;
	bool strict = 1;
	int nWarn = 0;
	int nError = 0;
	int nSkip = 0;
	int nDigit;
	int nJob = 0;

	if(argc < 2)
	{
		cout << "Need v2d file" << endl;

		return EXIT_FAILURE;
	}

	// force program to work in Universal Time
	setenv("TZ", "", 1);
	tzset();


	for(int a = 1; a < argc; ++a)
	{
		if(argv[a][0] == '-')
		{
			if(strcmp(argv[a], "-v") == 0 ||
				strcmp(argv[a], "--verbose") == 0)
			{
				++verbose;
			}
			else if(strcmp(argv[a], "-f") == 0 ||
				strcmp(argv[a], "--force") == 0)
			{
				strict = 0;
			}
			else if(strcmp(argv[a], "-s") == 0 ||
				strcmp(argv[a], "--strict") == 0)
			{
				strict = 1;
			}
			else
			{
				cerr << "Error: unknown option " << argv[a] << endl;
				cerr << "Run with -h for help information." << endl;
				
				exit(EXIT_FAILURE);
			}
		}
		else
		{
			if(!v2dFile.empty())
			{
				cerr << "Error: multiple configuration files provided, only one expected." << endl;
				cerr << "Run with -h for help information." << endl;
				
				exit(EXIT_FAILURE);
			}
			v2dFile = argv[a];
		}
	}

	if(v2dFile.size() > DIFX_MESSAGE_PARAM_LENGTH-2)
	{
		//job numbers into the tens of thousands will be truncated in difxmessage.  Better warn the user.
		cout << "Filename " << v2dFile << " is too long - its job name might be truncated by difxmessage!" << endl;
		cout << "You are strongly suggested to choose a shorter .v2d name (root shorter than 26 characters)" << endl;
	}

	if(v2dFile.empty())
	{
		cerr << "Error: configuration (.v2d) file expected." << endl;
		cerr << "Run with -h for help information." << endl;
		
		exit(EXIT_FAILURE);
	}

	if(v2dFile.find("_") != string::npos)
	{
		cerr << "Error: you cannot have an underscore (_) in the filename!" << endl;
		cerr << "Please rename it and run again." << endl;
		
		exit(EXIT_FAILURE);
	}

	if(!isalpha(v2dFile[0]))
	{
		cerr << "Error: pass name (.v2d file name) must start with a letter!" << endl;
		cerr << "Please rename it and run again." << endl;
		
		exit(EXIT_FAILURE);
	}

	P = new CorrParams(v2dFile);
	if(P->vexFile.empty())
	{
		cerr << "Error: vex file parameter (vex) not found in file." << endl;
		
		exit(EXIT_FAILURE);
	}

	nWarn = P->parseWarnings;

	umask(02);


	// delete "no data" file before starting
	missingDataFile = v2dFile.substr(0, v2dFile.find_last_of('.'));
	missingDataFile += string(".nodata");
	command = "rm -f " + missingDataFile;
	system(command.c_str());

	V = loadVexFile(P->vexFile, &nWarn);

	if(!V)
	{
		cerr << "Error: cannot load vex file: " << P->vexFile << endl;
		
		exit(EXIT_FAILURE);
	}

	nWarn += applyCorrParams(V, *P);
	
	V->generateEvents(events);
	V->addBreakEvents(events, P->manualBreaks);

	return EXIT_SUCCESS;
}
