#include <cstdlib>
#include "applycorrparams.h"

int applyCorrParams(VexData *V, const CorrParams &params, int &nWarn, int &nError)
{
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
	for(unsigned int a = 0; a < V->nAntenna(); ++a)
	{
		const VexAntenna *A;

		A = V->getAntenna(a);
		if(!A)
		{
			std::cerr << "Developer error: applyCorrParams: Antenna number " << a << " cannot be gotten even though nAntenna() reports " << V->nAntenna() << std::endl;

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

	// apply source parameters
	for(unsigned int s = 0; s < V->nSource(); ++s)
	{
		const VexSource *S = V->getSource(s);
		if(!S)
		{
			std::cerr << "Developer error: applyCorrParams: Source number " << s << " cannot be gotten even though nSource() reports " << V->nSource() << std::endl;

			exit(EXIT_FAILURE);
		}
		
		const SourceSetup *ss = params.getSourceSetup(S->defName);
		if(ss)
		{
			if(ss->pointingCentre.calCode != ' ')
			{
				V->setSourceCalCode(S->defName, ss->pointingCentre.calCode);
			}
		}

		// FIXME: here put multiphasecenter things, eventually

	}

	// remove scans that are not linked from v2d setups
	for(unsigned int s = 0; s < V->nScan(); )
	{
		const VexScan *S = V->getScan(s);
		if(!S)
		{
			std::cerr << "Developer error: applyCorrParams: Scan number " << s << " cannot be gotten even though nScan() reports " << V->nScan() << std::endl;

			exit(EXIT_FAILURE);
		}

		const VexSource *src = V->getSourceByDefName(S->defName);
		if(!src)
		{
			std::cerr << "Developer error: applyCorrParams: Source " << S->defName << " not found" << std::endl;

			exit(EXIT_FAILURE);
		}

		const std::string &corrSetupName = params.findSetup(S->defName, S->sourceDefName, S->modeDefName);

		if(corrSetupName == "" || corrSetupName == "SKIP")
		{
			V->removeScan(S->defName);
		}
		else
		{
			if(params.getCorrSetup(corrSetupName) == 0)
			{
				std::cerr << "Error: Scan=" << S->defName << " correlator setup " << corrSetupName << " not defined!" << std::endl;

				exit(EXIT_FAILURE);
			}

			V->setScanCorrSetup(S->defName, corrSetupName);
			++s;
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

	// FIXME: transfer dataSampling to VexStream

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
			const AntennaSetup *as = params.getAntennaSetup(it->first);
			if(!as)
			{
				// no antenna setup defined, continue;
				continue;
			}
			int corrparamsDatastreams = as->datastreamSetups.size();
			if(corrparamsDatastreams == 0)
			{
				// no setup datastreams defined, continue;
				continue;
			}
			int nDatastream = it->second.streams.size();
			if(nDatastream > 1 && nDatastream != corrparamsDatastreams)
			{
				std::cerr << "Error: multiple streams defined for mode " << M->defName << " antenna " << it->first << " but a non-matching number of DATASTREAMS are defined for this antenna in the .v2d file" << std::endl;

				++nError;

				continue;
			}

			// if number of DATASTREAMS in .v2d > 1 and number of streams found in vex == 1, then replicate the later to be of the size of the former, initializing with the same data.

			if(nDatastream == 1 && corrparamsDatastreams > 1)
			{
				V->cloneStreams(M->defName, it->first, corrparamsDatastreams);
			}

			for(int ds = 0; ds < corrparamsDatastreams; ++ds)
			{
				const DatastreamSetup &DS = as->datastreamSetups[ds];

				if(!DS.format.empty())
				{
					bool v;

					v = V->setFormat(M->defName, it->first, ds, DS.format);

					if(!v)
					{
						std::cerr << "Error: format " << DS.format << " did not parse sensibly," << std::endl;
						++nError;
					}
				}

				if(DS.nBand > 0 || DS.startBand >= 0)
				{
					V->setStreamBands(M->defName, it->first, ds, DS.nBand, DS.startBand);
				}
			}

			// apply canonical VDIF mapping if appropriate and if needed
			if(usesCanonicalVDIF(it->first) && it->second.usesFormat(VexStream::FormatVDIF))
			{
				V->setCanonicalVDIF(M->defName, it->first);
			}
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

	return 0;
}
