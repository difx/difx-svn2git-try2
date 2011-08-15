/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.nrao.difx.difxview;

import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 *
 * @author jspitzak
 */
public class JobNode extends BrowserNode {
    
    public JobNode( String name ) {
        super( name );
        this.setHeight( 20 );
    }
    
    @Override
    public void createAdditionalItems() {
        _startButton = new JButton( "Start" );
        this.add( _startButton );
        _editButton = new JButton( "Edit" );
        _editButton.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                editAction( e );
            }
        });
        this.add( _editButton );
        //  Create a popup menu appropriate to a "job".
        _popup = new JPopupMenu();
        JMenuItem menuItem;
        menuItem = new JMenuItem( "Edit" );
        _popup.add( menuItem );
        menuItem = new JMenuItem( "Delete" );
        _popup.add( menuItem );
    }
    
    @Override
    public void positionItems() {
        super.positionItems();
        _startButton.setBounds( _level * 30 + 150, 0, 70, 20 );
        _editButton.setBounds( _level * 30 + 230, 0, 70, 20 );
    }
    
    /*
     * Show the editor window.  If one has not been created yet, create it first.
     */
    public void editAction( ActionEvent e ) {
        if ( _editor == null )
            _editor = new JobEditor();
        _editor.setVisible( true );
    }
    
    JButton _startButton;
    JButton _editButton;
    JPopupMenu _popup;
    JobEditor _editor;

}
