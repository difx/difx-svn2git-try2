//=============================================================================
//
//   ServerSideConnection::getDirectory Function (and associated functions)
//
//!  Called when an instruction to either obtain an existing VSN directory
//!  or generate a new one.
//
//=============================================================================
#include <ServerSideConnection.h>
#include <sys/statvfs.h>
#include <network/TCPClient.h>
#include <JobMonitorConnection.h>
#include <signal.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <dirent.h>
#include <set>
#include <vector>
#include <fcntl.h>
#include <ExecuteSystem.h>

using namespace guiServer;

//-----------------------------------------------------------------------------
//!  Called in response to a user request for the contents of a VSN directory
//!  OR a request for to create that contents.  A forked process opens a two-way
//!  TCP connection to the source of the request.  Data are transfered over this
//!  connection and a limited command protocol is provided.
//-----------------------------------------------------------------------------
void ServerSideConnection::getDirectory( DifxMessageGeneric* G ) {
	const DifxMessageGetDirectory *S;
	char message[DIFX_MESSAGE_LENGTH];
	char command[MAX_COMMAND_SIZE];
	pid_t childPid;
    char mark5[DIFX_MESSAGE_PARAM_LENGTH];
    char vsn[DIFX_MESSAGE_PARAM_LENGTH];
    char address[DIFX_MESSAGE_PARAM_LENGTH];
    int port;
    int generateNow;
	
	//  Cast the message to a GetDirectory message, then make a copy of all
	//  parameters included.  We are entering a fork and we can't depend on the
	//  structure not being over-written.
	S = &G->body.getDirectory;
	strncpy( mark5, S.mark5, DIFX_MESSAGE_PARAM_LENGTH );
	strncpy( vsn, S.vsn, DIFX_MESSAGE_PARAM_LENGTH );
	strncpy( address, S.address, DIFX_MESSAGE_PARAM_LENGTH );
	port = S.port;
	generateNow = S.generateNow;
	
	//  Fork a process to do everything from here.
	signal( SIGCHLD, SIG_IGN );
	childPid = fork();
	if( childPid == 0 ) {
	    //  Open a TCP socket connection to the server that should be running for us on the
	    //  remote host.
	    int sockfd;
	    struct sockaddr_in servaddr;
	    sockfd = socket( AF_INET, SOCK_STREAM, 0 );
	    bzero( &servaddr, sizeof( servaddr ) );
	    servaddr.sin_family = AF_INET;
	    servaddr.sin_port = htons( port );
	    inet_pton( AF_INET, address, &servaddr.sin_addr );
  	    int filesize = connect( sockfd, (const sockaddr*)&servaddr, sizeof( servaddr ) );
  	    
  	    //  Assuming the socket connection was successful, write the file contents to the socket.
  	    if ( filesize == 0 ) {
        	//snprintf( message, DIFX_MESSAGE_LENGTH, "Client address: %s   port: %d - connection looks good", S->address, S->port );
        	//printf( "%s\n", message );
        	//difxMessageSendDifxAlert( message, DIFX_ALERT_LEVEL_WARNING );
        	//  Get the number of bytes we expect.
        	int n = 0;
        	fd_set rfds;
            struct timeval tv;
        	int rn = 0;
        	int trySec = 5;
        	while ( trySec > 0 && rn < (int)(sizeof( int )) ) {
        	    FD_ZERO(&rfds);
                FD_SET( sockfd, &rfds );
                tv.tv_sec = 1;
                tv.tv_usec = 0;
                int rtn = select( sockfd + 1, &rfds, NULL, NULL, &tv );
                if ( rtn >= 0 ) {
        	        int readRtn = read( sockfd, (unsigned char*)(&n) + rn, sizeof( int ) - rn );
        	        if ( readRtn > 0 )
        	            rn += readRtn;
        	    }
        	    else if ( rtn < 0 ) {
        	        snprintf( message, DIFX_MESSAGE_LENGTH, "Select error (%s) %s port: %d - transfer FAILED", strerror( errno ), S->address, S->port );
        	        difxMessageSendDifxAlert( message, DIFX_ALERT_LEVEL_ERROR );
        	    }
        	    --trySec;
        	}
        	filesize = ntohl( n );
        	//  Then read the data.
        	int ret = 0;
        	int count = 0;
        	short blockSize = 1024;
        	char blockData[blockSize + 1];
	        const int tmpFileSize = 100;
        	char tmpFile[tmpFileSize];
        	snprintf( tmpFile, tmpFileSize, "/tmp/filetransfer_%d", S->port );
        	FILE *fp = fopen( tmpFile, "w" );
        	int rtn = 0;
        	while ( count < filesize && rtn != -1 ) {
        	    int readn = blockSize;
        	    if ( filesize - count < readn )
        	        readn = filesize - count;
        	    FD_ZERO(&rfds);
                FD_SET( sockfd, &rfds );
                tv.tv_sec = 1;
                tv.tv_usec = 0;
                rtn = select( sockfd + 1, &rfds, NULL, NULL, &tv );
                if ( rtn != -1 ) {
        	        ret = read( sockfd, blockData, readn );
        	        if ( ret > 0 ) {
        	            count += ret;
        	            blockData[ret] = 0;
        	            fprintf( fp, "%s", blockData );
        	        }
                }
        	    else {
        	        snprintf( message, DIFX_MESSAGE_LENGTH, "Select error (%s) %s port: %d - transfer FAILED", strerror( errno ), S->address, S->port );
        	        difxMessageSendDifxAlert( message, DIFX_ALERT_LEVEL_ERROR );
        	    }
        	}
        	fclose( fp );
        	       
  	    }
  	    else {
        	snprintf( message, DIFX_MESSAGE_LENGTH, "Client address: %s   port: %d - transfer FAILED", S->address, S->port );
        	difxMessageSendDifxAlert( message, DIFX_ALERT_LEVEL_ERROR );
  	    }

	    //  Check the destination filename
    	if( S->destination[0] != '/' )  {
    		filesize = -1;
    	}
    	else {
    	    //  Check the existence of the destination directory
    	    char path[DIFX_MESSAGE_FILENAME_LENGTH];
    	    snprintf( path, DIFX_MESSAGE_FILENAME_LENGTH, "%s", S->destination );
    	    int i = strlen( path );
    	    while ( i > 0 && path[i] != '/' ) --i;
    	    path[i] = 0;
	        struct stat stt;
	        int ret = stat( path, &stt );
	        if ( ret == -1 ) {
	            //  Either we aren't allowed to view this directory
	            if ( errno == EACCES )
	                filesize = -3;
	            //  Or it doesn't exist at all
	            else
	                filesize = -2;
	        }
	        //  Make sure the destination is a directory
	        else if ( !(stt.st_mode & S_IFDIR) ) {
	            filesize = -5;
	        }
	        else {
	            //  Check write permissions and uid for the difx user
	            struct passwd *pwd = getpwnam( user );
	            if ( pwd == NULL ) {
	                snprintf( message, DIFX_MESSAGE_LENGTH, "DiFX username %s is not valid", user );
	                difxMessageSendDifxAlert( message, DIFX_ALERT_LEVEL_ERROR );
	                filesize = -4;
	            }
	            else {
	                //  Make sure the DiFX user has write permission in the destination directory (via owner, group, or world).
	                if ( ( stt.st_uid == pwd->pw_uid && stt.st_mode & S_IRUSR ) || ( stt.st_gid == pwd->pw_gid && stt.st_mode & S_IRGRP ) ||
	                     ( stt.st_mode & S_IROTH ) ) {
                  		//  Change permissions on the temporary file so the DiFX user can read it.
                		//snprintf( command, MAX_COMMAND_SIZE, "chmod 644 /tmp/filetransfer_%d", S->port );
                        //Mk5Daemon_system( D, command, 1 );
  		
                        //  Copy the new file to its specified location (as the DiFX user).
                		snprintf( command, MAX_COMMAND_SIZE, "cp /tmp/filetransfer_%d %s", 
                				 S->port,
                				 S->destination );
                  		system( command );
              		}
 	                //  Otherwise, we can't read it.
	                else
	                    filesize = -3;
	            }
	        }
	    }

        //printf( "sending %d as confirmation\n", filesize );
        int n = htonl( filesize );            	
        write( sockfd, &n, sizeof( int ) );

  		//  Then clean up our litter.
		snprintf( command, MAX_COMMAND_SIZE, "rm -f /tmp/filetransfer_%d", S->port );
        system( command );
        close( sockfd );
  		
		exit(EXIT_SUCCESS);
	}
	    
}

