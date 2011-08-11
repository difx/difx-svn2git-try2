/***************************************************************************
 *   Copyright (C) 2008-2011 by Walter Brisken                             *
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

#include <cstdio>
#include <cstring>
#include <cstdlib>
#include <unistd.h>
#include <ctype.h>
#include <xlrapi.h>
#include <difxmessage.h>
#include <mark5ipc.h>
#include "mk5daemon.h"
#include "smart.h"

void getModuleInfo(SSHANDLE xlrDevice, Mk5Daemon *D, int bank)
{
	getMk5Smart(xlrDevice, D, bank);
}

void clearModuleInfo(Mk5Daemon *D, int bank)
{
	clearMk5Smart(D, bank);
	clearMk5DirInfo(D, bank);
	D->driveStatsIndex[bank] = 0;
	D->driveFail[bank] = -1;

	D->dirLength[bank] = 0;
	if(D->dirData[bank])
	{
		free(D->dirData[bank]);
		D->dirData[bank] = 0;
	}
}



/* see if vsn matches <chars> {+|-} <digits> */
int legalVSN(const char *vsn)
{
	int state=0;

	for(int i = 0; i < 8; i++)
	{
		switch(state)
		{
		case 0:
			if(!isalpha(vsn[i]))
			{
				return 0;
			}
			state = 1;
			break;
		case 1:
			if(i >= 6)
			{
				return 0;
			}
			if(vsn[i] == '-' || vsn[i] == '+')
			{
				state = 2;
			}
			else if(!isalpha(vsn[i]))
			{
				return 0;
			}
			break;
		case 2:
			if(!isdigit(vsn[i]))
			{
				return 0;
			}
			break;
		}
	}

	return 1;
}

static int XLR_get_modules(Mk5Daemon *D)
{
	SSHANDLE xlrDevice;
	XLR_RETURN_CODE xlrRC;
	unsigned int xlrError;
	char message[DIFX_MESSAGE_LENGTH];
	char xlrErrorStr[XLR_ERROR_LENGTH];
	char temp[N_BANK][SMART_TEMP_STRING_LENGTH]; 
	const char id[] = "GetModules";
	int v;

	v = lockStreamstor(D, id, MARK5_LOCK_DONT_WAIT);
	if(v < 0)
	{
		return 0;
	}
	
	xlrRC = XLROpen(1, &xlrDevice);
	D->nXLROpen++;
	if(xlrRC != XLR_SUCCESS)
	{
		xlrError = XLRGetLastError();
		XLRGetErrorMessage(xlrErrorStr, xlrError);
		snprintf(message, DIFX_MESSAGE_LENGTH, 
			"ERROR: XLR_get_modules: Cannot open streamstor card.  N=%d Error=%u (%s)\n",
			D->nXLROpen, xlrError, xlrErrorStr);
		Logger_logData(D->log, message);

		unlockStreamstor(D, id);

		return 1;
	}

	xlrRC = XLRSetOption(xlrDevice, SS_OPT_SKIPCHECKDIR);
	if(xlrRC != XLR_SUCCESS)
	{
		xlrError = XLRGetLastError();
		if(xlrError != 148) /* XLR_ERR_DRIVEMODULE_NOTREADY */
		{
			XLRGetErrorMessage(xlrErrorStr, xlrError);
			snprintf(message, DIFX_MESSAGE_LENGTH,
				"ERROR: XLR_get_modules: Cannot set SkipCheckDir.  N=%d Error=%u (%s)\n",
				D->nXLROpen, xlrError, xlrErrorStr);
			XLRClose(xlrDevice);

			unlockStreamstor(D, id);

			Logger_logData(D->log, message);
		
			return 1;
		}
	}
	
	for(int bank = N_BANK-1; bank >= 0; bank--)
	{
		xlrRC = XLRGetBankStatus(xlrDevice, bank, &(D->bank_stat[bank]));
		if(xlrRC != XLR_SUCCESS)
		{
			D->vsns[bank][0] = 0;

			xlrError = XLRGetLastError();
			XLRGetErrorMessage(xlrErrorStr, xlrError);
			snprintf(message, DIFX_MESSAGE_LENGTH,
				"ERROR: XLR_get_modules: BANK_%c XLRGetBankStatus failed.  N=%d Error=%u (%s)\n",
				'A' + bank, D->nXLROpen, xlrError, xlrErrorStr);
			Logger_logData(D->log, message);
			clearModuleInfo(D, bank);
			clearMk5Stats(D, bank);
		}
		else if(strncmp(D->vsns[bank], D->bank_stat[bank].Label, 8) != 0)
		{
			if(legalVSN(D->vsns[bank]))
			{
				snprintf(message, DIFX_MESSAGE_LENGTH, "Module %s removed from bank %c", D->vsns[bank], 'A'+bank);
				difxMessageSendDifxAlert(message, DIFX_ALERT_LEVEL_VERBOSE);
			}

			if(legalVSN(D->bank_stat[bank].Label))
			{
				difxMessageSendDifxAlert(message, DIFX_ALERT_LEVEL_VERBOSE);
				strncpy(D->vsns[bank], D->bank_stat[bank].Label, 8);
				D->vsns[bank][8] = 0;
				snprintf(message, DIFX_MESSAGE_LENGTH, "Module %s inserted into bank %c", D->vsns[bank], 'A'+bank);
				getModuleInfo(xlrDevice, D, bank);
				logMk5Smart(D, BANK_A);
			}
			else
			{
				D->vsns[bank][0] = 0;
				clearModuleInfo(D, bank);
				clearMk5Stats(D, bank);
			}
/*	FIXME: detect illegal VSNs
			else if(strncmp(D->vsns[bank], "illegal", 7) != 0)
			{
				snprintf(message, DIFX_MESSAGE_LENGTH, "Module with illegal VSN inserted into bank %c", 'A'+bank);
				difxMessageSendDifxAlert(message, DIFX_ALERT_LEVEL_ERROR);
				sprintf(D->vsns[bank], "illegal%c", 'A'+bank);
			}
*/
		}
		if(D->vsns[bank][0] && D->smartData[bank].mjd < 50000)
		{
			getModuleInfo(xlrDevice, D, bank);
		}
	}

	XLRClose(xlrDevice);

	unlockStreamstor(D, id);

	for(int bank = 0; bank < N_BANK; bank++)
	{
		extractSmartTemps(temp[bank], D, bank);
	}

	snprintf(message, DIFX_MESSAGE_LENGTH, "XLR VSNs: <%s> %s  <%s> %s  N=%d\n",
		D->vsns[0], temp[0], D->vsns[1], temp[1], D->nXLROpen);
	Logger_logData(D->log, message);

	return 0;
}

void Mk5Daemon_getModules(Mk5Daemon *D)
{
	DifxMessageMk5Status dm;
	int n, v;
	char message[DIFX_MESSAGE_LENGTH];

	if(!D->isMk5)
	{
		return;
	}

	memset(&dm, 0, sizeof(DifxMessageMk5Status));

	/* don't let the process type change while getting vsns */
	pthread_mutex_lock(&D->processLock);

	if(D->process != PROCESS_NONE)
	{
		v = lockStreamstor(D, "getvsn", 0);
		if(v >= 0)
		{
			unlockStreamstor(D, "getvsn");
			D->process = PROCESS_NONE;
		}
	}

	switch(D->process)
	{
	case PROCESS_NONE:
		n = XLR_get_modules(D);
		if(n == 0)
		{
			dm.state = MARK5_STATE_IDLE;
		}
		else
		{
			dm.state = MARK5_STATE_ERROR;
		}
		break;
	default:
		pthread_mutex_unlock(&D->processLock);

		return;
	}

	if(D->activeBank >= 0 && D->activeBank < N_BANK)
	{
		if(D->vsns[D->activeBank][0] == 0)
		{
			int nextBank;
			
			for(int i = 1; i < N_BANK; i++)
			{
				nextBank = (D->activeBank + i) % N_BANK;
				if(D->vsns[nextBank][0] != 0)
				{
					D->activeBank = nextBank;

					break;
				}
			}

			if(D->vsns[D->activeBank][0] == 0)	/* no new bank found */
			{
				snprintf(message, DIFX_MESSAGE_LENGTH, "No bank is active now.  Active bank was previously %c", D->activeBank + 'A');
				difxMessageSendDifxAlert(message, DIFX_ALERT_LEVEL_VERBOSE);

				D->activeBank = -1;
			}
			else
			{
				snprintf(message, DIFX_MESSAGE_LENGTH, "Bank %c is active now.  Active bank was previously %c", nextBank + 'A', D->activeBank + 'A');
				difxMessageSendDifxAlert(message, DIFX_ALERT_LEVEL_VERBOSE);

				D->activeBank = nextBank;
			}
		}
	}
	else
	{
		for(int i = 0; i < N_BANK; i++)
		{
			if(D->vsns[i][0] != 0)
			{
				D->activeBank = i;
				snprintf(message, DIFX_MESSAGE_LENGTH, "Bank %c is active now.", D->activeBank + 'A');
				difxMessageSendDifxAlert(message, DIFX_ALERT_LEVEL_VERBOSE);

				break;
			}
		}
	}

	strncpy(dm.vsnA, D->vsns[0], 8);
	strncpy(dm.vsnB, D->vsns[1], 8);
	
	pthread_mutex_unlock(&D->processLock);

	difxMessageSendMark5Status(&dm);
}

/* on > 0 for mount, 0 for dismount */
static int XLR_disc_power(Mk5Daemon *D, const char *banks, int on)
{
	SSHANDLE xlrDevice;
	XLR_RETURN_CODE xlrRC;
	unsigned int xlrError;
	char message[DIFX_MESSAGE_LENGTH];
	char xlrErrorStr[XLR_ERROR_LENGTH];
	int bank;
	const char id[] = "BankPower";
	int v;

	v = lockStreamstor(D, id, MARK5_LOCK_DONT_WAIT);
	if(v != 0)
	{
		return 0;
	}
	
	xlrRC = XLROpen(1, &xlrDevice);
	D->nXLROpen++;
	if(xlrRC != XLR_SUCCESS)
	{
		xlrError = XLRGetLastError();
		XLRGetErrorMessage(xlrErrorStr, xlrError);
		snprintf(message, DIFX_MESSAGE_LENGTH, 
			"ERROR: XLR_disc_power: Cannot open streamstor card.  N=%d Error=%u (%s)\n",
			D->nXLROpen, xlrError, xlrErrorStr);
		Logger_logData(D->log, message);

		unlockStreamstor(D, id);

		return 1;
	}

	for(int i = 0; banks[i]; i++)
	{
		if(banks[i] == 'A' || banks[i] == 'a')
		{
			bank = BANK_A;
		}
		else if(banks[i] == 'B' || banks[i] == 'b')
		{
			bank = BANK_B;
		}
		else
		{
			snprintf(message, DIFX_MESSAGE_LENGTH, 
				"XLR_disc_power: illegal args: bank=%c, on=%d\n", banks[i], on);
			Logger_logData(D->log, message);
			continue;
		}
		if(on)
		{
			xlrRC = XLRMountBank(xlrDevice, bank);
		}
		else
		{
			xlrRC = XLRDismountBank(xlrDevice, bank);
		}
		if(xlrRC != XLR_SUCCESS)
		{
			xlrError = XLRGetLastError();
			XLRGetErrorMessage(xlrErrorStr, xlrError);
			snprintf(message, DIFX_MESSAGE_LENGTH, 
				"XLR_disc_power: error for bank=%c on=%d Error=%u (%s)\n",
				banks[i], on, xlrError, xlrErrorStr);
			Logger_logData(D->log, message);
		}
	}

	XLRClose(xlrDevice);

	unlockStreamstor(D, id);

	return 0;
}

void Mk5Daemon_diskOn(Mk5Daemon *D, const char *banks)
{
	/* don't let the process type change while getting vsns */
	pthread_mutex_lock(&D->processLock);

	if(D->process == PROCESS_NONE)
	{
		XLR_disc_power(D, banks, 1);
	}

	pthread_mutex_unlock(&D->processLock);
}

void Mk5Daemon_diskOff(Mk5Daemon *D, const char *banks)
{
	/* don't let the process type change while getting vsns */
	pthread_mutex_lock(&D->processLock);

	if(D->process == PROCESS_NONE)
	{
		XLR_disc_power(D, banks, 0);
	}

	pthread_mutex_unlock(&D->processLock);
}

static int XLR_error(Mk5Daemon *D, unsigned int *xlrError , char *msg)
{
	SSHANDLE xlrDevice;
	XLR_RETURN_CODE xlrRC;
	char message[DIFX_MESSAGE_LENGTH];
	const char id[] = "Error";
	int v;

	*xlrError = 0;

	v = lockStreamstor(D, id, MARK5_LOCK_DONT_WAIT);
	if(v != 0)
	{
		strcpy(message, "Streamstor card in use");

		return 5;	/* too busy */
	}

	xlrRC = XLROpen(1, &xlrDevice);
	D->nXLROpen++;
	if(xlrRC != XLR_SUCCESS)
	{
		*xlrError = XLRGetLastError();
		XLRGetErrorMessage(msg, *xlrError);
	}
	else
	{
		XLRClose(xlrDevice);
	}

	unlockStreamstor(D, id);

	return 0;
}

static int XLR_setProtect(Mk5Daemon *D, enum WriteProtectState state, char *msg)
{
	SSHANDLE xlrDevice;
	XLR_RETURN_CODE xlrRC;
	unsigned int xlrError;
	char message[DIFX_MESSAGE_LENGTH];
	const char id[] = "SetProtect";
	int v;

	v = lockStreamstor(D, id, MARK5_LOCK_DONT_WAIT);
	if(v != 0)
	{
		strcpy(message, "Streamstor card in use");

		return 5;	/* too busy */
	}

	xlrRC = XLROpen(1, &xlrDevice);
	D->nXLROpen++;
	if(xlrRC != XLR_SUCCESS)
	{
		xlrError = XLRGetLastError();
		XLRGetErrorMessage(msg, xlrError);
		snprintf(message, DIFX_MESSAGE_LENGTH,
			"ERROR: XLR_setProtect: Cannot open streamstor card.  N=%d Error=%u (%s)\n",
			D->nXLROpen, xlrError, msg);
		Logger_logData(D->log, message);

		unlockStreamstor(D, id);

		return 4;	/* error */
	}

	xlrRC = XLRSelectBank(xlrDevice, D->activeBank);
	if(xlrRC != XLR_SUCCESS)
	{
		xlrError = XLRGetLastError();
		XLRGetErrorMessage(msg, xlrError);
		snprintf(message, DIFX_MESSAGE_LENGTH,
			"ERROR: XLR_setProtect: cannot select bank %d Error=%u (%s)\n",
			D->activeBank, xlrError, msg);
		Logger_logData(D->log, message);

		XLRClose(xlrDevice);
		unlockStreamstor(D, id);

		return 4;	/* error */
	}

	XLRGetDirectory(xlrDevice, &(D->dir_info[D->activeBank]));

	switch(state)
	{
	case PROTECT_ON:
		xlrRC = XLRSetWriteProtect(xlrDevice);
		break;
	case PROTECT_OFF:
		xlrRC = XLRClearWriteProtect(xlrDevice);
		break;
	}

	xlrRC = XLRSelectBank(xlrDevice, D->activeBank);
	if(xlrRC != XLR_SUCCESS)
	{
		xlrError = XLRGetLastError();
		XLRGetErrorMessage(msg, xlrError);
		snprintf(message, DIFX_MESSAGE_LENGTH,
			"ERROR: XLR_setProtect: cannot set protect to %d Error=%u (%s)\n",
			state, xlrError, msg);
		Logger_logData(D->log, message);

		XLRClose(xlrDevice);
		unlockStreamstor(D, id);

		return 4;	/* error */
	}

	XLRClose(xlrDevice);
	unlockStreamstor(D, id);

	return 0;
}

int Mk5Daemon_setProtect(Mk5Daemon *D, enum WriteProtectState state, char *msg)
{
	int v;

	msg[0] = 0;

	/* don't let the process type change while getting vsns */
	pthread_mutex_lock(&D->processLock);

	if(D->process == PROCESS_NONE)
	{
		v = XLR_setProtect(D, state, msg);
		if(v == 0)
		{
			D->bank_stat[D->activeBank].WriteProtected = state;
		}
	}
	else
	{
		v = 5;	/* too busy */
	}

	pthread_mutex_unlock(&D->processLock);

	return v;
}

int Mk5Daemon_error(Mk5Daemon *D, unsigned int *xlrError , char *msg)
{
	int v;

	msg[0] = 0;

	/* don't let the process type change while getting vsns */
	pthread_mutex_lock(&D->processLock);

	if(D->process == PROCESS_NONE)
	{
		v = XLR_error(D, xlrError, msg);
	}
	else
	{
		v = 5;	/* too busy */
	}

	pthread_mutex_unlock(&D->processLock);

	return v;
}
