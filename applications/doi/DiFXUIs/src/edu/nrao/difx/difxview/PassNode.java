/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.nrao.difx.difxview;

import mil.navy.usno.widgetlib.BrowserNode;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JTextField;
import javax.swing.JSeparator;
import javax.swing.JOptionPane;

import java.util.Iterator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import edu.nrao.difx.difxdatabase.DBConnection;
import java.sql.ResultSet;

/**
 *
 * @author jspitzak
 */
public class PassNode extends QueueBrowserNode {
    
    public PassNode( String name, SystemSettings settings ) {
        super( name );
        _name = name;
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
        menuItem4.setToolTipText( "Rename this Pass." );
        menuItem4.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                renameAction();
            }
        });
        _popup.add( menuItem4 );
        JMenu typeMenu = new JMenu( "Set Type" );
        _popup.add( typeMenu );
        _productionItem = new JCheckBoxMenuItem( "Production" );
        typeMenu.add( _productionItem );
        _productionItem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                productionItemAction();
            }
        });
        _clockItem = new JCheckBoxMenuItem( "Clock" );
        typeMenu.add( _clockItem );
        _clockItem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                clockItemAction();
            }
        });
        _testItem = new JCheckBoxMenuItem( "Test" );
        typeMenu.add( _testItem );
        _testItem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                testItemAction();
            }
        });
        JMenuItem copyItem = new JMenuItem( "Copy" );
        copyItem.setToolTipText( "Make a copy of this Pass, its properties, and all contained Jobs." );
        copyItem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                copyAction();
            }
        });
        _popup.add( copyItem );
        copyItem.setEnabled( false );
        JMenuItem menuItem2 = new JMenuItem( "Delete Selected Jobs" );
        menuItem2.setToolTipText( "Delete any selected jobs within this Pass." );
        menuItem2.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                deleteSelectedAction();
            }
        });
        _popup.add( menuItem2 );
        JMenuItem deletePassItem = new JMenuItem( "Delete Pass" );
        deletePassItem.setToolTipText( "Delete this Pass from the database.  The Pass should be empty." );
        deletePassItem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                deleteAction();
            }
        });
        _popup.add( deletePassItem );
    }
    
    /*
     * Adjust the position of items to correspond to the current level.
     */
    @Override
    public void positionItems() {
        super.positionItems();
        _nameEditor.setBounds( _level * _levelOffset, 0, _labelWidth, _ySize );
    }
    
    public void selectAllJobsAction() {
        for ( Iterator<BrowserNode> iter = childrenIterator(); iter.hasNext(); ) {
            JobNode thisJob = (JobNode)(iter.next());
            thisJob.selected( true );
        }
    }
    
    public void unselectAllJobsAction() {
        for ( Iterator<BrowserNode> iter = childrenIterator(); iter.hasNext(); ) {
            JobNode thisJob = (JobNode)(iter.next());
            thisJob.selected( false );
        }
    }
    
    /*
     * This function responds to a rename request from the popup menu.  It replaces
     * the "label" text field with an editable field containing the name.
     */
    public void renameAction() {
        _nameEditor.setText( _name );
        _nameEditor.setVisible( true );
        _label.setVisible( false );
    }
    
    /*
     * This is the callback for the editable name field triggered by a rename
     * request.  It replaces the label containing the name with whatever is in
     * the edited field.  The change must be sent to the database as well!
     */
    public void nameEditorAction() {
        _name = _nameEditor.getText();
        setLabelText();
        _label.setVisible( true );
        _nameEditor.setVisible( false );
        updateDatabase( "passName", name() );
    }

    public void copyAction() {
        //  TODO: copy needs to work!
    }
    
    /*
     * Attempt to delete this pass from the database.  A check is made to assure that
     * the pass is empty of all jobs - if not, the user is given the option of deleting
     * all jobs first.
     */
    public void deleteAction() {
        if ( !this._children.isEmpty() ) {
            Object[] options = { "Delete All Jobs", "Cancel" };
            int ans = JOptionPane.showOptionDialog( this,
                    "The Pass still contains jobs which must be deleted first!\n" +
                    "Do you wish to delete them now?",
                    "Pass Contains Jobs",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[1] );
            if ( ans == 1 )
                return;
        }
        deleteThisPass();
    }
    
    /*
     * Delete all of the children of this pass (if there are any) and then delete the
     * pass itself.
     */
    public void deleteThisPass() {
        for ( Iterator<BrowserNode> iter = childrenIterator(); iter.hasNext(); ) {
            JobNode thisJob = (JobNode)(iter.next());
            thisJob.removeFromDatabase();
        }
        clearChildren();
        //  DATABASE TODO: Remove this pass from the database.
        System.out.println( "Remove pass " + _name.toString() + " from the database" );
        //  Remove this pass from its parent experiment.
        ((BrowserNode)(this.getParent())).removeChild( this );
    }
    
    /*
     * Delete any selected jobs from this pass.
     */
    public void deleteSelectedAction() {
        for ( Iterator<BrowserNode> iter = childrenIterator(); iter.hasNext(); ) {
            JobNode thisJob = (JobNode)(iter.next());
            if ( thisJob.selected() ) {
                thisJob.removeFromDatabase();
                removeChild( thisJob );
            }
        }
    }
    
    public void type( String newVal ) {
        _type = newVal;
        setLabelText();
        _productionItem.setSelected( false );
        _clockItem.setSelected( false );
        _testItem.setSelected( false );
        if ( newVal.equalsIgnoreCase( "production" ) )
            _productionItem.setSelected( true );
        if ( newVal.equalsIgnoreCase( "clock" ) )
            _clockItem.setSelected( true );
        if ( newVal.equalsIgnoreCase( "test" ) )
            _testItem.setSelected( true );
    }
    
    protected void setLabelText() {
        _label.setText( _name + " (" + _type + ")" );
    }
    
    public void name( String newVal ) {
        _name = newVal;
        setLabelText();
    }
    @Override
    public String name() { return _name; }
    
    protected void productionItemAction() {
        _productionItem.setSelected( true );
        _clockItem.setSelected( false );
        _testItem.setSelected( false );
        type( "production" );
        changeTypeInDatabase();
    }
    
    protected void clockItemAction() {
        _productionItem.setSelected( false );
        _clockItem.setSelected( true );
        _testItem.setSelected( false );
        type( "clock" );
        changeTypeInDatabase();
    }
    
    protected void testItemAction() {
        _productionItem.setSelected( false );
        _clockItem.setSelected( false );
        _testItem.setSelected( true );
        type( "test" );
        changeTypeInDatabase();
    }
    
    /*
     * Change the pass type in the database.  This is done using a pass type ID, 
     * which we have to look up in the database first.
     */
    public void changeTypeInDatabase() {
        DBConnection dbConnection = new DBConnection( _settings.dbURL(), _settings.jdbcDriver(),
            _settings.dbSID(), _settings.dbPWD() );
        try {
            dbConnection.connectToDB();
            ResultSet passIdInfo = dbConnection.selectData( "select * from " + _settings.dbName() + ".PassType" );
            while ( passIdInfo.next() ) {
                String passIdName = passIdInfo.getString( "type" );                
                Integer passId = passIdInfo.getInt( "id" );
                if ( passIdName.contentEquals( _type ) )
                    updateDatabase( "passTypeID", passId.toString() );
            }
        } catch ( Exception e ) {
            java.util.logging.Logger.getLogger( "global" ).log( java.util.logging.Level.SEVERE, null, e );
        }
    }
    
    /*
     * This is a generic database update function for this object.  It will change
     * a specific field to a specific value - both are strings.
     */
    public void updateDatabase( String param, String setting ) {
        DBConnection dbConnection = new DBConnection( _settings.dbURL(), _settings.jdbcDriver(),
            _settings.dbSID(), _settings.dbPWD() );
        try {
            dbConnection.connectToDB();
            int updateCount = dbConnection.updateData( "update " + _settings.dbName() + 
                    ".Pass set " + param + " = \"" + setting + "\" where id = \"" + _id.toString() + "\"" );
        } catch ( Exception e ) {
            java.util.logging.Logger.getLogger( "global" ).log( java.util.logging.Level.SEVERE, null, e );
        }
    }

    public ExperimentNode experimentNode() {
        return _experimentNode;
    }
    public void experimentNode( ExperimentNode newNode ) {
        _experimentNode = newNode;
    }
    
    protected ExperimentNode _experimentNode;
    protected String _name;
    protected String _type;
    protected JCheckBoxMenuItem _productionItem;
    protected JCheckBoxMenuItem _clockItem;
    protected JCheckBoxMenuItem _testItem;
    protected JTextField _nameEditor;
    protected SystemSettings _settings;
    
}
