/*
 * This class produces a modal pop-up window for adjusting the properties
 * specific to an experiment.  This window is modal.
 */
package edu.nrao.difx.difxview;

import mil.navy.usno.widgetlib.SaneTextField;
import mil.navy.usno.widgetlib.NumberBox;
import mil.navy.usno.widgetlib.SimpleTextEditor;
import mil.navy.usno.widgetlib.AePlayWave;

import edu.nrao.difx.difxutilities.DiFXCommand_getFile;
import edu.nrao.difx.difxutilities.DiFXCommand_sendFile;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.Timer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.SwingConstants;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import java.awt.Frame;
import java.awt.Color;
import java.awt.Point;
import java.awt.Component;

import java.util.Map;
import java.util.Iterator;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.Date;

import java.text.SimpleDateFormat;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.Locale;
import mil.navy.usno.widgetlib.NodeBrowserScrollPane;
import mil.navy.usno.widgetlib.IndexedPanel;
import mil.navy.usno.widgetlib.ButtonGrid;

import javax.swing.event.EventListenerList;

/**
 *
 * @author jspitzak
 */
public class ExperimentEditor extends JDialog {
        
    public ExperimentEditor( Frame frame, int x, int y, SystemSettings settings ) {
        super( frame, "", true );
        _settings = settings;
        this.setBounds( x, y, _settings.windowConfiguration().experimentEditorW,
                _settings.windowConfiguration().experimentEditorH );
        this.getContentPane().setLayout( null );
        _this = this;
        _menuBar = new JMenuBar();
        JMenu helpMenu = new JMenu( "  Help  " );
        _menuBar.add( helpMenu );
        JMenuItem settingsHelpItem = new JMenuItem( "Experiment Editor Help" );
        settingsHelpItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _settings.launchGUIHelp( "experimentEditor.html" );
            }
        } );
        helpMenu.add( settingsHelpItem );
        JMenuItem helpIndexItem = new JMenuItem( "Help Index" );
        helpIndexItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _settings.launchGUIHelp( "index.html" );
            }
        } );
        helpMenu.add( helpIndexItem );
        this.getContentPane().add( _menuBar );
        _scrollPane = new NodeBrowserScrollPane();
        _scrollPane.addTimeoutEventListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _this.newSize();
            }
        } );
        this.getContentPane().add( _scrollPane );
        
        //  The "namePanel" holds all of the stuff that ALWAYS is shown.
        IndexedPanel namePanel = new IndexedPanel( "" );
        namePanel.openHeight( 240 );
        namePanel.alwaysOpen( true );
        namePanel.noArrow( true );
        _scrollPane.addNode( namePanel );
        
        _inDataBase = new JCheckBox( "" );
        _inDataBase.setBounds( 100, 110, 25, 25 );
        _inDataBase.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                inDataBaseAction();
            }
        });
        namePanel.add( _inDataBase );
        JLabel inDataBaseLabel = new JLabel( "In Database:" );
        inDataBaseLabel.setBounds( 10, 110, 85, 25 );
        inDataBaseLabel.setHorizontalAlignment( JLabel.RIGHT );
        namePanel.add( inDataBaseLabel );
        JLabel idLabel = new JLabel( "Database ID:" );
        idLabel.setBounds( 140, 110, 85, 25 );
        idLabel.setHorizontalAlignment( JLabel.RIGHT );
        namePanel.add( idLabel );
        _id = new JLabel( "" );
        _id.setBounds( 230, 110, 70, 25 );
        namePanel.add( _id );
        _name = new SaneTextField();
        _name.setBounds( 100, 20, 210, 25 );
        _name.textWidthLimit( 20 );
        _name.setToolTipText( "Name assigned to the experiment (up to 20 characters)." );
        _name.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                nameChangeAction();
            }
        });
        namePanel.add( _name );
        _name.setVisible( false );
        _nameAsLabel = new JLabel( "" );
        _nameAsLabel.setBounds( 100, 20, 210, 25 );
        _nameAsLabel.setToolTipText( "Name assigned to the experiment." );
        namePanel.add( _nameAsLabel );
        JLabel nameLabel = new JLabel( "Name:" );
        nameLabel.setBounds( 10, 20, 85, 25 );
        nameLabel.setHorizontalAlignment( JLabel.RIGHT );
        namePanel.add( nameLabel );
        _number = new NumberBox();
        _number.setBounds( 100, 50, 80, 25 );
        _number.limits( 0.0, 9999.0 );
        _number.setToolTipText( "Number (up to four digits) used to associate experiments with the same name." );
        namePanel.add( _number );
        _number.setVisible( false );
        _numberAsLabel = new JLabel( "" );
        _numberAsLabel.setBounds( 100, 50, 80, 25 );
        _numberAsLabel.setToolTipText( "Number used to associate experiments with the same name." );
        namePanel.add( _numberAsLabel );
        _numberAsLabel.setVisible( true );
        JLabel numberLabel = new JLabel( "Number:" );
        numberLabel.setBounds( 10, 50, 85, 25 );
        numberLabel.setHorizontalAlignment( JLabel.RIGHT );
        namePanel.add( numberLabel );
        _status = new JLabel( "unknown" );
        _status.setBounds( 100, 80, 210, 25 );
        _status.setToolTipText( "Current status of this experiment." );
        namePanel.add( _status );
        _status.setVisible( true );
        JLabel statusLabel = new JLabel( "Status:" );
        statusLabel.setBounds( 10, 80, 85, 25 );
        statusLabel.setHorizontalAlignment( JLabel.RIGHT );
        namePanel.add( statusLabel );
        _statusList = new JComboBox();
        _statusList.setBounds( 100, 80, 210, 25 );
        _statusList.setToolTipText( "List of possible status settings for this experiment." );
        _statusList.setBackground( Color.WHITE );
        _statusList.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _status.setText( (String)_statusList.getSelectedItem() );
            }
        });
        namePanel.add( _statusList );
        _statusList.setVisible( false );
        _created = new JLabel( "" );
        _created.setBounds( 100, 140, 210, 25 );
        _created.setToolTipText( "Date this experiment was created (assigned by database if available)." );
        namePanel.add( _created );
        JLabel createdLabel = new JLabel( "Created:" );
        createdLabel.setBounds( 10, 140, 85, 25 );
        createdLabel.setHorizontalAlignment( JLabel.RIGHT );
        namePanel.add( createdLabel );
        _directory = new SaneTextField();
        _directory.setBounds( 100, 170, 310, 25 );
        _directory.setToolTipText( "\"Working\" directory containing all files for this experiment." );
        _directory.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                directoryChangeAction();
            }
        });
        namePanel.add( _directory );
        _directory.setVisible( false );
        _directoryAsLabel = new JLabel( "" );
        _directoryAsLabel.setBounds( 100, 170, 310, 25 );
        _directoryAsLabel.setToolTipText( "\"Working\" directory containing all files for this experiment." );
        namePanel.add( _directoryAsLabel );
        JLabel directoryLabel = new JLabel( "Directory:" );
        directoryLabel.setBounds( 10, 170, 85, 25 );
        directoryLabel.setHorizontalAlignment( JLabel.RIGHT );
        namePanel.add( directoryLabel );
        _vexFileName = new SaneTextField();
        _vexFileName.setBounds( 100, 200, 205, 25 );
        _vexFileName.setToolTipText( "Name of the .vex file associated with this experiment." );
        namePanel.add( _vexFileName );
        _vexFileNameAsLabel = new JLabel( "" );
        _vexFileNameAsLabel.setBounds( 100, 200, 200, 25 );
        _vexFileNameAsLabel.setToolTipText( "Name of the .vex file associated with this experiment." );
        namePanel.add( _vexFileNameAsLabel );
        JLabel vexFileLabel = new JLabel( ".vex File:" );
        vexFileLabel.setBounds( 10, 200, 85, 25 );
        vexFileLabel.setHorizontalAlignment( JLabel.RIGHT );
        namePanel.add( vexFileLabel );
        _previousVexFileMenu = new JPopupMenu( "" );
        _previousVexFileButton = new JButton( "Previous Files" );
        _previousVexFileButton.setToolTipText( "Choose from the list of all .vex files associated with this Experiment." );
        _previousVexFileButton.setEnabled( false );
        _previousVexFileButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _previousVexFileMenu.show( _previousVexFileButton, 0, 0 );
            }
        });
        namePanel.add( _previousVexFileButton );
        
        //  This panel is used to find a new vex file.
        IndexedPanel findVexPanel = new IndexedPanel( "Get .vex File Content" );
        findVexPanel.openHeight( 210 );
        findVexPanel.closedHeight( 20 );
        findVexPanel.open( false );
        _scrollPane.addNode( findVexPanel );
        _fromHost = new JCheckBox( "from DiFX Host" );
        _fromHost.setToolTipText( "Copy the .vex file data from a named file on the DiFX Host." );
        _fromHost.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                vexSourceChoice( _fromHost );
            }
        });
        findVexPanel.add( _fromHost );
        _fromHostLocation = new SaneTextField();
        _fromHostLocation.setToolTipText( "Full path to the file on the DiFX Host." );
        _fromHostLocation.setEnabled( false );
        _fromHostLocation.setText( _settings.defaultNames().vexFileSource );
        findVexPanel.add( _fromHostLocation );
        _viaHttp = new JCheckBox( "via HTTP" );
        _viaHttp.setToolTipText( "Copy the .vex file data from a given location using HTTP." );
        _viaHttp.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                vexSourceChoice( _viaHttp );
            }
        });
        findVexPanel.add( _viaHttp );
        _viaHttpLocation = new SaneTextField();
        _viaHttpLocation.setToolTipText( "HTTP location of .vex file." );
        _viaHttpLocation.setEnabled( false );
        _viaHttpLocation.setText( _settings.defaultNames().viaHttpLocation );
        findVexPanel.add( _viaHttpLocation );
        _viaFtp = new JCheckBox( "via FTP" );
        _viaFtp.setToolTipText( "Copy the .vex file data from a given address via FTP." );
        _viaFtp.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                vexSourceChoice( _viaFtp );
            }
        });
        findVexPanel.add( _viaFtp );
        _viaFtpLocation = new SaneTextField();
        _viaFtpLocation.setToolTipText( "FTP address of .vex file." );
        _viaFtpLocation.setEnabled( false );
        _viaFtpLocation.setText( _settings.defaultNames().viaFtpLocation );
        findVexPanel.add( _viaFtpLocation );
        _localFile = new JCheckBox( "from Local File" );
        _localFile.setToolTipText( "Copy the .vex file data from a file on the local host." );
        _localFile.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                vexSourceChoice( _localFile );
            }
        });
        findVexPanel.add( _localFile );
        _localFileLocation = new SaneTextField();
        _localFileLocation.setToolTipText( "Location of the .vex file on the local machine." );
        _localFileLocation.setEnabled( false );
        _localFileLocation.setText( _settings.defaultNames().localFileLocation );
        findVexPanel.add( _localFileLocation );
        _fromExperiment = new JCheckBox( "from Experiment" );
        _fromExperiment.setToolTipText( "Copy the .vex file from another experiment." );
        _fromExperiment.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                vexSourceChoice( _fromExperiment );
            }
        });
        findVexPanel.add( _fromExperiment );
        _vexBrowseButton = new JButton( "Browse" );
        _vexBrowseButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                getVexFromSource();
            }
        });
        _goButton = new JButton( "GO!" );
        findVexPanel.add( _vexBrowseButton );
        _goButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                getVexFromSource();
            }
        });
        findVexPanel.add( _goButton );
        
        //  This panel contains menus that permit setting of .vex file parameters.
//        IndexedPanel vexPropertiesPanel = new IndexedPanel( ".vex File Properties" );
//        vexPropertiesPanel.openHeight( 160 );
//        vexPropertiesPanel.closedHeight( 20 );
//        _scrollPane.addNode( vexPropertiesPanel );
        
        //  This panel contains a text editor so the .vex file can be edited by
        //  hand.
        IndexedPanel editorPanel = new IndexedPanel( ".vex File Editor" );
        editorPanel.openHeight( 500 );
        editorPanel.closedHeight( 20 );
        editorPanel.open( false );
        _scrollPane.addNode( editorPanel );
        _editor = new SimpleTextEditor();
        editorPanel.add( _editor );
        _useMyEditor = new JButton( "Alternate Editor" );
        _useMyEditor.setToolTipText( "Use your preferred text editor to edit the .vex file.\nThe preferred editor is specified in the Settings Window." );
        _useMyEditor.setBounds( 20, 20, 140, 25 );
        _useMyEditor.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                useMyEditor();
            }
        });
        editorPanel.add( _useMyEditor );
        
        //  This panel is used to display and adjust antenna information.
        IndexedPanel antennaPanel = new IndexedPanel( "Antennas" );
        antennaPanel.openHeight( 200 );
        antennaPanel.closedHeight( 20 );
        antennaPanel.open( false );
        _scrollPane.addNode( antennaPanel );
        _antennaPane = new NodeBrowserScrollPane();
        antennaPanel.add( _antennaPane );
        
        
        //  This panel is used to select scans, which turn into jobs.
        IndexedPanel scanPanel = new IndexedPanel( "Scan Selection" );
        scanPanel.openHeight( 400 );
        scanPanel.closedHeight( 20 );
        scanPanel.open( false );
        _scrollPane.addNode( scanPanel );
        _scanGrid = new ButtonGrid();
        _scanGrid.addChangeListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                produceV2dFile();
            }
        });
        scanPanel.add( _scanGrid );
        
        //  The v2d editor allows the .v2d file to be edited by hand.  At the
        //  moment it is visible, but I plan to make it not so!
        IndexedPanel v2dEditorPanel = new IndexedPanel( ".v2d File" );
        v2dEditorPanel.openHeight( 500 );
        v2dEditorPanel.closedHeight( 20 );
        v2dEditorPanel.open( false );
        _scrollPane.addNode( v2dEditorPanel );
        _v2dEditor = new SimpleTextEditor();
        v2dEditorPanel.add( _v2dEditor );
        _v2dFileName = new SaneTextField();
        v2dEditorPanel.add( _v2dFileName );
        JLabel v2dFileNameLabel = new JLabel( "File Name:" );
        v2dFileNameLabel.setBounds( 10, 20, 85, 25 );
        v2dFileNameLabel.setHorizontalAlignment( JLabel.RIGHT );
        v2dEditorPanel.add( v2dFileNameLabel );
        
        //  The "button" panel contains buttons and other items that always appear
        //  at the bottom of the frame.  It can not be closed.
        IndexedPanel buttonPanel = new IndexedPanel( "" );
        buttonPanel.openHeight( 180 );
        buttonPanel.alwaysOpen( true );
        buttonPanel.noArrow( true );
        _scrollPane.addNode( buttonPanel );
        _passLabel = new JLabel( "Create Pass:" );
        _passLabel.setBounds( 10, 20, 85, 25 );
        _passLabel.setHorizontalAlignment( JLabel.RIGHT );
        buttonPanel.add( _passLabel );
        _createPass = new JCheckBox( "" );
        _createPass.setBounds( 100, 20, 25, 25 );
        _createPass.setToolTipText( "Create a pass for this experiment." );
        _createPass.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                createPassSetting();
            }
        });
        buttonPanel.add( _createPass );
        _passName = new SaneTextField();
        _passName.setBounds( 285, 20, 280, 25 );
        _passName.setToolTipText( "Name of the new pass." );
        _passName.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                autoSetPassDirectory();
            }
        });
        buttonPanel.add( _passName );
        _passDirectory = new SaneTextField();
        _passDirectory.setBounds( 285, 50, 280, 25 );
        _passDirectory.setToolTipText( "Directory for all input files and data associated with the new pass." );
        _passDirectory.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                keepPassDirectory( true );
            }
        });
        buttonPanel.add( _passDirectory );
        JLabel passDirectoryLabel = new JLabel( "Pass Sub-Directory:" );
        passDirectoryLabel.setBounds( 10, 50, 270, 25 );
        passDirectoryLabel.setHorizontalAlignment( JLabel.RIGHT );
        buttonPanel.add( passDirectoryLabel );
        _stagingArea = new SaneTextField();
        _stagingArea.setBounds( 285, 80, 280, 25 );
        _stagingArea.setToolTipText( "Directory into which to copy all input files for processing." );
        _stagingArea.setText( _settings.stagingArea() );
        buttonPanel.add( _stagingArea );
        JLabel stagingAreaLabel = new JLabel( "Use Staging Area:" );
        stagingAreaLabel.setBounds( 10, 80, 240, 25 );
        stagingAreaLabel.setHorizontalAlignment( JLabel.RIGHT );
        buttonPanel.add( stagingAreaLabel );
        _useStagingArea = new JCheckBox( "" );
        _useStagingArea.setBounds( 255, 80, 25, 25 );
        _useStagingArea.setSelected( _settings.useStagingArea() );
        if ( _useStagingArea.isSelected() ) {
            _stagingArea.setEnabled( true );
        }
        else {
            _stagingArea.setEnabled( false );
        }
        _useStagingArea.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                if ( _useStagingArea.isSelected() ) {
                    _stagingArea.setEnabled( true );
                }
                else {
                    _stagingArea.setEnabled( false );
                }
            }
        });
        buttonPanel.add( _useStagingArea );
        _passTypeList = new JComboBox();
        _passTypeList.setBounds( 130, 20, 150, 25 );
        _passTypeList.setToolTipText( "List of possible pass types." );
        _passTypeList.setBackground( Color.WHITE );
        _passTypeList.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                //  Maybe set the pass name to something that contains (String)_passTypeList.getSelectedItem()
                //  if the user hasn't specified a name yet
                //  See if the name of the pass looks like the default name for
                //  the previous (called "current") pass type.  If so, make a new
                //  default name out of this new pass type.
                if ( _passName.getText().equalsIgnoreCase( _currentPassType + " Pass" ) ) {
                    String newType = (String)_passTypeList.getSelectedItem();
                    _passName.setText( newType.substring( 0, 1 ).toUpperCase() + newType.substring( 1 ) + " Pass" );
                    autoSetPassDirectory();
                }
                _currentPassType = (String)_passTypeList.getSelectedItem();
                
            }
        });
        buttonPanel.add( _passTypeList );
        _currentPassType = (String)_passTypeList.getSelectedItem();
        Iterator iter = _settings.passTypeList().entrySet().iterator();
        for ( ; iter.hasNext(); )
            _passTypeList.addItem( (String)((Map.Entry)iter.next()).getValue() );
        _dataLabel = new JLabel( "Containing:" );
        _dataLabel.setBounds( 10, 110, 85, 25 );
        _dataLabel.setHorizontalAlignment( JLabel.RIGHT );
        buttonPanel.add( _dataLabel );
        _currentDataCheck = new JCheckBox( "Current Data" );
        _currentDataCheck.setBounds( 100, 110, 125, 25 );
        _currentDataCheck.setToolTipText( "New pass will contain all existing jobs and data for this experiment not already in a pass." );
        _currentDataCheck.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _newJobsCheck.setSelected( false );
                _currentDataCheck.setSelected( true );
                _singleInputFileCheck.setEnabled( false );
            }
        });
        buttonPanel.add( _currentDataCheck );
        _newJobsCheck = new JCheckBox( "New Jobs" );
        _newJobsCheck.setBounds( 230, 110, 100, 25 );
        _newJobsCheck.setToolTipText( "New pass will contain new jobs specified below." );
        _newJobsCheck.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _newJobsCheck.setSelected( true );
                _currentDataCheck.setSelected( false );
                _singleInputFileCheck.setEnabled( true );
            }
        });
        buttonPanel.add( _newJobsCheck );
        //  By default, "new jobs" will be checked.  Perhaps we can be smarter about
        //  this - base the setting on the existence of jobs outside a pass, or
        //  something else?
        _newJobsCheck.setSelected( true );
        _singleInputFileCheck = new JCheckBox( "Combine Scans in One Job" );
        _singleInputFileCheck.setBounds( 330, 110, 200, 25 );
        _singleInputFileCheck.setToolTipText( "Combine all specified scans in a single job (as opposed to one scan per job)." );
        _singleInputFileCheck.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _settings.defaultNames().singleInputFile = _singleInputFileCheck.isSelected();
            }
        });
        buttonPanel.add( _singleInputFileCheck );
        _singleInputFileCheck.setSelected( _settings.defaultNames().singleInputFile );
        _cancelButton = new JButton( "Dismiss" );
        _cancelButton.setBounds( 100, 140, 100, 25 );
        _cancelButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                ok( false );
            }
        });
        buttonPanel.add( _cancelButton );
        _okButton = new JButton( "Edit" );
        _okButton.setBounds( 210, 140, 100, 25 );
        _okButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                ok( true );
            }
        });
        buttonPanel.add( _okButton );
        
        _allObjectsBuilt = true;
        
    }        

    /*
     * Resize all components to fit properly anytime the window is resized or 
     * panes are opened and closed.
     */
    public void newSize() {
        int w = this.getWidth();
        int h = this.getHeight();
        _settings.windowConfiguration().experimentEditorW = w;
        _settings.windowConfiguration().experimentEditorH = h;
        if ( _menuBar != null )
            _menuBar.setBounds( 0, 0, w, 25 );
        if ( _allObjectsBuilt ) {
            _scrollPane.setBounds( 0, 25, w, h - 47 );
            if ( _displayPassInfo ) {
                _createPass.setBounds( 100, 20, 25, 25 );
                _passName.setBounds( 285, 20, w - 310, 25 );
                _cancelButton.setBounds( w - 235, 140, 100, 25 );
                _okButton.setBounds( w - 125, 140, 100, 25 );
            }
            else {
                _createPass.setVisible( false );
                _passName.setVisible( false );
                _passLabel.setVisible( false );
                _cancelButton.setBounds( w - 235, 140, 100, 25 );
                _okButton.setBounds( w - 125, 140, 100, 25 );
            }
            _directory.setBounds( 100, 170, w - 125, 25 );
            _vexFileName.setBounds( 100, 200, w - 255, 25 );
            _previousVexFileButton.setBounds( w - 150, 200, 125, 25 );
            _directoryAsLabel.setBounds( 100, 170, w - 125, 25 );
            _vexFileNameAsLabel.setBounds( 100, 200, w - 125, 25 );
            _editor.setBounds( 10, 50, w - 35, 440 );
            _fromHost.setBounds( 20, 20, 150, 25 );
            _fromHostLocation.setBounds( 175, 20, w - 200, 25 );
            _viaHttp.setBounds( 20, 50, 150, 25 );
            _viaHttpLocation.setBounds( 175, 50, w - 200, 25 );
            _viaFtp.setBounds( 20, 80, 150, 25 );
            _viaFtpLocation.setBounds( 175, 80, w - 200, 25 );
            _localFile.setBounds( 20, 110, 150, 25 );
            _localFileLocation.setBounds( 175, 110, w - 200, 25 );
            _fromExperiment.setBounds( 20, 140, 150, 25 );
            _vexBrowseButton.setBounds( 175, 140, 100, 25 );
            _goButton.setBounds( w - 125, 170, 100, 25 );
            _scanGrid.setBounds( 10, 40, w - 35, 350 );
            _v2dFileName.setBounds( 100, 20, w - 125, 25 );
            _v2dEditor.setBounds( 10, 50, w - 35, 440 );
            _passDirectory.setBounds( 285, 50, w - 310, 25 );
            _stagingArea.setBounds( 285, 80, w - 310, 25 );
            _antennaPane.setBounds( 10, 30, w - 35, 160 );
        }
    }
    
    /*
     * One of the checkboxes for vex file sources was checked.
     */
    public void vexSourceChoice( JCheckBox selection ) {
        _fromHost.setSelected( false );
        _fromHostLocation.setEnabled( false );
        _viaHttp.setSelected( false );
        _viaHttpLocation.setEnabled( false );
        _viaFtp.setSelected( false );
        _viaFtpLocation.setEnabled( false );
        _localFile.setSelected( false );
        _localFileLocation.setEnabled( false );
        _fromExperiment.setSelected( false );
        _vexBrowseButton.setEnabled( false );
        if ( _fromHost == selection ) {
            _fromHost.setSelected( true );
            _fromHostLocation.setEnabled( true );
        }
        else if ( _viaHttp == selection ) {
            _viaHttp.setSelected( true );
            _viaHttpLocation.setEnabled( true );
        }
        else if ( _viaFtp == selection ) {
            _viaFtp.setSelected( true );
            _viaFtpLocation.setEnabled( true );
        }
        else if ( _localFile == selection ) {
            _localFile.setSelected( true );
            _localFileLocation.setEnabled( true );
        }
        else if ( _fromExperiment == selection ) {
            _fromExperiment.setSelected( true );
            _vexBrowseButton.setEnabled( true );
        }
    }
    
    /*
     * Set the name of the pass directory based on the pass name.
     */
    public void autoSetPassDirectory() {
        if ( !_keepPassDirectory ) {
            String str = _passName.getText();
            //  Replace any spaces with underscores.
            str = str.replace( ' ', '_' );
            str = str.replace( '\t', '_' );
            _passDirectory.setText( str );
        }
    }
    
    private DiFXCommand_getFile _fileGet;
    /*
     * Obtain .vex file from the user-specified source.
     */
    public void getVexFromSource() {
        if ( _fromHost.isSelected() ) {
            _settings.defaultNames().vexFileSource = _fromHostLocation.getText();
            _fileGet = new DiFXCommand_getFile( _fromHostLocation.getText(), _settings );
            _fileGet.addEndListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    //  Check the file size....this will tell us if anything went
                    //  wrong, and to some degree what.
                    int fileSize = _fileGet.fileSize();
                    if ( fileSize > 0 ) {
                        //  Was it only partially read?
                        if ( fileSize > _fileGet.inString().length() )
                            JOptionPane.showMessageDialog( _this, "Warning - connection terminated with "
                                    + _fileGet.inString().length() + " of "
                                    + fileSize + " bytes read." );
                        _editor.text( _fileGet.inString() );
                        parseNewVexFile();
                    }
                    else if ( fileSize == 0 ) {
                         JOptionPane.showMessageDialog( _this, "File \"" + _fromHostLocation.getText() + "\" has zero length.",
                                 "Zero Length File", JOptionPane.WARNING_MESSAGE );
                    }
                    else if ( fileSize == -10 ) {
                         JOptionPane.showMessageDialog( _this, "Socket connection timed out before DiFX host connected.",
                                 "Socket Timeout", JOptionPane.ERROR_MESSAGE );                                        }
                    else if ( fileSize == -11 ) {
                         JOptionPane.showMessageDialog( _this, "File transfer failed - " + _fileGet.error(),
                                 "File Transfer Error", JOptionPane.ERROR_MESSAGE );
                    }
                    else if ( fileSize == -1 ) {
                        JOptionPane.showMessageDialog( _this, "Bad file name (probably the path was not complete.",
                                "File Name Error", JOptionPane.ERROR_MESSAGE );
                    }
                    else if ( fileSize == -2 ) {
                        JOptionPane.showMessageDialog( _this, "Requested file \"" + _fromHostLocation.getText() + "\" does not exist.",
                                "File Not Found", JOptionPane.ERROR_MESSAGE );
                    }
                    else if ( fileSize == -3 ) {
                        JOptionPane.showMessageDialog( _this, "Error - DiFX user " + _settings.difxControlUser()
                             + " does not have read permission for named file.",
                                "Permission Denied", JOptionPane.ERROR_MESSAGE );
                    }
                    else if ( fileSize == -4 ) {
                        JOptionPane.showMessageDialog( _this, "DiFX user name " + _settings.difxControlUser() +
                             " not valid on DiFX host.", "Bad User Name",
                             JOptionPane.ERROR_MESSAGE );   
                    }
                    else {
                        JOptionPane.showMessageDialog( _this, "Unknown error encountered during file transfer.",
                                "Unknown Error",
                                JOptionPane.ERROR_MESSAGE );
                    }

                }
            });            
            _fileGet.readString();
        }
        else if ( _viaHttp.isSelected() ) {
            _settings.defaultNames().viaHttpLocation = _viaHttpLocation.getText();
            try {
                URL url = new URL( "http://" + _viaHttpLocation.getText() );
                url.openConnection();
                InputStream reader = url.openStream();
                byte[] buffer = new byte[153600];
                int bytesRead = 0;
                _editor.text( "" );
                while ( ( bytesRead = reader.read( buffer, 0, 153600 ) ) > 0 ) {
                    buffer[bytesRead] = 0;
                    _editor.addText( new String( buffer ) );
                }
                parseNewVexFile();
            } catch ( MalformedURLException e ) {
                JOptionPane.showMessageDialog( _this, "Malformed URL: \"http://" + _viaHttpLocation.getText() + "\"",
                        "Bad URL",
                        JOptionPane.ERROR_MESSAGE );
            } catch ( IOException e ) {
                JOptionPane.showMessageDialog( _this, e.toString(),
                        "IO Error",
                        JOptionPane.ERROR_MESSAGE );
            }
        }
        else if ( _viaFtp.isSelected() ) {
            _settings.defaultNames().viaFtpLocation = _viaFtpLocation.getText();
            try {
                URL url = new URL( "ftp://" + _viaHttpLocation.getText() );
                url.openConnection();
                InputStream reader = url.openStream();
                byte[] buffer = new byte[153600];
                int bytesRead = 0;
                _editor.text( "" );
                while ( ( bytesRead = reader.read( buffer, 0, 153600 ) ) > 0 ) {
                    buffer[bytesRead] = 0;
                    _editor.addText( new String( buffer ) );
                }
                parseNewVexFile();
            } catch ( MalformedURLException e ) {
                JOptionPane.showMessageDialog( _this, "Malformed URL: \"ftp://" + _viaFtpLocation.getText() + "\"",
                        "Bad URL",
                        JOptionPane.ERROR_MESSAGE );
            } catch ( IOException e ) {
                JOptionPane.showMessageDialog( _this, e.toString(),
                        "IO Error",
                        JOptionPane.ERROR_MESSAGE );
            }
        }
        else if ( _localFile.isSelected() ) {
            _settings.defaultNames().localFileLocation = _localFileLocation.getText();
            try {
                FileInputStream reader = new FileInputStream( _localFileLocation.getText() );
                byte[] buffer = new byte[153600];
                int bytesRead = 0;
                _editor.text( "" );
                while ( ( bytesRead = reader.read( buffer, 0, 153600 ) ) > 0 ) {
                    buffer[bytesRead] = 0;
                    _editor.addText( new String( buffer ) );
                }
                parseNewVexFile();
            } catch ( FileNotFoundException e ) {
                JOptionPane.showMessageDialog( _this, "Local File \"" + _localFileLocation.getText() + "\" was not found.",
                        "File Not Found",
                        JOptionPane.ERROR_MESSAGE );
            } catch ( IOException e ) {
                JOptionPane.showMessageDialog( _this, e.toString(),
                        "IO Error",
                        JOptionPane.ERROR_MESSAGE );
            }
        }
        else {
            new AePlayWave( _settings.guiDocPath() + "/cantdo.wav" ).start();
            JOptionPane.showMessageDialog( this, "That feature has not been implemented." );
        }
    }
    
    /*
     * This class is used to display information about an antenna.  It is an IndexedPanel
     * with a bunch of controls.
     */
    public class AntennaPanel extends IndexedPanel {
        
        public AntennaPanel( String title ) {
            super( title );
            this.closedHeight( 20 );
            this.openHeight( 130 );
            this.open( false );
            _useCheck = new JCheckBox( "" );
            _useCheck.setBounds( 100, 2, 18, 20 );
            _useCheck.setSelected( true );
            _useCheck.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                    dispatchChangeCallback();
                }
            } );
            this.add( _useCheck );
            _dataSource = new JLabel( "unknown" );
            _dataSource.setBounds( 230, 2, 400, 20 );
            this.add( _dataSource );
            JLabel dataSourceLabel= new JLabel( "Data Source: " );
            dataSourceLabel.setBounds( 100, 2, 125, 20 );
            dataSourceLabel.setHorizontalAlignment( JLabel.RIGHT );
            this.add( dataSourceLabel );
            _vsnCheck = new JCheckBox( "" );
            _vsnCheck.setBounds( 200, 30, 25, 25 );
            _vsnCheck.setSelected( false );
            _vsnCheck.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                    _vsnCheck.setSelected( true );
                    _vsnList.setEnabled( true );
                    _fileCheck.setSelected( false );
                    _eVLBICheck.setSelected( false );
                    _dataSource.setText( (String)_vsnList.getSelectedItem() );
                    produceV2dFile();
                }
            } );
            this.add( _vsnCheck );
            JLabel vsnLabel = new JLabel( "Module: " );
            vsnLabel.setBounds( 100, 30, 95, 25 );
            vsnLabel.setHorizontalAlignment( JLabel.RIGHT );
            this.add( vsnLabel );
            _vsnList = new JComboBox();
            _vsnList.setBounds( 230, 30, 210, 25 );
            _vsnList.setToolTipText( "VSN of module containing data for this antenna." );
            _vsnList.setEditable( true );
            //  This little bit causes a typed-in item to be treated as a module name.
            _vsnList.getEditor().addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    _dataSource.setText( (String)_vsnList.getEditor().getItem() );
                    //  If not already in the list of VSNs, add this name.
                    if ( !_settings.dataSourceInList( (String)_vsnList.getEditor().getItem(), "VSN" ) ) {
                        if ( ((String)_vsnList.getEditor().getItem()).length() > 0 )
                            _settings.addDataSource( (String)_vsnList.getEditor().getItem(), "VSN", "hardware" );
                    }
                    produceV2dFile();
                }
            });
            _vsnList.setBackground( Color.WHITE );
            _vsnList.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    _dataSource.setText( (String)_vsnList.getSelectedItem() );
                    produceV2dFile();
                }
            });
            this.add( _vsnList );
            _vsnList.setEnabled( true );
            Iterator<SystemSettings.DataSource> iter = _settings.listDataSources( "VSN" ).iterator();
            for ( ; iter.hasNext(); )
                _vsnList.addItem( iter.next().name );
            _fileCheck = new JCheckBox( "" );
            _fileCheck.setBounds( 200, 55, 25, 25 );
            _fileCheck.setSelected( false );
            _fileCheck.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                    _vsnCheck.setSelected( false );
                    _fileCheck.setSelected( true );
                    _eVLBICheck.setSelected( false );
                    _vsnList.setEnabled( false );
                    _dataSource.setText( "some file" );
                    produceV2dFile();
                }
            } );
            this.add( _fileCheck );
            JLabel fileLabel = new JLabel( "File: " );
            fileLabel.setBounds( 100, 55, 95, 25 );
            fileLabel.setHorizontalAlignment( JLabel.RIGHT );
            this.add( fileLabel );
            _eVLBICheck = new JCheckBox( "" );
            _eVLBICheck.setBounds( 200, 80, 25, 25 );
            _eVLBICheck.setSelected( false );
            _eVLBICheck.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                    _vsnCheck.setSelected( false );
                    _fileCheck.setSelected( false );
                    _eVLBICheck.setSelected( true );
                    _vsnList.setEnabled( false );
                    _dataSource.setText( "some file" );
                    produceV2dFile();
                }
            } );
            this.add( _eVLBICheck );
            JLabel eVLBILabel = new JLabel( "E-Transfer: " );
            eVLBILabel.setBounds( 100, 80, 95, 25 );
            eVLBILabel.setHorizontalAlignment( JLabel.RIGHT );
            this.add( eVLBILabel );
            //  Default setup.  This should come from the SystemSettings class so
            //  it is saved between runs.
            _vsnCheck.setSelected( true );
            _changeListeners = new EventListenerList();
        }
        
        public boolean use() { return _useCheck.isSelected(); }
        public boolean useVsn() { return _vsnCheck.isSelected(); }
        public boolean useFile() { return _fileCheck.isSelected(); }
        public boolean useEVLBI() { return _eVLBICheck.isSelected(); }
        public String vsnSource() { return (String)_vsnList.getSelectedItem(); }
        
        /*
         * Add a "listener" to callbacks when any changes to button states occur.
         */
        public void addChangeListener( ActionListener a ) {
            _changeListeners.add( ActionListener.class, a );
        }

        protected void dispatchChangeCallback() {
            Object[] listeners = _changeListeners.getListenerList();
            // loop through each listener and pass on the event if needed
            int numListeners = listeners.length;
            for ( int i = 0; i < numListeners; i+=2 ) {
                if ( listeners[i] == ActionListener.class )
                    ((ActionListener)listeners[i+1]).actionPerformed( new ActionEvent( this, ActionEvent.ACTION_PERFORMED, "" ) );
            }
        }

        protected JCheckBox _useCheck;
        protected JCheckBox _vsnCheck;
        protected JCheckBox _fileCheck;
        protected JCheckBox _eVLBICheck;
        protected JComboBox _vsnList;
        protected JLabel _dataSource;
        protected EventListenerList _changeListeners;
        
    }
    
    /*
     * This is a list of antenna panels that provides some useful information about the
     * data in the list.
     */
    public class AntennaList extends ArrayList<AntennaPanel> {
        
        public ArrayList<AntennaPanel> useList() {
            ArrayList<AntennaPanel> list = new ArrayList<AntennaPanel>();
            for ( Iterator<AntennaPanel> iter = this.iterator(); iter.hasNext(); ) {
                AntennaPanel panel = iter.next();
                if ( panel.use() )
                    list.add( panel );
            }
            return list;
        }
        
    }
    
    /*
     * Read the current vex file, which is stored in the editor, and parse out items
     * that we can use in the .v2d file.
     */
    public void parseNewVexFile() {
        VexFileParser vexData = new VexFileParser();
        vexData.data( _editor.text() );
        //  Build a grid out of the scans found
        if ( vexData.scanList() != null ) {
            for ( Iterator iter = vexData.scanList().iterator(); iter.hasNext(); ) {
                VexFileParser.Scan scan = (VexFileParser.Scan)iter.next();
                String tooltip = scan.name + "\n" + scan.source + "\n" +
                        scan.start.get( Calendar.YEAR ) + "-" + scan.start.get( Calendar.DAY_OF_YEAR ) + " (" +
                        scan.start.get( Calendar.MONTH ) + "/" + scan.start.get( Calendar.DAY_OF_MONTH ) + ")  " +
                        scan.start.get( Calendar.HOUR_OF_DAY ) + ":" +
                        scan.start.get( Calendar.MINUTE ) + ":" + scan.start.get( Calendar.SECOND ) + "\n";
                for ( Iterator jter = scan.station.iterator(); jter.hasNext(); ) {
                    VexFileParser.ScanStation station = (VexFileParser.ScanStation)jter.next();
                    tooltip += station.wholeString + "\n";
                }
                _scanGrid.addButton( scan.name, tooltip, true );
            }
        }
        //  Add panels of information about each antenna.
        if ( vexData.stationList() != null ) {
            for ( Iterator<VexFileParser.Station> iter = vexData.stationList().iterator(); iter.hasNext(); ) {
                VexFileParser.Station station = iter.next();
                //  Make a new panel to hold this station.
                if ( _antennaList == null )
                    _antennaList = new AntennaList();
                AntennaPanel panel = new AntennaPanel( station.name );
                panel.addChangeListener( new ActionListener() {
                    public void actionPerformed( ActionEvent evt ) {
                        produceV2dFile();
                    }
                } );
                _antennaPane.addNode( panel );
                _antennaList.add( panel );
            }
        }
        produceV2dFile();
    }

    public String name() { return _name.getText(); }
    public void name( String newVal ) { 
        _name.setText( newVal );
        _nameAsLabel.setText( newVal );
    }
    public Integer number() { return _number.intValue(); }
    public void number( Integer newVal ) { 
        _number.intValue( newVal );
        _numberAsLabel.setText( newVal.toString() );
    }
    protected void ok( boolean newVal ) {
        _ok = newVal;
        if ( _editMode )
            this.setVisible( false );
        else {
            if ( !_ok )
                this.setVisible( false );
            else
                editMode( true );
        }
    }
    public boolean ok() { return _ok; }
    public void visible() {
        _ok = false;
        this.setVisible( true );
    }
    public boolean inDataBase() { return _inDataBase.isSelected(); }
    public void inDataBase( boolean newVal ) { 
        _inDataBase.setSelected( newVal );
        _saveInDataBase = newVal;  //  Used to maintain a value in non-edit mode.
    };
    public void id( Integer newVal ) { 
        if ( newVal == null )
            _id.setText( "" );
        else
            _id.setText( newVal.toString() );
    }
    public boolean editMode() { return _editMode; }
    public void editMode( boolean newVal ) { 
        _editMode = newVal;
        if ( _editMode ) {
            _name.setVisible( true );
            _nameAsLabel.setVisible( false );
            _number.setVisible( true );
            _numberAsLabel.setVisible( false );
            _directory.setVisible( true );
            _directoryAsLabel.setVisible( false );
            _okButton.setText( "Apply" );
            _cancelButton.setText( "Cancel" );
            Iterator iter = _settings.experimentStatusList().entrySet().iterator();
            String saveStatus = status();
            for ( ; iter.hasNext(); )
                _statusList.addItem( ((SystemSettings.ExperimentStatusEntry)((Map.Entry)iter.next()).getValue()).status );
            _statusList.setVisible( true );
            _status.setVisible( false );
            this.status( saveStatus );
            _vexFileName.setVisible( true );
            _vexFileNameAsLabel.setVisible( false );
            if ( _displayPassInfo ) {
                _passName.setVisible( true );
                _createPass.setVisible( true );
                JLabel passLabel = new JLabel( "Create Pass:" );
                passLabel.setBounds( 10, 230, 85, 25 );
                passLabel.setHorizontalAlignment( JLabel.RIGHT );
                this.getContentPane().add( passLabel );
            }
            else {
                _passName.setVisible( false );
                _createPass.setVisible( false );
            }
        }
        else {
            _name.setVisible( false );
            _nameAsLabel.setVisible( true );
            _number.setVisible( false );
            _numberAsLabel.setVisible( true );
            _directory.setVisible( false );
            _directoryAsLabel.setVisible( true );
            _okButton.setText( "Edit" );
            _cancelButton.setText( "Dismiss" );
            _statusList.setVisible( false );
            _status.setVisible( true );
            _vexFileName.setVisible( false );
            _vexFileNameAsLabel.setVisible( true );
            _passName.setVisible( false );
            _createPass.setVisible( false );
        }
    }
    protected void inDataBaseAction() {
        if ( !_editMode )
            _inDataBase.setSelected( _saveInDataBase );
        else
            _saveInDataBase = _inDataBase.isSelected();
    }
    public void created( String newVal ) { _created.setText( newVal ); }
    public void status( String newVal ) { 
        _status.setText( newVal );
        for ( int i = 0; i < _statusList.getItemCount(); ++i ) {
            if ( ((String)_statusList.getItemAt( i )).contentEquals( newVal ) ) {
                _statusList.setSelectedIndex( i );
            }
        }
    }
    public String status() { return _status.getText(); }
    public void directory( String newVal ) { 
        _directory.setText( newVal );
        _directoryAsLabel.setText( newVal );
        _keepDirectory = true;
    }
    public String directory() { return _directory.getText(); }
    public void keepDirectory( boolean newVal ) { _keepDirectory = newVal; }
    
    public String passName() { return _passName.getText(); }
    public void passName( String newVal ) { 
        _passName.setText( newVal );
        autoSetPassDirectory();
    }
    
    public String passDirectory() { return _passDirectory.getText(); }
    public void passDirectory( String newVal ) { _passDirectory.setText( newVal ); }
    
    public void keepPassDirectory( boolean newVal ) { _keepPassDirectory = newVal; }
    
    public boolean createPass() { return _createPass.isSelected(); }
    public void createPass( boolean newVal ) { 
        _createPass.setSelected( newVal );
        createPassSetting();
    }
    
    public String passType() {
        return (String)_passTypeList.getItemAt( _passTypeList.getSelectedIndex() );
    }
    
    public void displayPassInfo( boolean newVal ) { _displayPassInfo = newVal; }
    
    public void vexFileName( String newVal ) { _vexFileName.setText( newVal ); }
    public String vexFileName() { return _vexFileName.getText(); }
    
    public void v2dFileName( String newVal ) { _v2dFileName.setText( newVal ); }
    public String v2dFileName() { return _v2dFileName.getText(); }
    
    /*
     * A change has occurred in the name, which *maybe* should be propogated
     * to the directory, but only if the directory hasn't been previously
     * set (which probably indicates the experiment is new, but you can control
     * it using the "keepDirectory" function).
     */
    protected void nameChangeAction() {
        if ( !_keepDirectory ) {
            directory( _settings.workingDirectory() + "/" + _name.getText() );
            vexFileName( _name.getText() + ".vex" );
            keepDirectory( false );
        }
    }
    protected void directoryChangeAction() {
        keepDirectory( true );
    }
    
    /*
     * Called when the "create pass" checkbox is changed.  This enables or
     * disables related items.
     */
    public void createPassSetting() {
        if ( _createPass.isSelected() ) {
            _passName.setEnabled( true );
            _passTypeList.setEnabled( true );
            _newJobsCheck.setEnabled( true );
            _newJobsCheck.setEnabled( true );
            _currentDataCheck.setEnabled( true );
            _dataLabel.setEnabled( true );
        }
        else {
            _passName.setEnabled( false );
            _passTypeList.setEnabled( false );
            _newJobsCheck.setEnabled( false );
            _newJobsCheck.setEnabled( false );
            _currentDataCheck.setEnabled( false );
            _dataLabel.setEnabled( false );
        }
    }
    
    /*
     * Launch the user's preferred text editor to edit the .vex file.  This involves
     * copying it to a temporary location, editing the copy, then copying it back.
     */
    protected void useMyEditor() {
        new AePlayWave( _settings.guiDocPath() + "/cantdo.wav" ).start();
        JOptionPane.showMessageDialog( this, "That feature has not been implemented." );
    }
    
    DiFXCommand_sendFile _fileSend;
    /*
     * Send the contents of the text editor to the DiFX host, writing it in the
     * location specified by the vexFileName.
     */
    public void writeVexFile() {
        //  First, make sure there IS a vex file name.
        if ( vexFileName().length() <= 0 ) {
            JOptionPane.showMessageDialog( _this, "No .vex file name was specified.",
                    "Missing Filename", JOptionPane.WARNING_MESSAGE );
            return;
        }
        String passDir = "";
        if ( this.createPass() )
            passDir = passDirectory() + "/";
        _fileSend = new DiFXCommand_sendFile( directory() + "/" + passDir + vexFileName(), _settings );
        _fileSend.addEndListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                int fileSize = _fileSend.fileSize();
                if ( fileSize >= 0 ) {
                    if ( fileSize < _editor.text().length() ) {
                        JOptionPane.showMessageDialog( _this, "Only " + _fileSend.fileSize() + 
                                " of " + _editor.text().length() + " characters were transmitted.",
                                "Transfer Incomplete", JOptionPane.ERROR_MESSAGE );
                    }
                }
                else if ( fileSize == -10 ) {
                     JOptionPane.showMessageDialog( _this, "Socket connection timed out before DiFX host connected.",
                             "Socket Timeout", JOptionPane.ERROR_MESSAGE );                                        }
                else if ( fileSize == -11 ) {
                     JOptionPane.showMessageDialog( _this, "File transfer failed - " + _fileGet.error(),
                             "File Transfer Error", JOptionPane.ERROR_MESSAGE );
                }
                else if ( fileSize == -1 ) {
                    JOptionPane.showMessageDialog( _this, "Mangled path name - does the destination directory start with \"/\"?",
                            "Path Error", JOptionPane.ERROR_MESSAGE );
                }
                else if ( fileSize == -2 ) {
                    JOptionPane.showMessageDialog( _this, "Destination directory does not exist.",
                            "Directory Not Found", JOptionPane.ERROR_MESSAGE );
                }
                else if ( fileSize == -3 ) {
                    JOptionPane.showMessageDialog( _this, "Error - DiFX user " + _settings.difxControlUser()
                         + " does not have proper permissions for the destination directory.",
                            "Permission Denied", JOptionPane.ERROR_MESSAGE );
                }
                else if ( fileSize == -4 ) {
                    JOptionPane.showMessageDialog( _this, "DiFX user name " + _settings.difxControlUser() +
                         " not valid on DiFX host.", "Bad User Name",
                         JOptionPane.ERROR_MESSAGE );   
                }
                else if ( fileSize == -5 ) {
                    JOptionPane.showMessageDialog( _this, "Destination directory is not valid.", "Bad Directory",
                         JOptionPane.ERROR_MESSAGE );   
                }
                else {
                    JOptionPane.showMessageDialog( _this, "Unknown error encountered during file transfer.",
                                "Unknown Error",
                         JOptionPane.ERROR_MESSAGE );   
                }
            }
        });
        _fileSend.sendString( _editor.text() );
    }
    
    DiFXCommand_sendFile _fileSendV2d;
    /*
     * Send the contents of the v2d text editor to the DiFX host, writing it in the
     * location of the .v2d file name.
     */
    public void writeV2dFile() {
        //  First, make sure there IS a vex file name.
        if ( v2dFileName().length() <= 0 ) {
            JOptionPane.showMessageDialog( _this, "No .v2d file name was specified.",
                    "Missing Filename", JOptionPane.WARNING_MESSAGE );
            return;
        }
        String passDir = "";
        if ( this.createPass() )
            passDir = passDirectory() + "/";
        _fileSendV2d = new DiFXCommand_sendFile( directory() + "/" + passDir + v2dFileName(), _settings );
        _fileSendV2d.addEndListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                int fileSize = _fileSendV2d.fileSize();
                if ( fileSize >= 0 ) {
                    if ( fileSize < _v2dEditor.text().length() ) {
                        JOptionPane.showMessageDialog( _this, "Only " + _fileSendV2d.fileSize() + 
                                " of " + _v2dEditor.text().length() + " characters were transmitted.",
                                "Transfer Incomplete", JOptionPane.ERROR_MESSAGE );
                    }
                }
                else if ( fileSize == -10 ) {
                     JOptionPane.showMessageDialog( _this, "Socket connection timed out before DiFX host connected.",
                             "Socket Timeout", JOptionPane.ERROR_MESSAGE );                                        }
                else if ( fileSize == -11 ) {
                     JOptionPane.showMessageDialog( _this, "File transfer failed - " + _fileGet.error(),
                             "File Transfer Error", JOptionPane.ERROR_MESSAGE );
                }
                else if ( fileSize == -1 ) {
                    JOptionPane.showMessageDialog( _this, "Mangled path name - does the destination directory start with \"/\"?",
                            "Path Error", JOptionPane.ERROR_MESSAGE );
                }
                else if ( fileSize == -2 ) {
                    JOptionPane.showMessageDialog( _this, "Destination directory does not exist.",
                            "Directory Not Found", JOptionPane.ERROR_MESSAGE );
                }
                else if ( fileSize == -3 ) {
                    JOptionPane.showMessageDialog( _this, "Error - DiFX user " + _settings.difxControlUser()
                         + " does not have proper permissions for the destination directory.",
                            "Permission Denied", JOptionPane.ERROR_MESSAGE );
                }
                else if ( fileSize == -4 ) {
                    JOptionPane.showMessageDialog( _this, "DiFX user name " + _settings.difxControlUser() +
                         " not valid on DiFX host.", "Bad User Name",
                         JOptionPane.ERROR_MESSAGE );   
                }
                else if ( fileSize == -5 ) {
                    JOptionPane.showMessageDialog( _this, "Destination directory is not valid.", "Bad Directory",
                         JOptionPane.ERROR_MESSAGE );   
                }
                else {
                    JOptionPane.showMessageDialog( _this, "Unknown error encountered during file transfer.",
                                "Unknown Error",
                         JOptionPane.ERROR_MESSAGE );   
                }
            }
        });
        _fileSendV2d.sendString( _v2dEditor.text() );
    }

    /*
     * Add a name to the current "vex file list".  This is a list of vex file names
     * that the experiment being edited knows about.  The names are listed in a
     * popup menu from which they can be picked.  We create a menu item and give it
     * a callback - all of the callbacks are to the same function.
     */
    public void addVexFileName( String name ) {
        JMenuItem newItem = new JMenuItem( name );
        final String finalName = name;
        newItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                pickVexFile( new String( finalName ) );
            }
        });        
        _previousVexFileMenu.add( newItem );
        _previousVexFileButton.setEnabled( true );
    }
    
    /*
     * This method is called when a "previous" vex file is chosen from the menu.
     */
    public void pickVexFile( String name ) {
        System.out.println( "picked " + name );
    }
    
    /*
     * Provide a list of scan names that have been chosen by the scan grid to
     * be part of the current pass.
     */
    public ArrayList<String> onScans() {
        return _scanGrid.onItems();
    }
    
    /*
     * Use the current settings to produce a .v2d "file".  This is stored in the
     * v2d editor.
     */
    public void produceV2dFile() {
        //  Before creating this file, check the scan selections, as changes to
        //  settings may have changed the restrictions on them.
        //  Create a v2d file name, if one hasn't been used already....we use the
        //  .vex file name (if it exists).
        if ( _v2dFileName.getText().length() == 0 ) {
            String str = "";
            if ( vexFileName().length() > 0 )
                str = vexFileName().substring( 0, vexFileName().lastIndexOf( '.' ) ) + ".v2d";
            else
                str = name() + ".v2d";
            //  Remove underscores in the .v2d file name...these have special meaning
            //  to vex2difx and are thus illegal.
            str = str.replace( "_", "" );
            _v2dFileName.setText( str );
        }
        String str = "";
        str +=    "#   -- Configuration file for vex2difx --\n"
                + "#   This file was automatically generated by the DiFX GUI\n"
                + "#   Created: " + new SimpleDateFormat( "MMMMM dd, yyyy hh:mm:ss z" ).format( new Date() ) + "\n"
                + "#   By:      " + System.getProperty("user.name") + "\n\n"                        
                ;
        
        //  The .vex file - the only required parameter.  We give the full path
        //  to the file.
        str += "vex = " + _vexFileName.getText() + "\n\n";
        
        //  This keeps vex2difx from splitting up jobs in an unexpected way
        str += "maxGap = 180000\n\n";
        
        //  Whether or not we should split into one scan per job
        if ( _singleInputFileCheck.isSelected() )
            str += "singleScan = false\n\n";  // this is the default
        else
            str += "singleScan = true\n\n";
        
        //  The antennas included in this experiment...
        if ( _antennaList != null && !_antennaList.useList().isEmpty() ) {
            str += "antennas = ";
            for ( Iterator<AntennaPanel> iter = _antennaList.useList().iterator(); iter.hasNext(); ) {
                AntennaPanel antenna = iter.next();
                if ( antenna.use() ) {
                    str += antenna.name();
                    if ( iter.hasNext() )
                        str += ",";
                }
            }
            str += "\n\n";
        }
        
        //  This is a dummy "setup".  We might dispose of this later?
        str +=    "SETUP normalSetup\n"
                + "{\n"
                + "tInt = 2\n"
                + "}\n\n";
        
        //  Produce a list of the scans we want to include.  These come from the
        //  scan grid.  We only do this if any scans have been chosen (we produce
        //  a warning if there are no scans included).
        if ( _scanGrid.onItems().size() > 0 ) {
            str +=    "RULE scansubset\n"
                    + "{\n"
                    + "scan = ";
            for ( Iterator<String> iter = _scanGrid.onItems().iterator(); iter.hasNext(); ) {
                str += iter.next();
                if ( iter.hasNext() )
                    str += ",";
            }
            str +=    "\n"
                    + "setup = normalSetup\n"
                    + "}\n\n";
        }
        else {
            JOptionPane.showMessageDialog( _this, "No scans have been chosen for this processing\n(so no jobs will be created)!",
                    "Zero Scans Included", JOptionPane.WARNING_MESSAGE );
        }
        
        //  Describe specifics for each used antenna...data source, etc.
        if ( _antennaList != null && !_antennaList.useList().isEmpty() ) {
            for ( Iterator<AntennaPanel> iter = _antennaList.useList().iterator(); iter.hasNext(); ) {
                AntennaPanel antenna = iter.next();
                if ( antenna.use() ) {
                    str += "ANTENNA " + antenna.name() + "\n";
                    str += "{\n";
                    str += "   format = Mark5B\n";
                    if ( antenna.useVsn() ) {
                        if ( antenna.vsnSource() != null && antenna.vsnSource().length() > 0 )
                            str += "   vsn = " + antenna.vsnSource() + "\n";
                        else {
                            // Warning, perhaps?
                        }
                    }
                    str += "}\n\n";
                }
            }
            str += "\n\n";
        }
        else {
            //  Don't bother warning about no antennas...the warning about "no scans" will occur first.
        }
        
        
        _v2dEditor.text( str );
    }
    
    /*
     * Return a string containing the .v2d file.
     */
    public String v2dFile() {
        return _v2dEditor.text();
    }
        
    protected SaneTextField _name;
    protected JLabel _nameAsLabel;
    protected NumberBox _number;
    protected JLabel _numberAsLabel;
    protected boolean _ok;
    protected Timer _timeoutTimer;
    protected JCheckBox _inDataBase;
    protected JLabel _id;
    protected JLabel _created;
    protected boolean _editMode;
    protected boolean _saveInDataBase;
    protected JButton _okButton;
    protected JButton _cancelButton;
    protected JLabel _status;
    protected JComboBox _statusList;
    protected SystemSettings _settings;
    protected JLabel _directoryAsLabel;
    protected SaneTextField _directory;
    protected boolean _keepDirectory;
    protected SaneTextField _vexFileName;
    protected JLabel _vexFileNameAsLabel;
    protected JButton _editVexFileButton;
    protected JCheckBox _createPass;
    protected SaneTextField _passName;
    protected JLabel _passLabel;
    protected boolean _displayPassInfo;
    protected ExperimentEditor _this;
    protected NodeBrowserScrollPane _scrollPane;
    protected JMenuBar _menuBar;
    protected boolean _allObjectsBuilt;
    protected SimpleTextEditor _editor;
    protected JButton _useMyEditor;
    protected SimpleTextEditor _v2dEditor;
    protected SaneTextField _v2dFileName;
    protected JCheckBox _fromHost;
    protected SaneTextField _fromHostLocation;
    protected JCheckBox _viaHttp;
    protected SaneTextField _viaHttpLocation;
    protected JCheckBox _viaFtp;
    protected SaneTextField _viaFtpLocation;
    protected JCheckBox _localFile;
    protected SaneTextField _localFileLocation;
    protected JCheckBox _fromExperiment;
    protected JButton _vexBrowseButton;
    protected JButton _goButton;
    protected JButton _previousVexFileButton;
    protected JPopupMenu _previousVexFileMenu;
    protected JComboBox _passTypeList;
    protected String _currentPassType;
    protected JLabel _dataLabel;
    protected JCheckBox _currentDataCheck;
    protected JCheckBox _newJobsCheck;
    protected ButtonGrid _scanGrid;
    protected JCheckBox _singleInputFileCheck;
    protected SaneTextField _passDirectory;
    protected SaneTextField _stagingArea;
    protected JCheckBox _useStagingArea;
    protected boolean _keepPassDirectory;
    protected NodeBrowserScrollPane _antennaPane;
    protected AntennaList _antennaList;
    
}
