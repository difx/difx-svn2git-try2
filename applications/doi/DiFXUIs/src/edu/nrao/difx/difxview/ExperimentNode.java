/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.nrao.difx.difxview;

import mil.navy.usno.widgetlib.BrowserNode;

import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.JOptionPane;

import java.awt.Frame;
import java.awt.Component;
import java.awt.Point;

import java.util.Iterator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import edu.nrao.difx.difxdatabase.QueueDBConnection;

import edu.nrao.difx.difxutilities.DiFXCommand_mv;

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
        JMenuItem menuItem4 = new JMenuItem( "Edit Properties" );
        menuItem4.setToolTipText( "Show/Edit the properties of this Experiment, add a new Pass, and create new Jobs." );
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
        System.out.println( "delete does not currently remove the entire experiement on the DiFX host!" );
        //  BLAT fix the above problem.
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
     * Set the editor window that is used to change items associated with this
     * experiment.
     */
    public void editor( ExperimentEditor newEditor ) {
        _editor = newEditor;
    }
    
    /*
     * Bring up a display/editor for the properties of this experiment.  The
     * editor may or may not already exist - if it does not, create it.
     */
    public void propertiesAction() {
        if ( _editor == null ) {
            Point pt = this.getLocationOnScreen();
            _editor = new ExperimentEditor( pt.x + 25, pt.y + 25, _settings );
            _editor.number( this.number() );
            _editor.name( this.name() );
            _editor.id( this.id() );
            _editor.inDataBase( this.inDataBase() );
            _editor.created( creationDate() );
            _editor.status( this.status() );
            _editor.directory( this.directory() );
            _editor.vexFileName( this.vexFile() );
        }
        _editor.setTitle( "Edit Experiment " + name() );
        _editor.newExperimentMode( false );
        _editor.visible();
//        //  See if the user made any edition changes.  If so, apply them to the
//        //  database (if requested) and locally.
//        if ( _editor.ok() && _editor.editMode() ) {
//            if ( _editor.inDataBase() ) {
//                QueueDBConnection db = new QueueDBConnection( _settings );
//                if ( db.connected() ) {
//                    //  If the item didn't already exist in the database, create it.
//                    if ( !this.inDataBase() ) {
//                        db.newExperiment( _editor.name(), _editor.number(), _settings.experimentStatusID( _editor.status() ),
//                                _editor.directory(), _editor.vexFileName() );
//                        //  See which ID the data base assigned...it will be the largest one.  Also
//                        //  save the creation date.
//                        int newExperimentId = 0;
//                        String creationDate = this.creationDate();
//                        ResultSet dbExperimentList = db.experimentList();
//                        try {
//                            while ( dbExperimentList.next() ) {
//                                int newId = dbExperimentList.getInt( "id" );
//                                if ( newId > newExperimentId ) {
//                                    newExperimentId = newId;
//                                    creationDate = dbExperimentList.getString( "dateCreated" );
//                                }
//                            }
//                        } catch ( Exception e ) {
//                                java.util.logging.Logger.getLogger( "global" ).log( java.util.logging.Level.SEVERE, null, e );
//                        }
//                        this.id( newExperimentId );
//                        this.creationDate( creationDate );
//                    }
//                    else {
//                        db.updateExperiment( this.id(), "code", _editor.name() );
//                        db.updateExperiment( this.id(), "number", win.number().toString() );
//                        db.updateExperiment( this.id(), "statusID", _settings.experimentStatusID(win.status() ).toString() );
//                    }
//                }
//                else {
//                    JOptionPane.showMessageDialog( this, "Unable to edit this item to the database\n"
//                            + "as it cannot be contacted or your \"Use Database\" setting is off.",
//                            "Database Warning", JOptionPane.WARNING_MESSAGE );
//                    win.inDataBase( false );
//                }
//            }
//            this.name( win.name() );
//            this.number( win.number() );
//            this.status( win.status() );
//            this.inDataBase( win.inDataBase() );
//            this.directory( win.directory() );
//            this.vexFile( win.vexFileName() );
//            //  Change the name of the directory on the DiFX host.
//            //  BLAT there might be other paths we need to change??  In the jobs entries in
//            //  database for instance?
//            if ( !this.directory().contentEquals( win.directory() ) ) {
//                DiFXCommand_mv mv = new DiFXCommand_mv( this.directory(), win.directory(), _settings );
//                mv.send();
//                this.directory( win.directory() );
//            }
//            //  Write the .vex file there.
//            win.writeVexFile();
//        }
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
    
    public void vexFile( String newVal ) { _vexFile = newVal; }
    public String vexFile() { return _vexFile; }
    
    protected SystemSettings _settings;
    protected String _name;
    protected String _creationDate;
    protected String _status;
    protected Integer _number;
    protected String _directory;
    protected String _vexFile;
    
    protected ExperimentEditor _editor;
    
}
