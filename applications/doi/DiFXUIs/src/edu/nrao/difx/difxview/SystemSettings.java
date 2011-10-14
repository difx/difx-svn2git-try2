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

import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.UIManager;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import mil.navy.usno.widgetlib.NodeBrowserScrollPane;
import mil.navy.usno.widgetlib.IndexedPanel;

import javax.swing.JFrame;

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
        this.setSize( 700, 500 );
        Dimension d = this.getSize();
        _menuBar = new JMenuBar();
        _menuBar.setVisible( true );
        JMenu fileMenu = new JMenu( " File " );
        JMenuItem openItem = new JMenuItem( "Open Settings File..." );
        openItem.setToolTipText( "Open a settings file" );
        openItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                openSettingsFile();
            }
        } );
        fileMenu.add( openItem );
        JMenuItem saveItem = new JMenuItem( "Save Settings" );
        saveItem.setToolTipText( "Save current settings to the given file name." );
        saveItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                saveSettings();
            }
        } );
        fileMenu.add( saveItem );
        JMenuItem saveAsItem = new JMenuItem( "Save Settings To..." );
        saveAsItem.setToolTipText( "Save the current settings to a new file name." );
        saveAsItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                saveSettingsAs();
            }
        } );
        fileMenu.add( saveAsItem );
        fileMenu.add( new JSeparator() );
        JMenuItem defaultItem = new JMenuItem( "Set Defaults" );
        defaultItem.setToolTipText( "Reset all settings to system default values." );
        saveItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                setDefaults();
            }
        } );
        fileMenu.add( defaultItem );
        fileMenu.add( new JSeparator() );
        JMenuItem closeItem = new JMenuItem( "Close" );
        closeItem.setToolTipText( "Close this window." );
        closeItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                closeWindow();
            }
        } );
        fileMenu.add( closeItem );
        _menuBar.add( fileMenu );
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
        _settingsFileName = new JTextField();
        settingsFilePanel.add( _settingsFileName );
        JLabel settingsFileLabel = new JLabel( "Current:" );
        settingsFileLabel.setBounds( 10, 25, 100, 25 );
        settingsFileLabel.setHorizontalAlignment( JLabel.RIGHT );
        settingsFilePanel.add( settingsFileLabel );
        
        IndexedPanel networkPanel = new IndexedPanel( "Broadcast Network" );
        networkPanel.openHeight( 100 );
        networkPanel.closedHeight( 20 );
        _scrollPane.addNode( networkPanel );
        
        IndexedPanel databasePanel = new IndexedPanel( "Database Configuration" );
        databasePanel.openHeight( 200 );
        databasePanel.closedHeight( 20 );
        _scrollPane.addNode( databasePanel );
        _dbHost = new JTextField();
        databasePanel.add( _dbHost );
        JLabel dbHostLabel = new JLabel( "Host:" );
        dbHostLabel.setBounds( 10, 25, 150, 25 );
        dbHostLabel.setHorizontalAlignment( JLabel.RIGHT );
        databasePanel.add( dbHostLabel );
        _dbSID = new JTextField();
        databasePanel.add( _dbSID );
        JLabel dbSIDLabel = new JLabel( "SID:" );
        dbSIDLabel.setBounds( 10, 55, 150, 25 );
        dbSIDLabel.setHorizontalAlignment( JLabel.RIGHT );
        databasePanel.add( dbSIDLabel );
        _dbPWD = new JTextField();
        databasePanel.add( _dbPWD );
        JLabel dbPWDLabel = new JLabel( "Password:" );
        dbPWDLabel.setBounds( 10, 85, 150, 25 );
        dbPWDLabel.setHorizontalAlignment( JLabel.RIGHT );
        databasePanel.add( dbPWDLabel );
        _oracleJdbcDriver = new JTextField();
        databasePanel.add( _oracleJdbcDriver );
        JLabel oracleDriverLabel = new JLabel( "Oracle Jdbc Driver:" );
        oracleDriverLabel.setBounds( 10, 115, 150, 25 );
        oracleDriverLabel.setHorizontalAlignment( JLabel.RIGHT );
        databasePanel.add( oracleDriverLabel );
         _oracleJdbcPort = new JTextField();
        databasePanel.add( _oracleJdbcPort );
        JLabel oraclePortLabel = new JLabel( "Oracle Jdbc Port:" );
        oraclePortLabel.setBounds( 10, 145, 150, 25 );
        oraclePortLabel.setHorizontalAlignment( JLabel.RIGHT );
        databasePanel.add( oraclePortLabel );
         
        _addressesPanel = new IndexedPanel( "Documentation Locations" );
        _addressesPanel.openHeight( 155 );
        _addressesPanel.closedHeight( 20 );
        _addressesPanel.labelWidth( 250 );
        JLabel guiDocPathLabel = new JLabel( "GUI Docs:" );
        guiDocPathLabel.setBounds( 10, 25, 100, 25 );
        guiDocPathLabel.setHorizontalAlignment( JLabel.RIGHT );
        guiDocPathLabel.setToolTipText( "Directory (or web address) containing all GUI documentation." );
        _guiDocPath = new JTextField();
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
        _difxUsersGroupURL = new JTextField();
        _difxUsersGroupURL.setToolTipText( "URL of the DiFX Users Group." );
        _addressesPanel.add( difxUsersGroupURLLabel );
        _addressesPanel.add( _difxUsersGroupURL );
        JLabel difxWikiURLLabel = new JLabel( "DiFX Wiki:" );
        difxWikiURLLabel.setBounds( 10, 85, 100, 25 );
        difxWikiURLLabel.setHorizontalAlignment( JLabel.RIGHT );
        difxWikiURLLabel.setToolTipText( "URL of the DiFX Wiki." );
        _difxWikiURL = new JTextField();
        _difxWikiURL.setToolTipText( "URL of the DiFX Wiki." );
        _addressesPanel.add( difxWikiURLLabel );
        _addressesPanel.add( _difxWikiURL );
        JLabel difxSVNLabel = new JLabel( "DiFX SVN:" );
        difxSVNLabel.setBounds( 10, 115, 100, 25 );
        difxSVNLabel.setHorizontalAlignment( JLabel.RIGHT );
        difxSVNLabel.setToolTipText( "URL of the DiFX Subversion repository." );
        _difxSVN = new JTextField();
        _difxSVN.setToolTipText( "URL of the DiFX Subversion repository." );
        _addressesPanel.add( difxSVNLabel );
        _addressesPanel.add( _difxSVN );

        _scrollPane.addNode( _addressesPanel );
        
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
        if ( _scrollPane != null ) {
            _scrollPane.setBounds( 0, 25, w, h - 25 );
            _settingsFileName.setBounds( 115, 25, w - 135, 25 );
            //  Database Configuration
            _dbHost.setBounds( 165, 25, 300, 25 );
            _dbSID.setBounds( 165, 55, 300, 25 );
            _dbPWD.setBounds( 165, 85, 300, 25 );
            _oracleJdbcDriver.setBounds( 165, 115, 300, 25 );
            _oracleJdbcPort.setBounds( 165, 145, 300, 25 );
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
        int returnVal = _fileChooser.showOpenDialog( this );
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
        int returnVal = _fileChooser.showSaveDialog( this );
    }
    
    /*
     * Save all current settings to the given filename.
     */
    public void saveSettingsToFile( String filename ) {
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
        _ipAddress = "224.2.2.1";
        _port = 52525;
        _bufferSize = 1500;
        _dbHost.setText( "c3po.aoc.nrao.edu" ); //"quigon.aoc.nrao.edu"
        _dbSID.setText( "vlbatest" );          //"vlba10"
        _dbPWD.setText( "vlba" );              //"chandra1999"
        _oracleJdbcDriver.setText( "oracle.jdbc.driver.OracleDriver" );
        _oracleJdbcPort.setText( "1521" );
        this.setDbURL();
        _reportLoc = "/users/difx/Desktop";
        _guiDocPath.setText( "file://" + System.getProperty( "user.dir" ) + "/doc" );
        _difxUsersGroupURL.setText( "http://groups.google.com/group/difx-users/topics" );
        _difxWikiURL.setText( "http://cira.ivec.org/dokuwiki/doku.php/difx/start" );
        _difxSVN.setText( "http://cira.ivec.org/dokuwiki/doku.php/difx/start" );
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
    
    public void ipAddress( String newVal ) { _ipAddress = newVal; }
    public String ipAddress() { return _ipAddress; }
    
    public void port( int newVal ) { _port = newVal; }
    public int port() { return _port; }
    
    public void bufferSize( int newVal ) { _bufferSize = newVal; }
    public int bufferSize() { return _bufferSize; }
    
    public void dbHost( String newVal ) { _dbHost.setText( newVal ); }
    public String dbHost() { return _dbHost.getText(); }
    
    public void dbSID( String newVal ) { _dbSID.setText( newVal ); }
    public String dbSID() { return _dbSID.getText(); }
    
    public void dbPWD( String newVal ) { _dbPWD.setText( newVal ); }
    public String dbPWD() { return _dbPWD.getText(); }
    
    public void oracleJdbcDriver( String newVal ) { _oracleJdbcDriver.setText( newVal ); }
    public String oracleJdbcDriver() { return _oracleJdbcDriver.getText(); }
    
    public void oracleJdbcPort( String newVal ) { _oracleJdbcPort.setText( newVal ); }
    public String oracleJdbcPort() { return _oracleJdbcPort.getText(); }
    
    public String dbURL() { return _dbURL; }
    public void dbURL( String newVal ) { _dbURL = newVal; }
    protected void setDbURL() {
        //  Sets the dbURL using other items - this is not accessible to the outside.
        _dbURL = "jdbc:oracle:thin:@" + _dbHost + ":" + _oracleJdbcPort + ":" + _dbSID;
    }
    
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
        BareBonesBrowserLaunch.openURL( _guiDocPath.getText() + "/" + topicAddress );
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
        _databaseChangeListeners.add( ActionListener.class, a );
    }

    /*
     * Inform all listeners of a change to database-related items.
     */
    protected void broadcastDatabaseChangeEvent() {
        Object[] listeners = _databaseChangeListeners.getListenerList();
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
            this.oracleJdbcDriver( doiConfig.getDbJdbcDriver() );
            this.oracleJdbcPort( doiConfig.getDbJdbcPort() );
            this.dbURL( doiConfig.getDbUrl() );
            this.ipAddress( doiConfig.getIpAddress() );
            this.port( doiConfig.getPort() );
            this.bufferSize( doiConfig.getBufferSize() );
            this.loggingEnabled( doiConfig.isLoggingEnabled() );
            this.statusValidDuration( doiConfig.getStatusValidDuration() );
        } catch (javax.xml.bind.JAXBException ex) {
            // XXXTODO Handle exception
            java.util.logging.Logger.getLogger("global").log( java.util.logging.Level.SEVERE, null, ex );
        }
        return false;
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
    
    protected SystemSettings _this;

    protected JMenuBar _menuBar;
    protected JTextField _settingsFileName;
    protected boolean _settingsFileRead;
    
    protected String _jaxbPackage;
    protected String _home;
    protected String _resourcesFile;
    protected boolean _loggingEnabled;
    protected long _statusValidDuration;
    protected String _ipAddress;
    protected int _port;
    protected int _bufferSize;
    //  Database configuration
    protected JTextField _dbHost;
    protected JTextField _dbSID;
    protected JTextField _dbPWD;
    protected JTextField _oracleJdbcDriver;
    protected JTextField _oracleJdbcPort;
    protected String _dbURL;
    //  Default report location
    protected String _reportLoc;
    
    //  These are locations for "help" - GUI and DiFX documentation.
    protected JTextField _guiDocPath;
    protected JButton _guiDocPathBrowseButton;
    protected JTextField _difxUsersGroupURL;
    protected JTextField _difxWikiURL;
    protected JTextField _difxSVN;
    
    //  The "look and feel" that applies to all GUI components.
    protected String _lookAndFeel;
    
    //  Different lists of event listeners.  Other classes can be informed of
    //  setting changes by adding themselves to these lists.
    EventListenerList _databaseChangeListeners;
    
    //  All settings use the same file chooser.
    JFileChooser _fileChooser;
    
    NodeBrowserScrollPane _scrollPane;
    IndexedPanel _addressesPanel;
    
}
