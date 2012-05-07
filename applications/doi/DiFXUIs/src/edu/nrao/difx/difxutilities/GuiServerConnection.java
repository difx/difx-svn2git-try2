/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
    
    public GuiServerConnection( String IP, int port, int timeout, Component component ) {
        _component = component;
        try {
            _socket = new Socket( IP, port );
            _socket.setSoTimeout( timeout );
            _in = new DataInputStream( _socket.getInputStream() );
            _out = new DataOutputStream( _socket.getOutputStream() );
            _connected = true;
        } catch ( java.net.UnknownHostException e ) {
            JOptionPane.showMessageDialog( component, IP + " port " + port + "\n" + e.toString(),
                        "UnknownHostException",
                        JOptionPane.ERROR_MESSAGE );
        } catch ( java.io.IOException e ) {
            JOptionPane.showMessageDialog( component, IP + " port " + port + "\n" + e.toString(),
                        "IOException",
                        JOptionPane.ERROR_MESSAGE );
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
        }
    }
    
    /*
     * Send a packet of the given type.  The type and number of bytes in
     * the packet are sent as integers - and thus are swapped (if necessary) to
     * network byte order.  The data are not.
     */
    public void sendPacket( int packetId, int nBytes, byte[] data ) {
        try {
            _out.writeInt( packetId );
            _out.writeInt( nBytes );
            _out.write( data );
        } catch ( Exception e ) {}
    }
    
    /*
     * Read the next relay packet from the inbound side of the socket.  Once again,
     * packet type and size are in network byte order, data are not.
     */
    public byte[] getRelay() throws SocketTimeoutException {
        byte[] data = null;
        try {
            int packetId = _in.readInt();
            int nBytes = _in.readInt();
            data = new byte[nBytes];
            _in.readFully( data );
        } catch ( SocketTimeoutException e ) {
            throw e;
        } catch ( java.io.IOException e ) {
            System.out.println( "io exception " + e );
        }
        return data;
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
    
    Socket _socket;
    protected DataInputStream _in;
    protected DataOutputStream _out;
    protected boolean _connected;
    protected Component _component;
}
