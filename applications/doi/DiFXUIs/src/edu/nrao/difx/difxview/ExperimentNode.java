/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.nrao.difx.difxview;

import mil.navy.usno.widgetlib.BrowserNode;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JTextField;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *
 * @author jspitzak
 */
public class ExperimentNode extends BrowserNode {
    
    public ExperimentNode( String name ) {
        super( name );
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
        JMenuItem menuItem1 = new JMenuItem( "Add New Pass" );
        menuItem1.setToolTipText( "Add a new Pass to this Experiment." );
        menuItem1.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                addNewPassAction();
            }
        });
        _popup.add( menuItem1 );
        JMenuItem menuItem4 = new JMenuItem( "Rename" );
        menuItem4.setToolTipText( "Rename this Experiment." );
        menuItem4.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                renameAction();
            }
        });
        _popup.add( menuItem4 );
        JMenuItem menuItem3 = new JMenuItem( "Copy" );
        menuItem3.setToolTipText( "Make a copy of this Experiment, its properties, and its Pass/Job structure." );
        menuItem3.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                copyAction();
            }
        });
        _popup.add( menuItem3 );
        JMenuItem menuItem2 = new JMenuItem( "Remove" );
        menuItem2.setToolTipText( "Remove this Experiment from the database.  The Experiment must be empty!" );
        menuItem2.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                removeAction();
            }
        });
        _popup.add( menuItem2 );
    }
    
    /*
     * Adjust the position of items to correspond to the current level.  This
     * method should be overridden by any inheriting classes that add new items.
     */
    public void positionItems() {
        super.positionItems();
        _nameEditor.setBounds( _level * _levelOffset, 0, _labelWidth, _ySize );
    }
    
    public void removeAction() {
        System.out.println( "java sucks" );
    }

    public void addNewPassAction() {
        System.out.println( "java sucks" );
    }

    public void copyAction() {
        System.out.println( "java sucks" );
    }

    /*
     * This function responds to a rename request from the popup menu.  It replaces
     * the "label" text field with an editable field containing the name.
     */
    public void renameAction() {
        _nameEditor.setText( _label.getText() );
        _nameEditor.setVisible( true );
        _label.setVisible( false );
    }
    
    /*
     * This is the callback for the editable name field triggered by a rename
     * request.  It replaces the label containing the name with whatever is in
     * the edited field.  The change must be sent to the database as well!
     */
    public void nameEditorAction() {
        _label.setText( _nameEditor.getText() );
        _label.setVisible( true );
        _nameEditor.setVisible( false );
        //  BLAT DATABASE
        System.out.println( "change experiment name in database" );
    }

    protected JTextField _nameEditor;
    
}
