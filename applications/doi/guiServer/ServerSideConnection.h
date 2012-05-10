#ifndef GUISERVER_SERVERSIDECONNECTION_H
#define GUISERVER_SERVERSIDECONNECTION_H
//=============================================================================
//
//   guiServer::ServerSideConnection Class
//
//!  Handles a single connection to the GUI server.  The connection is
//!  bi-directional, so there is a receive thread that monitors incoming data
//!  and write functions.  The PacketExchange class is inherited.
//
//=============================================================================
#include <stdlib.h>
#include <pthread.h>
#include <string>
#include <network/UDPSocket.h>
#include <PacketExchange.h>
#include <difxmessage.h>

namespace guiServer {

    class ServerSideConnection : public PacketExchange {
    
        static const int MAX_COMMAND_SIZE = 1024;
    
    public:

        ServerSideConnection( network::GenericSocket* sock, const bool relayDifx ) : PacketExchange( sock ) {
            _relayDifx = false;
            _commandSocket = NULL;
            _difxAlertsOn = true;
            //  If we are relaying DiFX broadcasts, set up a thread to receive (and relay) them.
            if ( relayDifx )
                startDifxRelay();
        }
        
        ~ServerSideConnection() {
            _relaySocket = 0;
        }
        
        //---------------------------------------------------------------------
        //!  Start a thread to collect DiFX broadcast messages and relay them.
        //---------------------------------------------------------------------
        void startDifxRelay() {
            //  Open a UDP client at the DiFX broadcast port.
            char* portStr = getenv( "DIFX_MESSAGE_PORT" );
            if ( portStr == NULL ) {
                fprintf( stderr, "Cannot start socket relay - environment variable DIFX_MESSAGE_PORT is not defined\n" );
                return;
            }
            int port = atoi( portStr );
            _relaySocket = new network::UDPSocket( network::UDPSocket::RECEIVE, getenv( "DIFX_MESSAGE_GROUP" ), port );
            _relaySocket->ignoreOwn( false );
            //  Start the thread that reads the broadcast messages.
            pthread_attr_init( &_relayMonAttr );
            pthread_create( &_relayMonId, &_relayMonAttr, staticRelayMonitor, this );      
            _relayDifx = true;         
        }
        
        //---------------------------------------------------------------------
        //!  This turns the relay off.
        //---------------------------------------------------------------------
        void stopDifxRelay() {
            _relayDifx = false;
            _relaySocket->closeFd();
            //sleep( 1 );
            delete _relaySocket;
        }
        
        //---------------------------------------------------------------------
        //!  Static thread start function (for relay).
        //---------------------------------------------------------------------
        static void* staticRelayMonitor( void* a ) {
            ( (ServerSideConnection*)a )->relayMonitor();
        }
        
        static const int MAX_MESSAGE_LENGTH = 10 * 1024;
        
        //---------------------------------------------------------------------
        //!  This is the function that actually monitors DiFX broadcasts for
        //!  relay.
        //---------------------------------------------------------------------
        void relayMonitor() {
            while ( _relaySocket ) {
                char message[MAX_MESSAGE_LENGTH + 1];
                int ret = _relaySocket->reader( message, MAX_MESSAGE_LENGTH );
                if ( ret == -1 ) {
                    _relaySocket = false;
                }
                else {
                    sendPacket( RELAY_PACKET, message, ret );
                }
            }
        }

        //---------------------------------------------------------------------
        //!  Override the relay function.  The relay command starts with a
        //!  single integer of data telling us to turn on (1) or off (0) the
        //!  relaying of DiFX broadcast messages to the client, followed by
        //!  an integer containing a multicast group address and then the port
        //!  that should be used.  The latter two items can obviously be 
        //!  ignored if relay is being turned off.
        //---------------------------------------------------------------------
        virtual void relay( char* data, const int nBytes ) {
            int relayOn = ntohl( *(int*)data );
            if ( relayOn ) {
                //  Turn relay on if it isn't already.
                if ( !_relayDifx )
                    startDifxRelay();
            }
            else {
                //  Turn off relay if it is on.
                if ( _relayDifx )
                    stopDifxRelay();
            }
        }
        
        //---------------------------------------------------------------------
        //!  This is a relayed command from the client - these are converted to UDP
        //!  messages that the DiFX processes (mk5daemon, etc) can pick up.
        //---------------------------------------------------------------------
        virtual void relayCommand( char* data, const int nBytes ) {
            //  Create a new command socket if we don't have one.
            if ( _commandSocket == NULL ) {
                char* portStr = getenv( "DIFX_MESSAGE_PORT" );
                if ( portStr == NULL ) {
                    fprintf( stderr, "Cannot start command socket - environment variable DIFX_MESSAGE_PORT is not defined\n" );
                    return;
                }
                int port = atoi( portStr );
                _commandSocket = new network::UDPSocket( network::UDPSocket::MULTICAST, getenv( "DIFX_MESSAGE_GROUP" ), port );
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
        //!  Structure used to pass information to the thread that starts and
        //!  monitors DiFX.
        //---------------------------------------------------------------------
        struct DifxStartInfo {
            ServerSideConnection* ssc;
            int force;
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
    
        network::UDPSocket* _relaySocket;
        network::UDPSocket* _commandSocket;
        pthread_attr_t _relayMonAttr;
        pthread_t _relayMonId;
        bool _relayDifx;
        bool _difxAlertsOn;
        bool _diagnosticPacketsOn;
        
    };

}

#endif
