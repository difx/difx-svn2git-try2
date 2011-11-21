/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.nrao.difx.difxcontroller;

import edu.nrao.difx.difxview.SystemSettings;
import edu.nrao.difx.difxdatamodel.DiFXSystemConfig;
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
    private ProcessMessageThread mMessageQueue;
    SystemSettings _systemSettings;

    // Constructor, give the thread a name and a link to the system settings.
    public ReadMessageThread( String name, SystemSettings systemSettings ) {
        mThreadName = name;
        _systemSettings = systemSettings;
        //  Set up a callback for changes to broadcast items in the system settings.
        _systemSettings.broadcastChangeListener( new ActionListener() {

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
        mMessageQueue = queue;
    }

    public ProcessMessageThread getQueue() {
        return mMessageQueue;
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

                        //  Check for changes to the broadcast settings on each cycle.
                        if ( _settingsChange || socket == null ) {
                            socket = new MulticastSocket( _systemSettings.port() );
                            socket.setSoTimeout( _systemSettings.timeout() );              // timeout 100ms
                            socket.setReceiveBufferSize( 512000 );   // max buffer size 512k Bytes
                            socket.joinGroup( InetAddress.getByName( _systemSettings.ipAddress() ) );
                            _settingsChange = false;
                        }

                        // create buffer and datagram packet
                        byte[] buffer = new byte[_systemSettings.bufferSize()];    //1050
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                        // Wait for datagram packet
                        if (packet != null) {
                            // do not process empty packets.
                            try {
                                // Insert raw packet into the queue
                                socket.receive(packet);
                                //  This allows the systems settings to show the packets as we receive them...very exciting.
                                _systemSettings.gotPacket( packet.getLength() );
                                if (!mMessageQueue.add(packet)) {
                                    System.out.printf("******** Read message thread packet FAILED to add into queue. \n");
                                }
                            } catch (SocketTimeoutException exception) {
                                _systemSettings.gotPacket( 0 );
                                // socket did not receive message within 100ms, clean up
                                buffer = null;
                                packet = null;
                                Thread.yield();
                            }

                        } else {
                            System.out.printf("******** Read message empty null packet - continue. \n", mThreadName);
                        }

                        // No need to throttle packet read

                        // Do not leave group and do not close socket

                        // clean up
                        buffer = null;
                        packet = null;

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
