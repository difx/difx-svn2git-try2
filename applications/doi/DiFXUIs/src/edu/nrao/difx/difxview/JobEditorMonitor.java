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
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JPopupMenu;

//import java.io.IOException;
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.io.BufferedWriter;
//import java.io.OutputStreamWriter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.Timer;
import java.awt.Color;

import java.util.Iterator;

import javax.swing.event.EventListenerList;

import mil.navy.usno.widgetlib.IndexedPanel;
import mil.navy.usno.widgetlib.NodeBrowserScrollPane;
import mil.navy.usno.widgetlib.NumberBox;
import mil.navy.usno.widgetlib.BrowserNode;

public class JobEditorMonitor extends JFrame {
    
    /*
     * The JobNode gives us access to all of the data known about this job.
     */
    public JobEditorMonitor( JobNode newNode, SystemSettings settings ) {
        super( "Job Editor/Monitor" );
        _jobNode = newNode;
        _settings = settings;
        _settings.setLookAndFeel();
        this.setLayout( null );
        this.setBounds( 500, 100, 800, 500 );
        this.setTitle( "Control/Monitor for " + _jobNode.name() );
        _menuBar = new JMenuBar();
        _menuBar.setVisible( true );
        JMenu helpMenu = new JMenu( "  Help  " );
        _menuBar.add( helpMenu );
        JMenuItem controlHelpItem = new JMenuItem( "Control/Monitor Help" );
        controlHelpItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _settings.launchGUIHelp( "Control_Monitor_Window.html" );
            }
        } );
        helpMenu.add( controlHelpItem );
        JMenuItem helpIndexItem = new JMenuItem( "Help Index" );
        helpIndexItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _settings.launchGUIHelp( "index.html" );
            }
        } );
        helpMenu.add( helpIndexItem );
        this.add( _menuBar );

        _scrollPane = new NodeBrowserScrollPane();
        _scrollPane.addTimeoutEventListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                newSize();
            }
        } );
        this.add( _scrollPane );

        IndexedPanel runControlPanel = new IndexedPanel( "Run Parameters" );
        runControlPanel.openHeight( 215 );
        runControlPanel.closedHeight( 20 );
        _scrollPane.addNode( runControlPanel );
        _dataSourcesPane = new NodeBrowserScrollPane();
        runControlPanel.add( _dataSourcesPane );
        _dataSourcesLabel = new JLabel( "Data Nodes:" );
        _dataSourcesLabel.setHorizontalAlignment( JLabel.LEFT );
        runControlPanel.add( _dataSourcesLabel );
        _processorsPane = new NodeBrowserScrollPane();
        runControlPanel.add( _processorsPane );
        _processorsLabel = new JLabel( "Processor Nodes:" );
        _processorsLabel.setHorizontalAlignment( JLabel.LEFT );
        runControlPanel.add( _processorsLabel );
        _headNode = new JTextField();
        runControlPanel.add( _headNode );
        _headNodeLabel = new JLabel( "HeadNode:" );
        _headNodeLabel.setHorizontalAlignment( JLabel.LEFT );
        runControlPanel.add( _headNodeLabel );
        _threads = new NumberBox();
        _threads.intValue( 0 );
        _threads.minimum( 0 );
        runControlPanel.add( _threads );
        _threadsLabel = new JLabel( "Threads:" );
        _threadsLabel.setHorizontalAlignment( JLabel.RIGHT );
        runControlPanel.add( _threadsLabel );
 
        _allObjectsBuilt = true;
        
        //  Set a timeout with a one second interval.
        Action updateDrawingAction = new AbstractAction() {
            @Override
            public void actionPerformed( ActionEvent e ) {
                timeoutIntervalEvent();
            }
        };
        new Timer( 1000, updateDrawingAction ).start();
        
        this.newSize();

    }
    
    @Override
    public void setBounds( int x, int y, int w, int h ) {
        super.setBounds( x, y, w, h );
        newSize();
    }
    
    public void newSize() {
        int w = this.getWidth();
        int h = this.getHeight();
        if ( _menuBar != null )
            _menuBar.setBounds( 0, 0, w, 25 );
        if ( _allObjectsBuilt ) {
            _scrollPane.setBounds( 0, 25, w, h - 25 );
            int thirdSize = ( w - 50 ) / 3;
            _dataSourcesLabel.setBounds( 10, 25, thirdSize, 25 );
            _dataSourcesPane.setBounds( 10, 50, thirdSize, 150 );
            _processorsLabel.setBounds( 20 + thirdSize, 25, thirdSize, 25 );
            _processorsPane.setBounds( 20 + thirdSize, 50, thirdSize, 150 );
            _headNodeLabel.setBounds( 30 + 2 * thirdSize, 25, thirdSize, 25 );
            _headNode.setBounds( 30 + 2 * thirdSize, 50, thirdSize, 25 );
            _threadsLabel.setBounds( 30 + 2 * thirdSize, 80, thirdSize - 55, 25 );
            _threads.setBounds( 30 + 3 * thirdSize - 50, 80, 50, 25 );
        }
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
        //manager.setNode( "swc01.usno.navy.mil" );
        manager.setNode( _headNode.getText() );
        jobStart.setManager( manager );

        // Get a string of Mark5 Units
        //String mark5String = getStringOfMark5Units();

        // -- set difx version to use
        //jobStart.setDifxVersion(getDifxVersion());

        // -- datastreams, enabled only
        DifxStart.Datastream dataStream = factory.createDifxStartDatastream();
        //dataStream.setNodes(mark5String);
        //  Grab all of the "checked" data stream node names...
        String nodeNames = "";
        for ( Iterator<BrowserNode> iter = _dataSourcesPane.browserTopNode().children().iterator();
                iter.hasNext(); ) {
            ListNode thisNode = (ListNode)(iter.next());
            if ( thisNode.selected() )
                nodeNames += thisNode.name() + " ";
        }
        dataStream.setNodes( nodeNames );
        jobStart.setDatastream(dataStream);

        // -- process and threads, enabled only
        DifxStart.Process process = factory.createDifxStartProcess();
        DifxStart.Process process2 = factory.createDifxStartProcess();
//        process.setNodes("SWC001 SWC002 SWC003 SWC004 SWC005 SWC006 SWC007 SWC008 SWC009 SWC010 MARK5FX23");
//        process.setThreads("7");
        nodeNames = "";
        for ( Iterator<BrowserNode> iter = _processorsPane.browserTopNode().children().iterator();
                iter.hasNext(); ) {
            ListNode thisNode = (ListNode)(iter.next());
            if ( thisNode.selected() )
                nodeNames += thisNode.name() + " ";
        }
        dataStream.setNodes( nodeNames );
        process.setNodes( nodeNames );
        process.setThreads( ( new Integer( _threads.intValue() ) ).toString() );
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

    /*
     * This class is used to contain the name of a single node for the data source
     * and processor lists.
     */
    protected class ListNode extends BrowserNode {
        
        public ListNode( String name ) {
            super( name );
        }
        
        @Override
        public void createAdditionalItems() {
            _selected = new JCheckBox();
            _selected.setBackground( Color.WHITE );
            this.add( _selected );
        }
        
        public void positionItems() {
            _selected.setBounds( 7, 2, 18, 18 );
            _label.setBounds( 30, 0, 250, _ySize );
        }
        
        public boolean selected() { return _selected.isSelected(); }
        public void selected( boolean newVal ) { _selected.setSelected( newVal ); }
        
        public boolean foundIt;
        protected JCheckBox _selected;
        
    }
    
    /*
     * This class inherits the ListNode class to provide a "Processor".  It allows
     * the user to use the processor as the "head node" and tracks the number of
     * cores available.
     */
    protected class ProcessorNode extends ListNode {
        
        public ProcessorNode( String name ) {
            super( name );
            _popup = new JPopupMenu();
            JMenuItem menuItem2 = new JMenuItem( "Make " + this.name() + " the head node" );
            menuItem2.addActionListener(new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    _headNode.setText( name() );
                }
            });
            _popup.add( menuItem2 );
        }
        
        public void cores( int newVal ) { _cores = newVal; }
        public int cores() { return _cores; }
        
        protected int _cores;
        
    }
    
    /*
     * Fill the processor and data source lists from the Hardware Monitor.
     */
    protected void loadHardwareLists() {
        //  We need to "relocate" everything in the existing processor list, so unset a
        //  "found" flag for each.
        for ( Iterator<BrowserNode> iter = _processorsPane.browserTopNode().children().iterator();
                iter.hasNext(); )
            ( (ListNode)(iter.next()) ).foundIt = false;        
        //  These are all of the processing nodes that the hardware monitor knows
        //  about.  See if they are in the list.
        for ( Iterator<BrowserNode> iter = _settings.hardwareMonitor().clusterNodes().children().iterator();
                iter.hasNext(); ) {
            BrowserNode thisModule = iter.next();
            if ( !( (ClusterNode)(thisModule) ).ignore() ) {
                BrowserNode foundNode = null;
                //  Is this processor in our list?
                for ( Iterator<BrowserNode> iter2 = _processorsPane.browserTopNode().children().iterator();
                        iter2.hasNext() && foundNode == null; ) {
                    BrowserNode testNode = iter2.next();
                    if ( testNode.name() == thisModule.name() ) {
                        foundNode = testNode;
                        ( (ListNode)(testNode) ).foundIt = true;
                    }
                }
                //  New node?  Then add it to the list.
                if ( foundNode == null ) {
                    ProcessorNode newNode = new ProcessorNode( thisModule.name() );
                    newNode.foundIt = true;
                    newNode.selected( !_processorsEdited );
                    _processorsPane.addNode( newNode );
                    if ( _headNode.getText() == null || _headNode.getText().contentEquals( "" ) )
                        _headNode.setText( thisModule.name() );
                }
            }
        }
        //  Now purge the list of any items that were not "found"....
        for ( Iterator<BrowserNode> iter = _processorsPane.browserTopNode().children().iterator();
                iter.hasNext(); ) {
            ListNode testNode = (ListNode)(iter.next());
            if ( !testNode.foundIt )
                _processorsPane.browserTopNode().removeChild( testNode );
            else {
                //  This lets us know if anyone is editing the list.  If so, we
                //  don't add new items "selected" by default.
                if ( !testNode.selected() )
                    _processorsEdited = true;
            }
        }

        //  Do the same stuff for the data source list
        for ( Iterator<BrowserNode> iter = _dataSourcesPane.browserTopNode().children().iterator();
                iter.hasNext(); )
            ( (ListNode)(iter.next()) ).foundIt = false;        
        //  These are all of the processing nodes that the hardware monitor knows
        //  about.  See if they are in the list.
        for ( Iterator<BrowserNode> iter = _settings.hardwareMonitor().mk5Modules().children().iterator();
                iter.hasNext(); ) {
            BrowserNode thisModule = iter.next();
            if ( !( (Mark5Node)(thisModule) ).ignore() ) {
                BrowserNode foundNode = null;
                //  Is this processor in our list?
                for ( Iterator<BrowserNode> iter2 = _dataSourcesPane.browserTopNode().children().iterator();
                        iter2.hasNext() && foundNode == null; ) {
                    BrowserNode testNode = iter2.next();
                    if ( testNode.name() == thisModule.name() ) {
                        foundNode = testNode;
                        ( (ListNode)(testNode) ).foundIt = true;
                    }
                }
                //  New node?  Then add it to the list.
                if ( foundNode == null ) {
                    ListNode newNode = new ListNode( thisModule.name() );
                    newNode.foundIt = true;
                    newNode.selected( !_dataSourcesEdited );
                    _dataSourcesPane.addNode( newNode );
                }
            }
        }
        //  Now purge the list of any items that were not "found"....
        for ( Iterator<BrowserNode> iter = _dataSourcesPane.browserTopNode().children().iterator();
                iter.hasNext(); ) {
            ListNode testNode = (ListNode)(iter.next());
            if ( !testNode.foundIt )
                _dataSourcesPane.browserTopNode().removeChild( testNode );
            else {
                //  This lets us know if anyone is editing the list.  If so, we
                //  don't add new items "selected" by default.
                if ( !testNode.selected() )
                    _dataSourcesEdited = true;
            }
        }

    }
    
    /*
     * Called once a second to update things.
     */
    protected void timeoutIntervalEvent() {
        if ( this.isVisible() )
            loadHardwareLists();
    }
    
    /*
     * Override the to load hardware lists before display.
     */
    @Override
    public void setVisible( boolean newVal ) {
        if ( newVal )
            loadHardwareLists();
        super.setVisible( newVal );
    }
    
    protected EventListenerList _stateChangeListeners;
    protected JobNode _jobNode;
    protected SystemSettings _settings;
    
    protected NodeBrowserScrollPane _scrollPane;
    
    protected JMenuBar _menuBar;
    protected JTextField _headNode;
    protected JLabel _headNodeLabel;
    protected NodeBrowserScrollPane _dataSourcesPane;
    protected JLabel _dataSourcesLabel;
    protected NodeBrowserScrollPane _processorsPane;
    protected JLabel _processorsLabel;
    protected NumberBox _threads;
    protected JLabel _threadsLabel;
    
    protected boolean _allObjectsBuilt;
    
    protected HardwareMonitorPanel _hardwareMonitor;
    
    protected boolean _processorsEdited;
    protected boolean _dataSourcesEdited;
}
