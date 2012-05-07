#ifndef NETWORK_GENERICSOCKET_H
#define NETWORK_GENERICSOCKET_H
//==============================================================================
//
//   network::GenericSocket Class
//
//!  Contains virtual reader and writer functions shared by TCP and UDP sockets.
//
//==============================================================================

namespace network {

    class GenericSocket {

    public:

        //  Reader and writer functions both return the number of bytes
        //  successfully transfered, -1 on a failure, or 0 on a timeout
        //  (if applicable).  On failure or timeout, the _partialRead and
        //  _partialWrite variables are set to the  number of bytes sent 
        //  or received.
        virtual int reader( char* buff, int nBytes ) = 0;
        virtual int writer( char* buff, int nBytes ) = 0;
        
        unsigned int partialRead() { return _partialRead; }
        unsigned int partialWrite() { return _partialWrite; }
        
    protected:
    
        unsigned int _partialRead;
        unsigned int _partialWrite;

    };

}

#endif
