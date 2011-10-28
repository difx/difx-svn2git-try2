/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.nrao.difx.difxview;

import mil.navy.usno.widgetlib.BrowserNode;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

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
        //  Create a popup menu appropriate to a "project".
        _popup = new JPopupMenu();
        JMenuItem menuItem1 = new JMenuItem( "Add Pass" );
        _popup.add( menuItem1 );
        JMenuItem menuItem2 = new JMenuItem( "Delete Pass" );
        menuItem2.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                javaSucks();
            }
        });
        _popup.add( menuItem2 );
    }
    
    public void javaSucks() {
        System.out.println( "java sucks" );
    }

}
