/*
 * The "ls" command performs an ls on a "filter" (path, with wildcards allowed)
 * on the DiFX Host.  It does this as the difx user, so permissions of that
 * user apply.  Data are returned via TCP socket.
 */
package edu.nrao.difx.difxutilities;

import edu.nrao.difx.difxview.SystemSettings;

import edu.nrao.difx.xmllib.difxmessage.DifxFileOperation;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.io.DataInputStream;

import javax.swing.event.EventListenerList;

/**
 *
 * @author jspitzak
 */
public class DiFXCommand_ls extends DiFXCommand {
    
    public DiFXCommand_ls( String filter, SystemSettings settings ) {
        super( settings );
        this.header().setType( "DifxFileOperation" );
        DifxFileOperation ls = this.factory().createDifxFileOperation();
        ls.setPath( filter );
        ls.setOperation( "ls" );
        ls.setArg( "-d -p" );
        //  The "data" node is assumed to be the same as the DiFX "control" node
        //  (at least for now).
        ls.setDataNode( settings.difxControlAddress() );
        _port = _settings.newDifxTransferPort();
        ls.setPort( _port );
        try {
            ls.setAddress( java.net.InetAddress.getLocalHost().getHostAddress() );
        } catch ( java.net.UnknownHostException e ) {
        }
        this.body().setDifxFileOperation( ls );
        //  These lists contain "listeners" for callbacks when things occur...incremental
        //  read progress and the end of reading.
        _incrementalListeners = new EventListenerList();
        _endListeners = new EventListenerList();
    }
    
    
    public void addIncrementalListener( ActionListener a ) {
        _incrementalListeners.add( ActionListener.class, a );
    }

    protected void incrementalCallback( String filename ) {
        Object[] listeners = _incrementalListeners.getListenerList();
        // loop through each listener and pass on the event if needed
        int numListeners = listeners.length;
        for ( int i = 0; i < numListeners; i+=2 ) {
            if ( listeners[i] == ActionListener.class )
                ((ActionListener)listeners[i+1]).actionPerformed( new ActionEvent( this, ActionEvent.ACTION_PERFORMED, filename ) );
        }
    }
    
    public void addEndListener( ActionListener a ) {
        _endListeners.add( ActionListener.class, a );
    }

    protected void endCallback() {
        Object[] listeners = _endListeners.getListenerList();
        // loop through each listener and pass on the event if needed
        int numListeners = listeners.length;
        for ( int i = 0; i < numListeners; i+=2 ) {
            if ( listeners[i] == ActionListener.class )
                ((ActionListener)listeners[i+1]).actionPerformed( new ActionEvent( this, ActionEvent.ACTION_PERFORMED, "" ) );
        }
    }
    
    @Override
    public void send() {
        ResultReader reader = new ResultReader();
        reader.start();
        super.send();
    }
        
    //  This thread reads the results of the ls operation as reported by mk5daemon
    //  (a list of files that match the pattern).  It produces two callbacks -
    //  the incremental callback for each filename read; and the end callback when
    //  all reading is complete.
    protected class ResultReader extends Thread {
        
        @Override
        public void run() {
            //  Open a new server socket and await a connection.  The connection
            //  will timeout after a given number of seconds (nominally 10).
            try {
                ServerSocket ssock = new ServerSocket( _port );
                ssock.setSoTimeout( 10000 );  //  timeout is in millisec
                try {
                    Socket sock = ssock.accept();
                    //  Turn the socket into a "data stream", which has useful
                    //  functions.
                    DataInputStream in = new DataInputStream( sock.getInputStream() );
                    //  Read each line of incoming data until there are no more.
                    boolean finished = false;
                    while ( !finished ) {
                        //  Read the size of the next file name....
                        int sz = in.readInt();
                        if ( sz == 0 )
                            finished = true;
                        else {
                            byte[] foo = new byte[sz + 1];
                            in.readFully( foo, 0, sz );
                            String inLine = new String( foo );
                            incrementalCallback( inLine );
                        }
                    }
                    sock.close();
                } catch ( SocketTimeoutException e ) {
                    
                }
                ssock.close();
            } catch ( Exception e ) {
                
            }
            endCallback();
        }
        
    }

    protected EventListenerList _incrementalListeners;
    protected EventListenerList _endListeners;
    protected int _port;

}
