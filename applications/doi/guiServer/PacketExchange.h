#ifndef GUISERVER_PACKETEXCHANGE_H
#define GUISERVER_PACKETEXCHANGE_H
//=============================================================================
//
//    guiServer::PacketExchange Class
//
//!  Defines the packet exchange protocol for a guiServer socket.  This class on
//!  the whole doesn't do much - the inheriting client and server classes do
//!  all of the work.  What it does is define message types and run a 
//!  "receive" thread on the socket, spawning do-nothing functions that may be
//!  overridden by the inheriting clients if action is required.
//
//=============================================================================
#include <pthread.h>
#include <network/PacketExchange.h>

namespace guiServer {

    class PacketExchange : public network::PacketExchange {
    
    public:
    
        //---------------------------------------------------------------------
        //!  These are the packet types recognized by this protocol.
        //---------------------------------------------------------------------
        static const int RELAY_PACKET                  = 1;
        static const int RELAY_COMMAND_PACKET          = 2;
        static const int COMMAND_PACKET                = 3;
        static const int INFORMATION_PACKET            = 4;
        static const int WARNING_PACKET                = 5;
        static const int ERROR_PACKET                  = 6;

        PacketExchange( network::GenericSocket* sock ) : network::PacketExchange( sock ) {
            _sock = sock;
            _receiveActive = false;
            pthread_attr_init( &_receiveAttr );
            pthread_create( &_receiveId, &_receiveAttr, staticReceiveThread, this );               
        }
        
        ~PacketExchange() {
            _receiveActive = false;
        }
        
        //---------------------------------------------------------------------
        //!  Static start function for the receive thread.
        //---------------------------------------------------------------------
        static void* staticReceiveThread( void* a ) {
            ( (PacketExchange*)a )->receiveThread();
        }
        
        //---------------------------------------------------------------------
        //!  Thread to receive all incoming data on the socket.  Each recognized
        //!  type spawns a do-nothing function that should be overridden by
        //!  inheriting classes to actually accomplish something.
        //---------------------------------------------------------------------
        void receiveThread() {
            _receiveActive = true;
            while ( _receiveActive ) {
                int packetId = 0;
                int nBytes;
                char* data;
                printf( "waiting for packet\n" );
                if ( getPacket( packetId, data, nBytes ) == -1 ) {
                    //  connection failure
                    _receiveActive = false;
                }
                else  {
                    printf( "packet id is %d size is %d\n", packetId, nBytes );
                    switch( packetId ) {
                    case RELAY_PACKET:
                        relay( data, nBytes );
                        break;
                    case RELAY_COMMAND_PACKET:
                        relayCommand( data, nBytes );
                        break;
                    case COMMAND_PACKET:
                        command( data, nBytes );
                        break;
                    //  Any packet we don't recognize we ignore.
                    default:
                        break;
                    }
                    //  Free the space allocated to this incoming message.
                    delete [] data;
                }
            }
        }
        
        virtual void relay( char* data, const int nBytes ) {}
        virtual void relayCommand( char* data, const int nBytes ) {}
        virtual void command( char* data, const int nBytes ) {}

    protected:
    
        network::GenericSocket* _sock;
        pthread_attr_t _receiveAttr;
        pthread_t _receiveId;
        bool _receiveActive;

    };

}

#endif
