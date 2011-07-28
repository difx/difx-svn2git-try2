/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.nrao.difx.difxview;

import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

/**
 *
 * @author jspitzak
 */
public class JobNode extends BrowserNode {
    
    public JobNode( String name ) {
        super( name );
    }
    
    @Override
    public void createAdditionalItems() {
        _testButton = new JButton( "Push Me" );
        this.add( _testButton );
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
        _testButton.setBounds( _level * 30 + 150, 0, 100, 20 );
    }
    
    JButton _testButton;
    JPopupMenu _popup;

}
