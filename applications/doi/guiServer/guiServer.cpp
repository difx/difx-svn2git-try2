//=============================================================================
//
//   guiServer (main)
//
//!  Collect and monitor connections from clients (presumably the GUI).  The
//!  ServerSideConnection class does most of the work.
//
//=============================================================================
#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include <vector>
#include <pthread.h>
#include <network/TCPServer.h>
#include <ServerSideConnection.h>

//  Structure for holding information about client connections.
struct ClientConnection {
    char IP[16];
    guiServer::ServerSideConnection* client;
    network::TCPSocket* clientSocket;
};

//  This vector holds a list of client connections that are active.
std::vector<ClientConnection*> _clientConnections;

//  This is a mutex to lock the list of client connections (since two threads mess
//  around with it).
pthread_mutex_t _clientConnectionsMutex;

//-----------------------------------------------------------------------------
//!  Thread to monitor the list of client connections.
//-----------------------------------------------------------------------------
void* connectionMonitor( void* arg ) {
    while ( 1 ) {
        //  Examine the list of client connections to make sure all are still
        //  connected.
        pthread_mutex_lock( &_clientConnectionsMutex );
        for ( std::vector<ClientConnection*>::iterator it = _clientConnections.begin();
              it != _clientConnections.end(); ) {
            ClientConnection* thisConnection = (ClientConnection*)*it;
            //  Remove the connection from the list if it has been broken.
            if ( !thisConnection->clientSocket->connected() ) {
                printf( "%s disconnected\n", thisConnection->IP );
                it = _clientConnections.erase( it );
                delete thisConnection;
            }
            else
                ++it;
        }
        pthread_mutex_unlock( &_clientConnectionsMutex );
        sleep( 1 );
    }
}
    
//-----------------------------------------------------------------------------
//!  main
//-----------------------------------------------------------------------------
main( int argc, char **argv ) {

    char* endptr;
    
    //  Configurables, with some sensible defaults.
    int serverPort = 50200;         //  Port for TCP connections
    bool relayDifx = false;         //  Act as a "relay" for DiFX broadcast messages
    
    //  Grab values from environment variables, since everyone seems to like environment
    //  variables.
    char* newPort = getenv( "DIFX_MESSAGE_PORT" );
    if ( newPort != NULL )
        serverPort = atoi( newPort );
    
    //  Command line arguments
    for ( int i = 1; i < argc; ++i ) {
        //  Command line args are null terminated so I can use strcmp...
        if ( !strcmp( argv[i], "-help" ) || !strcmp( argv[i], "-h" ) ) {
        }
        else if ( !strcmp( argv[i], "--relay" ) || !strcmp( argv[i], "-r" ) ) {
            relayDifx = !relayDifx;
        }
        else {
            //  Try to interpret the final argument as the server port number
            bool allNum = true;
            char* newNum = argv[i];
            for ( int j = 0; j < strlen( newNum ); ++j ) {
                if ( !isdigit( newNum[j] ) )
                    allNum = false;
            }
            if ( allNum )
                serverPort = atoi( newNum );
        }
    }
    
    //  The TCP server for all connections.
    network::TCPServer* server = new network::TCPServer( serverPort );
    if ( !server->serverUp() )
        exit( EXIT_FAILURE );
    printf( "server at port %d\n", serverPort );
    
    //  Initialize the list of client connections.
    _clientConnections.clear();
    pthread_mutex_init( &_clientConnectionsMutex, NULL );
    
    //  Start the thread that monitors client connections.
    pthread_attr_t _monitorAttr;
    pthread_t _monitorId;
    pthread_attr_init( &_monitorAttr );
    pthread_create( &_monitorId, &_monitorAttr, connectionMonitor, NULL );               
    
    //  Enter an endless loop looking for client connections at the server port.
    while ( 1 ) {

        //  Wait for the next client to connect and spawn a new socket to cover it.
        network::TCPSocket* newClientSocket = server->acceptClient();
        printf( "client connection from address %s\n", server->lastClientIP() );

        //  Open a packet exchange mechanism to deal with this connection as a server.
        guiServer::ServerSideConnection* newClient = new guiServer::ServerSideConnection( newClientSocket, relayDifx );
        
        //  Save information about the new client.
        ClientConnection* newClientConnection = new ClientConnection;
        newClientConnection->clientSocket = newClientSocket;
        newClientConnection->client = newClient;
        sprintf( newClientConnection->IP, "%s", server->lastClientIP() );
        
        //  Put the information in our list of clients, which is mutex locked.
        pthread_mutex_lock( &_clientConnectionsMutex );
        _clientConnections.push_back( newClientConnection );
        pthread_mutex_unlock( &_clientConnectionsMutex );

    }
    
    delete server;

}
