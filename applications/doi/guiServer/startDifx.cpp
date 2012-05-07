//=============================================================================
//
//   ServerSideConnection::startDifx Function
//
//!  Called when an instruction to start a DiFX job is received.  This function
//!  is a member of the ServerSideConnection class.
//!
//!  Much of this code is swiped directly from the Mk5Daemon_startMpifxcorr()
//!  function of mk5daemon, with many thanks to Walter Brisken.
//
//=============================================================================
#include <ServerSideConnection.h>
#include <sys/statvfs.h>

using namespace guiServer;

void
ServerSideConnection::startDifx( DifxMessageGeneric* G ) {
	const int RestartOptionLength = 16;
	int l, n;
	int childPid;
	char filebase[DIFX_MESSAGE_FILENAME_LENGTH];
	char filename[DIFX_MESSAGE_FILENAME_LENGTH];
	char workingDir[DIFX_MESSAGE_FILENAME_LENGTH];
	char destdir[DIFX_MESSAGE_FILENAME_LENGTH];
	char message[DIFX_MESSAGE_LENGTH];
  	char restartOption[RestartOptionLength];
	char command[MAX_COMMAND_SIZE];
	char chmodCommand[MAX_COMMAND_SIZE];
	FILE *out;
	const char *jobName;
	const DifxMessageStart *S;
	bool outputExists = false;
	const char *mpiOptions;
	const char *mpiWrapper;
	const char *difxProgram;
	int returnValue;
	char altDifxProgram[64];
	const char *user;

    //  Cast the body of this message to a DifxMessageStart structure.
	S = &G->body.start;
	
	//  Make sure all needed parameters are included in the message.
	if ( S->headNode[0] == 0 ) {
        ServerSideConnection::diagnostic( ServerSideConnection::ERROR, "DiFX start failed - no headnode specified." );
		return;
	}
	if ( S->nDatastream <= 0 ) {
        ServerSideConnection::diagnostic( ServerSideConnection::ERROR, "DiFX start failed - no data sources specified." );
		return;
	}
	if ( S->nProcess <= 0 ) {
        ServerSideConnection::diagnostic( ServerSideConnection::ERROR, "DiFX start failed - no processing nodes  specified." );
		return;
	}
	if ( S->inputFilename[0] == 0 ) {
        ServerSideConnection::diagnostic( ServerSideConnection::ERROR, "DiFX start failed - no input file specified" );
		return;
	}

	//  Check to make sure the input file exists
	if( access(S->inputFilename, R_OK) != 0 ) {
		ServerSideConnection::diagnostic( ServerSideConnection::ERROR, "DiFX start failed - input file %s not found.", S->inputFilename );
		return;
	}

    //  Make sure the filename can fit in our allocated space for such things.
	if( strlen( S->inputFilename ) + 12 > DIFX_MESSAGE_FILENAME_LENGTH ) {
		ServerSideConnection::diagnostic( ServerSideConnection::ERROR, "Filename %s is too long.", S->inputFilename );
		return;
	}
	
    //  Find the "working directory" (where the .input file resides and data will be put), the
    //  "filebase" (the path of the input file with ".input") and the "job name" (the name of
    //  the input file without .input or directory path).
	strcpy( workingDir, S->inputFilename );
    l = strlen( workingDir );
    for( int i = l-1; i > 0; i-- ) {
        if( workingDir[i] == '/') {
    		workingDir[i] = 0;
    		break;
    	}
    }	
	strcpy( filebase, S->inputFilename );
	l = strlen( filebase );
	for( int i = l-1; i > 0; i-- ) {
		if( filebase[i] == '.' ) {
			filebase[i] = 0;
			break;
		}
	}
	jobName = filebase;
	for( int i = 0; filebase[i]; ++i ) {
		if( filebase[i] == '/' ) {
			jobName = filebase + i + 1;
		}
	}

	//  Make sure the working directory exists and has enough free space
	struct statvfs fiData;
	int v = statvfs( workingDir, &fiData );
	if( v == 0 ) {
		long long freeSpace;
		freeSpace = fiData.f_bsize * fiData.f_bavail;
		if( freeSpace < 100000000 ) {
			ServerSideConnection::diagnostic( ServerSideConnection::WARNING, 
			    "Working directory %s has only %lld bytes free - mpifxcorr will likely crash!", 
				workingDir, freeSpace );
		}
		else if( fiData.f_ffree < 3 )
		{
			ServerSideConnection::diagnostic( ServerSideConnection::WARNING, "%s has no free inodes - mpifxcorr will likely crash!", 
				workingDir );
		}
	}
	else {
		ServerSideConnection::diagnostic( ServerSideConnection::ERROR, 
		    "statvfs failed when accessing directory %s : it seems not to exist!", 
			workingDir );
		return;
	}
	
	//  See if the output file already exists.
	snprintf( filename, DIFX_MESSAGE_FILENAME_LENGTH, "%s.difx", filebase );
	if( access( filename, F_OK ) == 0 )
	    outputExists = true;
    //  Should we be over writing it?  If not, we need to bail out.
	if( outputExists && !S->force ) {
		ServerSideConnection::diagnostic( ServerSideConnection::ERROR, 
		    "Output file %s exists.  Aborting correlation.", 
			filename );
		return;
	}

    //  Enough checking - let's go.  
	fprintf( stdout, "headnode is %s\n", S->headNode );
	for( int i = 0; i < S->nDatastream; ++i ) {
		fprintf( stdout, "data stream node %s\n", S->datastreamNode[i] );
	}
	for( int i = 0; i < S->nProcess; ++i ) {
		fprintf( stdout, "process node %s\n", S->processNode[i] );
	}

    //  Write the machines file.
	snprintf(filename, DIFX_MESSAGE_FILENAME_LENGTH, "%s.machines", filebase);
    out = fopen( filename, "w" );
	if( !out ) {
		ServerSideConnection::diagnostic( ServerSideConnection::ERROR, 
		    "Cannot open machines file \"%s\" for writing", filename );
		return;
	}
	//  The "head" or "manager" node is always first.
    fprintf(out, "%s\n", S->headNode);
    //  Then the data source machines.
	for( int i = 0; i < S->nDatastream; ++i )
		fprintf( out, "%s\n", S->datastreamNode[i] );
    //  Finally, the processing machines.  Only include those which include at least
    //  one processing thread - for the head node this must be an "extra" thread.
	for( int i = 0; i < S->nProcess; ++i )
	    if ( !strcmp( S->processNode[i], S->headNode ) ) {
	        if ( S->nThread[i] > 1 )
	            fprintf( out, "%s\n", S->processNode[i] );
	    }
	    else {
	        if ( S->nThread[i] > 0 )
		        fprintf( out, "%s\n", S->processNode[i] );
		}
    fclose( out );

	//  Write the threads file.
	snprintf(filename, DIFX_MESSAGE_FILENAME_LENGTH, "%s.threads", filebase);
	out = fopen( filename, "w" );
	if( !out ) {
		ServerSideConnection::diagnostic( ServerSideConnection::ERROR, 
		    "Cannot open threads file \"%s\" for writing", filename );
		return;
	}
	fprintf(out, "NUMBER OF CORES:    %d\n", S->nProcess );
	//  Tally the total number of processing threads - make sure there is at
	//  least one!
	int threadCount = 0;
	for( int i = 0; i < S->nProcess; ++i ) {
	    //  The head node reserves one thread for its management duties.
	    if ( !strcmp( S->processNode[i], S->headNode ) ) {
	        if ( S->nThread[i] > 1 ) {
	            fprintf( out, "%d\n", S->nThread[i] - 1 );
	            threadCount += S->nThread[i] - 1;
	        }
	    }
		else {
	        if ( S->nThread[i] > 0 ) {
	            fprintf( out, "%d\n", S->nThread[i] );
	            threadCount += S->nThread[i];
	        }
		}
	}
	fclose(out);
	
	//  There must be at least one thread to run!
	if ( !threadCount ) {
	    ServerSideConnection::diagnostic( ServerSideConnection::ERROR,
	        "No threads available for processing this job." );
	    return;
	}
	
	

}
