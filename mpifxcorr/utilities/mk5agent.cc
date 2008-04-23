#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <xlrapi.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <difxmessage.h>

const char program[] = "mk5agent";
const char author[]  = "Walter Brisken <wbrisken@nrao.edu>";
const char version[] = "1.0";


int usage(const char *pgm)
{
	fprintf(stderr, "\n%s ver. %s   %s\n\n",
		program, version, author);
	fprintf(stderr, "A program allow remote queries of Mark5 status\n\n");
	fprintf(stderr, "Usage: mk5agent [<options>]\n\n");

	return 0;
}

int XLR_get_modules(char *vsna, char *vsnb)
{
	SSHANDLE xlrDevice;
	S_BANKSTATUS bank_stat;
	XLR_RETURN_CODE xlrRC;
	
	xlrRC = XLROpen(1, &xlrDevice);
	if(xlrRC != XLR_SUCCESS)
	{
		printf("ERROR Cannot open streamstor card\n");
		return 1;
	}

	xlrRC = XLRSetOption(xlrDevice, SS_OPT_SKIPCHECKDIR);
	if(xlrRC != XLR_SUCCESS)
	{
		printf("ERROR Cannot set SkipCheckDir\n");
		return 1;
	}
	
	xlrRC = XLRGetBankStatus(xlrDevice, BANK_B, &bank_stat);
	if(xlrRC != XLR_SUCCESS)
	{
		vsna[0] = 0;
	}
	else
	{
		strncpy(vsnb, bank_stat.Label, 16);
		vsnb[15] = 0;
		if(vsnb[8] == '/')
		{
			vsnb[8] = 0;
		}
		else
		{
			vsnb[0] = 0;
		}
	}

	xlrRC = XLRGetBankStatus(xlrDevice, BANK_A, &bank_stat);
	if(xlrRC != XLR_SUCCESS)
	{
		vsna[0] = 0;
	}
	else
	{
		strncpy(vsna, bank_stat.Label, 16);
		vsna[15] = 0;
		if(vsna[8] == '/')
		{
			vsna[8] = 0;
		}
		else
		{
			vsna[0] = 0;
		}
	}

	XLRClose(xlrDevice);

	return 0;
}

int Mark5A_get_modules(char *vsna, char *vsnb)
{
	char cmd[] = "echo \"bank_set?\" | tstMark5A";
	char line[512];
	FILE *in;
	int n;

	vsna[0] = vsnb[0] = 0;

	in = popen(cmd, "r");
	if(!in)
	{
		printf("ERROR Cannot run tstMark5A\n");
		return 1;
	}

	n = fread(line, 1, 512, in);
	line[511] = 0;
	fclose(in);

	if(line[48] == 'A' && line[52] != '-')
	{
		strncpy(vsna, line+52, 8);
		vsna[8] = 0;
	}
	if(line[48] == 'B' && line[52] != '-')
	{
		strncpy(vsnb, line+52, 8);
		vsnb[8] = 0;
	}

	if(line[73] == 'A' && line[77] != '-')
	{
		strncpy(vsna, line+77, 8);
		vsna[8] = 0;
	}
	if(line[73] == 'B' && line[77] != '-')
	{
		strncpy(vsnb, line+77, 8);
		vsnb[8] = 0;
	}

	return 0;
}

int running(const char *name)
{
	FILE *in;
	int n;
	char cmd[256];
	char line[512];

	sprintf(cmd, "ps -e | grep %s", name);
	
	in = popen(cmd, "r");
	if(!in)
	{
		printf("ERROR Cannot run ps\n");
		return 1;
	}

	n = fread(line, 1, 512, in);
	line[511] = 0;
	fclose(in);

	if(n > 0)
	{
		return 1;
	}
	else
	{
		return 0;
	}
}

enum Mk5State getvsns(char *vsna, char *vsnb)
{
	char cmd[] = "ps -e | grep Mark5A";
	char line[512];
	FILE *in;
	int n;

	vsna[0] = vsnb[0] = 0;

	if(running("SSErase") ||
	   running("SSReset") ||
	   running("ssopen")  ||
	   running("mpifxcorr"))
	{
		return MARK5_STATE_BUSY;
	}
	
	in = popen(cmd, "r");
	if(!in)
	{
		return MARK5_STATE_ERROR;
	}

	n = fread(line, 1, 512, in);
	line[511] = 0;
	fclose(in);

	if(n > 0)
	{
		n = Mark5A_get_modules(vsna, vsnb);
		if(n == 0)
		{
			return MARK5_STATE_BUSY;
		}
		else
		{
			return MARK5_STATE_ERROR;
		}
	}
	
	n = XLR_get_modules(vsna, vsnb);
	if(n == 0)
	{
		return MARK5_STATE_IDLE;
	}
	else
	{
		return MARK5_STATE_ERROR;
	}
}

int main(int argc, char **argv)
{
	int sock;
	char from[32];
	char message[1000];
	char hostname[100];
	DifxMessageMk5Status dm;
	int n;

	if(argc > 1)
	{
		if(strcmp(argv[1], "-h") == 0 ||
		   strcmp(argv[1], "--help") == 0)
		{
			return usage(argv[0]);
		}
	}

	memset(&dm, 0, sizeof(DifxMessageMk5Status));
	dm.activeBank = ' ';

	difxMessageInit(-1, "mk5agent");
	difxMessagePrint();

	sock = openMultiCastSocket("224.2.2.1", 50201);

	for(;;)
	{
		n = MultiCastReceive(sock, message, 999, from);
		if(n < 0)
		{
			continue;
		}
		if(strncmp(message, "VSN?", 4) == 0)
		{
			dm.state = getvsns(dm.vsnA, dm.vsnB);
			if(dm.vsnA[0] == 0)
			{
				strcpy(dm.vsnA, "none");
			}
			if(dm.vsnB[0] == 0)
			{
				strcpy(dm.vsnB, "none");
			}
			difxMessageSendMark5Status(&dm);
		}
		else if(strncmp(message, "QUIT", 4) == 0)
		{
			break;
		}
	}

	closeMultiCastSocket(sock);

	return 0;
}
