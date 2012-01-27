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

import java.awt.Frame;
import java.awt.Color;
import java.awt.Point;
import java.awt.Component;

import java.util.Map;
import java.util.Iterator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import mil.navy.usno.widgetlib.NodeBrowserScrollPane;
import mil.navy.usno.widgetlib.IndexedPanel;

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
        JMenuItem settingsHelpItem = new JMenuItem( "Vex File Editor Help" );
        settingsHelpItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _settings.launchGUIHelp( "vexFileEditor.html" );
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
        
        //  This panel is used to find a new vex file.
        IndexedPanel findVexPanel = new IndexedPanel( "Get a .vex File" );
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
        
        //  The "button" panel contains buttons and other items that always appear
        //  at the bottom of the frame.  It can not be closed.
        IndexedPanel buttonPanel = new IndexedPanel( "" );
        buttonPanel.openHeight( 100 );
        buttonPanel.alwaysOpen( true );
        buttonPanel.noArrow( true );
        _scrollPane.addNode( buttonPanel );
        _passLabel = new JLabel( "Default Pass:" );
        _passLabel.setBounds( 10, 20, 85, 25 );
        _passLabel.setHorizontalAlignment( JLabel.RIGHT );
        buttonPanel.add( _passLabel );
        _createPass = new JCheckBox( "" );
        _createPass.setBounds( 100, 20, 25, 25 );
        _createPass.setToolTipText( "Create a default pass for this new experiment." );
        buttonPanel.add( _createPass );
        _passName = new SaneTextField();
        _passName.setBounds( 130, 20, 280, 25 );
        _passName.setToolTipText( "Default pass name." );
        buttonPanel.add( _passName );
        _cancelButton = new JButton( "Dismiss" );
        _cancelButton.setBounds( 100, 50, 100, 25 );
        _cancelButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                ok( false );
            }
        });
        buttonPanel.add( _cancelButton );
        _okButton = new JButton( "Edit" );
        _okButton.setBounds( 210, 50, 100, 25 );
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
                _passName.setBounds( 130, 20, w - 155, 25 );
                _cancelButton.setBounds( 100, 50, 100, 25 );
                _okButton.setBounds( 210, 50, 100, 25 );
            }
            else {
                _createPass.setVisible( false );
                _passName.setVisible( false );
                _passLabel.setVisible( false );
                _cancelButton.setBounds( 100, 20, 100, 25 );
                _okButton.setBounds( 210, 20, 100, 25 );
            }
            _directory.setBounds( 100, 170, w - 125, 25 );
            _vexFileName.setBounds( 100, 200, w - 125, 25 );
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
     * Read the current vex file, which is stored in the editor, and parse out items
     * that we can use in the .v2d file.
     */
    public void parseNewVexFile() {
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
    public void passName( String newVal ) { _passName.setText( newVal ); }
    
    public boolean createPass() { return _createPass.isSelected(); }
    public void createPass( boolean newVal ) { _createPass.setSelected( newVal ); }
    
    public void displayPassInfo( boolean newVal ) { _displayPassInfo = newVal; }
    
    public void vexFileName( String newVal ) { _vexFileName.setText( newVal ); }
    public String vexFileName() { return _vexFileName.getText(); }
    
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
        _fileSend = new DiFXCommand_sendFile( directory() + "/" + vexFileName(), _settings );
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
    
    protected String _vexFile;

}
