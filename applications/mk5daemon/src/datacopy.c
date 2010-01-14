#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include "mk5daemon.h"

struct mk5cpParams
{
	Mk5Daemon *D;
	char options[512];
};

static void *mk5cpRun(void *ptr)
{
	struct mk5cpParams *params;
	char cmd[640];

	params = (struct mk5cpParams *)ptr;

	Logger_logData(params->D->log, "mk5cp starting\n");

	sprintf(cmd, "su -l difx -c 'mk5cp %s'", params->options);
	system(cmd);

	Logger_logData(params->D->log, "mk5cp done\n");

	params->D->processDone = 1;

	pthread_exit(0);

	free(params);

	return 0;
}

static void makedir(Mk5Daemon *D, const char *options)
{
	char dir[256];
	int a=-1;
	int i, l;
	char cmd[768], message[1024];

	/* look for a / character and assume that is the output directory */
	for(i = 0; options[i]; i++)
	{
		if(a == -1)
		{
			if(options[i] == '/')
			a = i;
		}
		else if(options[i] <= ' ')
		{
			break;
		}
	}

	l = i-a;
	strncpy(dir, options+a, l);
	dir[l] = 0;

	sprintf(cmd, "mkdir -m 777 -p %s", dir);
	sprintf(message, "Executing: %s\n", cmd);
	Logger_logData(D->log, message);
	system(cmd);
}

void Mk5Daemon_startMk5Copy(Mk5Daemon *D, const char *options)
{
	struct mk5cpParams *P;

	P = (struct mk5cpParams *)calloc(1, sizeof(struct mk5cpParams));

	if(!D->isMk5)
	{
		return;
	}

	/* Make sure output directory exists and has permissions */
	makedir(D, options);

	pthread_mutex_lock(&D->processLock);

	if(D->process == PROCESS_NONE)
	{
		D->processDone = 0;
		D->process = PROCESS_MK5COPY;

		P->D = D;
		strcpy(P->options, options);
		pthread_create(&D->processThread, 0, &mk5cpRun, P);
	}

	pthread_mutex_unlock(&D->processLock);
}

void Mk5Daemon_stopMk5Copy(Mk5Daemon *D)
{
	system("killall -HUP mk5cp");
}
