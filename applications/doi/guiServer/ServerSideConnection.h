#ifndef GUISERVER_SERVERSIDECONNECTION_H
#define GUISERVER_SERVERSIDECONNECTION_H
//=============================================================================
//
//   guiServer::ServerSideConnection Class
//
//!  Handles a single connection to the GUI server.  The connection is
//!  bi-directional, so there is a receive thread that monitors incoming data
//!  and write functions.  The PacketExchange class is inherited.
//!
//!  Much of the DiFX interprocess communication occurs by multicasts.  This
//!  class can monitor that traffic and be set to "relay" it to the GUI client.
//!  It can also send commands and other communications to other DiFX processes
//!  using the multicast network.  
//
//=============================================================================
#include <stdlib.h>
#include <pthread.h>
#include <string>
#include <network/UDPSocket.h>
#include <PacketExchange.h>
#include <difxmessage.h>
#include <JobMonitorConnection.h>

namespace guiServer {

    class ServerSideConnection : public PacketExchange {
    
        static const int MAX_COMMAND_SIZE = 1024;
        
    public:

        ServerSideConnection( network::GenericSocket* sock, const char* clientIP ) : PacketExchange( sock ) {
            _commandSocket = NULL;
            _monitorSocket = NULL;
            _multicastGroup[0] = 0;
            _multicastPort = 0;
            _newMulticastSettings = true;            
            _difxAlertsOn = true;
            _relayDifxMulticasts = false;
            snprintf( _clientIP, 16, "%s", clientIP );
        }
        
        ~ServerSideConnection() {
            if ( _monitorSocket != NULL ) {
                _monitorSocket->closeFd();
                delete _monitorSocket;
            }
            if ( _commandSocket != NULL ) {
                _commandSocket->closeFd();
                delete _commandSocket;
            }
            _monitorSocket = NULL;
            _commandSocket = NULL;
        }
        
        //---------------------------------------------------------------------
        //!  Start a thread to collect DiFX multicast messages using the current
        //!  multicast group and port settings.
        //---------------------------------------------------------------------
        void startMulticastMonitor() {
            //  Shut down the existing monitor if there is one.
            if ( _monitorSocket != NULL ) {
                _monitorSocket->closeFd();
                sleep( 1 );  //  this seems a bit long!
            }
            _monitorSocket = new network::UDPSocket( network::UDPSocket::RECEIVE, _multicastGroup, _multicastPort );
            _monitorSocket->ignoreOwn( false );
            //  Start the thread that reads the multicast messages.
            pthread_attr_init( &_monitorAttr );
            pthread_create( &_monitorId, &_monitorAttr, staticDifxMonitor, this );      
        }
        
        //---------------------------------------------------------------------
        //!  Static thread start function (for the multicast monitor).
        //---------------------------------------------------------------------
        static void* staticDifxMonitor( void* a ) {
            ( (ServerSideConnection*)a )->difxMonitor();
        }
        
        static const int MAX_MESSAGE_LENGTH = 10 * 1024;
        
        //---------------------------------------------------------------------
        //!  This is the function that actually monitors DiFX multicasts - it
        //!  runs as a continuous thread until the socket it closed or returns
        //!  an error.
        //---------------------------------------------------------------------
        void difxMonitor() {
            while ( _monitorSocket != NULL ) {
                char message[MAX_MESSAGE_LENGTH + 1];
                int ret = _monitorSocket->reader( message, MAX_MESSAGE_LENGTH );
                if ( ret == -1 ) {
                    delete _monitorSocket;
                    _monitorSocket = NULL;
                }
                else {
                    //  Decide what to do with this packet.
                    if ( _relayDifxMulticasts )
                        sendPacket( RELAY_PACKET, message, ret );
                }
            }
        }

        //---------------------------------------------------------------------
        //!  Override the relay function.  The relay command starts with a
        //!  single integer of data telling us to turn on (1) or off (0) the
        //!  relaying of DiFX multicast messages to the client.
        //---------------------------------------------------------------------
        virtual void relay( char* data, const int nBytes ) {
            relayDifxMulticasts( ntohl( *(int*)data ) );
        }
        
        //---------------------------------------------------------------------
        //!  This is a relayed command from the client - these are converted to UDP
        //!  messages that the DiFX processes (mk5daemon, etc) can pick up.
        //---------------------------------------------------------------------
        virtual void relayCommand( char* data, const int nBytes ) {
            //  Close the old command socket if we have new settings for the multicast network.
            if ( _newMulticastSettings ) {
                if ( _commandSocket != NULL ) {
                    _commandSocket->closeFd();
                    _commandSocket = NULL;
                }
                _newMulticastSettings = false;
            }
            //  Create a new command socket if we don't have one.
            if ( _commandSocket == NULL ) {
                if ( _multicastPort == 0 ) {
                    fprintf( stderr, "Cannot start command socket - multicast port is not defined\n" );
                    return;
                }
                if ( _multicastGroup == NULL ) {
                    fprintf( stderr, "Cannot start command socket - multicast group is not defined\n" );
                    return;
                }
                _commandSocket = new network::UDPSocket( network::UDPSocket::MULTICAST, _multicastGroup, _multicastPort );
            }
            //  Send this message to the socket (assuming its good)
            if ( _commandSocket != NULL ) {
                _commandSocket->writer( data, nBytes );
            }
        }

        //---------------------------------------------------------------------
        //!  This is a command from the client that guiServer should try to
        //!  execute on its own.  It uses the same XML format as relayed
        //!  commands.
        //---------------------------------------------------------------------
        virtual void command( char* data, const int nBytes ) {
            //  Use the DiFX message parser on the XML this message presumably
            //  contains.
            char message[DIFX_MESSAGE_LENGTH];
            strncpy( message, data, nBytes );
            message[ nBytes ] = 0;
            DifxMessageGeneric G;
            if ( !difxMessageParse( &G, message ) ) {
                switch( G.type ) {
                case DIFX_MESSAGE_START:
                    startDifx( &G );
                    break;
                default:
                    diagnostic( WARNING, "Received command message type %d - don't know what this is....\n", G.type );
                }
            }
            else {
                ServerSideConnection::diagnostic( ServerSideConnection::WARNING, 
                    "The guiServer DiFX parser received a command it could not parse." );
            }
        }
        
        //---------------------------------------------------------------------
        //!  This is a packet from the client containing the port number and
        //!  group IP of the DiFX multicasts.  It will trigger a change in the
        //!  multicast monitor as well as where relayed commands go.
        //---------------------------------------------------------------------
        virtual void multicastSettings( char* data, const int nBytes ) {
            //  The first line of the string contains the group IP.  Look for the
            //  terminating newline character.
            int i = 0;
            char group[16];
            while ( data[i] != '\n' ) {
                group[i] = data[i];
                ++i;
            }
            group[i] = 0;
            //  The next line contains the new port setting as a string.
            int port = atoi( data + i + 1 );
            multicast( group, port );
        }
        
        //---------------------------------------------------------------------
        //!  Sets the group IP and port number for DiFX multicasts.  This
        //!  changes the destination for any message this class sends on the
        //!  network and restarts the monitor.
        //---------------------------------------------------------------------
        void multicast( char* group, const int port ) {
            snprintf( _multicastGroup, 16, "%s", group );
            _multicastPort = port;
            _newMulticastSettings = true;
            startMulticastMonitor();
        }
        
        //---------------------------------------------------------------------
        //!  Types of messages - used as the "severity" in calls to the diagnostic()
        //!  function.
        //---------------------------------------------------------------------
        static const int INFORMATION      = 0;
        static const int WARNING          = 1;
        static const int ERROR            = 2;
        
        //---------------------------------------------------------------------
        //!  Toggle "DiFX alert" messages - these are sent as multicast UDP
        //!  messages and should appear in the message window of the GUI.
        //---------------------------------------------------------------------
        void difxAlertsOn( const bool newVal ) { _difxAlertsOn = newVal; }
        
        //---------------------------------------------------------------------
        //!  Toggle diagnostic packet messages - these are sent as TCP packets
        //!  directly to the GUI.
        //---------------------------------------------------------------------
        void diagnosticPacketsOn( const bool newVal ) { _diagnosticPacketsOn = newVal; }
        
        //---------------------------------------------------------------------
        //!  Relay all collected DiFX multicast messages back to the GUI via
        //!  the TCP connection.
        //---------------------------------------------------------------------
        void relayDifxMulticasts( const bool newVal ) { _relayDifxMulticasts = newVal; }
        
        //---------------------------------------------------------------------
        //!  Structure used to pass information to the thread that starts and
        //!  monitors DiFX.
        //---------------------------------------------------------------------
        struct DifxStartInfo {
            ServerSideConnection* ssc;
            JobMonitorConnection* jmc;
            int force;
            int sockFd;
            char removeCommand[MAX_COMMAND_SIZE];
            char startCommand[MAX_COMMAND_SIZE];
            char jobName[MAX_COMMAND_SIZE];
        };

        //-----------------------------------------------------------------------------
        //!  Static function called to start the DiFX run thread.
        //-----------------------------------------------------------------------------	
        static void* staticRunDifxThread( void* a ) {
            DifxStartInfo* startInfo = (DifxStartInfo*)a;
            startInfo->ssc->runDifxThread( startInfo );
            printf( "thread is done - delete the startInfo structure\n" );
            delete startInfo;
        }

        //---------------------------------------------------------------------
        //!  Function prototypes - guts contained in [FUNCTON_NAME].cpp files
        //!  unless otherwise noted.
        //---------------------------------------------------------------------
        void startDifx( DifxMessageGeneric* G );
        void runDifxThread( DifxStartInfo* startInfo );  //  in startDifx.cpp
        void diagnostic( const int severity, const char *fmt, ... );

    protected:
    
        network::UDPSocket* _monitorSocket;
        network::UDPSocket* _commandSocket;
        pthread_attr_t _monitorAttr;
        pthread_t _monitorId;
        bool _relayDifxMulticasts;
        bool _difxAlertsOn;
        bool _diagnosticPacketsOn;
        bool _newMulticastSettings;
        char _multicastGroup[16];
        int _multicastPort;
        char _clientIP[16];
        
    };

}

#endif
