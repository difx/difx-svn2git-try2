/*
 * This is a special case of the BrowserNode that contains a hierarchical list
 * of BrowserNode objects.
 */
package edu.nrao.difx.difxview;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Dimension;

import java.util.Iterator;

/**
 *
 * @author jspitzak
 */
public class NodeBrowserPane extends BrowserNode {
    
    public NodeBrowserPane() {
        super( "" );
    }
    
    public int dataHeight() {
        return _dataHeight;
    }
    
    public void clear() {
        this.clearChildren();
    }
    
    @Override
    public void setBounds( int x, int y, int w, int h ) {
        super.setBounds( x, y, w, h );
        //  Propogate the new width to all children.
        for ( Iterator<BrowserNode> iter = _children.iterator(); iter.hasNext(); ) {
            iter.next().setWidth( w );
        }
        measureDataBounds( h );
    }
    
    public void measureDataBounds( int h ) {
        //  Offset the top of the browser data by an amount determined above this
        //  class.
        int yOffset = _yOffset;
        for ( Iterator<BrowserNode> iter = _children.iterator(); iter.hasNext(); ) {
            //  Don't bother setting the draw conditions for items that fall off the
            //  bottom of the browser window.
            if ( yOffset < h )
                yOffset += iter.next().setDrawConditions( yOffset, true );
            else
                iter.next();
        }
        //  Measure the total height of the data currently displayed in the
        //  browser.
        _dataHeight = yOffset - _yOffset;
        //  Pad the height slightly - this leaves a gap at the bottom of the
        //  browser when it fills with data, which helps a user know when they
        //  are at the end.
        _dataHeight += 5;
    }
    
    @Override
    public void paintComponent( Graphics g ) {
        Dimension d = getSize();
        g.setColor( this.getBackground() );
        g.fillRect( 0, 0, d.width, d.height );
    }
    
    public void yOffset( int newOffset ) {
        _yOffset = newOffset;
    }
    
    int _dataHeight;
    int _yOffset;

}
