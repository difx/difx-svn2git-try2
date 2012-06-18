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
import edu.nrao.difx.xmllib.difxmessage.DifxMachinesDefinition;

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
import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
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
        
        //  This panel shows us the input file, which can be edited and sent to
        //  the DiFX host.  The existing file on the DiFX host can also be downloaded.
        IndexedPanel inputFilePanel = new IndexedPanel( "Input File" );
        inputFilePanel.openHeight( 400 );
        inputFilePanel.closedHeight( 25 );
        inputFilePanel.open( false );
        _scrollPane.addNode( inputFilePanel );
        _inputFileEditor = new SimpleTextEditor();
        inputFilePanel.add( _inputFileEditor );
        _inputFileName = new JLabel( "" );
        inputFilePanel.add( _inputFileName );
        _refreshInputButton = new JButton( "Refresh" );
        _refreshInputButton.setToolTipText( "Read the Input File stored on the DiFX host." );
        _refreshInputButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                statusInfo( "Obtaining file \"" + _inputFileName.getText() + "\" from DiFX host." );
                Component comp = _refreshInputButton;
                while ( comp.getParent() != null )
                    comp = comp.getParent();
                Point pt = _refreshInputButton.getLocationOnScreen();
                GetFileMonitor getFile = new GetFileMonitor(  (Frame)comp, pt.x + 25, pt.y + 25,
                        _inputFileName.getText(), _settings );
                if ( getFile.inString() != null && getFile.inString().length() > 0 ) {
                    _inputFileEditor.text( getFile.inString() );
                    parseInputFile( _inputFileEditor.text() );
                }
            }
        } );
        inputFilePanel.add( _refreshInputButton );
        _uploadInputButton = new JButton( "Save" );
        _uploadInputButton.setToolTipText( "Parse all settings from the editor text and upload to the Input File location on the DiFX host (not necessary unless you have changed the text)." );
        _uploadInputButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                parseInputFile( _inputFileEditor.text() );
                Component comp = _uploadInputButton;
                while ( comp.getParent() != null )
                    comp = comp.getParent();
                Point pt = _uploadInputButton.getLocationOnScreen();
                SendFileMonitor sendFile = new SendFileMonitor(  (Frame)comp, pt.x + 25, pt.y + 25,
                        _inputFileName.getText(), _inputFileEditor.text(), _settings );
            }
        } );
        inputFilePanel.add( _uploadInputButton );

        //  This panel shows us the calc file, which can be edited and sent to
        //  the DiFX host.  The existing file on the DiFX host can also be downloaded.
        IndexedPanel calcFilePanel = new IndexedPanel( "Calc File" );
        calcFilePanel.openHeight( 400 );
        calcFilePanel.closedHeight( 25 );
        calcFilePanel.open( false );
        _scrollPane.addNode( calcFilePanel );
        _calcFileEditor = new SimpleTextEditor();
        calcFilePanel.add( _calcFileEditor );
        _calcFileName = new JLabel( "" );
        calcFilePanel.add( _calcFileName );
        _refreshCalcButton = new JButton( "Refresh" );
        _refreshCalcButton.setToolTipText( "Read the Calc File as it is stored on the DiFX host." );
        _refreshCalcButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                Component comp = _refreshCalcButton;
                while ( comp.getParent() != null )
                    comp = comp.getParent();
                Point pt = _refreshCalcButton.getLocationOnScreen();
                GetFileMonitor getFile = new GetFileMonitor(  (Frame)comp, pt.x + 25, pt.y + 25,
                        _calcFileName.getText(), _settings );
                if ( getFile.inString() != null && getFile.inString().length() > 0 ) {
                    _calcFileEditor.text( getFile.inString() );
                    parseInputFile( _calcFileEditor.text() );
                }
            }
        } );
        calcFilePanel.add( _refreshCalcButton );
        _uploadCalcButton = new JButton( "Save" );
        _uploadCalcButton.setToolTipText( "Parse all settings from the editor text and upload to the Calc File location on the DiFX host (not necessary unless you have changed the text)." );
        _uploadCalcButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                parseCalcFile( _calcFileEditor.text() );
                Component comp = _uploadCalcButton;
                while ( comp.getParent() != null )
                    comp = comp.getParent();
                Point pt = _uploadCalcButton.getLocationOnScreen();
                SendFileMonitor sendFile = new SendFileMonitor(  (Frame)comp, pt.x + 25, pt.y + 25,
                        _calcFileName.getText(), _calcFileEditor.text(), _settings );
            }
        } );
        calcFilePanel.add( _uploadCalcButton );

        IndexedPanel machinesListPanel = new IndexedPanel( "Machines List" );
        machinesListPanel.openHeight( 395 );
        machinesListPanel.closedHeight( 25 );
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
        _coresLabel = new JLabel( "Cores:" );
        _coresLabel.setHorizontalAlignment( JLabel.RIGHT );
        machinesListPanel.add( _coresLabel );
        _cpuUsageLabel = new JLabel( "% CPU Usage:" );
        _cpuUsageLabel.setHorizontalAlignment( JLabel.RIGHT );
        machinesListPanel.add( _cpuUsageLabel );
        _mpiTestLabel = new JLabel( "mpi Test:" );
        _mpiTestLabel.setHorizontalAlignment( JLabel.RIGHT );
        machinesListPanel.add( _mpiTestLabel );
        _headNode = new JFormattedTextField();
        _headNode.setFocusLostBehavior( JFormattedTextField.COMMIT );
        _headNode.setText( _settings.difxControlAddress() );
        machinesListPanel.add( _headNode );
        _headNodeLabel = new JLabel( "HeadNode:" );
        _headNodeLabel.setHorizontalAlignment( JLabel.LEFT );
        machinesListPanel.add( _headNodeLabel );
        _applyMachinesButton = new JButton( "Apply" );
        _applyMachinesButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                //  Track whether this button was ever pushed - indicating the user
                //  has already generated the .threads and .machines files.  This
                //  is used later when the "Start" button is pushed - it will need
                //  to generate these files if the user has not.
                _machinesAppliedByHand = true;
                applyMachinesAction();
            }
        } );
        machinesListPanel.add( _applyMachinesButton );
        _eliminateNonrespondingProcessors = new JCheckBox( "Eliminate Non-Responding Processors" );
        _eliminateNonrespondingProcessors.setSelected( _settings.defaultNames().eliminateNonrespondingProcessors );
        _eliminateNonrespondingProcessors.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _settings.defaultNames().eliminateNonrespondingProcessors = _eliminateNonrespondingProcessors.isSelected();
            }
        } );
        machinesListPanel.add( _eliminateNonrespondingProcessors );
        _eliminateBusyProcessors = new JCheckBox( "Eliminate Processors Over" );
        _eliminateBusyProcessors.setSelected( _settings.defaultNames().eliminateNonrespondingProcessors );
        _eliminateBusyProcessors.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                changeBusyProcessorsSettings();
                _settings.defaultNames().eliminateBusyProcessors = _eliminateBusyProcessors.isSelected();
            }
        } );
        machinesListPanel.add( _eliminateBusyProcessors );
        _busyPercentage = new NumberBox();
        _busyPercentage.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                changeBusyProcessorsSettings();
                _settings.defaultNames().busyPercentage = _busyPercentage.value();
            }
        } );
        _busyPercentage.precision( 0 );
        _busyPercentage.value( _settings.defaultNames().busyPercentage );
        _busyPercentage.limits( 0.0, 100.0 );
        machinesListPanel.add( _busyPercentage );
        _busyPercentageLabel = new JLabel( "% Busy" );
        machinesListPanel.add( _busyPercentageLabel );
        _chooseBasedOnModule = new JCheckBox( "Choose Data Source Based on Module" );
        _chooseBasedOnModule.setSelected( _settings.defaultNames().chooseBasedOnModule );
        _chooseBasedOnModule.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                checkDataSourceList();
                _settings.defaultNames().chooseBasedOnModule = _chooseBasedOnModule.isSelected();
            }
        } );
        machinesListPanel.add( _chooseBasedOnModule );

        //  This panel shows us the machines file, which can be edited and sent to
        //  the DiFX host.  The existing file on the DiFX host can also be downloaded.
        IndexedPanel machinesFilePanel = new IndexedPanel( "Machines File" );
        machinesFilePanel.openHeight( 300 );
        machinesFilePanel.closedHeight( 25 );
        machinesFilePanel.open( false );
        _scrollPane.addNode( machinesFilePanel );
        _machinesFileEditor = new SimpleTextEditor();
        machinesFilePanel.add( _machinesFileEditor );
        _machinesFileName = new JLabel( "" );
        machinesFilePanel.add( _machinesFileName );
        _refreshMachinesButton = new JButton( "Refresh" );
        _refreshMachinesButton.setToolTipText( "Read the Machines File as it is stored on the DiFX host." );
        _refreshMachinesButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                Component comp = _refreshMachinesButton;
                while ( comp.getParent() != null )
                    comp = comp.getParent();
                Point pt = _refreshMachinesButton.getLocationOnScreen();
                GetFileMonitor getFile = new GetFileMonitor(  (Frame)comp, pt.x + 25, pt.y + 25,
                        _machinesFileName.getText(), _settings );
                if ( getFile.inString() != null && getFile.inString().length() > 0 ) {
                    _machinesFileEditor.text( getFile.inString() );
                }
            }
        } );
        machinesFilePanel.add( _refreshMachinesButton );
        _uploadMachinesButton = new JButton( "Save" );
        _uploadMachinesButton.setToolTipText( "Parse all settings from the editor text and upload to the Machines File location on the DiFX host (not necessary unless you have changed the text)." );
        _uploadMachinesButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _machinesAppliedByHand = true;
                Component comp = _uploadMachinesButton;
                while ( comp.getParent() != null )
                    comp = comp.getParent();
                Point pt = _uploadMachinesButton.getLocationOnScreen();
                SendFileMonitor sendFile = new SendFileMonitor(  (Frame)comp, pt.x + 25, pt.y + 25,
                        _machinesFileName.getText(), _machinesFileEditor.text(), _settings );
                statusInfo( "Sent machines file content (" + _machinesFileEditor.text().length()
                        + " chars) to \"" + _machinesFileName.getText() + "\" on DiFX host." );
            }
        } );
        machinesFilePanel.add( _uploadMachinesButton );

        //  This panel shows us the threads file, which can be edited and sent to
        //  the DiFX host.  The existing file on the DiFX host can also be downloaded.
        IndexedPanel threadsFilePanel = new IndexedPanel( "Threads File" );
        threadsFilePanel.openHeight( 300 );
        threadsFilePanel.closedHeight( 25 );
        threadsFilePanel.open( false );
        _scrollPane.addNode( threadsFilePanel );
        _threadsFileEditor = new SimpleTextEditor();
        threadsFilePanel.add( _threadsFileEditor );
        _threadsFileName = new JLabel( "" );
        threadsFilePanel.add( _threadsFileName );
        _refreshThreadsButton = new JButton( "Refresh" );
        _refreshThreadsButton.setToolTipText( "Read the Threads File as it is stored on the DiFX host." );
        _refreshThreadsButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                statusInfo( "Reading thread file \"" + _threadsFileName.getText() + "\" from DiFX host." );
                Component comp = _refreshThreadsButton;
                while ( comp.getParent() != null )
                    comp = comp.getParent();
                Point pt = _refreshThreadsButton.getLocationOnScreen();
                GetFileMonitor getFile = new GetFileMonitor(  (Frame)comp, pt.x + 25, pt.y + 25,
                        _threadsFileName.getText(), _settings );
                if ( getFile.inString() != null && getFile.inString().length() > 0 ) {
                    statusInfo( "\"" + _threadsFileName.getText() + "\" (" + getFile.inString().length() + " chars) read from DiFX host." );
                    _threadsFileEditor.text( getFile.inString() );
                }
                else {
                    if ( getFile.inString() == null )
                        statusWarning( "No \"" + _threadsFileName.getText() + "\" file found on DiFX host." );
                    else
                        statusWarning( "\"" + _threadsFileName.getText() + "\" was zero length on DiFX host." );
                }
            }
        } );
        threadsFilePanel.add( _refreshThreadsButton );
        _uploadThreadsButton = new JButton( "Save" );
        _uploadThreadsButton.setToolTipText( "Parse all settings from the editor text and upload to the Threads File location on the DiFX host (not necessary unless you have changed the text)." );
        _uploadThreadsButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _machinesAppliedByHand = true;
                Component comp = _uploadThreadsButton;
                while ( comp.getParent() != null )
                    comp = comp.getParent();
                Point pt = _uploadThreadsButton.getLocationOnScreen();
                SendFileMonitor sendFile = new SendFileMonitor(  (Frame)comp, pt.x + 25, pt.y + 25,
                        _threadsFileName.getText(), _threadsFileEditor.text(), _settings );
                statusInfo( "Sent threads file content (" + _threadsFileEditor.text().length()
                        + " chars) to \"" + _threadsFileName.getText() + "\" on DiFX host." );
            }
        } );
        threadsFilePanel.add( _uploadThreadsButton );

        IndexedPanel runControlPanel = new IndexedPanel( "Run Controls" );
        runControlPanel.openHeight( 100 );
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
        statusPanel.setBackground( Color.GRAY );
        _statusLabel = new JLabel( "" );
        _statusLabel.setHorizontalAlignment( JLabel.RIGHT );
        statusPanel.add( _statusLabel );
        
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
        //  The current sizes are saved in the settings...but we can't be certain
        //  the _settings variable has been set yet.
        if ( _settings != null ) {
            _settings.windowConfiguration().jobEditorMonitorWindowW = this.getWidth();
            _settings.windowConfiguration().jobEditorMonitorWindowH = this.getHeight();
        }
        if ( _allObjectsBuilt ) {
            int w = this.getContentPane().getSize().width;
            int h = this.getContentPane().getSize().height;
            _menuBar.setBounds( 0, 0, w, 25 );
            _scrollPane.setBounds( 0, 25, w, h - 25 );
            int thirdSize = ( w - 60 ) / 3;
            _dataSourcesLabel.setBounds( 10, 25, 2 * thirdSize, 25 );
            _dataSourcesPane.setBounds( 10, 50, 2 * thirdSize, 150 );
            _chooseBasedOnModule.setBounds( 30 + 2 * thirdSize, 50, thirdSize, 25 );
            _processorsLabel.setBounds( 10, 205, 2 * thirdSize, 25 );
            _processorsPane.setBounds( 10, 230, 2 * thirdSize, 150 );
            _threadsLabel.setBounds( 170, 205, 80, 25 );
            _coresLabel.setBounds( 240, 205, 80, 25 );
            _cpuUsageLabel.setBounds( 340, 205, 100, 25 );
            _mpiTestLabel.setBounds( 440, 205, 100, 25 );
            _headNodeLabel.setBounds( 30 + 2 * thirdSize, 205, thirdSize, 25 );
            _headNode.setBounds( 30 + 2 * thirdSize, 230, thirdSize, 25 );
            _applyMachinesButton.setBounds( 30 + 2 * thirdSize, 355, thirdSize/2 - 5, 25 );
            _eliminateNonrespondingProcessors.setBounds( 30 + 2 * thirdSize, 295, thirdSize, 25 );
            _eliminateBusyProcessors.setBounds( 30 + 2 * thirdSize, 325, thirdSize - 110, 25 );
            _busyPercentage.setBounds( w - 135, 325, 30, 25 );
            _busyPercentageLabel.setBounds( w - 100, 325, 100, 25 );
            _inputFileEditor.setBounds( 10, 60, w - 35, 330 );
            _calcFileEditor.setBounds( 10, 60, w - 35, 330 );
            _inputFileName.setBounds( 10, 30, w - 350, 25 );
            _calcFileName.setBounds( 10, 30, w - 350, 25 );
            _refreshInputButton.setBounds( w - 125, 30, 100, 25 );
            _refreshCalcButton.setBounds( w - 125, 30, 100, 25 );
            _uploadInputButton.setBounds( w - 230, 30, 100, 25 );
            _uploadCalcButton.setBounds( w - 230, 30, 100, 25 );
            _machinesFileEditor.setBounds( 10, 60, w - 35, 230 );
            _machinesFileName.setBounds( 10, 30, w - 350, 25 );
            _refreshMachinesButton.setBounds( w - 125, 30, 100, 25 );
            _uploadMachinesButton.setBounds( w - 230, 30, 100, 25 );
            _threadsFileEditor.setBounds( 10, 60, w - 35, 230 );
            _threadsFileName.setBounds( 10, 30, w - 350, 25 );
            _refreshThreadsButton.setBounds( w - 125, 30, 100, 25 );
            _uploadThreadsButton.setBounds( w - 230, 30, 100, 25 );
            _messageDisplayPanel.setBounds( 2, 25, w - 23, 173 );
            _statusLabel.setBounds( 10, 0, w - 35, 25 );
        }
    }
    
    public void statusInfo( String newText ) {
        _statusLabel.setForeground( Color.BLACK );
        _statusLabel.setText( newText );
    }
    
    public void statusWarning( String newText ) {
        _statusLabel.setForeground( Color.YELLOW );
        _statusLabel.setText( newText );
    }
    
    public void statusError( String newText ) {
        _statusLabel.setForeground( Color.RED );
        _statusLabel.setText( newText );
    }
    
    public void inputFileName( String newName ) { _inputFileName.setText( newName ); }
    
    public void calcFileName( String newName ) { _calcFileName.setText( newName ); }
    
    public ProcessorNode processorNodeByName( String name ) {
        for ( Iterator<BrowserNode> iter = _processorsPane.browserTopNode().children().iterator();
                iter.hasNext(); ) {
            ProcessorNode thisNode = (ProcessorNode)(iter.next());
            if ( thisNode.name().contentEquals( name ) )
                return thisNode;
        }
        return null;
    }

    public DataNode dataNodeByName( String name ) {
        for ( Iterator<BrowserNode> iter = _dataSourcesPane.browserTopNode().children().iterator();
                iter.hasNext(); ) {
            DataNode thisNode = (DataNode)(iter.next());
            if ( thisNode.name().contentEquals( name ) )
                return thisNode;
        }
        return null;
    }

    public BrowserNode nodeByName( String name ) {
        for ( Iterator<BrowserNode> iter = _processorsPane.browserTopNode().children().iterator();
                iter.hasNext(); ) {
            BrowserNode thisNode = iter.next();
            if ( thisNode.name().contentEquals( name ) )
                return thisNode;
        }
        for ( Iterator<BrowserNode> iter = _dataSourcesPane.browserTopNode().children().iterator();
                iter.hasNext(); ) {
            BrowserNode thisNode = iter.next();
            if ( thisNode.name().contentEquals( name ) )
                return thisNode;
        }
        return null;
    }

    /*
     * Send the current machines and thread settings to guiServer to produce .machines
     * and .threads files.
     */
    public void applyMachinesAction() {
        DiFXCommand command = new DiFXCommand( _settings );
        command.header().setType( "DifxMachinesDefinition" );
        command.mpiProcessId( "-1" );
        command.identifier( _jobNode.name() );
        int monitorPort = 0;

        //  We are going to test processor nodes, so remove the results of any previous
        //  tests.
        for ( Iterator<BrowserNode> iter = _processorsPane.browserTopNode().children().iterator();
                iter.hasNext(); ) {
            ( (ProcessorNode)(iter.next()) ).clearTest();
        }
        
        // Create start job command
        DifxMachinesDefinition cmd = command.factory().createDifxMachinesDefinition();
        cmd.setInput( _jobNode.inputFile() );

        // If we are using the TCP connection, set the address and port for diagnostic
        // reporting.
        if ( _settings.sendCommandsViaTCP() ) {
            try {
                cmd.setAddress( java.net.InetAddress.getLocalHost().getHostAddress() );
            } catch ( java.net.UnknownHostException e ) {
            }
            monitorPort = _settings.newDifxTransferPort();
            cmd.setPort( monitorPort );
        }
        
        // -- manager, enabled only
        DifxMachinesDefinition.Manager manager = command.factory().createDifxMachinesDefinitionManager();
        manager.setNode( _headNode.getText() );
        cmd.setManager( manager );

        // -- datastreams, enabled only
        DifxMachinesDefinition.Datastream dataStream = command.factory().createDifxMachinesDefinitionDatastream();

        //  Include all of the "checked" data stream node names...
        String dataNodeNames = "";
        for ( Iterator<BrowserNode> iter = _dataSourcesPane.browserTopNode().children().iterator();
                iter.hasNext(); ) {
            DataNode thisNode = (DataNode)(iter.next());
            if ( thisNode.selected() )
                dataNodeNames += thisNode.name() + " ";
        }
        dataStream.setNodes( dataNodeNames );
        cmd.setDatastream(dataStream);

        // Add enabled processors and threads.  Don't include processors that have no
        // threads!
        String processNodeNames = "";
        for ( Iterator<BrowserNode> iter = _processorsPane.browserTopNode().children().iterator();
                iter.hasNext(); ) {
            ProcessorNode thisNode = (ProcessorNode)(iter.next());
            if ( thisNode.selected() ) {
                DifxMachinesDefinition.Process process = command.factory().createDifxMachinesDefinitionProcess();
                process.setNodes( thisNode.name() );
                process.setThreads( thisNode.threadsText() );
                cmd.getProcess().add( process );
            }
        }
        
        //  Test the processors if the user wants to (this will generate feedback that
        //  will be reflected in the processors list).
        if ( _eliminateNonrespondingProcessors.isSelected() )
            cmd.setTestProcessors( true );
        else
            cmd.setTestProcessors( false );
        
        //  Send the names for the machine and thread files.  For the moment we are basing
        //  these on the input file name.
        String fileStr = _inputFileName.getText();
        cmd.setMachinesFile( fileStr.substring( 0, fileStr.lastIndexOf( '.' ) + 1 ).trim() + "machines" );
        cmd.setThreadsFile( fileStr.substring( 0, fileStr.lastIndexOf( '.' ) + 1 ).trim() + "threads" );

        //  Set up a monitor thread to collect and interpret diagnostic messages from
        //  guiServer as it sets up the threads and machine files.
        if ( _settings.sendCommandsViaTCP() ) {
            MachinesDefinitionMonitor monitor = new MachinesDefinitionMonitor( monitorPort );
            monitor.start();
        }
        
        statusInfo( "Using criteria to create .machines and .threads files." );

        // -- Create the XML defined messages and process through the system
        command.body().setDifxMachinesDefinition( cmd );
        try {
            //command.sendPacket( _settings.guiServerConnection().COMMAND_PACKET );
            command.send();
        } catch ( java.net.UnknownHostException e ) {
            java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.SEVERE, null,
                    e.getMessage() );  //BLAT should be a pop-up
        }        
    }
    
    /*
     * This thread opens and monitors a TCP socket for diagnostic reports from the
     * guiServer as it sets up thread and machine files.  The opposite side of this
     * communication link is in the file "guiServer/src/machinesDefinition.cpp" in
     * the DiFX source tree.
     */
    protected class MachinesDefinitionMonitor extends Thread {
        
        public MachinesDefinitionMonitor( int port ) {
            _port = port;
        }
        
        /*
         * These packet types are sent by the "JobMonitorConnection" class in the
         * guiServer application on the DiFX host.
         */
        protected final int TASK_TERMINATED                     = 100;
        protected final int TASK_ENDED_GRACEFULLY               = 101;
        protected final int TASK_STARTED                        = 102;
        protected final int PARAMETER_CHECK_IN_PROGRESS         = 103;
        protected final int PARAMETER_CHECK_SUCCESS             = 104;
        protected final int FAILURE_NO_HEADNODE                 = 105;
        protected final int FAILURE_NO_DATASOURCES              = 106;
        protected final int FAILURE_NO_PROCESSORS               = 107;
        protected final int WARNING_NO_MACHINES_FILE_SPECIFIED  = 108;
        protected final int WARNING_NO_THREADS_FILE_SPECIFIED   = 109;
        protected final int THREADS_FILE_NAME                   = 110;
        protected final int MACHINES_FILE_NAME                  = 111;
        protected final int FAILURE_NO_FILES_SPECIFIED          = 112;
        protected final int FAILURE_OPEN_MACHINES_FILE          = 113;
        protected final int FAILURE_OPEN_THREADS_FILE           = 114;
        protected final int MACHINES_FILE_CREATED               = 115;
        protected final int THREADS_FILE_CREATED                = 116;
        protected final int FAILURE_FILE_REMOVAL                = 117;
        protected final int FAILURE_POPEN                       = 118;
        protected final int FAILURE_MPIRUN                      = 119;
        protected final int SUCCESS_MPIRUN                      = 120;
        protected final int LOW_THREAD_COUNT                    = 121;
        
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
                    
                    //  Loop collecting diagnostic packets from the guiServer.  These
                    //  are identified by an initial integer, and then are followed
                    //  by a data length, then data.
                    boolean connected = true;
                    int workState = 0;
                    while ( connected ) {
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
                        //  Interpret the packet type.
                        if ( packetType == TASK_TERMINATED ) {
                            _messageDisplayPanel.warning( 0, "machines monitor", "Task terminated prematurely." );
                            connected = false;
                        }
                        else if ( packetType == TASK_ENDED_GRACEFULLY ) {
                            _messageDisplayPanel.warning( 0, "machines monitor", "Task finished gracefully." );
                            statusInfo( ".machines and .threads files created." );
                            connected = false;
                        }
                        else if ( packetType == TASK_STARTED ) {
                            _messageDisplayPanel.message( 0, "machines monitor", "Task started by guiServer." );
                        }
                        else if ( packetType == PARAMETER_CHECK_IN_PROGRESS ) {
                            _messageDisplayPanel.message( 0, "machines monitor", "Checking parameters." );
                        }
                        else if ( packetType == PARAMETER_CHECK_SUCCESS ) {
                            _messageDisplayPanel.message( 0, "machines monitor", "Parameter check successful." );
                        }
                        else if ( packetType == FAILURE_NO_HEADNODE ) {
                            _messageDisplayPanel.error( 0, "machines monitor", "No headnone was specified." );
                            statusError( "Headnode needs to be specified to create .machines and .threads files." );
                        }
                        else if ( packetType == FAILURE_NO_DATASOURCES ) {
                            _messageDisplayPanel.error( 0, "machines monitor", "No valid data streams were specified." );
                            statusError( "No valid data streams were specified - could not create .mahcines and .threads files." );
                        }
                        else if ( packetType == FAILURE_NO_PROCESSORS ) {
                            _messageDisplayPanel.error( 0, "machines monitor", "No valid processors were specified." );
                            statusError( "No valid processors were specified - could not create .mahcines and .threads files." );
                        }
                        else if ( packetType == WARNING_NO_MACHINES_FILE_SPECIFIED ) {
                            workState = packetType;
                            _messageDisplayPanel.message( 0, "machines monitor", "No machines file name was specified - forming one using input file name." );
                        }
                        else if ( packetType == MACHINES_FILE_NAME ) {
                            _machinesFileName.setText( new String( data ) );
                            _messageDisplayPanel.message( 0, "machines monitor", "Creating machines file \"" + _machinesFileName.getText() + "\"" );
                            statusInfo( "creating \"" + _machinesFileName.getText() + "\"" );
                        }
                        else if ( packetType == THREADS_FILE_NAME ) {
                            _threadsFileName.setText( new String( data ) );
                            _messageDisplayPanel.message( 0, "machines monitor", "Creating threads file \"" + _threadsFileName.getText() + "\"" );
                            statusInfo( "creating \"" + _threadsFileName.getText() + "\"" );
                        }
                        else if ( packetType == WARNING_NO_THREADS_FILE_SPECIFIED ) {
                            workState = packetType;
                            _messageDisplayPanel.message( 0, "machines monitor", "No threads file name was specified - forming one using input file name." );
                        }
                        else if ( packetType == FAILURE_NO_FILES_SPECIFIED ) {
                            if ( workState == WARNING_NO_MACHINES_FILE_SPECIFIED )
                                _messageDisplayPanel.message( 0, "machines monitor", "No input file name specified - unable to form machines file name." );
                            else if ( workState == WARNING_NO_THREADS_FILE_SPECIFIED )
                                _messageDisplayPanel.message( 0, "machines monitor", "No input file name specified - unable to form threads file name." );
                            else
                                _messageDisplayPanel.message( 0, "machines monitor", "Unknown error involving missing file names." );
                        }
                        else if ( packetType == FAILURE_OPEN_MACHINES_FILE ) {
                            _machinesFileName.setText( new String( data ) );
                            _messageDisplayPanel.message( 0, "machines monitor", "Failure to open machines file (" + new String( data ) + ")" );
                            statusError( "could not open machines file \"" + new String( data ) + "\"" );
                        }
                        else if ( packetType == FAILURE_OPEN_THREADS_FILE ) {
                            _machinesFileName.setText( new String( data ) );
                            _messageDisplayPanel.message( 0, "machines monitor", "Failure to open threads file (" + new String( data ) + ")" );
                            statusError( "could not open threads file \"" + new String( data ) + "\"" );
                        }
                        else if ( packetType == MACHINES_FILE_CREATED ) {
                            _messageDisplayPanel.message( 0, "machines monitor", "machines file created" );
                            //  Download the machines file to its editor.
                            Component comp = _applyMachinesButton;
                            while ( comp.getParent() != null )
                                comp = comp.getParent();
                            Point pt = _applyMachinesButton.getLocationOnScreen();
                            GetFileMonitor getFile = new GetFileMonitor(  (Frame)comp, pt.x + 25, pt.y + 25,
                                    _machinesFileName.getText(), _settings );
                            if ( getFile.inString() != null && getFile.inString().length() > 0 ) {
                                _machinesFileEditor.text( getFile.inString() );
                            }
                            _messageDisplayPanel.message( 0, "machines monitor", "machines file successfully downloaded" );
                            statusInfo( ".machines file created" );
                        }
                        else if ( packetType == THREADS_FILE_CREATED ) {
                            _messageDisplayPanel.message( 0, "machines monitor", "threads file created" );
                            //  Download the threads file to its editor.
                            Component comp = _applyMachinesButton;
                            while ( comp.getParent() != null )
                                comp = comp.getParent();
                            Point pt = _applyMachinesButton.getLocationOnScreen();
                            GetFileMonitor getFile = new GetFileMonitor(  (Frame)comp, pt.x + 25, pt.y + 25,
                                    _threadsFileName.getText(), _settings );
                            if ( getFile.inString() != null && getFile.inString().length() > 0 ) {
                                _threadsFileEditor.text( getFile.inString() );
                            }
                            _messageDisplayPanel.message( 0, "machines monitor", "threads file successfully downloaded" );
                            statusInfo( ".threads file created" );
                        }
                        else if ( packetType == FAILURE_FILE_REMOVAL ) {
                            _messageDisplayPanel.error( 0, "machines monitor", "Failed to remove file on DiFX host: " + new String( data ) );
                            statusError( "permissions prevent removal of a file on DiFX host" );
                        }
                        else if ( packetType == FAILURE_POPEN ) {
                            _messageDisplayPanel.error( 0, "machines monitor", "Popen failed on DiFX host: " + new String( data ) );
                        }
                        else if ( packetType == FAILURE_MPIRUN ) {
                            ProcessorNode node = processorNodeByName( new String( data ) );
                            if ( node != null )
                                node.mpiTest( false, _eliminateNonrespondingProcessors.isSelected() );
                            if ( _eliminateNonrespondingProcessors.isSelected() )
                                _messageDisplayPanel.warning( 0, "machines monitor", "Processing node " + new String( data ) + " failed mpirun test - it will not be used" );
                            else
                                _messageDisplayPanel.warning( 0, "machines monitor", "Processing node " + new String( data ) + " failed mpirun test" );
                        }
                        else if ( packetType == SUCCESS_MPIRUN ) {
                            ProcessorNode node = processorNodeByName( new String( data ) );
                            if ( node != null )
                                node.mpiTest( true, true );
                        }
                        else if ( packetType == LOW_THREAD_COUNT ) {
                            _messageDisplayPanel.error( 0, "machines monitor", "Number of processing threads is zero" );
                        }
                        else {
                            _messageDisplayPanel.warning( 0, "GUI", "Ignoring unrecongized job monitor packet type (" + packetType + ")." );
                        }
                    }
                    sock.close();
                } catch ( SocketTimeoutException e ) {
                }
                ssock.close();
            } catch ( java.io.IOException e ) {
                e.printStackTrace();
            }
        }
        
        protected int _port;
        
    }
    
    public void applyToAction() {
        _machinesApplyPopup.show( _applyToButton, 25, 25 );
    }
    
    /*
     * Called with the settings governing whether to check for "busy" processors
     * or how to define them are changed.
     */
    public void changeBusyProcessorsSettings() {
        for ( Iterator<BrowserNode> iter = _processorsPane.browserTopNode().children().iterator();
                iter.hasNext(); ) {
            ProcessorNode thisNode = (ProcessorNode)(iter.next());
            boolean keepIt = thisNode.cpuTest( (float)_busyPercentage.value(),
                    _eliminateBusyProcessors.isSelected()  );
        }
    }
    
    public Iterator<BrowserNode> dataSourcesIterator() {
        return _dataSourcesPane.browserTopNode().children().iterator();
    }
    
    public Iterator<BrowserNode> processorsIterator() {
        return _processorsPane.browserTopNode().children().iterator();
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
        //  Has the user already generated .threads and .machines files (which is
        //  done when the "Apply" button in the Machines List settings is pushed)?
        //  Alternatively, has the use edited and uploaded .machines and .threads
        //  files by hand?  If these things have not been done, the files need to
        //  be generated before running.
        if ( !_machinesAppliedByHand )
            applyMachinesAction();
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
            //command.sendPacket( _settings.guiServerConnection().COMMAND_PACKET );
            command.send();
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
            _selected = new JCheckBox();
            _selected.setBackground( Color.WHITE );
            //  Any time the user specifically selects an item, we change it state
            //  slightly - it can't be automatically selected or unselected based
            //  on cpu load.
            _selected.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    _handSelected = true;
                }
            });
            this.add( _selected );
            _dataObjectA = new JLabel( "" );
            this.add( _dataObjectA );
            _dataObjectB = new JLabel( "" );
            this.add( _dataObjectB );
        }
        
        @Override
        public void positionItems() {
            super.positionItems();
            _selected.setBounds( 7, 2, 18, 18 );
            _label.setBounds( 30, 0, 215, _ySize );
            _dataObjectA.setBounds( 250, 0, 500, 25 );
            _dataObjectB.setBounds( 400, 0, 500, 25 );
        }
        
        public void dataObjectA( String newVal ) { _dataObjectA.setText( newVal ); }
        public String dataObjectA() { return _dataObjectA.getText(); }
        public void dataObjectB( String newVal ) { _dataObjectB.setText( newVal ); }
        public String dataObjectB() { return _dataObjectB.getText(); }
        
        public void foundA( boolean newVal ) {
            _foundA = newVal;
            if ( newVal ) {
                _dataObjectA.setForeground( Color.BLUE );
            }
            else {
                _dataObjectA.setForeground( Color.GRAY );
            }
        }
        public void foundB( boolean newVal ) {
            _foundB = newVal;
            if ( newVal ) {
                _dataObjectB.setForeground( Color.BLUE );
            }
            else {
                _dataObjectB.setForeground( Color.GRAY );
            }
        }
        
        public void checkFound( boolean doCheck ) {
            if ( doCheck && !_handSelected ) {
                if ( _foundA || _foundB )
                    _selected.setSelected( true );
                else
                    _selected.setSelected( false );
            }
        }
        
        public void missingA() {
            _missing = true;
            _dataObjectA.setForeground( Color.RED );
        }
        public boolean missing() { return _missing; }
        
        public boolean selected() { return _selected.isSelected(); }
        public void selected( boolean newVal ) { _selected.setSelected( newVal ); }
        public void hideSelection() { _selected.setVisible( false ); }
        
        public boolean foundIt;
        protected JCheckBox _selected;
        protected boolean _handSelected;
        protected JLabel _dataObjectA;
        protected JLabel _dataObjectB;
        protected boolean _foundA;
        protected boolean _foundB;
        protected boolean _missing;
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
            //  Any time the user specifically selects an item, we change it state
            //  slightly - it can't be automatically selected or unselected based
            //  on cpu load.
            _selected.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    _handSelected = true;
                }
            });
            this.add( _selected );
            _popup = new JPopupMenu();
            JMenuItem menuItem2 = new JMenuItem( "Make " + this.name() + " the head node" );
            menuItem2.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    _headNode.setText( name() );
                }
            });
            _popup.add( menuItem2 );
            _threads = new NumberBox();
            _threads.setHorizontalAlignment( JTextField.RIGHT );
            _threads.intValue( 0 );
            _threads.minimum( 0 );
            this.add( _threads );
            _coresDisplay = new JLabel("");
            _coresDisplay.setHorizontalAlignment( JLabel.RIGHT );
            this.add( _coresDisplay );
            _cpuDisplay = new JLabel("");
            _cpuDisplay.setHorizontalAlignment( JLabel.RIGHT );
            this.add( _cpuDisplay );
            _mpiTestDisplay = new JLabel("");
            _mpiTestDisplay.setHorizontalAlignment( JLabel.RIGHT );
            this.add( _mpiTestDisplay );
        }
        
        @Override
        public void positionItems() {
            super.positionItems();
            _selected.setBounds( 7, 2, 18, 18 );
            _threads.setBounds( 210, 1, 30, 18 );
            _coresDisplay.setBounds( 280, 1, 30, 18 );
            _cpuDisplay.setBounds( 360, 1, 70, 18 );
            _mpiTestDisplay.setBounds( 460, 1, 70, 18 );
        }
        
        public void cores( int newVal ) { 
            _cores = newVal;
            _coresDisplay.setText( String.valueOf( newVal ) );
        }
        public int cores() { return _cores; }
        public void threads( int newVal ) { _threads.intValue( newVal ); }
        public Integer threads() { return _threads.intValue(); }
        public String threadsText() { return _threads.getText(); }
        public float cpu() { return Float.parseFloat( _cpuDisplay.getText() ); }
        public void cpu( String newVal ) {
            _cpuDisplay.setText( newVal );
        }
        /*
         * Change the color of the cpu load if it exceeds a limit.  Also, select
         * or deselect if desired.
         */
        public boolean cpuTest( float limit, boolean selectLimit ) {
            if ( Float.parseFloat( _cpuDisplay.getText() ) > limit ) {
                _cpuDisplay.setForeground( Color.RED );
                if ( selectLimit && !_handSelected )
                    _selected.setSelected( false );
                return false;
            }
            else {
                _cpuDisplay.setForeground( Color.BLACK );
                if ( selectLimit && !_handSelected )
                    _selected.setSelected( true );
                return true;
            }
        }
        /*
         * Set the result of the mpi test (and govern the selection if the test
         * failed).
         */
        public void mpiTest( boolean passed, boolean baseSelect ) {
            if ( passed ) {
                _mpiTestDisplay.setText( "OK" );
                _mpiTestDisplay.setForeground( Color.BLACK );
            }
            else {
                _mpiTestDisplay.setText( "Fail" );
                _mpiTestDisplay.setForeground( Color.RED );
                if ( baseSelect && !_handSelected ) {
                    _selected.setSelected( false );
                    _handSelected = true;
                }
            }           
        }
        
        public void clearTest() {
            _mpiTestDisplay.setText( "" );
        }
        
        public boolean selected() { return _selected.isSelected(); }
        public void selected( boolean newVal ) { _selected.setSelected( newVal ); }
        
        protected int _cores;
        protected NumberBox _threads;
        public boolean foundIt;
        protected JCheckBox _selected;
        protected JLabel _coresDisplay;
        protected JLabel _cpuDisplay;
        protected boolean _handSelected;
        protected JLabel _mpiTestDisplay;
        
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
                    newNode.cores( ((ClusterNode)(thisModule)).numCores() );
                    newNode.threads( ((ClusterNode)(thisModule)).numCores() );
                    newNode.cpu( ((ClusterNode)(thisModule)).cpuUsage() );
                    newNode.cpuTest( (float)_busyPercentage.value(),
                            _eliminateBusyProcessors.isSelected() );
                    newNode.foundIt = true;
                    newNode.selected( !_processorsEdited );
                    _processorsPane.addNode( newNode );
                }
                else {
                    ( (ProcessorNode)foundNode ).cpu( ((ClusterNode)(thisModule)).cpuUsage() );
                    ( (ProcessorNode)foundNode ).cpuTest( (float)_busyPercentage.value(),
                            _eliminateBusyProcessors.isSelected() );
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
//        _dataSourcesPane.browserTopNode().clearChildren();
//        if ( _dataObjects != null ) {
//            for ( Iterator<String> jter = _dataObjects.iterator(); jter.hasNext(); ) {
//                String dataObject = jter.next();
//                DataNode newNode = new DataNode( _dataSources.get( dataObject ) );
//                _dataSourcesPane.addNode( newNode );
//            }
//        }
        
        //  Do the same stuff for the data source list
        for ( Iterator<BrowserNode> iter = _dataSourcesPane.browserTopNode().children().iterator();
                iter.hasNext(); )
            ( (DataNode)(iter.next()) ).foundIt = false;        
        //  These are all of the processing nodes that the hardware monitor knows
        //  about.  See if they are in the list.
        for ( Iterator<BrowserNode> iter = _settings.hardwareMonitor().mk5Modules().children().iterator();
                iter.hasNext(); ) {
            Mark5Node thisModule = (Mark5Node)(iter.next());
            if ( !thisModule.ignore() ) {
                DataNode foundNode = null;
                //  Is this processor in our list?
                for ( Iterator<BrowserNode> iter2 = _dataSourcesPane.browserTopNode().children().iterator();
                        iter2.hasNext() && foundNode == null; ) {
                    DataNode testNode = (DataNode)(iter2.next());
                    if ( testNode.name() == thisModule.name() ) {
                        foundNode = testNode;
                        testNode.foundIt = true;
                    }
                }
                //  New node?  Then add it to the list.
                if ( foundNode == null ) {
                    foundNode = new DataNode( thisModule.name() );
                    foundNode.foundIt = true;
                    foundNode.selected( !_dataSourcesEdited );
                    _dataSourcesPane.addNode( foundNode );
                }
                //  Add the data modules (if this is a Mark5).
                foundNode.dataObjectA( thisModule.bankAVSN() );
                foundNode.dataObjectB( thisModule.bankBVSN() );
                //foundNode.dataObjectCheck( _dataObjects );
            }
        }
        //  Now purge the list of any items that were not "found"....
        for ( Iterator<BrowserNode> iter = _dataSourcesPane.browserTopNode().children().iterator();
                iter.hasNext(); ) {
                DataNode testNode = (DataNode)(iter.next());
                if ( !testNode.foundIt ) {
                    _dataSourcesPane.browserTopNode().remove( testNode );
                    iter.remove();
                }
                else {
                    //  This lets us know if anyone is editing the list.  If so, we
                    //  don't add new items "selected" by default.
                    if ( !testNode.selected() )
                        _dataSourcesEdited = true;
                }
        }
        _dataSourcesPane.updateUI();
        
        checkDataSourceList();
        
    }
    
    /*
     * Check the items in the data source list against things that we need.  Flag
     * things that don't apply, or are missing, and in some cases eliminate items
     * that are not wanted.
     */
    public void checkDataSourceList() {
        //  We want to locate all of our data objects.  Turn the checklist flags
        //  to false before we search.
        if ( _dataObjects == null )
            return;
        //  Make a copy of the list of data objects.  We will delete those we find in
        //  data sources.
        ArrayList<String> testObjects = new ArrayList<String>();
        for ( Iterator<String> iter = _dataObjects.iterator(); iter.hasNext(); ) {
            testObjects.add( iter.next() );
        } 
        //  Look at all remaining items in the data source list and determine whether their
        //  data objects are in the list of things we need for this job.
        for ( Iterator<BrowserNode> iter = _dataSourcesPane.browserTopNode().children().iterator();
                iter.hasNext(); ) {
            DataNode testNode = (DataNode)(iter.next());
            testNode.foundA( false );
            testNode.foundB( false );
            //  Check the data objects for this source agains the list of required
            //  data objects.
            for ( Iterator<String> iter1 = testObjects.iterator(); iter1.hasNext(); ) {
                String testObject = iter1.next();
                if ( testObject.contentEquals( testNode.dataObjectA() ) ) {   
                    testNode.foundA( true );
                    iter1.remove();
                }
                if ( testObject.contentEquals( testNode.dataObjectB() ) ) {
                    testNode.foundB( true );
                    iter1.remove();
                }                
            }
            testNode.checkFound( _chooseBasedOnModule.isSelected() );
        }
        
        // Add missing modules to the data source list.  These are data source nodes
        // masquerading as something else, which is a bit kludgey.
        for ( Iterator<String> iter = testObjects.iterator(); iter.hasNext(); ) {
            String testObject = iter.next();
            DataNode newNode = new DataNode( "missing module" );
            newNode.dataObjectA( testObject );
            newNode.missingA();
            newNode.hideSelection();
            _dataSourcesPane.addNode( newNode );
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
    
    /*
     * Parse the string data as if it came from an .input file (which, presumably,
     * it did).
     */
    synchronized public void parseInputFile( String str ) {
        
        //  This is a list that holds our data the files/modules/whatever that are
        //  our data requirements.
        if ( _dataObjects == null )
            _dataObjects = new ArrayList<String>();
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
                
                //  Keep a list of uniquely-named input objects (if the same input object
                //  appears several times, it will only be added to the list once).
                boolean foundIt = false;
                for ( Iterator<String> iter = _dataObjects.iterator(); iter.hasNext(); ) {
                    String testObject = iter.next();
                    if ( inputObject.contentEquals( testObject ) )
                        foundIt = true;
                }
                if ( !foundIt ) {
                    _dataObjects.add( inputObject );
                }
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
    protected JLabel _coresLabel;
    protected JLabel _cpuUsageLabel;
    protected JLabel _mpiTestLabel;
    protected JPopupMenu _machinesApplyPopup;
    protected JMenuItem _thisJobItem;
    protected JMenuItem _passJobsItem;
    protected JMenuItem _selectedJobsItem;
    protected JMenuItem _allJobsItem;
    protected JButton _applyToButton;
    protected JButton _applyMachinesButton;
    protected boolean _machinesAppliedByHand;
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
    protected JButton _uploadInputButton;
    protected JButton _uploadCalcButton;
    protected JLabel _calcFileName;
    protected JLabel _inputFileName;
    protected SimpleTextEditor _machinesFileEditor;
    protected JButton _refreshMachinesButton;
    protected JButton _uploadMachinesButton;
    protected JLabel _machinesFileName;
    protected SimpleTextEditor _threadsFileEditor;
    protected JButton _refreshThreadsButton;
    protected JButton _uploadThreadsButton;
    protected JLabel _threadsFileName;
    protected ExperimentEditor _editor;
    protected JCheckBox _eliminateNonrespondingProcessors;
    protected JCheckBox _eliminateBusyProcessors;
    protected NumberBox _busyPercentage;
    protected JLabel _busyPercentageLabel;
    protected JCheckBox _chooseBasedOnModule;
    protected JLabel _statusLabel;
    
    protected ArrayList<String> _dataObjects;
    
    protected Integer _executeTime;
    protected Integer _startMJD;
    protected Integer _startSeconds;
    
    protected MessageDisplayPanel _messageDisplayPanel;
    
}
