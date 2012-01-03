/*
 * All settings for the DiFX GUI are contained in this class.  The class provides
 * functions to allow other classes to obtain and set these data, as well as a
 * window that allows the user to view and set them via the GUI.  Facilities for
 * loading settings from files and saving them to files are provided as well.
 * 
 * The original DiFX GUI did what to me appears to be a rather strange thing when
 * it came to reading settings from a file - data were dumped into a DiFXObject and
 * then treated by the same structure that handles messages in the DataModel class.
 * The only reason I can imagine to do this is to allow settings to be issued via
 * network messaging, a procedure for which no facilities exist.  My belief that
 * this was rather strange might mean I didn't understand why it was valuable.
 * In any case it has been swept away.  
 * 
 * This class generates events when items are changed.  For instance, any change to
 * database parameters produces a "databaseChangeEvent".  Other classes can set
 * themselves up to listen for these using the "databaseChangeListener()" function.
 */
package edu.nrao.difx.difxview;

import edu.nrao.difx.difxutilities.BareBonesBrowserLaunch;

import edu.nrao.difx.xmllib.difxmessage.*;
import java.io.File;
import javax.swing.event.EventListenerList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.Dimension;
import java.awt.Color;
import java.util.Calendar;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.FileWriter;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.UIManager;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JCheckBox;

import mil.navy.usno.widgetlib.NodeBrowserScrollPane;
import mil.navy.usno.widgetlib.IndexedPanel;
import mil.navy.usno.widgetlib.NumberBox;
import mil.navy.usno.plotlib.PlotWindow;
import mil.navy.usno.plotlib.Plot2DObject;
import mil.navy.usno.plotlib.Track2D;
import mil.navy.usno.widgetlib.PingTest;
import mil.navy.usno.widgetlib.MessageScrollPane;
import mil.navy.usno.widgetlib.MessageNode;

import javax.swing.JFrame;

import edu.nrao.difx.difxdatabase.QueueDBConnection;

import java.sql.ResultSet;

public class SystemSettings extends JFrame {
    
    SystemSettings( String settingsFile ) {
        
        //  The "look and feel" isn't a setting in the sense that these others are...it
        //  must be set prior to building menus, so ONLY the default value will be
        //  used.  However it is put here due to a lack of anywhere better to put it.
        //  It is NOT saved as part of the settings file.  All JFrames should call
        //  the "setLookAndFeel()" function before they create any GUI components.
        //
        //  If left as "null" the look and feel will be whatever the local machine
        //  uses.  Although unpredictable, it does give us native file choosers.
        //  Unfortunately it is not wise to mix the look and feel within an application
        //  (thus we can't use the local one only for file choosers) - at least on the 
        //  Mac it causes confusion in the window manager and odd error messages.
        //_lookAndFeel = null;
        //  The "cross platform" look and feel is consistent across all platforms,
        //  which is why I tend to like it.  It gives us ugly and annoying file
        //  choosers though.
        _lookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
        
        //  Some organizational structure here - some items are stored in class
        //  structures that are based on where they apply.  We need to create these
        //  structures here.
        _queueBrowserSettings = new QueueBrowserSettings();
        
        //  Create all of the components of the user interface (long, messy function).
        createGUIComponents();
        
        //  Set the default settings for all values (these are hard-coded).
        setDefaults();

        //  Try loading settings from the given file.  If this fails, try to load
        //  the default settings file.
        settingsFileName( settingsFile );
        if ( !_settingsFileRead )
            settingsFileName( this.getDefaultSettingsFileName() );
        //  Buttons for dealing with the settings file.
        //this.newSize();
            
        //  This stuff is used to trap resize events.
		this.addComponentListener(new java.awt.event.ComponentAdapter() {
			public void componentResized( ComponentEvent e ) {
				_this.newSize();
			}
			public void componentMoved( ComponentEvent e ) {
				_this.newSize();
			}
		} );

    }
    
    /*
     * This function creates all of the components of the user interface for the
     * settings window (there are many components).  It does not initialize values
     * for user-changeable fields (that is done in setDefaults()), nor does it
     * determine the size and position of components unless they are fixed (usually
     * labels and things).  Sizes are set on the fly in the "newSize()" function.
     * The settings are contained in a browsable list of panes, matching the design
     * of the overall DiFX GUI.
     */
    public void createGUIComponents() {
        
        //  Use the universal "look and feel" setting for this window.  This MUST
        //  be used (at least on the Mac) or Java barfs errors and doesn't draw
        //  things correctly.
        this.setLookAndFeel();

        //  One file chooser for all settings operations.  This means it will pop
        //  up with whatever directory the user previously gave it unless otherwise
        //  specified.
        _fileChooser = new JFileChooser();
        
        //  Build a user interface for all settings items and load with default
        //  values.
        _this = this;
        this.setLayout( null );
        this.setSize( 800, 775 );
        this.setTitle( "DiFX GUI Settings" );
        _menuBar = new JMenuBar();
//        _menuBar.setVisible( true );
//        JMenu fileMenu = new JMenu( " File " );
//        JMenuItem openItem = new JMenuItem( "Open Settings File..." );
//        JMenuItem closeItem = new JMenuItem( "Close" );
//        closeItem.setToolTipText( "Close this window." );
//        closeItem.addActionListener( new ActionListener() {
//            public void actionPerformed( ActionEvent e ) {
//                closeWindow();
//            }
//        } );
//        fileMenu.add( closeItem );
//        _menuBar.add( fileMenu );
        JMenu helpMenu = new JMenu( "  Help  " );
        _menuBar.add( helpMenu );
        JMenuItem settingsHelpItem = new JMenuItem( "Settings Help" );
        settingsHelpItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                launchGUIHelp( "settings.html" );
            }
        } );
        helpMenu.add( settingsHelpItem );
        JMenuItem helpIndexItem = new JMenuItem( "Help Index" );
        helpIndexItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                launchGUIHelp( "index.html" );
            }
        } );
        helpMenu.add( helpIndexItem );
        this.add( _menuBar );
        _scrollPane = new NodeBrowserScrollPane();
        _scrollPane.addTimeoutEventListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _this.newSize();
            }
        } );
        this.add( _scrollPane );
        
        IndexedPanel settingsFilePanel = new IndexedPanel( "Settings File" );
        settingsFilePanel.openHeight( 100 );
        settingsFilePanel.closedHeight( 20 );
        _scrollPane.addNode( settingsFilePanel );
        _settingsFileName = new JFormattedTextField();
        _settingsFileName.setFocusLostBehavior( JFormattedTextField.COMMIT );
        settingsFilePanel.add( _settingsFileName );
        JLabel settingsFileLabel = new JLabel( "Current:" );
        settingsFileLabel.setBounds( 10, 25, 100, 25 );
        settingsFileLabel.setHorizontalAlignment( JLabel.RIGHT );
        settingsFilePanel.add( settingsFileLabel );
        JButton loadFromFileButton = new JButton( "Open..." );
        loadFromFileButton.setBounds( 115, 55, 100, 25 );
        loadFromFileButton.setToolTipText( "Open a settings file" );
        loadFromFileButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                openSettingsFile();
            }
        } );
        settingsFilePanel.add( loadFromFileButton );
        JButton saveButton = new JButton( "Save" );
        saveButton.setBounds( 220, 55, 100, 25 );
        saveButton.setToolTipText( "Save current settings to the given file name." );
        saveButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                saveSettings();
            }
        } );
        settingsFilePanel.add( saveButton );
        JButton saveAsButton = new JButton( "Save As..." );
        saveAsButton.setBounds( 325, 55, 100, 25 );
        saveAsButton.setToolTipText( "Save the current settings to a new file name." );
        saveAsButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                saveSettingsAs();
            }
        } );
        settingsFilePanel.add( saveAsButton );
        JButton defaultsButton = new JButton( "Defaults" );
        defaultsButton.setBounds( 430, 55, 100, 25 );
        defaultsButton.setToolTipText( "Reset all settings to internal default values." );
        defaultsButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                setDefaults();
            }
        } );
        settingsFilePanel.add( defaultsButton );
        
        IndexedPanel difxControlPanel = new IndexedPanel( "DiFX Control Connection" );
        difxControlPanel.openHeight( 180 );
        difxControlPanel.closedHeight( 20 );
        _scrollPane.addNode( difxControlPanel );
        _difxControlAddress = new JFormattedTextField();
        _difxControlAddress.setFocusLostBehavior( JFormattedTextField.COMMIT );
        _difxControlAddress.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                //generateControlChangeEvent();
            }
        } );
        difxControlPanel.add( _difxControlAddress );
        JLabel ipAddressLabel = new JLabel( "Host:" );
        ipAddressLabel.setBounds( 10, 25, 150, 25 );
        ipAddressLabel.setHorizontalAlignment( JLabel.RIGHT );
        difxControlPanel.add( ipAddressLabel );
        _difxControlPort = new NumberBox();
        _difxControlPort.setHorizontalAlignment( NumberBox.LEFT );
        _difxControlPort.minimum( 0 );
        _difxControlPort.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                //generateControlChangeEvent();
            }
        } );
        difxControlPanel.add( _difxControlPort );
        JLabel portLabel = new JLabel( "Port:" );
        portLabel.setBounds( 10, 55, 150, 25 );
        portLabel.setHorizontalAlignment( JLabel.RIGHT );
        difxControlPanel.add( portLabel );
        _difxControlUser = new JFormattedTextField();
        _difxControlUser.setFocusLostBehavior( JFormattedTextField.COMMIT );
        _difxControlUser.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                //generateControlChangeEvent();
            }
        } );
        difxControlPanel.add( _difxControlUser );
        JLabel userAddressLabel = new JLabel( "Username:" );
        userAddressLabel.setBounds( 10, 85, 150, 25 );
        userAddressLabel.setHorizontalAlignment( JLabel.RIGHT );
        difxControlPanel.add( userAddressLabel );
        _difxControlPWD = new JPasswordField();
        _difxControlPWD.setHorizontalAlignment( NumberBox.LEFT );
        _difxControlPWD.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                //generateControlChangeEvent();
            }
        } );
        difxControlPanel.add( _difxControlPWD );
        JLabel pwdLabel = new JLabel( "Password:" );
        pwdLabel.setBounds( 10, 115, 150, 25 );
        pwdLabel.setHorizontalAlignment( JLabel.RIGHT );
        difxControlPanel.add( pwdLabel );
        _difxVersion = new JFormattedTextField();
        _difxVersion.setFocusLostBehavior( JFormattedTextField.COMMIT );
        _difxVersion.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                //generateControlChangeEvent();
            }
        } );
        difxControlPanel.add( _difxVersion );
        JLabel versionAddressLabel = new JLabel( "DiFX Version:" );
        versionAddressLabel.setBounds( 10, 145, 150, 25 );
        versionAddressLabel.setHorizontalAlignment( JLabel.RIGHT );
        difxControlPanel.add( versionAddressLabel );
        
        IndexedPanel networkPanel = new IndexedPanel( "Broadcast Network" );
        networkPanel.openHeight( 170 );
        networkPanel.closedHeight( 20 );
        _scrollPane.addNode( networkPanel );
        _ipAddress = new JFormattedTextField();
        _ipAddress.setFocusLostBehavior( JFormattedTextField.COMMIT );
        _ipAddress.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                generateBroadcastChangeEvent();
            }
        } );
        networkPanel.add( _ipAddress );
        JLabel difxControlAddressLabel = new JLabel( "Group IP Address:" );
        difxControlAddressLabel.setBounds( 10, 25, 150, 25 );
        difxControlAddressLabel.setHorizontalAlignment( JLabel.RIGHT );
        networkPanel.add( difxControlAddressLabel );
        _port = new NumberBox();
        _port.setHorizontalAlignment( NumberBox.LEFT );
        _port.minimum( 0 );
        _port.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                generateBroadcastChangeEvent();
            }
        } );
        networkPanel.add( _port );
        JLabel controlPortLabel = new JLabel( "Port:" );
        controlPortLabel.setBounds( 10, 55, 150, 25 );
        controlPortLabel.setHorizontalAlignment( JLabel.RIGHT );
        networkPanel.add( controlPortLabel );
        _bufferSize = new NumberBox();
        _bufferSize.setHorizontalAlignment( NumberBox.LEFT );
        _bufferSize.minimum( 0 );
        _bufferSize.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                generateBroadcastChangeEvent();
            }
        } );
        networkPanel.add( _bufferSize );
        JLabel bufferSizeLabel = new JLabel( "Buffer Size:" );
        bufferSizeLabel.setBounds( 10, 85, 150, 25 );
        bufferSizeLabel.setHorizontalAlignment( JLabel.RIGHT );
        networkPanel.add( bufferSizeLabel );
        _timeout = new NumberBox();
        _timeout.setHorizontalAlignment( NumberBox.LEFT );
        _timeout.minimum( 0 );
        _timeout.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                generateBroadcastChangeEvent();
            }
        } );
        networkPanel.add( _timeout );
        JLabel timeoutLabel = new JLabel( "Timeout (ms):" );
        timeoutLabel.setBounds( 10, 115, 150, 25 );
        timeoutLabel.setHorizontalAlignment( JLabel.RIGHT );
        networkPanel.add( timeoutLabel );
        _plotWindow = new PlotWindow();
        _plotWindow.backgroundColor( this.getBackground() );
        networkPanel.add( _plotWindow );
        _broadcastPlot = new Plot2DObject();
        _broadcastPlot.title( "Packet Traffic (bytes/buffer size)", Plot2DObject.LEFT_JUSTIFY );
        _broadcastPlot.titlePosition( 0.0, 4.0 );
        _broadcastTrack = new Track2D();
        _broadcastPlot.addTrack( _broadcastTrack );
        _broadcastTrack.color( Color.GREEN );
        _broadcastTrack.sizeLimit( 1000 );
        _broadcastPlot.frame( 10, 23, 0.95, 90 );
        _broadcastPlot.backgroundColor( Color.BLACK );
        _plotWindow.add2DPlot( _broadcastPlot );
        
        IndexedPanel databasePanel = new IndexedPanel( "Database Configuration" );
        databasePanel.openHeight( 275 );
        databasePanel.closedHeight( 20 );
        _scrollPane.addNode( databasePanel );
        _dbUseDataBase = new JCheckBox();
        _dbUseDataBase.setBounds( 165, 25, 25, 25 );
        databasePanel.add( _dbUseDataBase );
        JLabel dbUseDataBaseLabel = new JLabel( "Use Data Base:" );
        dbUseDataBaseLabel.setBounds( 10, 25, 150, 25 );
        dbUseDataBaseLabel.setHorizontalAlignment( JLabel.RIGHT );
        databasePanel.add( dbUseDataBaseLabel );
        _dbVersion = new JFormattedTextField();
        _dbVersion.setFocusLostBehavior( JFormattedTextField.COMMIT );
        _dbVersion.setBounds( 285, 25, 180, 25 );
        databasePanel.add( _dbVersion );
        JLabel dbVersionLabel = new JLabel( "Version:" );
        dbVersionLabel.setBounds( 200, 25, 80, 25 );
        dbVersionLabel.setHorizontalAlignment( JLabel.RIGHT );
        databasePanel.add( dbVersionLabel );
        _dbHost = new JFormattedTextField();
        _dbHost.setFocusLostBehavior( JFormattedTextField.COMMIT );
        _dbHost.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                setDbURL();
            }
        } );
        databasePanel.add( _dbHost );
        JLabel dbHostLabel = new JLabel( "Host:" );
        dbHostLabel.setBounds( 10, 55, 150, 25 );
        dbHostLabel.setHorizontalAlignment( JLabel.RIGHT );
        databasePanel.add( dbHostLabel );
        _dbSID = new JFormattedTextField();
        _dbSID.setFocusLostBehavior( JFormattedTextField.COMMIT );
        databasePanel.add( _dbSID );
        _dbSID.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                setDbURL();
            }
        } );
        JLabel dbSIDLabel = new JLabel( "User:" );
        dbSIDLabel.setBounds( 10, 85, 150, 25 );
        dbSIDLabel.setHorizontalAlignment( JLabel.RIGHT );
        databasePanel.add( dbSIDLabel );
        _dbPWD = new JPasswordField();
        _dbPWD.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                generateDatabaseChangeEvent();
            }
        } );
        databasePanel.add( _dbPWD );
        JLabel dbPWDLabel = new JLabel( "Password:" );
        dbPWDLabel.setBounds( 10, 115, 150, 25 );
        dbPWDLabel.setHorizontalAlignment( JLabel.RIGHT );
        databasePanel.add( dbPWDLabel );
        _dbName = new JFormattedTextField();
        _dbName.setFocusLostBehavior( JFormattedTextField.COMMIT );
        databasePanel.add( _dbName );
        JLabel dbNameLabel = new JLabel( "DiFX Database:" );
        dbNameLabel.setBounds( 10, 145, 150, 25 );
        dbNameLabel.setHorizontalAlignment( JLabel.RIGHT );
        databasePanel.add( dbNameLabel );
        _jdbcDriver = new JFormattedTextField();
        _jdbcDriver.setFocusLostBehavior( JFormattedTextField.COMMIT );
        _jdbcDriver.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                generateDatabaseChangeEvent();
            }
        } );
        databasePanel.add( _jdbcDriver );
        JLabel oracleDriverLabel = new JLabel( "JDBC Driver:" );
        oracleDriverLabel.setBounds( 10, 175, 150, 25 );
        oracleDriverLabel.setHorizontalAlignment( JLabel.RIGHT );
        databasePanel.add( oracleDriverLabel );
        _jdbcPort = new JFormattedTextField();
        _jdbcPort.setFocusLostBehavior( JFormattedTextField.COMMIT );
        _jdbcPort.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                setDbURL();
            }
        } );
        databasePanel.add( _jdbcPort );
        JLabel oraclePortLabel = new JLabel( "JDBC Port:" );
        oraclePortLabel.setBounds( 10, 205, 150, 25 );
        oraclePortLabel.setHorizontalAlignment( JLabel.RIGHT );
        databasePanel.add( oraclePortLabel );
        _dbAutoUpdate = new JCheckBox();
        _dbAutoUpdate.setBounds( 165, 235, 25, 25 );
        databasePanel.add( _dbAutoUpdate );
        JLabel dbAutoUpdateLabel = new JLabel( "Periodic Update:" );
        dbAutoUpdateLabel.setBounds( 10, 235, 150, 25 );
        dbAutoUpdateLabel.setHorizontalAlignment( JLabel.RIGHT );
        databasePanel.add( dbAutoUpdateLabel );
        _dbAutoUpdateInterval = new NumberBox();
        _dbAutoUpdateInterval.setBounds( 230, 235, 50, 25 );
        _dbAutoUpdateInterval.minimum( 1.0 );
        databasePanel.add( _dbAutoUpdateInterval );
        JLabel dbAutoUpdateIntervalLabel1 = new JLabel( "every" );
        dbAutoUpdateIntervalLabel1.setBounds( 130, 235, 95, 25 );
        dbAutoUpdateIntervalLabel1.setHorizontalAlignment( JLabel.RIGHT );
        databasePanel.add( dbAutoUpdateIntervalLabel1 );
        JLabel dbAutoUpdateIntervalLabel2 = new JLabel( "seconds" );
        dbAutoUpdateIntervalLabel2.setBounds( 285, 235, 65, 25 );
        databasePanel.add( dbAutoUpdateIntervalLabel2 );
        _pingHostButton = new JButton( "Ping Host" );
        _pingHostButton.setToolTipText( "ping the database host" );
        _pingHostButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                pingDatabaseHost();
            }
        } );
        databasePanel.add( _pingHostButton );
        _testDatabaseButton = new JButton( "Test Database" );
        _testDatabaseButton.setToolTipText( "Run a connection test using the current database settings." );
        _testDatabaseButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                testDatabaseAction();
            }
        } );
        databasePanel.add( _testDatabaseButton );
        _databaseMessages = new MessageScrollPane();
        databasePanel.add( _databaseMessages );
         
        _addressesPanel = new IndexedPanel( "Documentation Locations" );
        _addressesPanel.openHeight( 155 );
        _addressesPanel.closedHeight( 20 );
        _addressesPanel.labelWidth( 250 );
        JLabel guiDocPathLabel = new JLabel( "GUI Docs:" );
        guiDocPathLabel.setBounds( 10, 25, 100, 25 );
        guiDocPathLabel.setHorizontalAlignment( JLabel.RIGHT );
        guiDocPathLabel.setToolTipText( "Directory (or web address) containing all GUI documentation." );
        _guiDocPath = new JFormattedTextField();
        _guiDocPath.setFocusLostBehavior( JFormattedTextField.COMMIT );
        _guiDocPath.setToolTipText( "Directory (or web address) containing all GUI documentation." );
        _addressesPanel.add( guiDocPathLabel );
        _addressesPanel.add( _guiDocPath );
        _guiDocPathBrowseButton = new JButton( "Browse..." );
        _guiDocPathBrowseButton.setToolTipText( "Browse the local directory structure for the location of documentation." );
        _guiDocPathBrowseButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                guiDocPathBrowse();
            }
        } );
        _addressesPanel.add( _guiDocPathBrowseButton );
        JLabel difxUsersGroupURLLabel = new JLabel( "Users Group:" );
        difxUsersGroupURLLabel.setBounds( 10, 55, 100, 25 );
        difxUsersGroupURLLabel.setHorizontalAlignment( JLabel.RIGHT );
        difxUsersGroupURLLabel.setToolTipText( "URL of the DiFX Users Group." );
        _difxUsersGroupURL = new JFormattedTextField();
        _difxUsersGroupURL.setFocusLostBehavior( JFormattedTextField.COMMIT );
        _difxUsersGroupURL.setToolTipText( "URL of the DiFX Users Group." );
        _addressesPanel.add( difxUsersGroupURLLabel );
        _addressesPanel.add( _difxUsersGroupURL );
        JLabel difxWikiURLLabel = new JLabel( "DiFX Wiki:" );
        difxWikiURLLabel.setBounds( 10, 85, 100, 25 );
        difxWikiURLLabel.setHorizontalAlignment( JLabel.RIGHT );
        difxWikiURLLabel.setToolTipText( "URL of the DiFX Wiki." );
        _difxWikiURL = new JFormattedTextField();
        _difxWikiURL.setFocusLostBehavior( JFormattedTextField.COMMIT );
        _difxWikiURL.setToolTipText( "URL of the DiFX Wiki." );
        _addressesPanel.add( difxWikiURLLabel );
        _addressesPanel.add( _difxWikiURL );
        JLabel difxSVNLabel = new JLabel( "DiFX SVN:" );
        difxSVNLabel.setBounds( 10, 115, 100, 25 );
        difxSVNLabel.setHorizontalAlignment( JLabel.RIGHT );
        difxSVNLabel.setToolTipText( "URL of the DiFX Subversion repository." );
        _difxSVN = new JFormattedTextField();
        _difxSVN.setFocusLostBehavior( JFormattedTextField.COMMIT );
        _difxSVN.setToolTipText( "URL of the DiFX Subversion repository." );
        _addressesPanel.add( difxSVNLabel );
        _addressesPanel.add( _difxSVN );

        _scrollPane.addNode( _addressesPanel );
        
        _allObjectsBuilt = true;
        
        //  This seems to be required to get the browser to draw the first time.
        //  Annoying and kludgey, but harmless.
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
            _scrollPane.setBounds( 0, 25, w, h - 47 );
            _settingsFileName.setBounds( 115, 25, w - 135, 25 );
            //  DiFX Controll Connection settings
            _difxControlAddress.setBounds( 165, 25, 300, 25 );
            _difxControlPort.setBounds( 165, 55, 300, 25 );
            _difxControlUser.setBounds( 165, 85, 300, 25 );
            _difxControlPWD.setBounds( 165, 115, 300, 25 );
            _difxVersion.setBounds( 165, 145, 300, 25 );
            //  Broadcast network settings
            _ipAddress.setBounds( 165, 25, 300, 25 );
            _port.setBounds( 165, 55, 300, 25 );
            _bufferSize.setBounds( 165, 85, 300, 25 );
            _timeout.setBounds( 165, 115, 300, 25 );
            _plotWindow.setBounds( 470, 25, w - 495, 120 );
            //  Database Configuration
            _dbHost.setBounds( 165, 55, 300, 25 );
            _dbSID.setBounds( 165, 85, 300, 25 );
            _dbPWD.setBounds( 165, 115, 300, 25 );
            _dbName.setBounds( 165, 145, 300, 25 );
            _jdbcDriver.setBounds( 165, 175, 300, 25 );
            _jdbcPort.setBounds( 165, 205, 300, 25 );
            _pingHostButton.setBounds( 480, 55, 125, 25 );
            _testDatabaseButton.setBounds( 610, 55, 125, 25 );
            _databaseMessages.setBounds( 480, 85, w - 505, 145 );
            //  Documentation Addresses
            _guiDocPath.setBounds( 115, 25, w - 240, 25 );
            _guiDocPathBrowseButton.setBounds( w - 120, 25, 100, 25 );
            _difxUsersGroupURL.setBounds( 115, 55, w - 135, 25 );
            _difxWikiURL.setBounds( 115, 85, w - 135, 25 );
            _difxSVN.setBounds( 115, 115, w - 135, 25 );
        }
    }
    
    /*
     * Open a new file containing settings.  This uses the file chooser.
     */
    public void openSettingsFile() {
        _fileChooser.setDialogTitle( "Open System Settings File..." );
        _fileChooser.setFileSelectionMode( JFileChooser.FILES_AND_DIRECTORIES );
        _fileChooser.setApproveButtonText( "Open" );
        _fileChooser.setCurrentDirectory( new File( this.settingsFileName() ) );
        int ret = _fileChooser.showOpenDialog( this );
        if ( ret == JFileChooser.APPROVE_OPTION )
            this.settingsFileName( _fileChooser.getSelectedFile().getAbsolutePath() );
    }
    
    /*
     * Browse for the directory that contains GUI documentation.
     */
    public void guiDocPathBrowse() {
        _fileChooser.setDialogTitle( "GUI Documentation Directory..." );
        _fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
        _fileChooser.setApproveButtonText( "Select" );
        int ret = _fileChooser.showOpenDialog( this );
        if ( ret == JFileChooser.APPROVE_OPTION )
            _guiDocPath.setText( "file://" + _fileChooser.getSelectedFile().getAbsolutePath() );
    }
    
    /*
     * Save setting to the current settings file.  If this is null, go to "saveSettingsAs()".
     */
    public void saveSettings() {
        if ( _settingsFileName.getText().equals( this.getDefaultSettingsFileName() ) ) {
            Object[] options = { "Continue", "Cancel", "Save to..." };
            int ans = JOptionPane.showOptionDialog( this, 
                    "This will overwrite the default settings file!\nAre you sure you want to do this?",
                    "Overwrite Defaults Warning",
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[2] );
            if ( ans == 0 )
                saveSettingsToFile( _settingsFileName.getName() );
            else if ( ans == 1 )
                return;
            else
                saveSettingsAs();
        }
        else
            saveSettingsToFile( _settingsFileName.getName() );
    }
    
    /*
     * Open a file chooser to pick a file for saving the current settings.
     */
    public void saveSettingsAs() {
        _fileChooser.setDialogTitle( "Save System Settings to File..." );
        int ret = _fileChooser.showSaveDialog( this );
        if ( ret == JFileChooser.APPROVE_OPTION )
            saveSettingsToFile( _fileChooser.getSelectedFile().getAbsolutePath() );
    }
    
    /*
     * Set all settings to their internal system defaults.
     */
    public void setDefaults() {
        _jaxbPackage = "edu.nrao.difx.xmllib.difxmessage";
        _home = "/home/swc/difx";
        _resourcesFile = "/cluster/difx/DiFX_trunk_64/conf/resources.difx";
        _loggingEnabled = false;
        _statusValidDuration = 2000l;
        _ipAddress.setText( "224.2.2.1" );
        _port.intValue( 52525 );
        _bufferSize.intValue( 1500 );
        _timeout.intValue( 100 );
        _difxControlAddress.setText( "swc01.usno.navy.mil" );
        _difxControlPort.intValue( 50200 );
        _difxControlUser.setText( "difx" );
        _difxControlPWD.setText( "difx2010" );
        _difxVersion.setText( "trunk" );
        _dbUseDataBase.setSelected( true );
        _dbVersion.setText( "unknown" );
        _dbHost.setText( "c3po.aoc.nrao.edu" );
        _dbSID.setText( "difx" );
        _dbPWD.setText( "difx2010" );
        _dbName.setText( "difxdb" );
        _jdbcDriver.setText( "com.mysql.jdbc.Driver" );
        _jdbcPort.setText( "3306" );
        this.setDbURL();
        _reportLoc = "/users/difx/Desktop";
        _guiDocPath.setText( "file://" + System.getProperty( "user.dir" ) + "/doc" );
        _difxUsersGroupURL.setText( "http://groups.google.com/group/difx-users/topics" );
        _difxWikiURL.setText( "http://cira.ivec.org/dokuwiki/doku.php/difx/start" );
        _difxSVN.setText( "https://svn.atnf.csiro.au/trac/difx" );
        _dbAutoUpdate.setSelected( false );
        _dbAutoUpdateInterval.intValue( 10 );
        _queueBrowserSettings.showCompleted = true;
        _queueBrowserSettings.showIncomplete = true;
        _queueBrowserSettings.showSelected = true;
        _queueBrowserSettings.showUnselected = true;
    }
    
    /*
     * Close this window.  At the moment this operation is not complicated.
     */
    public void closeWindow() {
        this.setVisible( false );
    }

    public void settingsFileName( String newVal ) {
        _settingsFileName.setText( newVal );
        //  Attempt to read the new settings.
        _settingsFileRead = getSettingsFromFile( settingsFileName() );
    }
    public String settingsFileName() { return _settingsFileName.getText(); }
    
    public void jaxbPackage( String newVal ) { _jaxbPackage = newVal; }
    public String jaxbPackage() { return _jaxbPackage; }
    
    public void home( String newVal ) { _home = newVal; }
    public String home() { return _home; }
    
    public void resourcesFile( String newVal ) { _resourcesFile = newVal; }
    public String resourcesFile() { return _resourcesFile; }
    
    public void loggingEnabled( boolean newVal ) { _loggingEnabled = newVal; }
    public boolean loggingEnabled() { return _loggingEnabled; }
    
    public void statusValidDuration( long newVal ) { _statusValidDuration = newVal; }
    public long statusValidDuration() { return _statusValidDuration; }
    
    public void difxControlAddress( String newVal ) { _difxControlAddress.setText( newVal ); }
    public String difxControlAddress() { return _difxControlAddress.getText(); }
    
    public void difxControlPort( int newVal ) { _difxControlPort.intValue( newVal ); }
    public int difxControlPort() { return _difxControlPort.intValue(); }
    public void difxControlPort( String newVal ) { difxControlPort( Integer.parseInt( newVal ) ); }

    public void difxControlUser( String newVal ) { _difxControlUser.setText( newVal ); }
    public String difxControlUser() { return _difxControlUser.getText(); }
    
    public void difxControlPassword( String newVal ) { _difxControlPWD.setText( newVal ); }
    public String difxControlPassword() { return new String( _difxControlPWD.getPassword() ); }
    
    public void difxVersion( String newVal ) { _difxVersion.setText( newVal ); }
    public String difxVersion() { return _difxVersion.getText(); }
    
    public void ipAddress( String newVal ) { _ipAddress.setText( newVal ); }
    public String ipAddress() { return _ipAddress.getText(); }
    
    public void port( int newVal ) { _port.intValue( newVal ); }
    public int port() { return _port.intValue(); }
    public void port( String newVal ) { port( Integer.parseInt( newVal ) ); }
    
    public void bufferSize( int newVal ) { _bufferSize.intValue( newVal ); }
    public int bufferSize() { return _bufferSize.intValue(); }
    public void bufferSize( String newVal ) { bufferSize( Integer.parseInt( newVal ) ); }
    
    public void timeout( int newVal ) { _timeout.intValue( newVal ); }
    public int timeout() { return _timeout.intValue(); }
    public void timeout( String newVal ) { timeout( Integer.parseInt( newVal ) ); }
    
    public boolean useDataBase() { return _dbUseDataBase.isSelected(); }
    public String dbVersion() { return _dbVersion.getText(); }
    public void dbHost( String newVal ) { 
        _dbHost.setText( newVal );
        setDbURL();
    }
    public String dbHost() { return _dbHost.getText(); }
    
    public void dbSID( String newVal ) { 
        _dbSID.setText( newVal );
        setDbURL();
    }
    public String dbSID() { return _dbSID.getText(); }
    
    public void dbPWD( String newVal ) { 
        _dbPWD.setText( newVal );
        generateDatabaseChangeEvent();
    }
    public String dbPWD() { return new String( _dbPWD.getPassword() ); }
    
    public void dbName( String newVal ) { 
        _dbName.setText( newVal );
    }
    public String dbName() { return _dbName.getText(); }
    
    public void jdbcDriver( String newVal ) { 
        _jdbcDriver.setText( newVal );
        generateDatabaseChangeEvent();
    }
    public String jdbcDriver() { return _jdbcDriver.getText(); }
    
    public void jdbcPort( String newVal ) { 
        _jdbcPort.setText( newVal );
        setDbURL();
    }
    public String jdbcPort() { return _jdbcPort.getText(); }
    
    public String dbURL() { return _dbURL; }
    public void dbURL( String newVal ) { 
        _dbURL = newVal;
        generateDatabaseChangeEvent();
    }
    protected void setDbURL() {
        //  Sets the dbURL using other items - this is not accessible to the outside.
        //_dbURL = "jdbc:oracle:thin:@" + _dbHost.getText() + ":" + _oracleJdbcPort.getText() + 
        _dbURL = "jdbc:mysql://" + _dbHost.getText() + ":" + _jdbcPort.getText() + "/mysql";
        generateDatabaseChangeEvent();
    }
    public boolean dbAutoUpdate() { return _dbAutoUpdate.isSelected(); }
    public void dbAutoUpdate( boolean newVal ) { _dbAutoUpdate.setSelected( newVal ); }
    public int dbAutoUpdateInterval() { return _dbAutoUpdateInterval.intValue(); }
    
    /*
     * Set the look and feel for a new JFrame.  This needs to be called before any
     * GUI components are created.
     */
    public void setLookAndFeel() {
        //  Don't do anything if the look and feel is "null".
        if ( _lookAndFeel == null )
            return;
        try {
            UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName() );
        }
        catch ( Exception e ) {
            //  This thing throws exceptions, but we ignore them.  Shouldn't hurt
            //  us - if the look and feel isn't set, the default should at least
            //  be visible.
        }
    }
    
    public void launchGUIHelp( String topicAddress ) {
        File file = new File( _guiDocPath.getText().substring( 7 ) + "/" + topicAddress );
        if ( file.exists() )
            BareBonesBrowserLaunch.openURL( _guiDocPath.getText() + "/" + topicAddress );
        else {
            //  The named file couldn't be found.  Since this file is formed by
            //  the GUI, the name is probably not wrong, so the path probably is.
            //  Generate a temporary file with instructions for setting the documentation
            //  path.
            try {
                BufferedWriter out = new BufferedWriter( new FileWriter( "/tmp/tmpIndex.html" ) );
                out.write( "<h2>Requested Documentation Not Found</h2>\n" );
                out.write( "\n" );
                out.write( "<p>The document you requested:\n" );
                out.write( "<br>\n" );
                out.write( "<pre>\n" );
                out.write( _guiDocPath.getText() + "/" + topicAddress + "\n" );
                out.write( "</pre>\n" );
                out.write( "was not found.\n" );
                out.write( "\n" );
                out.write( "<p>The most likely reasons for this is\n" );
                out.write( "that the GUI Documentation Path is not set correctly.\n" );
                out.write( "Check the \"GUI Docs\" setting under the \"Documentation Locations\"\n" );
                out.write( "subset in the Settings Window (to launch the Settings Window pick\n" );
                out.write( "\"Settings/Show Settings\" from the menu bar of the main DiFX GUI Window).\n" );
                out.write( "<p>Note that the path should start with \"<code>file:///</code>\" followed by a complete\n" );
                out.write( "pathname, as in \"<code>file:///tmp/foo.html</code>\".\n" );
                out.close();
                BareBonesBrowserLaunch.openURL( "file:///tmp/tmpIndex.html" );
            } catch ( IOException e ) {
            }
        }
    }
    
    public void launchDiFXUsersGroup() {
        BareBonesBrowserLaunch.openURL( _difxUsersGroupURL.getText() );
    }
    
    public void launchDiFXWiki() {
        BareBonesBrowserLaunch.openURL( _difxWikiURL.getText() );
    }
    
    public void launchDiFXSVN() {
        BareBonesBrowserLaunch.openURL( _difxSVN.getText() );
    }
    
    /*
     * Add a new listener for database changes.
     */
    public void databaseChangeListener( ActionListener a ) {
        if ( _databaseChangeListeners == null )
            _databaseChangeListeners = new EventListenerList();
        _databaseChangeListeners.add( ActionListener.class, a );
    }

    /*
     * Inform all listeners of a change to database-related items.
     */
    protected void generateDatabaseChangeEvent() {
        if ( _databaseChangeListeners == null )
            return;
        Object[] listeners = _databaseChangeListeners.getListenerList();
        // loop through each listener and pass on the event if needed
        int numListeners = listeners.length;
        for ( int i = 0; i < numListeners; i+=2 ) {
            if ( listeners[i] == ActionListener.class )
                ((ActionListener)listeners[i+1]).actionPerformed( null );
        }
    }
    
    /*
     * Add a new listener for broadcast changes.
     */
    public void broadcastChangeListener( ActionListener a ) {
        if ( _broadcastChangeListeners == null )
            _broadcastChangeListeners = new EventListenerList();
        _broadcastChangeListeners.add( ActionListener.class, a );
    }

    /*
     * Inform all listeners of a change to broadcast-related items.
     */
    protected void generateBroadcastChangeEvent() {
        if ( _broadcastChangeListeners == null )
            return;
        Object[] listeners = _broadcastChangeListeners.getListenerList();
        // loop through each listener and pass on the event if needed
        int numListeners = listeners.length;
        for ( int i = 0; i < numListeners; i+=2 ) {
            if ( listeners[i] == ActionListener.class )
                ((ActionListener)listeners[i+1]).actionPerformed( null );
        }
    }
    
    /*
     * Parse a settings file (XML).  This function produces lots of log messages
     * and returns true only if it succeeds.
     */
    public boolean getSettingsFromFile( String filename ) {
        //  Can't read a non-existent filename
        if ( filename == null )
            return false;
        //  Or a non-existent file
        File theFile = new File( filename );
        if ( !theFile.exists() ) {
            java.util.logging.Logger.getLogger( "global" ).log( java.util.logging.Level.SEVERE,
                "Settings file " + filename + " does not exist." );
            return false;
        }
        //  Now parse the thing, or try to.
        ObjectFactory factory = new ObjectFactory();
        DoiSystemConfig doiConfig = factory.createDoiSystemConfig();
        try {
            javax.xml.bind.JAXBContext jaxbCtx = javax.xml.bind.JAXBContext.newInstance(doiConfig.getClass().getPackage().getName());
            javax.xml.bind.Unmarshaller unmarshaller = jaxbCtx.createUnmarshaller();
            doiConfig = (DoiSystemConfig) unmarshaller.unmarshal( theFile );
            this.home( doiConfig.getDifxHome() );
            this.resourcesFile( doiConfig.getResourcesFile() );
            this.dbHost( doiConfig.getDbHost() );
            this.dbSID( doiConfig.getDbSID() );
            this.dbPWD( doiConfig.getDbPassword() );
            this.jdbcDriver( doiConfig.getDbJdbcDriver() );
            this.jdbcPort( doiConfig.getDbJdbcPort() );
            this.dbURL( doiConfig.getDbUrl() );
            setDbURL();
            this.ipAddress( doiConfig.getIpAddress() );
            this.port( doiConfig.getPort() );
            this.bufferSize( doiConfig.getBufferSize() );
            this.loggingEnabled( doiConfig.isLoggingEnabled() );
            this.statusValidDuration( doiConfig.getStatusValidDuration() );
            
            this.jaxbPackage( doiConfig.getJaxbPackage() );
            this.timeout( doiConfig.getTimeout() );
            this.difxControlAddress( doiConfig.getDifxControlAddress() );
            this.difxControlPort( doiConfig.getDifxControlPort() );
            this.difxControlUser( doiConfig.getDifxControlUser() );
            this.difxControlPassword( doiConfig.getDifxControlPWD() );
            this.difxVersion( doiConfig.getDifxVersion() );
            _dbUseDataBase.setSelected( doiConfig.isDbUseDataBase() );
            _dbVersion.setText( doiConfig.getDbVersion() );
            this.dbName( doiConfig.getDbName() );
            _reportLoc = doiConfig.getReportLoc();
            _guiDocPath.setText( doiConfig.getGuiDocPath() );
            _difxUsersGroupURL.setText( doiConfig.getDifxUsersGroupURL() );
            _difxWikiURL.setText( doiConfig.getDifxWikiURL() );
            _difxSVN.setText( doiConfig.getDifxSVN() );
            this.dbAutoUpdate( doiConfig.isDbAutoUpdate() );
            _dbAutoUpdateInterval.intValue( doiConfig.getDbAutoUpdateInterval() );
            doiConfig.setQueueShowCompleted( _queueBrowserSettings.showCompleted );
            doiConfig.setQueueShowIncomplete( _queueBrowserSettings.showIncomplete );
            doiConfig.setQueueShowSelected( _queueBrowserSettings.showSelected );
            doiConfig.setQueueShowUnselected( _queueBrowserSettings.showUnselected );
            _queueBrowserSettings.showCompleted = doiConfig.isQueueShowCompleted();
            _queueBrowserSettings.showIncomplete = doiConfig.isQueueShowIncomplete();
            _queueBrowserSettings.showSelected = doiConfig.isQueueShowSelected();
            _queueBrowserSettings.showUnselected = doiConfig.isQueueShowUnselected();
            generateBroadcastChangeEvent();
            generateDatabaseChangeEvent();
            
        } catch (javax.xml.bind.JAXBException ex) {
            // XXXTODO Handle exception
            java.util.logging.Logger.getLogger("global").log( java.util.logging.Level.SEVERE, null, ex );
        }
        return false;
    }

    /*
     * Save all current settings to the given filename.
     */
    public void saveSettingsToFile( String filename ) {
        System.out.println( "write to " + filename );
        ObjectFactory factory = new ObjectFactory();
        DoiSystemConfig doiConfig = factory.createDoiSystemConfig();
        doiConfig.setDifxHome( this.home() );
        doiConfig.setResourcesFile( this.resourcesFile() );
        doiConfig.setDbHost( this.dbHost() );
        doiConfig.setDbSID( this.dbSID() );
        doiConfig.setDbPassword( this.dbPWD() );
        doiConfig.setDbJdbcDriver( this.jdbcDriver() );
        doiConfig.setDbJdbcPort( this.jdbcPort() );
        doiConfig.setDbUrl( this.dbURL() );
        doiConfig.setIpAddress( this.ipAddress() );
        doiConfig.setPort( this.port() );
        doiConfig.setBufferSize( this.bufferSize() );
        doiConfig.setLoggingEnabled( this.loggingEnabled() );
        doiConfig.setStatusValidDuration( this.statusValidDuration() );
        
        doiConfig.setJaxbPackage( this.jaxbPackage() );
        doiConfig.setTimeout( this.timeout() );
        doiConfig.setDifxControlAddress( this.difxControlAddress() );
        doiConfig.setDifxControlPort( this.difxControlPort() );
        doiConfig.setDifxControlUser( this.difxControlUser() );
        doiConfig.setDifxControlPWD( new String( this.difxControlPassword() ) );
        doiConfig.setDifxVersion( this.difxVersion() );
        doiConfig.setDbUseDataBase( this.useDataBase() );
        doiConfig.setDbVersion( this.dbVersion() );
        doiConfig.setDbName( this.dbName() );
        doiConfig.setReportLoc( _reportLoc );
        doiConfig.setGuiDocPath( _guiDocPath.getText() );
        doiConfig.setDifxUsersGroupURL( _difxUsersGroupURL.getText() );
        doiConfig.setDifxWikiURL( _difxWikiURL.getText() );
        doiConfig.setDifxSVN( _difxSVN.getText() );
        doiConfig.setDbAutoUpdate( this.dbAutoUpdate() );
        doiConfig.setDbAutoUpdateInterval( _dbAutoUpdateInterval.intValue() );
        doiConfig.setQueueShowCompleted( _queueBrowserSettings.showCompleted );
        doiConfig.setQueueShowIncomplete( _queueBrowserSettings.showIncomplete );
        doiConfig.setQueueShowSelected( _queueBrowserSettings.showSelected );
        doiConfig.setQueueShowUnselected( _queueBrowserSettings.showUnselected );
        
        try {
            javax.xml.bind.JAXBContext jaxbCtx = javax.xml.bind.JAXBContext.newInstance( doiConfig.getClass().getPackage().getName() );
            javax.xml.bind.Marshaller marshaller = jaxbCtx.createMarshaller();
            File theFile = new File( filename );
            theFile.createNewFile();
            marshaller.marshal( doiConfig, theFile );
        } catch ( java.io.IOException e ) {
                System.out.println( "SystemSettings: can't write file \"" + filename + "\" - some appropriate complaint here." );
        } catch (javax.xml.bind.JAXBException ex) {
            // XXXTODO Handle exception
            java.util.logging.Logger.getLogger("global").log( java.util.logging.Level.SEVERE, null, ex );
        }
   }
    
    public String getDefaultSettingsFileName() {
        //  See if a DIFXROOT environment variable has been defined.  If not,
        //  guess that the current working directory is the DIFXROOT.
        String difxRoot = System.getenv( "DIFXROOT" );
        if (difxRoot == null) {
            difxRoot = System.getProperty( "user.dir" );
        }
        return difxRoot + "/conf/DOISystemConfig.xml";
    }
    
    /*
     * This function is called from the thread that receives broadcasts each time
     * it cycles.  It gives us the size of a received packet, or a 0 if this thread
     * timed out.  The information is added to a plot if this window is visible
     * (otherwise it is kind of a waste of time).
     */
    public void gotPacket( int newSize ) {
//        if ( this.isVisible() ) {
//            _broadcastPlot.limits( (double)(_broadcastTrackSize - _broadcastPlot.w()), (double)(_broadcastTrackSize), -.05, 1.0 );
//            _broadcastTrack.add( (double)(_broadcastTrackSize), (double)(newSize)/(double)bufferSize() );
//            _broadcastTrackSize += 1;
//            _plotWindow.updateUI();
//        }
//        else {
//            _broadcastTrack.clear();
//            _broadcastTrackSize = 0;
//        }
        if ( this.isVisible() )
            _broadcastPlot.limits( (double)(_broadcastTrackSize - _broadcastPlot.w()), (double)(_broadcastTrackSize), -.05, 1.0 );
        _broadcastTrack.add( (double)(_broadcastTrackSize), (double)(newSize)/(double)bufferSize() );
        _broadcastTrackSize += 1;
        _plotWindow.updateUI();
    }
    
    /*
     * Make a test connection to the database amd 
     */
    public void testDatabaseAction() {
        databaseSuccess( "" );
        databaseWarning( "Connecting to database (" + this.dbURL() + ")..." );
        QueueDBConnection db = new QueueDBConnection( this );
        try {
            databaseSuccess( "Connection successful...reading DiFX jobs..." );
            ResultSet jobInfo = db.jobList();

            Integer n = 0;
            while ( jobInfo.next() )
                ++n;
            databaseSuccess( "Database contains " + n.toString() + " jobs." );
            databaseSuccess( "" );

        } catch ( java.sql.SQLException e ) {
            databaseFailure( "SQLException: " + e.getMessage() );
            databaseSuccess( "" );
        } catch ( Exception e ) {
            databaseFailure( "Connection Failure (Exception): " + e.getMessage()  );
            databaseSuccess( "" );
        }
    }
    
    /*
     * Perform a "ping" test on the specified database host.
     */
    public void pingDatabaseHost() {
        databaseSuccess( "" );
        databaseWarning( "Starting ping test..." );
        final PingTest tester = new PingTest( _dbHost.getText() );
        tester.pings( 6 );
        tester.addSuccessListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                databaseSuccess( tester.lastMessage() );
            }
        } );            
        tester.addFailureListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                databaseFailure( tester.lastError() );
            }
        } );            
        tester.start();
    }
    
    protected void databaseWarning( String message ) {
        MessageNode node = new MessageNode( Calendar.getInstance().getTimeInMillis(), MessageNode.WARNING, null, message );
        node.showDate( false );
        node.showTime( true );
        node.showSource( false );
        node.showWarnings( true );
        _databaseMessages.addMessage( node );
        _databaseMessages.scrollToEnd();
    }
    
    protected void databaseSuccess( String message ) {
        MessageNode node = new MessageNode( 0, MessageNode.INFO, null, message );
        node.showDate( false );
        node.showTime( false );
        node.showSource( false );
        node.showMessages( true );
        _databaseMessages.addMessage( node );
        _databaseMessages.scrollToEnd();
    }
    
    protected void databaseFailure( String message ) {
        MessageNode node = new MessageNode( 0, MessageNode.ERROR, null, message );
        node.showDate( false );
        node.showTime( false );
        node.showSource( false );
        node.showErrors( true );
        _databaseMessages.addMessage( node );
        _databaseMessages.scrollToEnd();
    }
    
    public void hardwareMonitor( HardwareMonitorPanel newMonitor ) {
        _hardwareMonitor = newMonitor;
    }
    public HardwareMonitorPanel hardwareMonitor() {
        return _hardwareMonitor;
    }
    
    public void queueBrowser( QueueBrowserPanel newBrowser ) {
        _queueBrowser = newBrowser;
    }
    public QueueBrowserPanel queueBrowser() {
        return _queueBrowser;
    }
    public QueueBrowserSettings queueBrowserSettings() { return _queueBrowserSettings; }
    
    protected SystemSettings _this;
    
    protected boolean _allObjectsBuilt;

    protected JMenuBar _menuBar;
    protected JFormattedTextField _settingsFileName;
    protected boolean _settingsFileRead;
    
    protected String _jaxbPackage;
    protected String _home;
    protected String _resourcesFile;
    protected boolean _loggingEnabled;
    protected long _statusValidDuration;
    //  DiFX Control Connection
    protected JFormattedTextField _difxControlAddress;
    protected NumberBox _difxControlPort;
    protected JFormattedTextField _difxControlUser;
    protected JPasswordField _difxControlPWD;
    protected JFormattedTextField _difxVersion;
    //  Broadcast network
    protected JFormattedTextField _ipAddress;
    protected NumberBox _port;
    protected NumberBox _bufferSize;
    protected NumberBox _timeout;
    PlotWindow _plotWindow;
    Plot2DObject _broadcastPlot;
    Track2D _broadcastTrack;
    int _broadcastTrackSize;
    //  Database configuration
    protected JCheckBox _dbUseDataBase;
    protected JFormattedTextField _dbVersion;
    protected JFormattedTextField _dbHost;
    protected JFormattedTextField _dbSID;
    protected JPasswordField _dbPWD;
    protected JFormattedTextField _dbName;
    protected JFormattedTextField _jdbcDriver;
    protected JFormattedTextField _jdbcPort;
    protected String _dbURL;
    protected JCheckBox _dbAutoUpdate;
    protected NumberBox _dbAutoUpdateInterval;
    protected JButton _pingHostButton;
    protected JButton _testDatabaseButton;
    protected MessageScrollPane _databaseMessages;
    //  Default report location
    protected String _reportLoc;
    
    //  These are locations for "help" - GUI and DiFX documentation.
    protected JFormattedTextField _guiDocPath;
    protected JButton _guiDocPathBrowseButton;
    protected JFormattedTextField _difxUsersGroupURL;
    protected JFormattedTextField _difxWikiURL;
    protected JFormattedTextField _difxSVN;
    
    //  The "look and feel" that applies to all GUI components.
    protected String _lookAndFeel;
    
    //  Settings in the queue browser.
    public class QueueBrowserSettings {
        boolean showSelected;
        boolean showUnselected;
        boolean showCompleted;
        boolean showIncomplete;
    }
    protected QueueBrowserSettings _queueBrowserSettings;
    
    //  Different lists of event listeners.  Other classes can be informed of
    //  setting changes by adding themselves to these lists.
    EventListenerList _databaseChangeListeners;
    EventListenerList _broadcastChangeListeners;
    
    //  All settings use the same file chooser.
    JFileChooser _fileChooser;
    
    NodeBrowserScrollPane _scrollPane;
    IndexedPanel _addressesPanel;
    
    //  These items are used by multiple classes - they are put here as a matter
    //  of convenience as all locations have access to this class.
    HardwareMonitorPanel _hardwareMonitor;
    QueueBrowserPanel _queueBrowser;
    
}
