/*
 * This class is used to form a connection to the guiServer application that
 * should be running on the DiFX host.  This is a bi-directional TCP connection.
 * Outgoing traffic (TO the guiServer) takes the form of commands, while incoming
 * traffic (FROM guiServer) relays multicast traffic on the DiFX cluster (much
 * of which is mk5server traffic).  The design tries to keep this connection
 * fairly simple (and thus hopefully robust) - any additional data relays (for 
 * instance if a command generates return data, or a file needs to be transfered)
 * are done using purpose-formed TCP connections with threads on the guiServer.
 * 
 * This class generates three types of callbacks: "connect" events accompanied by
 * an explanatory String (either the connection status in the case of a change or
 * an exception string in the case of an error); "send" events accompanied
 * by an integer number of bytes sent; and  "receive" events accompanied by an
 * integer number of bytes received.  
 */
package edu.nrao.difx.difxutilities;

import java.net.Socket;
import java.net.SocketTimeoutException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.swing.JOptionPane;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.event.EventListenerList;

/**
 *
 * @author johnspitzak
 */
public class GuiServerConnection {
    
    public final int RELAY_PACKET                = 1;
    public final int RELAY_COMMAND_PACKET        = 2;
    public final int COMMAND_PACKET              = 3;
    public final int INFORMATION_PACKET          = 4;
    public final int WARNING_PACKET              = 5;
    public final int ERROR_PACKET                = 6;
    public final int MULTICAST_SETTINGS_PACKET   = 7;
    
    public GuiServerConnection( String IP, int port, int timeout ) {
        _connectListeners = new EventListenerList();
        _sendListeners = new EventListenerList();
        _receiveListeners = new EventListenerList();
        _IP = new String( IP );
        _port = port;
        _timeout = timeout;
    }
    
    public boolean connect() {
        try {
            _socket = new Socket( _IP, _port );
            _socket.setSoTimeout( _timeout );
            _in = new DataInputStream( _socket.getInputStream() );
            _out = new DataOutputStream( _socket.getOutputStream() );
            _connected = true;
            connectEvent( "connected" );
            return true;
        } catch ( java.net.UnknownHostException e ) {
            _connected = false;
//            java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.SEVERE,
//                        _IP + " port " + _port + "\n" + e.toString() );
            return false;
        } catch ( java.io.IOException e ) {
            _connected = false;
//            java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.SEVERE,
//                        _IP + " port " + _port + "\n" + e.toString() );
            return false;
        }
    }
    
    public void close() {
        if ( connected() ) {
            try {
                _socket.close();
            } catch ( java.io.IOException e ) {
                //  Not being able to close the socket probably indicates something out of
                //  the user's ability to fix is wrong, so we won't trouble them by reporting
                //  the problem.
            }
            _connected = false;
            connectEvent( "connection closed" );
        }
    }
    
    /*
     * Send a packet of the given type.  The type and number of bytes in
     * the packet are sent as integers - and thus are swapped (if necessary) to
     * network byte order.  The data are not.
     */
    synchronized public void sendPacket( int packetId, int nBytes, byte[] data ) {
        if ( _connected ) {
            try {
                _out.writeInt( packetId );
                _out.writeInt( nBytes );
                _out.write( data );
                sendEvent( nBytes );
            } catch ( Exception e ) {
                java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.SEVERE, null, 
                    e.toString() );
                connectEvent( e.toString() );
            }
        } else {
            sendEvent( -nBytes );
        }
    }
    
    /*
     * Read the next relay packet from the inbound side of the socket.  Once again,
     * packet type and size are in network byte order, data are not.
     */
    public byte[] getRelay() throws SocketTimeoutException {
        if ( _connected ) {
            byte[] data = null;
            try {
                int packetId = _in.readInt();
                int nBytes = _in.readInt();
                data = new byte[nBytes];
                _in.readFully( data );
            } catch ( SocketTimeoutException e ) {
                //  Timeouts are actually expected and should not cause alarm.
                throw e;
            } catch ( java.io.IOException e ) {
                _connected = false;
                connectEvent( e.toString() );
            }
            if ( data != null )
                receiveEvent( data.length );
            return data;
        } else {
            return null;
        }
    }
        
    
    /*
     * This turns on (or off) the broadcast relay capability.
     */
    public void relayBroadcast( boolean on ) {
        ByteBuffer b = ByteBuffer.allocate( 4 );
        b.order( ByteOrder.BIG_ENDIAN );
        if ( on )
            b.putInt( 1 );
        else
            b.putInt( 0 );
        sendPacket( RELAY_PACKET, 4, b.array() );
    }
    
    public boolean connected() { return _connected; }
    
    public void addConnectionListener( ActionListener a ) {
        _connectListeners.add( ActionListener.class, a );
    }

    public void addSendListener( ActionListener a ) {
        _sendListeners.add( ActionListener.class, a );
    }

    public void addReceiveListener( ActionListener a ) {
        _receiveListeners.add( ActionListener.class, a );
    }
    
    protected void connectEvent( String mess ) {
        Object[] listeners = _connectListeners.getListenerList();
        int numListeners = listeners.length;
        for ( int i = 0; i < numListeners; i+=2 ) {
            if ( listeners[i] == ActionListener.class )
                ((ActionListener)listeners[i+1]).actionPerformed( new ActionEvent( this, ActionEvent.ACTION_PERFORMED, mess ) );
        }
    }

    protected void sendEvent( Integer nBytes ) {
        Object[] listeners = _sendListeners.getListenerList();
        int numListeners = listeners.length;
        for ( int i = 0; i < numListeners; i+=2 ) {
            if ( listeners[i] == ActionListener.class )
                ((ActionListener)listeners[i+1]).actionPerformed( new ActionEvent( this, ActionEvent.ACTION_PERFORMED, nBytes.toString() ) );
        }
    }

    protected void receiveEvent( Integer nBytes ) {
        Object[] listeners = _receiveListeners.getListenerList();
        int numListeners = listeners.length;
        for ( int i = 0; i < numListeners; i+=2 ) {
            if ( listeners[i] == ActionListener.class )
                ((ActionListener)listeners[i+1]).actionPerformed( new ActionEvent( this, ActionEvent.ACTION_PERFORMED, nBytes.toString() ) );
        }
    }


    protected Socket _socket;
    protected DataInputStream _in;
    protected DataOutputStream _out;
    protected boolean _connected;
    protected String _IP;
    protected int _port;
    protected int _timeout;
    protected Component _component;
    protected EventListenerList _connectListeners;
    protected EventListenerList _sendListeners;
    protected EventListenerList _receiveListeners;
}
