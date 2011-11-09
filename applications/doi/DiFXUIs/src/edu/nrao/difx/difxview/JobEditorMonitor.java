/*
 * This window contains all of the settings for a single job, as well as controls
 * and displays to run it and monitor its progress.
 */
package edu.nrao.difx.difxview;

import edu.nrao.difx.difxutilities.SendMessage;
import edu.nrao.difx.xmllib.difxmessage.ObjectFactory;
import edu.nrao.difx.xmllib.difxmessage.Header;
import edu.nrao.difx.xmllib.difxmessage.Body;
import edu.nrao.difx.xmllib.difxmessage.DifxStart;
import edu.nrao.difx.xmllib.difxmessage.DifxMessage;

import edu.nrao.difx.difxcontroller.JAXBDiFXProcessor;

import javax.swing.JFrame;

//import java.io.IOException;
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.io.BufferedWriter;
//import java.io.OutputStreamWriter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.event.EventListenerList;

public class JobEditorMonitor extends JFrame {
    
    /*
     * The JobNode gives us access to all of the data known about this job.
     */
    public JobEditorMonitor( JobNode newNode, SystemSettings settings ) {
        super( "Job Editor/Monitor" );
        this.setBounds( 500, 100, 500, 500 );
        _jobNode = newNode;
        _settings = settings;
    }
    
    public void startJob() {
        ObjectFactory factory = new ObjectFactory();

        // Create header
        Header header = factory.createHeader();
        header.setFrom( "doi" );
        header.setTo( "swc01.usno.navy.mil" );
        header.setMpiProcessId( "-1" );
        header.setIdentifier( _jobNode.name() );
        header.setType( "DifxStart" );

        // Create start job command
        DifxStart jobStart = factory.createDifxStart();
        jobStart.setInput( _jobNode.inputFile() ); //_jobNode.directoryPath() + "/" + _jobNode.name() + ".input");

        // -- manager, enabled only
        DifxStart.Manager manager = factory.createDifxStartManager();
        manager.setNode( "swc01.usno.navy.mil" );
        jobStart.setManager( manager );

        // Get a string of Mark5 Units
        //String mark5String = getStringOfMark5Units();

        // -- set difx version to use
        //jobStart.setDifxVersion(getDifxVersion());

        // -- datastreams, enabled only
        DifxStart.Datastream dataStream = factory.createDifxStartDatastream();
        //dataStream.setNodes(mark5String);
        //  For the moment we hard-wire these...
        dataStream.setNodes( "mark5-759.usno.navy.mil mark5-760.usno.navy.mil" );
        jobStart.setDatastream(dataStream);

        // -- process and threads, enabled only
        DifxStart.Process process = factory.createDifxStartProcess();
        DifxStart.Process process2 = factory.createDifxStartProcess();
//        process.setNodes("SWC001 SWC002 SWC003 SWC004 SWC005 SWC006 SWC007 SWC008 SWC009 SWC010 MARK5FX23");
//        process.setThreads("7");
        process.setNodes( "swc01.usno.navy.mil swc02.usno.navy.mil" );
        process.setThreads( "7" );
        jobStart.getProcess().add(process);
        //process2.setNodes("SWC000");
        //process2.setThreads("5");
        //jobStart.getProcess().add(process2);

        // force deletion of existing output file
        jobStart.setForce(1);

        // -- Create the XML defined messages and process through the system
        Body body = factory.createBody();
        body.setDifxStart(jobStart);

        DifxMessage difxMsg = factory.createDifxMessage();
        difxMsg.setHeader(header);
        difxMsg.setBody(body);
        
        JAXBDiFXProcessor xmlProc = new JAXBDiFXProcessor(difxMsg);
        String xmlString = xmlProc.ConvertToXML();
        
        if ( xmlString != null )
            SendMessage.writeToSocket( xmlString, _settings );
        else
            System.out.println( "blew it!" );

        // -- return null if the message is invalid, otherwise return the message
//        if (mark5String.equals("") || mark5String.isEmpty())
//        {
//        return null; // did not create the proper list of mark units.
//        }
//        else
//        {
//        return difxMsg;
//        }
//        //  Start a job by running "startdifx" through ssh on the DiFX Host....
//        try {
//            //  Try the ssh...            
//            Process _ssh = Runtime.getRuntime().exec( "ssh " + _settings.difxControlUser() + "@" + _settings.difxControlAddress() );
//            BufferedReader _output = new BufferedReader( new InputStreamReader( _ssh.getInputStream() ) );
//            BufferedReader _error = new BufferedReader( new InputStreamReader( _ssh.getErrorStream() ) );
//            BufferedWriter _input = new BufferedWriter( new OutputStreamWriter( _ssh.getOutputStream() ) );
//            while ( _error.ready() ) {
//                String foo = _error.readLine();
//                System.out.println( "ERROR! " + foo );
//            }
//            while ( _output.ready() ) {
//                String foo = _output.readLine();
//                System.out.println( foo );
//            }
//        } catch ( IOException e ) {
//        }
    }
    
    public void pauseJob() {}
    
    public void stopJob() {}
    
    /*
     * Add a listener to "state change" events.  These occur when this class starts,
     * pauses, or stops a job, or recognizes that a job has finished running.
     */
    public void addActionListener( ActionListener a ) {
        _stateChangeListeners.add( ActionListener.class, a );
    }

    /*
     * Send a "state change" event to all listeners.
     */
    protected void stateChangeEvent() {
        Object[] listeners = _stateChangeListeners.getListenerList();
        int numListeners = listeners.length;
        for ( int i = 0; i < numListeners; i+=2 ) {
            if ( listeners[i] == ActionListener.class )
                ((ActionListener)listeners[i+1]).actionPerformed( null );
        }
    }
    
    protected EventListenerList _stateChangeListeners;
    protected JobNode _jobNode;
    protected SystemSettings _settings;
    
}
