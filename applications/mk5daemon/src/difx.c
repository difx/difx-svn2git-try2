#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include "mk5daemon.h"

const char defaultMpiOptions[] = "--mca btl ^udapl,openib --mca mpi_yield_when_idle 1";
const char defaultDifxProgram[] = "mpifxcorr";

typedef struct
{
	char hostname[DIFX_MESSAGE_PARAM_LENGTH];
	int n;
} Uses;

static int addUse(Uses *U, const char *hostname)
{
	int i;

	for(i = 0; U[i].n; i++)
	{
		if(strcmp(hostname, U[i].hostname) == 0)
		{
			U[i].n++;
			return i;
		}
	}

	strcpy(U[i].hostname, hostname);
	U[i].n = 1;

	return i;
}

int getUse(const Uses *U, const char *hostname)
{
	int i;

	for(i = 0; U[i].n; i++)
	{
		if(strcmp(hostname, U[i].hostname) == 0)
		{
			return U[i].n;
		}
	}

	return 0;
}

void Mk5Daemon_startMpifxcorr(Mk5Daemon *D, const DifxMessageGeneric *G)
{
	int i, l, n;
	char filebase[DIFX_MESSAGE_FILENAME_LENGTH];
	char filename[DIFX_MESSAGE_FILENAME_LENGTH];
	char message[512];
	char command[512];
	FILE *out;
	Uses *uses;
	const char *jobName;
	const DifxMessageStart *S;

	S = &G->body.start;

	if(G->nTo != 1)
	{
		return;
	}

	if(strcmp(G->to[0], D->hostName) != 0)
	{
		return;
	}

	if(S->headNode[0] == 0 || S->nDatastream <= 0 || S->nProcess <= 0 || S->inputFilename[0] != '/')
	{
		difxMessageSendDifxAlert("Malformed DifxStart message received", DIFX_ALERT_LEVEL_ERROR);
		Logger_logData(D->log, "Mk5Daemon_startMpifxcorr: degenerate request\n");
		return;
	}

	if(!D->isHeadNode)
	{
		difxMessageSendDifxAlert("Attempt to start job from non head node", DIFX_ALERT_LEVEL_ERROR);
		Logger_logData(D->log, "Mk5Daemon_startMpifxcorr: I am not a head node\n");
		return;
	}

	/* generate filebase */
	strcpy(filebase, S->inputFilename);
	l = strlen(filebase);
	for(i = l-1; i > 0; i--)
	{
		if(filebase[i] == '.')
		{
			filebase[i] = 0;
			break;
		}
	}
	jobName = filebase;
	for(i = 0; filebase[i]; i++)
	{
		if(filebase[i] == '/')
		{
			jobName = filebase + i + 1;
		}
	}

	if(access(S->inputFilename, F_OK) != 0)
	{
		sprintf(message, "Input file %s does not exist.  Aborting correlation.", S->inputFilename);
		difxMessageSendDifxAlert(message, DIFX_ALERT_LEVEL_ERROR);
		sprintf(message, "Mk5Daemon_startMpifxcorr: input file %s does not exist\n", filename);
		Logger_logData(D->log, message);
		return;
	}

	sprintf(filename, "%s.difx", filebase);
	if(access(filename, F_OK) == 0)
	{
		sprintf(message, "Output file %s exists.  Aborting correlation.", filename);
		difxMessageSendDifxAlert(message, DIFX_ALERT_LEVEL_ERROR);
		sprintf(message, "Mk5Daemon_startMpifxcorr: output file %s exists\n", filename);
		Logger_logData(D->log, message);
		return;
	}

	/* lock state.  Make sure to unlock if early return happens! */
	pthread_mutex_lock(&D->processLock);

	/* determine usage of each node */
	uses = (Uses *)calloc(1 + S->nProcess + S->nDatastream, sizeof(Uses));
	addUse(uses, S->headNode);
	for(i = 0; i < S->nProcess; i++)
	{
		addUse(uses, S->processNode[i]);
	}
	for(i = 0; i < S->nDatastream; i++)
	{
		addUse(uses, S->datastreamNode[i]);
	}

	/* write machines file */
	sprintf(filename, "%s.machines", filebase);
	out = fopen(filename, "w");
	if(!out)
	{
		sprintf(message, "Cannot open %s for write", filename);
		difxMessageSendDifxAlert(message, DIFX_ALERT_LEVEL_ERROR);
		sprintf(message, "Mk5Daemon_startMpifxcorr: cannot open %s for write\n", filename);
		Logger_logData(D->log, message);
		pthread_mutex_unlock(&D->processLock);
		free(uses);
		return;
	}

	fprintf(out, "%s slots=1 max-slots=%d\n", S->headNode, getUse(uses, S->headNode));
	for(i = 0; i < S->nDatastream; i++)
	{
		n = getUse(uses, S->datastreamNode[i]);
		fprintf(out, "%s slots=1 max-slots=%d\n", S->datastreamNode[i], n);
	}
	for(i = 0; i < S->nProcess; i++)
	{
		n = getUse(uses, S->processNode[i]);
		fprintf(out, "%s slots=1 max-slots=%d\n", S->processNode[i], n);
	}

	fclose(out);

	/* write threads file */
	sprintf(filename, "%s.threads", filebase);
	out = fopen(filename, "w");
	if(!out)
	{
		sprintf(message, "Cannot open %s for write", filename);
		difxMessageSendDifxAlert(message, DIFX_ALERT_LEVEL_ERROR);
		sprintf(message, "Mk5Daemon_startMpifxcorr: cannot open %s for write\n", filename);
		Logger_logData(D->log, message);
		pthread_mutex_unlock(&D->processLock);
		free(uses);
		return;
	}

	fprintf(out, "NUMBER OF CORES:    %d\n", S->nProcess);

	for(i = 0; i < S->nProcess; i++)
	{
		n = S->nThread[i] - getUse(uses, S->processNode[i]) + 1;
		if(n <= 0)
		{
			n = 1;
		}
		fprintf(out, "%d\n", n);
	}

	fclose(out);

	/* Don't need usage info anymore */
	free(uses);

	pthread_mutex_unlock(&D->processLock);

	if(fork() == 0)
	{
		const char *mpiOptions;
		const char *difxProgram;

		if(S->mpiOptions[0])
		{
			mpiOptions = S->mpiOptions;
		}
		else
		{
			mpiOptions = defaultMpiOptions;
		}

		if(S->difxProgram[0])
		{
			difxProgram = S->difxProgram;
		}
		else
		{
			difxProgram = defaultDifxProgram;
		}

		difxMessageSendDifxAlert("mpifxcorr spawning process", DIFX_ALERT_LEVEL_INFO);
		
		sprintf(command, "ssh -l difx %s mpirun -np %d --bynode --hostfile %s.machines %s %s %s.input", 
			S->headNode,
			1 + S->nDatastream + S->nProcess,
			filebase,
			mpiOptions,
			difxProgram,
			filebase);
		
		sprintf(message, "Executing: %s", command);
		difxMessageSendDifxAlert(message, DIFX_ALERT_LEVEL_INFO);

		sprintf(message, "Spawning %d processes", 1 + S->nDatastream + S->nProcess);
		difxMessageSendDifxStatus2(jobName, DIFX_STATE_SPAWNING, message);
		system(command);
		difxMessageSendDifxStatus2(jobName, DIFX_STATE_MPIDONE, "");

		difxMessageSendDifxAlert("mpifxcorr process done", DIFX_ALERT_LEVEL_INFO);
		exit(0);
	}
}
