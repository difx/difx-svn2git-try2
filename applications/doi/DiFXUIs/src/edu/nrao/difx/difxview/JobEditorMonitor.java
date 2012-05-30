/*
 * This window contains all of the settings for a single job, as well as controls
 * and displays to run it and monitor its progress.
 */
package edu.nrao.difx.difxview;

import edu.nrao.difx.difxutilities.SendMessage;
import edu.nrao.difx.difxutilities.DiFXCommand;
import edu.nrao.difx.xmllib.difxmessage.ObjectFactory;
import edu.nrao.difx.xmllib.difxmessage.Header;
import edu.nrao.difx.xmllib.difxmessage.Body;
import edu.nrao.difx.xmllib.difxmessage.DifxStart;
import edu.nrao.difx.xmllib.difxmessage.DifxStop;
import edu.nrao.difx.xmllib.difxmessage.DifxMessage;

import edu.nrao.difx.difxcontroller.JAXBDiFXProcessor;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JTextField;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JPopupMenu;
import javax.swing.JButton;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.Timer;
import java.awt.Color;
import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import java.util.*;

import javax.swing.event.EventListenerList;

import mil.navy.usno.widgetlib.IndexedPanel;
import mil.navy.usno.widgetlib.NodeBrowserScrollPane;
import mil.navy.usno.widgetlib.NumberBox;
import mil.navy.usno.widgetlib.BrowserNode;
import mil.navy.usno.widgetlib.SimpleTextEditor;
import mil.navy.usno.widgetlib.MessageDisplayPanel;

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
        this.setBounds( 500, 100, _settings.windowConfiguration().jobEditorMonitorWindowW,
                _settings.windowConfiguration().jobEditorMonitorWindowH );
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
        
        //  This panel shows us the input file.  This can actually be edited,
        //  but editing won't do anything.
        IndexedPanel inputFilePanel = new IndexedPanel( "View Input File" );
        inputFilePanel.openHeight( 400 );
        inputFilePanel.closedHeight( 25 );
        inputFilePanel.open( false );
        _scrollPane.addNode( inputFilePanel );
        _inputFileEditor = new SimpleTextEditor();
        inputFilePanel.add( _inputFileEditor );
        _refreshInputButton = new JButton( "Read .input File" );
        _refreshInputButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _jobNode.requestInputFile();
            }
        } );
        inputFilePanel.add( _refreshInputButton );

        //  This panel shows us the input file.  This can actually be edited,
        //  but editing won't do anything.
        IndexedPanel calcFilePanel = new IndexedPanel( "View Calc File" );
        calcFilePanel.openHeight( 400 );
        calcFilePanel.closedHeight( 25 );
        calcFilePanel.open( false );
        _scrollPane.addNode( calcFilePanel );
        _calcFileEditor = new SimpleTextEditor();
        calcFilePanel.add( _calcFileEditor );
        _refreshCalcButton = new JButton( "Read .calc File" );
        _refreshCalcButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _jobNode.requestCalcFile();
            }
        } );
        calcFilePanel.add( _refreshCalcButton );

        IndexedPanel machinesListPanel = new IndexedPanel( "Machines List" );
        machinesListPanel.openHeight( 395 );
        machinesListPanel.closedHeight( 20 );
        _scrollPane.addNode( machinesListPanel );
        _dataSourcesPane = new NodeBrowserScrollPane();
        _dataSourcesPane.setBackground( Color.WHITE );
        machinesListPanel.add( _dataSourcesPane );
        _dataSourcesLabel = new JLabel( "Data Nodes:" );
        _dataSourcesLabel.setHorizontalAlignment( JLabel.LEFT );
        machinesListPanel.add( _dataSourcesLabel );
        _processorsPane = new NodeBrowserScrollPane();
        _processorsPane.setBackground( Color.WHITE );
        machinesListPanel.add( _processorsPane );
        _processorsLabel = new JLabel( "Processor Nodes:" );
        _processorsLabel.setHorizontalAlignment( JLabel.LEFT );
        machinesListPanel.add( _processorsLabel );
        _threadsLabel = new JLabel( "Threads:" );
        _threadsLabel.setHorizontalAlignment( JLabel.RIGHT );
        machinesListPanel.add( _threadsLabel );
        _headNode = new JFormattedTextField();
        _headNode.setFocusLostBehavior( JFormattedTextField.COMMIT );
        _headNode.setText( _settings.difxControlAddress() );
        machinesListPanel.add( _headNode );
        _headNodeLabel = new JLabel( "HeadNode:" );
        _headNodeLabel.setHorizontalAlignment( JLabel.LEFT );
        machinesListPanel.add( _headNodeLabel );
        _machinesApplyPopup = new JPopupMenu();
        _thisJobItem = new JMenuItem( "This Job Only" );
        _thisJobItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                applyThisJob();
            }
        } );
        _machinesApplyPopup.add( _thisJobItem );
        _passJobsItem = new JMenuItem( "All Jobs in Pass" );
        _passJobsItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                applyPass();
            }
        } );
        _machinesApplyPopup.add( _passJobsItem );
        _selectedJobsItem = new JMenuItem( "All Selected Jobs" );
        _selectedJobsItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                applySelected();
            }
        } );
        _machinesApplyPopup.add( _selectedJobsItem );
        _allJobsItem = new JMenuItem( "All Jobs" );
        _allJobsItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                applyAll();
            }
        } );
        _machinesApplyPopup.add( _allJobsItem );
        _applyButton = new JButton( "Apply To... \u25bc" );
        _applyButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                applyToAction();
            }
        } );
        machinesListPanel.add( _applyButton );
        _machinesLock = new JCheckBox( "Lock From Apply" );
        _machinesLock.setToolTipText( "Protect these settings from \"universal\" appications (apply all, apply selected, etc) by other jobs." );
        machinesListPanel.add( _machinesLock );
        
        IndexedPanel runControlPanel = new IndexedPanel( "Run Controls" );
        runControlPanel.openHeight( 200 );
        runControlPanel.closedHeight( 20 );
        _scrollPane.addNode( runControlPanel );
        _startButton = new JButton( "Start" );
        _startButton.setBounds( 10, 30, 110, 25 );
        _startButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                startJob();
            }
        } );
        runControlPanel.add( _startButton );
        _stopButton = new JButton( "Stop" );
        _stopButton.setBounds( 10, 60, 110, 25 );
        _stopButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                stopJob();
            }
        } );
        runControlPanel.add( _stopButton );
        _restartAt = new JCheckBox( "Restart" );
        _restartAt.setBounds( 130, 30, 75, 25 );
        _restartAt.setSelected( false );
        runControlPanel.add( _restartAt );
        _restartSeconds = new NumberBox();
        _restartSeconds.precision( 4 );
        _restartSeconds.value( 0.0 );
        _restartSeconds.setBounds( 230, 30, 100, 25 );
        runControlPanel.add( _restartSeconds );
        _forceOverwrite = new JCheckBox( "Force Overwrite" );
        _forceOverwrite.setToolTipText( "Force the overwrite of the \".difx\" output file if one already exists." );
        _forceOverwrite.setSelected( true );
        _forceOverwrite.setBounds( 130, 60, 150, 25 );
        runControlPanel.add( _forceOverwrite );
        JLabel restartAtLabel = new JLabel( "at:" );
        restartAtLabel.setBounds( 180, 30, 45, 25 );
        restartAtLabel.setHorizontalAlignment( JLabel.RIGHT );
        runControlPanel.add( restartAtLabel );
        JLabel restartSecondsLabel = new JLabel( "seconds" );
        restartSecondsLabel.setBounds( 335, 30, 90, 25 );
        restartSecondsLabel.setHorizontalAlignment( JLabel.LEFT );
        runControlPanel.add( restartSecondsLabel );
        
        //  The Status Panel shows the current state of the job.
        IndexedPanel statusPanel = new IndexedPanel( "" );
        _scrollPane.addNode( statusPanel );
        statusPanel.openHeight( 50 );
        statusPanel.alwaysOpen( true );
        statusPanel.noArrow( true );
        
        //  The message panel shows raw message data pertaining to the job.
        IndexedPanel messagePanel = new IndexedPanel( "Messages" );
        _scrollPane.addNode( messagePanel );
        messagePanel.openHeight( 200 );
        messagePanel.closedHeight( 25 );
        _messageDisplayPanel = new MessageDisplayPanel();
        messagePanel.add( _messageDisplayPanel );
 
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
        //  The current sizes are saved in the settings...but we can't be certain
        //  the _settings variable has been set yet.
        if ( _settings != null ) {
            _settings.windowConfiguration().jobEditorMonitorWindowW = w;
            _settings.windowConfiguration().jobEditorMonitorWindowH = h;
        }
        if ( _menuBar != null )
            _menuBar.setBounds( 0, 0, w, 25 );
        if ( _allObjectsBuilt ) {
            _scrollPane.setBounds( 0, 25, w, h - 25 );
            int thirdSize = ( w - 60 ) / 3;
            _dataSourcesLabel.setBounds( 10, 25, w - 50, 25 );
            _dataSourcesPane.setBounds( 10, 50, w - 50, 150 );
            _processorsLabel.setBounds( 10, 205, 2 * thirdSize, 25 );
            _processorsPane.setBounds( 10, 230, 2 * thirdSize, 150 );
            _threadsLabel.setBounds( 170, 205, 80, 25 );
            _headNodeLabel.setBounds( 30 + 2 * thirdSize, 205, thirdSize, 25 );
            _headNode.setBounds( 30 + 2 * thirdSize, 230, thirdSize, 25 );
            _applyButton.setBounds( 30 + 2 * thirdSize, 355, thirdSize/2 - 5, 25 );
            _machinesLock.setBounds( 30 + 3 * thirdSize - thirdSize/2 - 5, 355, thirdSize/2 - 5, 25 );
            _inputFileEditor.setBounds( 10, 30, w - 35, 360 );
            _calcFileEditor.setBounds( 10, 30, w - 35, 360 );
            _refreshInputButton.setBounds( w - 185, 2, 150, 20 );
            _refreshCalcButton.setBounds( w - 185, 2, 150, 20 );
            _messageDisplayPanel.setBounds( 2, 25, w - 23, 173 );
        }
    }
    
    public boolean machinesLock() { return _machinesLock.isSelected(); }
    
    public void applyToAction() {
        _machinesApplyPopup.show( _applyButton, 25, 25 );
    }
    
    public void applyThisJob() {
        /*
        for ( Iterator<BrowserNode> iter = _dataSourcesPane.browserTopNode().children().iterator();
                iter.hasNext(); ) {
            DataNode thisNode = (DataNode)(iter.next());
            if ( thisNode.selected() )
                System.out.println( thisNode.name() );
        }
         * 
         */
        for ( Iterator<BrowserNode> iter = _processorsPane.browserTopNode().children().iterator();
                iter.hasNext(); ) {
            ProcessorNode thisNode = (ProcessorNode)(iter.next());
            if ( thisNode.selected() )
                System.out.println( thisNode.name() + " " + thisNode.threadsText() );
        }
    }
    
    public Iterator<BrowserNode> dataSourcesIterator() {
        return _dataSourcesPane.browserTopNode().children().iterator();
    }
    
    public Iterator<BrowserNode> processorsIterator() {
        return _processorsPane.browserTopNode().children().iterator();
    }
    
    /*
     * Copy the settings of our machines to another set of machines in the
     * "target".  It is assumed in doing this that both have the same list of
     * machines, just different settings.  Hope this is safe!
     */
    protected void copyMachineSettings( JobEditorMonitor target ) {
        //  Make sure the target has up to date hardware lists before we do anything.
        target.loadHardwareLists();
        if ( target.lockFromApply() )
            return;
        if ( target == this )
            return;
        /*
        Iterator<BrowserNode> targetIter = target.dataSourcesIterator();
        for ( Iterator<BrowserNode> iter = _dataSourcesPane.browserTopNode().children().iterator();
                iter.hasNext() && targetIter.hasNext(); ) {
            DataNode thisNode = (DataNode)(iter.next());
            DataNode targetNode = (DataNode)(targetIter.next());
            targetNode.selected( thisNode.selected() );               
        }
         * 
         */
        Iterator<BrowserNode> targetIter = target.processorsIterator();
        for ( Iterator<BrowserNode> iter = _processorsPane.browserTopNode().children().iterator();
                iter.hasNext() && targetIter.hasNext(); ) {
            ProcessorNode thisNode = (ProcessorNode)(iter.next());
            ProcessorNode targetNode = (ProcessorNode)(targetIter.next());
            targetNode.selected( thisNode.selected() );
            targetNode.threads( thisNode.threads() );
        }
        target.headNode( headNode() );
        
    }
    
    public void applyPass() {
        for ( Iterator<BrowserNode> iter = _jobNode.passNode().childrenIterator(); iter.hasNext(); ) {
            JobNode thisJob = (JobNode)(iter.next());
            copyMachineSettings( thisJob.editorMonitor() );
        }
    }
    
    public void applySelected() {
        Iterator<BrowserNode> iter = _settings.queueBrowser().experimentsIterator();
        iter.next();  //  skip the header!!!
        for ( ; iter.hasNext(); ) {
            ExperimentNode thisExperiment = (ExperimentNode)(iter.next());
            for ( Iterator<BrowserNode> iter2 = thisExperiment.childrenIterator(); iter2.hasNext(); ) {
                PassNode thisPass = (PassNode)(iter2.next());
                for ( Iterator<BrowserNode> iter3 = _jobNode.passNode().childrenIterator(); iter3.hasNext(); ) {
                    JobNode thisJob = (JobNode)(iter3.next());
                    if ( thisJob.selected() )
                        copyMachineSettings( thisJob.editorMonitor() );
                }
            }
        }
    }
    
    public void applyAll() {
        Iterator<BrowserNode> iter = _settings.queueBrowser().experimentsIterator();
        iter.next();  //  skip the header!!!
        for ( ; iter.hasNext(); ) {
            ExperimentNode thisExperiment = (ExperimentNode)(iter.next());
            for ( Iterator<BrowserNode> iter2 = thisExperiment.childrenIterator(); iter2.hasNext(); ) {
                PassNode thisPass = (PassNode)(iter2.next());
                for ( Iterator<BrowserNode> iter3 = _jobNode.passNode().childrenIterator(); iter3.hasNext(); ) {
                    JobNode thisJob = (JobNode)(iter3.next());
                    copyMachineSettings( thisJob.editorMonitor() );
                }
            }
        }
    }
    
    public boolean lockFromApply() {
        return _machinesLock.isSelected();
    }
    
    public String headNode() { return _headNode.getText(); }
    public void headNode( String newVal ) { _headNode.setText( newVal ); }
    
    /*
     * This function runs the job based on all current settings.  Jobs can be
     * run using a TCP connection to guiServer, or through mk5daemon via UDP.  If
     * using the former, a thread is started to monitor the job progress via a
     * dedicated socket and report any errors.  In the latter case the job start
     * instruction is more of a "set and forget" kind of operation.
     */
    synchronized public void startJob() {
        DiFXCommand command = new DiFXCommand( _settings );
        command.header().setType( "DifxStart" );
        command.mpiProcessId( "-1" );
        command.identifier( _jobNode.name() );
        int monitorPort = 0;

        // Create start job command
        DifxStart jobStart = command.factory().createDifxStart();
        jobStart.setInput( _jobNode.inputFile() );

        // If we are using the TCP connection, set the address and port for diagnostic
        // reporting.
        if ( _settings.sendCommandsViaTCP() ) {
            try {
                jobStart.setAddress( java.net.InetAddress.getLocalHost().getHostAddress() );
            } catch ( java.net.UnknownHostException e ) {
            }
            monitorPort = _settings.newDifxTransferPort();
            jobStart.setPort( monitorPort );
        }
        
        jobStart.setFunction( "USNO" );

        // -- manager, enabled only
        DifxStart.Manager manager = command.factory().createDifxStartManager();
        manager.setNode( _headNode.getText() );
        jobStart.setManager( manager );

        // -- set difx version to use
        jobStart.setDifxVersion( _settings.difxVersion() );
        
        //  Set the "restart" time in seconds from the job start, if this has been
        //  requested.
        if ( _restartAt.isSelected() )
            jobStart.setRestartSeconds( _restartSeconds.value() );

        // -- datastreams, enabled only
        DifxStart.Datastream dataStream = command.factory().createDifxStartDatastream();

        //  Include all of the "checked" data stream node names...
        String dataNodeNames = "";
        for ( Iterator<BrowserNode> iter = _dataSourcesPane.browserTopNode().children().iterator();
                iter.hasNext(); ) {
            DataNode thisNode = (DataNode)(iter.next());
            dataNodeNames += thisNode.name() + " ";
        }
        dataStream.setNodes( dataNodeNames );
        jobStart.setDatastream(dataStream);

        // Add enabled processors and threads.  Don't include processors that have no
        // threads!
        String processNodeNames = "";
        for ( Iterator<BrowserNode> iter = _processorsPane.browserTopNode().children().iterator();
                iter.hasNext(); ) {
            ProcessorNode thisNode = (ProcessorNode)(iter.next());
            if ( thisNode.selected() ) {
                DifxStart.Process process = command.factory().createDifxStartProcess();
                process.setNodes( thisNode.name() );
                process.setThreads( thisNode.threadsText() );
                jobStart.getProcess().add( process );
            }
        }

        // force deletion of existing output file if this box has been checked.
        if ( _forceOverwrite.isSelected() )
            jobStart.setForce( 1 );
        else
            jobStart.setForce( 0 ); 
        
        //  Set up a monitor thread if this job is being run using guiServer.  This
        //  thread will collect and interpret diagnostic messages directly from
        //  guiServer as it sets up and runs the job.  If the job is being run by
        //  mk5daemon, it is simply started and forgotten.
        if ( _settings.sendCommandsViaTCP() ) {
            RunningJobMonitor runMonitor = new RunningJobMonitor( monitorPort );
            runMonitor.start();
        }

        // -- Create the XML defined messages and process through the system
        command.body().setDifxStart(jobStart);
        try {
            command.sendPacket( _settings.guiServerConnection().COMMAND_PACKET );
        } catch ( java.net.UnknownHostException e ) {
            java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.SEVERE, null,
                    e.getMessage() );  //BLAT should be a pop-up
        }
    }
    
    /*
     * This thread opens and monitors a TCP socket for diagnostic reports from the
     * guiServer as it runs a job.
     */
    protected class RunningJobMonitor extends Thread {
        
        public RunningJobMonitor( int port ) {
            _port = port;
        }
        
        /*
         * These packet types are sent by the "JobMonitorConnection" class in the
         * guiServer application on the DiFX host.
         */
        protected final int JOB_TERMINATED                   = 100;
        protected final int JOB_ENDED_GRACEFULLY             = 101;
        protected final int JOB_STARTED                      = 102;
        protected final int PARAMETER_CHECK_IN_PROGRESS      = 103;
        protected final int PARAMETER_CHECK_SUCCESS          = 104;
        protected final int FAILURE_NO_HEADNODE              = 105;
        protected final int FAILURE_NO_DATASOURCES           = 106;
        protected final int FAILURE_NO_PROCESSORS            = 107;
        protected final int FAILURE_NO_INPUTFILE_SPECIFIED   = 108;
        protected final int FAILURE_INPUTFILE_NOT_FOUND      = 109;
        protected final int FAILURE_INPUTFILE_NAME_TOO_LONG  = 110;
        
        @Override
        public void run() {
            //  Open a new server socket and await a connection.  The connection
            //  will timeout after a given number of seconds (nominally 10).
            try {
                ServerSocket ssock = new ServerSocket( _port );
                ssock.setSoTimeout( 10000 );  //  timeout is in millisec
                try {
                    Socket sock = ssock.accept();
                    System.out.println( "got socket connection" );
//                    acceptCallback();
                    //  Turn the socket into a "data stream", which has useful
                    //  functions.
                    DataInputStream in = new DataInputStream( sock.getInputStream() );
                    
                    //  Loop collecting diagnostic packets from the guiServer.  These
                    //  are identified by an initial integer, and then are followed
                    //  by a data length, then data.
                    boolean connected = true;
                    while ( connected ) {
                        System.out.println( "getting next packet" );
                        //  Read the packet type as an integer.  The packet types
                        //  are defined above (within this class).
                        int packetType = in.readInt();
                        //  Read the size of the incoming data (bytes).
                        int packetSize = in.readInt();
                        //  Read the data (as raw bytes)
                        byte [] data = null;
                        if ( packetSize > 0 ) {
                            data = new byte[packetSize];
                            in.readFully( data );
                        }
//                    _inString = "";
//                    incrementalCallback();
//                    while ( _inString.length() < _fileSize ) {
//                        int sz = _fileSize - _inString.length();
//                        if ( sz > 1024 )
//                            sz = 1024;
//                        byte [] data = new byte[sz];
//                        int n = in.read( data, 0, sz );
//                        //_inString += in.readUTF();
//                        _inString += new String( Arrays.copyOfRange( data, 0, n ) );
//                        incrementalCallback();
                        System.out.println( "packet type is " + packetType );
                        System.out.println( "packet size is " + packetSize );
                        //  Interpret the packet type.
                        if ( packetType == JOB_TERMINATED ) {
                            _messageDisplayPanel.warning( 0, "job monitor", "Job terminated prematurely." );
                            connected = false;
                        }
                        else if ( packetType == JOB_ENDED_GRACEFULLY ) {
                            _messageDisplayPanel.warning( 0, "job monitor", "Job finished gracefully." );
                            connected = false;
                        }
                        else if ( packetType == JOB_STARTED ) {
                            _messageDisplayPanel.message( 0, "job monitor", "Job started by guiServer." );
                        }
                        else if ( packetType == PARAMETER_CHECK_IN_PROGRESS ) {
                            _messageDisplayPanel.message( 0, "job monitor", "Checking parameters." );
                        }
                        else if ( packetType == PARAMETER_CHECK_SUCCESS ) {
                            _messageDisplayPanel.message( 0, "job monitor", "Parameter check successful." );
                        }
                        else if ( packetType == FAILURE_NO_HEADNODE ) {
                            _messageDisplayPanel.error( 0, "job monitor", "No headnone was specified." );
                        }
                        else if ( packetType == FAILURE_NO_DATASOURCES ) {
                            _messageDisplayPanel.error( 0, "job monitor", "No valid data sources were specified." );
                        }
                        else if ( packetType == FAILURE_NO_PROCESSORS ) {
                            _messageDisplayPanel.error( 0, "job monitor", "No valid processors were specified." );
                        }
                        else if ( packetType == FAILURE_NO_INPUTFILE_SPECIFIED ) {
                            _messageDisplayPanel.error( 0, "job monitor", "No input file was specified." );
                        }
                        else if ( packetType == FAILURE_INPUTFILE_NOT_FOUND ) {
                            _messageDisplayPanel.error( 0, "job monitor", "Input file " + _jobNode.inputFile() + " was not found on DiFX host." );
                        }
                        else if ( packetType == FAILURE_INPUTFILE_NAME_TOO_LONG ) {
                            _messageDisplayPanel.message( 0, "job monitor", "Input file name \"" + _jobNode.inputFile() + "\" is too long for DiFX." );
                        }
                        else {
                            _messageDisplayPanel.warning( 0, "GUI", "Ignoring unrecongized job monitor packet type (" + packetType + ")." );
                        }
                    }
                    sock.close();
                } catch ( SocketTimeoutException e ) {
//                    _fileSize = -10;
                }
                ssock.close();
            } catch ( java.io.IOException e ) {
                e.printStackTrace();
//                _error = "IOException : " + e.toString();
//                _fileSize = -11;
            }
//            endCallback();
        }
        
        protected int _port;
        
    }
    
    public void pauseJob() {}
    
    public void stopJob() {
        ObjectFactory factory = new ObjectFactory();

        // Create header
        Header header = factory.createHeader();
        header.setFrom( "doi" );
        header.setTo( _settings.difxControlAddress() );
        header.setMpiProcessId( "0" );
        header.setIdentifier( _jobNode.name() );
        header.setType( "DifxStop" );

        // Create start job command
        DifxStop jobStop = factory.createDifxStop();
        jobStop.setInput( _jobNode.inputFile() );

        // -- Create the XML defined messages and process through the system
        Body body = factory.createBody();
        body.setDifxStop( jobStop );

        DifxMessage difxMsg = factory.createDifxMessage();
        difxMsg.setHeader( header );
        difxMsg.setBody( body );

        JAXBDiFXProcessor xmlProc = new JAXBDiFXProcessor(difxMsg);
        String xmlString = xmlProc.ConvertToXML();
        
        if ( xmlString != null )
            try {
            SendMessage.writeToSocket( xmlString, _settings );
            }
            catch ( Exception e ) {
               java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.SEVERE, null,
                       e.getMessage() );  //BLAT should be a pop-up
            }
    }
    
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
    protected class DataNode extends BrowserNode {
        
        public DataNode( String name ) {
            super( name );
        }
        
        @Override
        public void createAdditionalItems() {
            _dataObject = new JLabel( "" );
            this.add( _dataObject );
        }
        
        @Override
        public void positionItems() {
            _label.setBounds( 10, 0, 190, _ySize );
            _dataObject.setBounds( 200, 0, 500, 25 );
        }
        
        public void dataObject( String newVal ) { _dataObject.setText( newVal ); }
        public String dataObject() { return _dataObject.getText(); }
        
        protected JLabel _dataObject;
    }
    
    /*
     * This class provides information about a "Processor".  It allows
     * the user to use the processor as the "head node" and tracks the number of
     * cores available.
     */
    protected class ProcessorNode extends BrowserNode {
        
        public ProcessorNode( String name ) {
            super( name );
        }
        
        @Override
        public void createAdditionalItems() {
            _selected = new JCheckBox();
            _selected.setBackground( Color.WHITE );
            this.add( _selected );
            _popup = new JPopupMenu();
            JMenuItem menuItem2 = new JMenuItem( "Make " + this.name() + " the head node" );
            menuItem2.addActionListener(new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    _headNode.setText( name() );
                }
            });
            _popup.add( menuItem2 );
            _threads = new NumberBox();
            _threads.intValue( 0 );
            _threads.minimum( 0 );
            this.add( _threads );
        }
        
        @Override
        public void positionItems() {
            super.positionItems();
            _selected.setBounds( 7, 2, 18, 18 );
            _threads.setBounds( 210, 1, 30, 18 );
        }
        
        public void cores( int newVal ) { _cores = newVal; }
        public int cores() { return _cores; }
        public void threads( int newVal ) { _threads.intValue( newVal ); }
        public Integer threads() { return _threads.intValue(); }
        public String threadsText() { return _threads.getText(); }
        
        public boolean selected() { return _selected.isSelected(); }
        public void selected( boolean newVal ) { _selected.setSelected( newVal ); }
        
        protected int _cores;
        protected NumberBox _threads;
        public boolean foundIt;
        protected JCheckBox _selected;
        
    }
    
    /*
     * Fill the processor list from the Hardware Monitor and the data sources
     * list from our collected information.
     */
    synchronized public void loadHardwareLists() {
        //  We need to "relocate" everything in the existing processor list, so unset a
        //  "found" flag for each.
        for ( Iterator<BrowserNode> iter = _processorsPane.browserTopNode().children().iterator();
                iter.hasNext(); )
            ( (ProcessorNode)(iter.next()) ).foundIt = false;        
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
                        ( (ProcessorNode)(testNode) ).foundIt = true;
                    }
                }
                //  New node?  Then add it to the list.
                if ( foundNode == null ) {
                    ProcessorNode newNode = new ProcessorNode( thisModule.name() );
                    newNode.threads( ((ClusterNode)(thisModule)).numCores() );
                    newNode.foundIt = true;
                    newNode.selected( !_processorsEdited );
                    _processorsPane.addNode( newNode );
                }
            }
        }
        //  Now purge the list of any items that were not "found"....
        for ( Iterator<BrowserNode> iter = _processorsPane.browserTopNode().children().iterator();
                iter.hasNext(); ) {
            ProcessorNode testNode = (ProcessorNode)(iter.next());
            if ( !testNode.foundIt )
                _processorsPane.browserTopNode().removeChild( testNode );
            else {
                //  This lets us know if anyone is editing the list.  If so, we
                //  don't add new items "selected" by default.
                if ( !testNode.selected() )
                    _processorsEdited = true;
            }
        }

        //  The data source list is built using the known data requirements (which
        //  we get by parsing the .input file).  These are contained in the "_dataSources"
        //  hash table.  This is a list of data items needed to run the job, along
        //  with data sources if they are known.  We can also search here for data
        //  sources, and check that those we know about are on line.
        _dataSourcesPane.browserTopNode().clearChildren();
        if ( _dataObjects != null ) {
            for ( Iterator<String> jter = _dataObjects.iterator(); jter.hasNext(); ) {
                String dataObject = jter.next();
                DataNode newNode = new DataNode( _dataSources.get( dataObject ) );
                _dataSourcesPane.addNode( newNode );
            }
        }
        /*
         * PROBABLY GETTING RID OF THIS JUNK
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
         * 
         */

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
    
    /*
     * Parse the string data as if it came from an .input file (which, presumably,
     * it did).
     */
    synchronized public void parseInputFile( String str ) {
        
        //  This is a list that holds our data sources and the files/modules/whatever
        //  associated with them (keyed by file/module/whatever).
        if ( _dataSources == null )
            _dataSources = new HashMap<String,String>();
        if ( _dataObjects == null )
            _dataObjects = new ArrayList<String>();
        _dataSources.clear();
        _dataObjects.clear();

        _inputFileEditor.text( str );
        Scanner strScan = new Scanner( str );
        strScan.useDelimiter( System.getProperty( "line.separator" ) );

        while ( strScan.hasNext() ) {
            String sInput = strScan.next();

            if (sInput.contains("DELAY FILENAME:")) {
                sInput = sInput.substring(sInput.indexOf(":") + 1);
//                setDelayFile(sInput.trim());
            } else if (sInput.contains("UVW FILENAME:")) {
                sInput = sInput.substring(sInput.indexOf(":") + 1);
//                setUvwFile(sInput.trim());
            } else if (sInput.contains("CORE CONF FILENAME:")) {
                sInput = sInput.substring(sInput.indexOf(":") + 1);
            } else if (sInput.contains("CALC FILENAME:")) {
                _jobNode.calcFile( sInput.substring(sInput.indexOf(":") + 1).trim() );
//                setCoreConfigFile(sInput.trim());
            } else if ( sInput.contains( "EXECUTE TIME (SEC):" ) ) {
                sInput = sInput.substring( sInput.indexOf(":") + 1 );
                executeTime( Integer.parseInt( sInput.trim() ) );
            } else if ( sInput.contains( "START MJD:" ) ) {
                sInput = sInput.substring(sInput.indexOf(":") + 1);
                startMJD( Integer.parseInt( sInput.trim() ) );
            } else if (sInput.contains("START SECONDS:")) {
                //  Throws away any fractional seconds...
                sInput = sInput.substring(sInput.indexOf(":") + 1);
                if (sInput.contains(".")) {
                    sInput = sInput.substring(0, sInput.indexOf("."));
                }
                startSeconds( Integer.parseInt( sInput.trim() ) );
            } else if (sInput.contains("ACTIVE DATASTREAMS:")) {
                sInput = sInput.substring(sInput.indexOf(":") + 1);
//                setActiveDatastreams(Integer.parseInt(sInput.trim()));
            } else if (sInput.contains("ACTIVE BASELINES:")) {
                sInput = sInput.substring(sInput.indexOf(":") + 1);
//                setActiveBaselines(Integer.parseInt(sInput.trim()));
            } else if ( sInput.contains( "VIS BUFFER LENGTH:" ) ) {
                sInput = sInput.substring(sInput.indexOf(":") + 1);
            } else if ( sInput.contains( "OUTPUT FORMAT:" ) ) {
                sInput = sInput.substring(sInput.indexOf(":") + 1);
            } else if ( sInput.contains( "OUTPUT FILENAME:" )) {
                sInput = sInput.substring(sInput.indexOf(":") + 1);
                _jobNode.outputFile( sInput.trim() );
            } else if (sInput.contains("NUM CHANNELS:")) {
                sInput = sInput.substring(sInput.indexOf(":") + 1);
//                setNumChannels(Integer.parseInt(sInput.trim()));
            } else if (sInput.contains("TELESCOPE ENTRIES:")) {
                sInput = sInput.substring(sInput.indexOf(":") + 1);
                _jobNode.numAntennas( Integer.parseInt( sInput.trim() ) );
            } else if (sInput.contains("TELESCOPE NAME ")) {
                // Create antenna for the job
//                Module newMod = new Module();
//                newMod.setObjType("Module");
//
//                String sInputObjID = sInput.substring(sInput.indexOf(":") - 2, sInput.indexOf(":"));
//                String sInputObjName = sInput.substring(sInput.indexOf(":") + 1);
//
//                // Note: the .input file is zero based
//                newMod.setObjId(Integer.parseInt(sInputObjID.trim()));
//                newMod.setObjName(sInputObjName.trim());
//
//                addModule(newMod);
            } else if (sInput.contains("FILE ")) {
                //  The "FILE" line has lists the data sources used in a job.  We need
                //  to use these to make a list of data sources for eventual transmission
                //  to mk5daemon when starting a job.
                String inputSourceID = sInput.substring(sInput.indexOf("/") - 2, sInput.indexOf("/"));
                String inputObject = sInput.substring(sInput.indexOf(":") + 1).trim();
                
                //  Find the data source associated with this input object.  First,
                //  search the list of file sources we have.
                String dataSource = null;
                if ( _editor != null ) {
                    dataSource = _editor.nodeForFile( inputObject );
                }
                
                //  Failing that, see if a mark5 unit has this data source as a module.
                if ( dataSource == null ) {
                    for ( Iterator<BrowserNode> iter = _settings.hardwareMonitor().mk5Modules().children().iterator();
                        iter.hasNext(); ) {
                        Mark5Node thisNode = (Mark5Node)iter.next();
                        if ( !thisNode.ignore() ) {
                            if ( thisNode.bankAVSN() != null && inputObject.contentEquals( thisNode.bankAVSN() ) )
                                dataSource = thisNode.name();
                            if ( thisNode.bankBVSN() != null && inputObject.contentEquals( thisNode.bankBVSN() ) )
                                dataSource = thisNode.name();
                        }
                    }
                }
                
                
                //  Put the data source that we've found for this object in our hash
                //  table...including "null" if we've failed to find it!
                _dataObjects.add( inputObject );
                _dataSources.put( inputObject, dataSource );

            }
        }
        
        _jobNode.jobStart( (double)startMJD() + (double)startSeconds() / 24.0 / 3600.0 );
        _jobNode.jobDuration( (double)executeTime() / 24.0 / 3600.0 );
        _jobNode.updateDatabase( "outputFile", _jobNode.outputFile() );
        _jobNode.updateDatabase( "jobStart", _jobNode.jobStart().toString() );
        _jobNode.updateDatabase( "jobDuration", _jobNode.jobDuration().toString() );
        _jobNode.updateDatabase( "numAntennas", _jobNode.numAntennas().toString() );

    }
    
    /*
     * Give this job a link to the editor, which we need for some things.
     */
    public void editor( ExperimentEditor newVal ) { _editor = newVal; }

    /*
     * Parse the string data as if it came from an .input file (which, presumably,
     * it did).
     */
    public void parseCalcFile( String str ) {

        _calcFileEditor.text( str );
        Scanner strScan = new Scanner( str );
        strScan.useDelimiter( System.getProperty( "line.separator" ) );

        while ( strScan.hasNext() ) {
            String sCalc = strScan.next();
                if (sCalc.contains("JOB ID:")) {
                    sCalc = sCalc.substring(sCalc.indexOf(":") + 1);
//                    setJobID(sCalc.trim());
                } else if (sCalc.contains("OBSCODE:")) {
                    sCalc = sCalc.substring(sCalc.indexOf(":") + 1);
//                    setObsCode(sCalc.trim());
                } else if (sCalc.contains("JOB START TIME:")) {
                    sCalc = sCalc.substring(sCalc.indexOf(":") + 1);
//                    setJobStartTimeMJD( new BigDecimal( sCalc.trim() ) );
                } else if (sCalc.contains("JOB STOP TIME:")) {
                    sCalc = sCalc.substring(sCalc.indexOf(":") + 1);
//                    setJobStopTimeMJD(new BigDecimal(sCalc.trim()));
                } else if (sCalc.contains("NUM TELESCOPES:")) {
                    sCalc = sCalc.substring(sCalc.indexOf(":") + 1);
//                    setNumTelescopes(Integer.parseInt(sCalc.trim()));
                } else if (sCalc.contains("DIFX VERSION:")) {
                    sCalc = sCalc.substring(sCalc.indexOf(":") + 1);
                    _jobNode.difxVersion( sCalc.trim() );
//                    setDifxVersion(sCalc.trim());
                } else if ( sCalc.contains( "DUTY CYCLE:" ) ) {
                    sCalc = sCalc.substring( sCalc.indexOf( ":" ) + 1 );
                    _jobNode.dutyCycle( Double.parseDouble( sCalc.trim() ) );
                } else if (sCalc.contains("NAME:")) {
                    sCalc = sCalc.substring(sCalc.indexOf(":") + 1);
                } else if (sCalc.contains("SHELF:")) {
                    String sCalcObjID = sCalc.substring(sCalc.indexOf("SHELF") - 3, sCalc.indexOf("SHELF"));
                    String sCalcShelf = sCalc.substring(sCalc.indexOf(":") + 1);

                    // Find antenna via object ID
///                    Module curMod = getModule(Integer.parseInt(sCalcObjID.trim()));

                    // update the antenna's shelf
//                    curMod.setShelf(sCalcShelf.trim());

                    //newJob.setNumTelescopes(Integer.parseInt(trimmed));
                }
        }

        _jobNode.updateDatabase( "difxVersion", _jobNode.difxVersion() );
        _jobNode.updateDatabase( "dutyCycle", _jobNode.dutyCycle().toString() );

//            frCalc.close();
            //System.out.printf("***************** Data model read input and calc file complete. \n");
    }
    
    public Integer startMJD() { return _startMJD; }
    public void startMJD( Integer newVal ) { _startMJD = newVal; }
    public Integer startSeconds() { return _startSeconds; }
    public void startSeconds( Integer newVal ) { _startSeconds = newVal; }
    public Integer executeTime() { return _executeTime; }
    public void executeTime( Integer newVal ) { _executeTime = newVal; }
   
    protected EventListenerList _stateChangeListeners;
    protected JobNode _jobNode;
    protected SystemSettings _settings;
    
    protected NodeBrowserScrollPane _scrollPane;
    
    protected JMenuBar _menuBar;
    protected JFormattedTextField _headNode;
    protected JLabel _headNodeLabel;
    protected NodeBrowserScrollPane _dataSourcesPane;
    protected JLabel _dataSourcesLabel;
    protected NodeBrowserScrollPane _processorsPane;
    protected JLabel _processorsLabel;
    protected JLabel _threadsLabel;
    protected JPopupMenu _machinesApplyPopup;
    protected JMenuItem _thisJobItem;
    protected JMenuItem _passJobsItem;
    protected JMenuItem _selectedJobsItem;
    protected JMenuItem _allJobsItem;
    protected JButton _applyButton;
    protected JCheckBox _machinesLock;
    protected JCheckBox _forceOverwrite;
    
    protected JButton _startButton;
    protected JButton _stopButton;
    protected NumberBox _restartSeconds;
    protected JCheckBox _restartAt;
    
    protected boolean _allObjectsBuilt;
    
    protected HardwareMonitorPanel _hardwareMonitor;
    
    protected boolean _processorsEdited;
    protected boolean _dataSourcesEdited;
    protected SimpleTextEditor _inputFileEditor;
    protected SimpleTextEditor _calcFileEditor;
    protected JButton _refreshInputButton;
    protected JButton _refreshCalcButton;
    protected ExperimentEditor _editor;
    
    protected HashMap<String,String> _dataSources;
    protected ArrayList<String> _dataObjects;
    
    protected Integer _executeTime;
    protected Integer _startMJD;
    protected Integer _startSeconds;
    
    protected MessageDisplayPanel _messageDisplayPanel;
    
}
