/*
 * This class produces a modal pop-up window for adjusting the properties
 * specific to a pass.  This window is modal.
 */
package edu.nrao.difx.difxview;

import mil.navy.usno.widgetlib.SaneTextField;
import mil.navy.usno.widgetlib.NumberBox;

import javax.swing.JLabel;
import javax.swing.JDialog;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import java.awt.Frame;

import java.util.Map;
import java.util.Timer;
import java.awt.Color;

import java.util.Iterator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import mil.navy.usno.widgetlib.NodeBrowserScrollPane;
import mil.navy.usno.widgetlib.IndexedPanel;

/**
 *
 * @author jspitzak
 */
public class PassEditor extends JDialog {
        
    public PassEditor( Frame frame, int x, int y, SystemSettings settings ) {
        super( frame, "", true );
        _settings = settings;
        this.setBounds( x, y, 420, 280 );
        _this = this;
        this.getContentPane().setLayout( null );
        _menuBar = new JMenuBar();
        JMenu helpMenu = new JMenu( "  Help  " );
        _menuBar.add( helpMenu );
        JMenuItem settingsHelpItem = new JMenuItem( "Pass Editor Help" );
        settingsHelpItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _settings.launchGUIHelp( "passEditor.html" );
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
        
        //  The "name" panel includes all of the generic information about the
        //  pass.  It doesn't "fold" - it is always visible.
        IndexedPanel namePanel = new IndexedPanel( "" );
        namePanel.openHeight( 180 );
        namePanel.alwaysOpen( true );
        namePanel.noArrow( true );
        _scrollPane.addNode( namePanel );
        _name = new SaneTextField();
        _name.setBounds( 100, 20, 310, 25 );
        _name.textWidthLimit( 30 );
        _name.setToolTipText( "Name assigned to the pass (up to 30 characters)." );
        namePanel.add( _name );
        JLabel nameLabel = new JLabel( "Name:" );
        nameLabel.setBounds( 10, 20, 85, 25 );
        nameLabel.setHorizontalAlignment( JLabel.RIGHT );
        namePanel.add( nameLabel );
        JLabel idLabel = new JLabel( "Data Base ID:" );
        idLabel.setBounds( 10, 50, 85, 25 );
        idLabel.setHorizontalAlignment( JLabel.RIGHT );
        namePanel.add( idLabel );
        _id = new JLabel( "" );
        _id.setBounds( 100, 50, 70, 25 );
        namePanel.add( _id );
        _type = new JLabel( "unknown" );
        _type.setBounds( 100, 80, 210, 25 );
        _type.setToolTipText( "Current status of this experiment." );
        namePanel.add( _type );
        _type.setVisible( true );
        JLabel statusLabel = new JLabel( "Type:" );
        statusLabel.setBounds( 10, 80, 85, 25 );
        statusLabel.setHorizontalAlignment( JLabel.RIGHT );
        namePanel.add( statusLabel );
        _typeList = new JComboBox();
        _typeList.setBounds( 100, 80, 210, 25 );
        _typeList.setToolTipText( "Pass type." );
        _typeList.setBackground( Color.WHITE );
        _typeList.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _type.setText( (String)_typeList.getSelectedItem() );
            }
        });
        namePanel.add( _typeList );
        _typeList.setVisible( false );
        _subdirectory = new SaneTextField();
        _subdirectory.setBounds( 100, 110, 210, 25 );
        _subdirectory.textWidthLimit( 30 );
        _subdirectory.setToolTipText( "Subdirectory for this Pass (under the directory of the parent Experiment)." );
        namePanel.add( _subdirectory );
        JLabel subdirectoryLabel = new JLabel( "Subdirectory:" );
        subdirectoryLabel.setBounds( 10, 110, 85, 25 );
        subdirectoryLabel.setHorizontalAlignment( JLabel.RIGHT );
        namePanel.add( subdirectoryLabel );
        _v2dFile = new SaneTextField();
        _v2dFile.setBounds( 100, 140, 210, 25 );
        _v2dFile.textWidthLimit( 30 );
        _v2dFile.setToolTipText( "Name of the .v2d file associated with this pass." );
        namePanel.add( _v2dFile );
        JLabel v2dFileLabel = new JLabel( ".v2d File:" );
        v2dFileLabel.setBounds( 10, 140, 85, 25 );
        v2dFileLabel.setHorizontalAlignment( JLabel.RIGHT );
        namePanel.add( v2dFileLabel );
        
        /*
         * The "generate" panel includes the controls for generating a new .v2d file.
         */
        IndexedPanel generatePanel = new IndexedPanel( "Generate .v2d File" );
        generatePanel.openHeight( 160 );
        generatePanel.closedHeight( 20 );
        _scrollPane.addNode( generatePanel );
        
        /*
         * The "edit" panel contains controls for editing the .v2d parameters.
         */
        IndexedPanel editPanel = new IndexedPanel( ".v2d Parameters" );
        editPanel.openHeight( 160 );
        editPanel.closedHeight( 20 );
        _scrollPane.addNode( editPanel );
        
        /*
         * The "button" panel contains the operational buttons used to apply changes,
         * generate jobs, etc.
         */
        IndexedPanel buttonPanel = new IndexedPanel( "" );
        buttonPanel.openHeight( 60 );
        buttonPanel.alwaysOpen( true );
        buttonPanel.noArrow( true );
        _scrollPane.addNode( buttonPanel );
        _cancelButton = new JButton( "Cancel" );
        _cancelButton.setBounds( 100, 20, 100, 25 );
        _cancelButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                ok( false );
            }
        });
        buttonPanel.add( _cancelButton );
        _okButton = new JButton( "Apply" );
        _okButton.setBounds( 210, 20, 100, 25 );
        _okButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                ok( true );
            }
        });
        buttonPanel.add( _okButton );
        
        _allObjectsBuilt = true;
    }        

    /*
     * This function redraws all components when timeouts occur.
     */
    public void newSize() {
        int w = this.getWidth();
        int h = this.getHeight();
        if ( _menuBar != null )
            _menuBar.setBounds( 0, 0, w, 25 );
        if ( _allObjectsBuilt ) {
            _scrollPane.setBounds( 0, 25, w, h - 47 );
            _subdirectory.setBounds( 100, 110, w - 130, 25 );
            _v2dFile.setBounds( 100, 140, w - 130, 25 );
        }
    }
    
    public String name() { return _name.getText(); }
    public void name( String newVal ) { 
        _name.setText( newVal );
    }
    protected void ok( boolean newVal ) {
        _ok = newVal;
        this.setVisible( false );
    }
    public boolean ok() { return _ok; }
    public void visible() {
        _ok = false;
        this.setVisible( true );
    }
    public void id( Integer newVal ) { 
        if ( newVal == null )
            _id.setText( "" );
        else
            _id.setText( newVal.toString() );
    }
    public void type( String newVal ) { 
        _type.setText( newVal );
        for ( int i = 0; i < _typeList.getItemCount(); ++i ) {
            if ( ((String)_typeList.getItemAt( i )).contentEquals( newVal ) ) {
                _typeList.setSelectedIndex( i );
            }
        }
    }
    public String type() { return _type.getText(); }

    protected SaneTextField _name;
    protected boolean _ok;
    protected Timer _timeoutTimer;
    protected JLabel _id;
    protected JButton _okButton;
    protected JButton _cancelButton;
    protected JLabel _type;
    protected JComboBox _typeList;
    protected SystemSettings _settings;
    protected SaneTextField _subdirectory;
    protected SaneTextField _v2dFile;

    protected PassEditor _this;
    protected JMenuBar _menuBar;
    protected NodeBrowserScrollPane _scrollPane;
    protected boolean _allObjectsBuilt;
}
