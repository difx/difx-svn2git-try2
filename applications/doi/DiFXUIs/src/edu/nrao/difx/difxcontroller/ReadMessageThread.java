/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.nrao.difx.difxcontroller;

import edu.nrao.difx.difxview.SystemSettings;
import java.net.*;
import java.io.*;

import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 *
 * @author mguerra
 */
public class ReadMessageThread implements Runnable {

    private String mThreadName;
    private boolean mDone = false;
    private boolean _settingsChange = true;
    // -- always start the process message thread before this thread.
    private ProcessMessageThread _processMessageThread;
    SystemSettings _settings;

    // Constructor, give the thread a name and a link to the system settings.
    public ReadMessageThread( String name, SystemSettings systemSettings ) {
        mThreadName = name;
        _settings = systemSettings;
        //  Set up a callback for changes to broadcast items in the system settings.
        _settings.broadcastChangeListener( new ActionListener() {

            public void actionPerformed( ActionEvent e ) {
                updateBroadcastSettings();
            }
        });

    }

    protected void updateBroadcastSettings() {
        _settingsChange = true;
    }

    // Stop thread
    public void shutDown() {
        mDone = true;
    }

    // Methods specific to the message queue
    public void addQueue(ProcessMessageThread queue) {
        _processMessageThread = queue;
    }

    public ProcessMessageThread getQueue() {
        return _processMessageThread;
    }

    private void printPacket(DatagramPacket packet) {
        System.out.println("******** Read message packet received data from: " + packet.getAddress().toString()
                + ":" + packet.getPort() + " with length: "
                + packet.getLength());

        System.out.write(packet.getData(), 0, packet.getLength());
        System.out.println();
    }

    // Implement the thread interface
    @Override
    public void run() {

        synchronized ( this ) {

            try {
                // start time stamp
                String startDate = Calendar.getInstance().getTime().toString();

                MulticastSocket socket = null;
                
                // loop forever, reading datagram packets
                while ( !mDone ) {
                    
                    try {
                        
                        //  We can get packets from either UDP or TCP.  
                        //  TCP relay...
                        if ( _settings.useTCPRelay() ) {
                            try {
                                byte [] buffer = _settings.guiServerConnection().getRelay();
                                if ( buffer != null ) {
                                    //  Feedback for the plot in the settings window
                                    _settings.gotPacket( buffer.length );
                                    if ( !_processMessageThread.add( new ByteArrayInputStream( buffer, 0, buffer.length ) ) ) {
                                        System.out.printf("******** Read message thread packet FAILED to add into queue. \n");
                                    }
                                    buffer = null;
                                }
                            } catch ( SocketTimeoutException e ) {
                                _settings.gotPacket( 0 );
                            }
                        }
                        
                        //  UDP multicast receive (the "original" way)...
                        else {

                            //  Check for changes to the broadcast settings on each cycle.
                            if ( _settingsChange || socket == null ) {
                                socket = new MulticastSocket( _settings.port() );
                                socket.setSoTimeout( _settings.timeout() );              // timeout 100ms
                                socket.setReceiveBufferSize( 512000 );   // max buffer size 512k Bytes
                                socket.joinGroup( InetAddress.getByName( _settings.ipAddress() ) );
                                _settingsChange = false;
                            }

                            // create buffer and datagram packet
                            byte[] buffer = new byte[_settings.bufferSize()];    //1050
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                            // Wait for datagram packet
                            if ( packet != null ) {
                                // do not process empty packets.
                                try {
                                    // Insert raw packet into the queue
                                    socket.receive(packet);
                                    //  This allows the systems settings to show the packets as we receive them...very exciting.
                                    _settings.gotPacket( packet.getLength() );
//                                    if (!_processMessageThread.add(packet)) {
                                    if ( !_processMessageThread.add( new ByteArrayInputStream( packet.getData(), 0, packet.getLength() ) ) ) {
                                        System.out.printf("******** Read message thread packet FAILED to add into queue. \n");
                                    }
                                } catch ( SocketTimeoutException exception ) {
                                    _settings.gotPacket( 0 );
                                    // socket did not receive message within 100ms, clean up
                                    buffer = null;
                                    packet = null;
                                    Thread.yield();  //  BLAT This should not be necessary
                                }

                            } else {
                                System.out.printf("******** Read message empty null packet - continue. \n", mThreadName);
                            }

                            // clean up  BLAT - recreating these each time is really strange...I guess the buffer
                            //  size can change, but enough to justify this constant memory allocation??
                            buffer = null;
                            packet = null;
                        
                        }

                        // catch an interrupt, stop thread
                        if (Thread.currentThread().isInterrupted() == true) {
                            System.out.printf("******** Read message thread %s interrupted. \n", mThreadName);
                            mDone = true;
                        }
                        
                    } catch (OutOfMemoryError exception) {
                        System.out.printf("******** Read message %s caught OutOfMemoryError(%s  %s) - done. \n",
                                mThreadName, startDate, Calendar.getInstance().getTime().toString());
                        mDone = true;
                        exception.printStackTrace();
                    }

                } // -- while (!mDone)

                System.out.printf("******** Read message thread %s done. \n", mThreadName);
            } catch ( IOException ex ) {
                Logger.getLogger(ReadMessageThread.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

    }

}
