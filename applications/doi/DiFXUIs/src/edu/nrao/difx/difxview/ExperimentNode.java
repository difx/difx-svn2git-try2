/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.nrao.difx.difxview;

import mil.navy.usno.widgetlib.BrowserNode;
import mil.navy.usno.widgetlib.SaneTextField;

import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JTextField;
import javax.swing.JSeparator;
import javax.swing.JOptionPane;

import java.util.Iterator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import edu.nrao.difx.difxdatabase.QueueDBConnection;

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
        //  This field is used to edit the name of the experiment when "rename"
        //  is picked from the popup menu.
        _nameEditor = new JTextField( "" );
        _nameEditor.setVisible( false );
        _nameEditor.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                nameEditorAction();
            }
        });
        this.add( _nameEditor );
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
        JMenuItem menuItem4 = new JMenuItem( "Rename" );
        menuItem4.setToolTipText( "Rename this Experiment." );
        menuItem4.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                renameAction();
            }
        });
        _popup.add( menuItem4 );
        JMenuItem segmentItem = new JMenuItem( "Change Segment" );
        segmentItem.setToolTipText( "Change the segment of this Experiment." );
        segmentItem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                segmentAction();
            }
        });
        _popup.add( segmentItem );
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
        _segmentEditor = new SaneTextField();
        _segmentEditor.setVisible( false );
        _segmentEditor.setFocusLostBehavior( SaneTextField.PERSIST );
        _segmentEditor.textWidthLimit( 2 );
        _segmentEditor.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                segmentEditorAction();
            }
        });
        this.add( _segmentEditor );

        setLabelText();
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
    
    /*
     * Test whether a segment matches.
     */
    public boolean segmentMatch( String testSegment ) {
        if ( segment() == null && testSegment == null )
            return true;
        if ( segment() == null && testSegment != null )
            return false;
        if ( segment() != null && testSegment == null )
            return false;
        if ( segment().contentEquals( testSegment ) )
            return true;
        return false;
    }
    
    /*
     * The segment may be empty.
     */
    public void segment( String newVal ) { 
        _segment = newVal;
        setLabelText();
    }
    public String segment() { return _segment; }
    
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
        _nameEditor.setBounds( _level * _levelOffset, 0, _labelWidth, _ySize );
        _segmentEditor.setBounds( _level * _levelOffset + _labelWidth, 0, 30, _ySize );
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
    
    public void copyAction() {
        //  TODO:  copy needs to be implemented
        System.out.println( "java sucks" );
    }

    /*
     * This function responds to a rename request from the popup menu.  It replaces
     * the "label" text field with an editable field containing the name.
     */
    public void renameAction() {
        _nameEditor.setText( name() );
        _nameEditor.setVisible( true );
        _label.setVisible( false );
    }
    
    /*
     * This is the callback for the editable name field triggered by a rename
     * request.  It replaces the label containing the name with whatever is in
     * the edited field.  The change must be sent to the database as well!
     */
    public void nameEditorAction() {
        name( _nameEditor.getText() );
        setLabelText();
        _label.setVisible( true );
        _nameEditor.setVisible( false );
        updateDatabase( "code", _name );
    }

    /*
     * This function responds to a "change segment" request from the popup menu.
     * It provides and editable field for the segment.
     */
    public void segmentAction() {
        _segmentEditor.setText( segment() );
        _segmentEditor.setVisible( true );
        segment( null );
        setLabelText();
        _label.setVisible( true );
    }
    
    /*
     * This is the callback for the editable segment field.  It replaces the segment
     * with the contents of the editor, then changes the database accordingly.
     */
    public void segmentEditorAction() {
        segment( _segmentEditor.getText() );
        setLabelText();
        _label.setVisible( true );
        _segmentEditor.setVisible( false );
        updateDatabase( "segment", _segment );
    }

    protected void setLabelText() {
        if ( _segment == null )
            _label.setText( _name );
        else if ( _segment.contentEquals( "" ) )
            _label.setText( _name );
        else
            _label.setText( _name + " (" + _segment + ")" );
    }
    
    public void name( String newVal ) {
        _name = newVal;
        setLabelText();
    }
    @Override
    public String name() { return _name; }
    
    /*
     * This is a generic database update function for this object.  It will change
     * a specific field to a specific value - both are strings.
     */
    public void updateDatabase( String param, String setting ) {
        QueueDBConnection db = new QueueDBConnection( _settings );
        if ( db.connected() )
            db.updateExperiment( _id, param, setting );
    }
    
    protected JTextField _nameEditor;
    protected SaneTextField _segmentEditor;
    protected SystemSettings _settings;
    protected String _name;
    protected String _segment;
    
}
