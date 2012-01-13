/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.nrao.difx.difxview;

import mil.navy.usno.widgetlib.BrowserNode;
import mil.navy.usno.widgetlib.SaneTextField;
import mil.navy.usno.widgetlib.NumberBox;

import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JTextField;
import javax.swing.JSeparator;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.Timer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import java.awt.Frame;
import java.awt.Component;
import java.awt.Point;
import java.awt.Color;

import java.util.Iterator;
import java.util.Map;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import edu.nrao.difx.difxdatabase.QueueDBConnection;

import java.sql.ResultSet;

/**
 *
 * @author jspitzak
 */
public class ExperimentNode extends QueueBrowserNode {
    
    public ExperimentNode( String name, SystemSettings settings ) {
        super( name );
        name( name );
        _settings = settings;
    }
    
    @Override
    public void createAdditionalItems() {
        //  Create a popup menu appropriate to a "project".
        _popup = new JPopupMenu();
        JMenuItem selectJobsItem = new JMenuItem( "Select All Jobs" );
        selectJobsItem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                selectAllJobsAction();
            }
        });
        _popup.add( selectJobsItem );
        JMenuItem unselectJobsItem = new JMenuItem( "Unselect All Jobs" );
        unselectJobsItem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                unselectAllJobsAction();
            }
        });
        _popup.add( unselectJobsItem );
        _popup.add( new JSeparator() );
        JMenuItem menuItem4 = new JMenuItem( "Properties" );
        menuItem4.setToolTipText( "Show/Edit the properties of this Experiment." );
        menuItem4.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                propertiesAction();
            }
        });
        _popup.add( menuItem4 );
        JMenuItem copyItem = new JMenuItem( "Copy" );
        copyItem.setToolTipText( "Make a copy of this Experiment, its properties, and its Pass/Job structure." );
        copyItem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                copyAction();
            }
        });
        _popup.add( copyItem );
        copyItem.setEnabled( false );
        JMenuItem deleteSelectedItem = new JMenuItem( "Delete Selected Jobs" );
        deleteSelectedItem.setToolTipText( "Delete any selected jobs within this Experiment." );
        deleteSelectedItem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                deleteSelectedAction();
            }
        });
        _popup.add( deleteSelectedItem );
        JMenuItem deleteExperimentItem = new JMenuItem( "Delete Experiment" );
        deleteExperimentItem.setToolTipText( "Remove this Experiment from the database.  The Experiment must be empty!" );
        deleteExperimentItem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                deleteAction();
            }
        });
        _popup.add( deleteExperimentItem );
        _popup.add( new JSeparator() );
        JMenuItem addPassItem = new JMenuItem( "Add New Pass" );
        addPassItem.setToolTipText( "Add a new Pass to this Experiment." );
        addPassItem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                addPassAction();
            }
        });
        _popup.add( addPassItem );

        _label.setText( _name );
    }
    
    /*
     * Test whether this node's ID and a given ID are equal.  This is messy due to
     * the possibility things can be null.
     */
    public boolean idMatch( Integer testId ) {
        if ( _id == null && testId == null )
            return true;
        if ( _id == null && testId != null )
            return false;
        if ( _id != null && testId == null )
            return false;
        if ( _id.intValue() != testId.intValue() )
            return false;
        return true;
    }
    
    /*
     * Test whether a name matches.
     */
    public boolean nameMatch( String testName ) {
        if ( name() == null && testName == null )
            return true;
        if ( name() == null && testName != null )
            return false;
        if ( name() != null && testName == null )
            return false;
        if ( name().contentEquals( testName ) )
            return true;
        return false;
    }
    
    public void selectAllJobsAction() {
        for ( Iterator<BrowserNode> iter = childrenIterator(); iter.hasNext(); ) {
            PassNode thisPass = (PassNode)(iter.next());
            thisPass.selectAllJobsAction();
        }
    }
    
    public void unselectAllJobsAction() {
        for ( Iterator<BrowserNode> iter = childrenIterator(); iter.hasNext(); ) {
            PassNode thisPass = (PassNode)(iter.next());
            thisPass.unselectAllJobsAction();
        }
    }
    
    /*
     * Adjust the position of items to correspond to the current level.  This
     * method should be overridden by any inheriting classes that add new items.
     */
    @Override
    public void positionItems() {
        super.positionItems();
    }
    
    /*
     * Delete this experiment and its contents.  If the experiment is not empty,
     * prompt the user to see if this is really what they want to do.
     */
    public void deleteAction() {
        if ( !this._children.isEmpty() ) {
            Object[] options = { "Delete All Passes", "Cancel" };
            int ans = JOptionPane.showOptionDialog( this,
                    "The Experiment still contains passes which must be deleted first!\n" +
                    "Do you wish to delete them now?",
                    "Experiment Contains Passes",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[1] );
            if ( ans == 1 )
                return;
        }
        deleteThisExperiment();        
    }

    /*
     * Delete this experiment and its entire contents.
     */
    public void deleteThisExperiment() {
        for ( Iterator<BrowserNode> iter = childrenIterator(); iter.hasNext(); ) {
            PassNode thisPass = (PassNode)(iter.next());
            thisPass.deleteThisPass();
        }
        clearChildren();
        //  Remove this experiment from the database.
        QueueDBConnection db = new QueueDBConnection( _settings );
        if ( db.connected() )
            db.deleteExperiment( _id );
        //  Remove this experiment from its parent - the queue browser.
        ((BrowserNode)(this.getParent())).removeChild( this );
    }
    
    /*
     * Delete selected jobs from this experiment.
     * QUESTION: should we delete a pass if it is emptied by this action?
     */
    public void deleteSelectedAction() {
        for ( Iterator<BrowserNode> iter = childrenIterator(); iter.hasNext(); ) {
            PassNode thisPass = (PassNode)(iter.next());
            thisPass.deleteSelectedAction();
        }
    }
    
    /*
     * Add a new pass to this experiment.  This brings up the "pass" display in
     * edit mode, pre-set to this experiment.  Being in edit mode, the user could
     * change the experiment.  Not a problem.
     */
    public void addPassAction() {
        //  The pass display is launched from the queue browser...which is this
        //  experiment's parent, presumably.
        ((QueueBrowserPanel)(this.getParent())).newPass( this );
    }
    
    /*
     * Bring up a display/editor for the properties of this experiment.  This is
     * a modal display.  Any changes are applied after the window is closed.
     */
    public void propertiesAction() {
        Component comp = this.getParent();
        while ( comp.getParent() != null )
            comp = comp.getParent();
        Point pt = this.getLocationOnScreen();
        ExperimentPropertiesWindow win =
                new ExperimentPropertiesWindow( (Frame)comp, pt.x + 25, pt.y + 25, _settings );
        win.setTitle( "Experiment Properties" );
        win.editMode( false );
        win.number( this.number() );
        win.name( this.name() );
        win.id( this.id() );
        win.inDataBase( this.inDataBase() );
        win.created( creationDate() );
        win.status( this.status() );
        win.directory( this.directory() );
        win.visible();
        //  See if the user made any edition changes.  If so, apply them to the
        //  database (if requested) and locally.
        if ( win.ok() && win.editMode() ) {
            if ( win.inDataBase() ) {
                QueueDBConnection db = new QueueDBConnection( _settings );
                if ( db.connected() ) {
                    //  If the item didn't already exist in the database, create it.
                    if ( !this.inDataBase() ) {
                        db.newExperiment( win.name(), win.number(), _settings.experimentStatusID( win.status() ) );
                        //  See which ID the data base assigned...it will be the largest one.  Also
                        //  save the creation date.
                        int newExperimentId = 0;
                        String creationDate = this.creationDate();
                        ResultSet dbExperimentList = db.experimentList();
                        try {
                            while ( dbExperimentList.next() ) {
                                int newId = dbExperimentList.getInt( "id" );
                                if ( newId > newExperimentId ) {
                                    newExperimentId = newId;
                                    creationDate = dbExperimentList.getString( "dateCreated" );
                                }
                            }
                        } catch ( Exception e ) {
                                java.util.logging.Logger.getLogger( "global" ).log( java.util.logging.Level.SEVERE, null, e );
                        }
                        this.id( newExperimentId );
                        this.creationDate( creationDate );
                    }
                    else {
                        db.updateExperiment( this.id(), "code", win.name() );
                        db.updateExperiment( this.id(), "number", win.number().toString() );
                        db.updateExperiment( this.id(), "statusID", _settings.experimentStatusID(win.status() ).toString() );
                    }
                }
                else {
                    JOptionPane.showMessageDialog( this, "Unable to edit this item to the database\n"
                            + "as it cannot be contacted or your \"Use Database\" setting is off.",
                            "Database Warning", JOptionPane.WARNING_MESSAGE );
                    win.inDataBase( false );
                }
            }
            this.name( win.name() );
            this.number( win.number() );
            this.status( win.status() );
            this.inDataBase( win.inDataBase() );
            this.directory( win.directory() );
        }
    }
    
    public void copyAction() {
        //  TODO:  copy needs to be implemented
        System.out.println( "java sucks" );
    }

    public void name( String newVal ) {
        _name = newVal;
        _label.setText( _name );
    }
    @Override
    public String name() { return _name; }
    
    public void creationDate( String newVal ) { _creationDate = newVal; }
    public String creationDate() { return _creationDate; }
    
    public void status( String newVal ) { _status = newVal; }
    public String status() { return _status; }
    
    public void number( Integer newVal ) { _number = newVal; }
    public Integer number() { return _number; }
    
    public void directory( String newVal ) { _directory = newVal; }
    public String directory() { return _directory; }
    
    protected SystemSettings _settings;
    protected String _name;
    protected String _creationDate;
    protected String _status;
    protected Integer _number;
    protected String _directory;
    
    /*
     * This class produces a modal pop-up window for adjusting the properties
     * specific to an experiment.  This window is modal.
     */
    static class ExperimentPropertiesWindow extends JDialog {
        
        public ExperimentPropertiesWindow( Frame frame, int x, int y, SystemSettings settings ) {
            super( frame, "", true );
            _settings = settings;
            this.setBounds( x, y, 420, 280 );
            this.setResizable( false );
            this.getContentPane().setLayout( null );
            _inDataBase = new JCheckBox( "" );
            _inDataBase.setBounds( 100, 110, 25, 25 );
            _inDataBase.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    inDataBaseAction();
                }
            });
            this.getContentPane().add( _inDataBase );
            JLabel inDataBaseLabel = new JLabel( "In Database:" );
            inDataBaseLabel.setBounds( 10, 110, 85, 25 );
            inDataBaseLabel.setHorizontalAlignment( JLabel.RIGHT );
            this.getContentPane().add( inDataBaseLabel );
            JLabel idLabel = new JLabel( "Database ID:" );
            idLabel.setBounds( 140, 110, 85, 25 );
            idLabel.setHorizontalAlignment( JLabel.RIGHT );
            this.getContentPane().add( idLabel );
            _id = new JLabel( "" );
            _id.setBounds( 230, 110, 70, 25 );
            this.getContentPane().add( _id );
            _name = new SaneTextField();
            _name.setBounds( 100, 20, 210, 25 );
            _name.textWidthLimit( 20 );
            _name.setToolTipText( "Name assigned to the experiment (up to 20 characters)." );
            _name.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    nameChangeAction();
                }
            });
            this.getContentPane().add( _name );
            _name.setVisible( false );
            _nameAsLabel = new JLabel( "" );
            _nameAsLabel.setBounds( 100, 20, 210, 25 );
            _nameAsLabel.setToolTipText( "Name assigned to the experiment." );
            this.getContentPane().add( _nameAsLabel );
            JLabel nameLabel = new JLabel( "Name:" );
            nameLabel.setBounds( 10, 20, 85, 25 );
            nameLabel.setHorizontalAlignment( JLabel.RIGHT );
            this.getContentPane().add( nameLabel );
            _number = new NumberBox();
            _number.setBounds( 100, 50, 80, 25 );
            _number.limits( 0.0, 9999.0 );
            _number.setToolTipText( "Number (up to four digits) used to associate experiments with the same name." );
            this.getContentPane().add( _number );
            _number.setVisible( false );
            _numberAsLabel = new JLabel( "" );
            _numberAsLabel.setBounds( 100, 50, 80, 25 );
            _numberAsLabel.setToolTipText( "Number used to associate experiments with the same name." );
            this.getContentPane().add( _numberAsLabel );
            _numberAsLabel.setVisible( true );
            JLabel numberLabel = new JLabel( "Number:" );
            numberLabel.setBounds( 10, 50, 85, 25 );
            numberLabel.setHorizontalAlignment( JLabel.RIGHT );
            this.getContentPane().add( numberLabel );
            _status = new JLabel( "unknown" );
            _status.setBounds( 100, 80, 210, 25 );
            _status.setToolTipText( "Current status of this experiment." );
            this.getContentPane().add( _status );
            _status.setVisible( true );
            JLabel statusLabel = new JLabel( "Status:" );
            statusLabel.setBounds( 10, 80, 85, 25 );
            statusLabel.setHorizontalAlignment( JLabel.RIGHT );
            this.getContentPane().add( statusLabel );
            _statusList = new JComboBox();
            _statusList.setBounds( 100, 80, 210, 25 );
            _statusList.setToolTipText( "List of possible status settings for this experiment." );
            _statusList.setBackground( Color.WHITE );
            _statusList.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    _status.setText( (String)_statusList.getSelectedItem() );
                }
            });
            this.getContentPane().add( _statusList );
            _statusList.setVisible( false );
            _created = new JLabel( "" );
            _created.setBounds( 100, 140, 210, 25 );
            _created.setToolTipText( "Date this experiment was created (assigned by database if available)." );
            this.getContentPane().add( _created );
            JLabel createdLabel = new JLabel( "Created:" );
            createdLabel.setBounds( 10, 140, 85, 25 );
            createdLabel.setHorizontalAlignment( JLabel.RIGHT );
            this.getContentPane().add( createdLabel );
            _directory = new SaneTextField();
            _directory.setBounds( 100, 170, 310, 25 );
            _directory.setToolTipText( "\"Working\" directory containing all files for this experiment." );
            _directory.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    directoryChangeAction();
                }
            });
            this.getContentPane().add( _directory );
            _directory.setVisible( false );
            _directoryAsLabel = new JLabel( "" );
            _directoryAsLabel.setBounds( 100, 170, 310, 25 );
            _directoryAsLabel.setToolTipText( "\"Working\" directory containing all files for this experiment." );
            this.getContentPane().add( _directoryAsLabel );
            JLabel directoryLabel = new JLabel( "Directory:" );
            directoryLabel.setBounds( 10, 170, 85, 25 );
            directoryLabel.setHorizontalAlignment( JLabel.RIGHT );
            this.getContentPane().add( directoryLabel );
            _cancelButton = new JButton( "Dismiss" );
            _cancelButton.setBounds( 100, 200, 100, 25 );
            _cancelButton.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    ok( false );
                }
            });
            this.getContentPane().add( _cancelButton );
            _okButton = new JButton( "Edit" );
            _okButton.setBounds( 210, 200, 100, 25 );
            _okButton.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    ok( true );
                }
            });
            this.getContentPane().add( _okButton );
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
        /*
         * A change has occurred in the name, which *maybe* should be propogated
         * to the directory, but only if the directory hasn't been previously
         * set (which probably indicates the experiment is new, but you can control
         * it using the "keepDirectory" function).
         */
        protected void nameChangeAction() {
            System.out.println( "yo!" );
            if ( !_keepDirectory ) {
                directory( _settings.workingDirectory() + "/" + _name.getText() );
                keepDirectory( false );
            }
        }
        protected void directoryChangeAction() {
            keepDirectory( true );
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

    };
    
}
