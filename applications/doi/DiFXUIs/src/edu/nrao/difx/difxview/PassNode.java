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
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JTextField;
import javax.swing.JSeparator;
import javax.swing.JOptionPane;
import javax.swing.JLabel;
import javax.swing.JDialog;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JButton;

import java.awt.Frame;

import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.awt.Color;

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
    
    /*
     * This class produces a modal pop-up window for adjusting the properties
     * specific to a pass.  This window is modal.
     */
    static class PassPropertiesWindow extends JDialog {
        
        public PassPropertiesWindow( Frame frame, int x, int y, SystemSettings settings ) {
            super( frame, "", true );
            _settings = settings;
            this.setBounds( x, y, 320, 280 );
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
            JLabel inDataBaseLabel = new JLabel( "In Data Base:" );
            inDataBaseLabel.setBounds( 10, 110, 85, 25 );
            inDataBaseLabel.setHorizontalAlignment( JLabel.RIGHT );
            this.getContentPane().add( inDataBaseLabel );
            JLabel idLabel = new JLabel( "Data Base ID:" );
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
            _cancelButton = new JButton( "Dismiss" );
            _cancelButton.setBounds( 100, 170, 100, 25 );
            _cancelButton.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    ok( false );
                }
            });
            this.getContentPane().add( _cancelButton );
            _okButton = new JButton( "Edit" );
            _okButton.setBounds( 210, 170, 100, 25 );
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

    };

}
