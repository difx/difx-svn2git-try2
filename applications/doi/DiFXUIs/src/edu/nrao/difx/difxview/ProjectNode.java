/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.nrao.difx.difxview;

import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

/**
 *
 * @author jspitzak
 */
public class ProjectNode extends BrowserNode {
    
    public ProjectNode( String name ) {
        super( name );
    }
    
    @Override
    public void createAdditionalItems() {
        //  Create a popup menu appropriate to a "project".
        _popup = new JPopupMenu();
        JMenuItem menuItem;
        menuItem = new JMenuItem( "Edit" );
        _popup.add( menuItem );
        menuItem = new JMenuItem( "Delete" );
        _popup.add( menuItem );
    }

}
