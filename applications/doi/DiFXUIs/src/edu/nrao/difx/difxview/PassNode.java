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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *
 * @author jspitzak
 */
public class PassNode extends BrowserNode {
    
    public PassNode( String name ) {
        super( name );
        _name = name;
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
        JMenuItem menuItem1 = new JMenuItem( "Add New Job" );
        menuItem1.setToolTipText( "Add a new Job to this Pass." );
        menuItem1.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                addNewJobAction();
            }
        });
        _popup.add( menuItem1 );
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
        JMenuItem menuItem3 = new JMenuItem( "Copy" );
        menuItem3.setToolTipText( "Make a copy of this Pass, its properties, and all contained Jobs." );
        menuItem3.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                copyAction();
            }
        });
        _popup.add( menuItem3 );
        JMenuItem menuItem2 = new JMenuItem( "Remove" );
        menuItem2.setToolTipText( "Remove this Pass from the database.  The Pass must be empty!" );
        menuItem2.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                removeAction();
            }
        });
        _popup.add( menuItem2 );
    }
    
    /*
     * Adjust the position of items to correspond to the current level.
     */
    @Override
    public void positionItems() {
        super.positionItems();
        _nameEditor.setBounds( _level * _levelOffset, 0, _labelWidth, _ySize );
    }
    
    public void addNewJobAction() {
        System.out.println( "java sucks" );
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
        //  BLAT DATABASE
        System.out.println( "change pass name in database" );
    }

    public void copyAction() {
        System.out.println( "java sucks" );
    }
    
    public void removeAction() {
        System.out.println( "java sucks" );
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
        //  BLAT DATABASE
        System.out.println( "change type to production in database" );
    }
    
    protected void clockItemAction() {
        _productionItem.setSelected( false );
        _clockItem.setSelected( true );
        _testItem.setSelected( false );
        type( "clock" );
        //  BLAT DATABASE
        System.out.println( "change type to clock in database" );
    }
    
    protected void testItemAction() {
        _productionItem.setSelected( false );
        _clockItem.setSelected( false );
        _testItem.setSelected( true );
        type( "test" );
        //  BLAT DATABASE
        System.out.println( "change type to test in database" );
    }
    
    protected String _name;
    protected String _type;
    protected JCheckBoxMenuItem _productionItem;
    protected JCheckBoxMenuItem _clockItem;
    protected JCheckBoxMenuItem _testItem;
    protected JTextField _nameEditor;
    
}
