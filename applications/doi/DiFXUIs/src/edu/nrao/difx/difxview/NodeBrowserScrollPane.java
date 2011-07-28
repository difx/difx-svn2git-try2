/*
 * This panel contains a NodeBrowserPane object.  It measures the size of the
 * NodeBrowserPane and adds a scrollbar if necessary.  The reason it doesn't simply
 * inherit the NodeBrowserPane is simple, if silly - the NodeBrowserPane does a lot of
 * drawing that might fall out of bounds and obscure things like the frame and
 * scrollbars.  By putting it in a sub-panel, anything drawn in it is effectively
 * clipped.
 */
package edu.nrao.difx.difxview;

import javax.swing.JPanel;
import javax.swing.Action;
import javax.swing.AbstractAction;

import java.awt.event.ActionEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.AdjustmentEvent;

import javax.swing.Timer;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Color;

import javax.swing.JScrollBar;

/**
 *
 * @author jspitzak
 */
public class NodeBrowserScrollPane extends JPanel implements MouseMotionListener, 
        MouseWheelListener, AdjustmentListener {
    
    public NodeBrowserScrollPane() {
        this.setLayout( null );
        browserPane = new NodeBrowserPane();
        this.add( browserPane );
        _scrollBar = new JScrollBar( JScrollBar.VERTICAL );
        this.add( _scrollBar );
        _scrollBar.addAdjustmentListener( this );
        _scrollBar.setUnitIncrement( 10 );
        
        //  Capture mouse motion and wheel events.  We don't really care about
        //  clicks.
        addMouseMotionListener( this );
        addMouseWheelListener( this );
        
        //  These are counters used for "momentum" in scrolling.
        _offsetMotion = 0;
        
        //  This turns momentum on or off.
        momentumOn( true );
        
        //  Set ourselves up to respond to a repeating timeout roughly 50 times
        //  a second.  This is used for animation of the browser content.  The
        //  time interval is set to match the timing of drag and mouse wheel
        //  events.
        Action updateDrawingAction = new AbstractAction() {
            @Override
            public void actionPerformed( ActionEvent e ) {
                timeoutIntervalEvent();
            }
        };
        new Timer( 20, updateDrawingAction ).start();
        
        //  The yOffset tracks where the browser data are located vertically.
        //  It is measured in pixels.
        _yOffset = 0;
    
    }
    
    /*
     * Adjust the browser to a change in the list.  This needs to be done when
     * the list is edited in any way.
     */
    public void listChange() {
        boolean scrolledToEnd = scrolledToEnd();
        browserPane.measureDataBounds();
        Dimension d = getSize();
        _scrollBar.setValues( -_yOffset, d.height, 0, browserPane.dataHeight() );
        if ( -_yOffset > browserPane.dataHeight() - d.height ) {
            _yOffset = - ( browserPane.dataHeight() - d.height );
            _scrollBar.setValues( -_yOffset, d.height, 0, browserPane.dataHeight() );
        }
        testScrollBar( d.height );
        if ( scrolledToEnd )
            scrollToEnd();
        _scrollBar.updateUI();
        browserPane.yOffset( _yOffset );
        browserPane.updateUI();
    }
    
    public void setYOffset( int newOffset ) {
        Dimension d = getSize();
        int offset;
        if ( newOffset > browserPane.dataHeight() - d.height )
            offset = -( browserPane.dataHeight() - d.height );
        else
            offset = -newOffset;
        _scrollBar.setValues( offset, d.height, 0, browserPane.dataHeight() );
        browserPane.yOffset( -offset );
        browserPane.updateUI();
    }
    
    public int getYOffset() {
        return _yOffset;
    }
    
    public void clear() {
        browserPane.clear();
        browserPane.measureDataBounds();
        Dimension d = getSize();
        _scrollBar.setValues( -_yOffset, d.height, 0, browserPane.dataHeight() ); 
        testScrollBar( d.height );
    }
    
    /*
     * Used to add a "top level" node to the browser panel.
     */
    public void addNode( BrowserNode newNode ) {
        browserPane.addChild( newNode );
        browserPane.measureDataBounds();
        Dimension d = getSize();
        _scrollBar.setValues( -_yOffset, d.height, 0, browserPane.dataHeight() ); 
        testScrollBar( d.height );
    }
    
    public boolean scrolledToEnd() {
        if ( _scrollable )
            return ( _scrollBar.getValue() == _scrollBar.getMaximum() - _scrollBar.getVisibleAmount() );
        else
            return true;
    }
    
    public void scrollToEnd() {
        if ( _scrollable ) {
            _scrollBar.setValue( _scrollBar.getMaximum() ); 
        }
    }
    
    public void scrollToTop() {
        if ( _scrollable ) {
            _scrollBar.setValue( 0 );
        }
    }
    
    @Override
    public void setBackground( Color newColor ) {
        if ( browserPane != null )
            browserPane.setBackground( newColor );
        super.setBackground( newColor );
    }
    
    @Override
    public void setBounds( int x, int y, int w, int h ) {
        super.setBounds( x, y, w, h ); 
        browserPane.setBounds( 1, 1, w - 2, h - 2 );
        testScrollBar( h );
        if ( _scrollable ) {
            //  We need to resize the browserPane now to avoid the scrollbar.
            browserPane.setBounds( 1, 1, w - SCROLLBAR_WIDTH - 2, h - 2 );
            _scrollBar.setBounds( w - SCROLLBAR_WIDTH - 1, 1, SCROLLBAR_WIDTH, h - 2);
            _scrollBar.setVisible( true );
        }
        else {
            _yOffset = 0;
            browserPane.yOffset( _yOffset );
            this.updateUI();
            _scrollBar.setVisible( false );
        }
    }
    
    public void testScrollBar( int h ) {
        //  See if there are enough data in the browser window to require a
        //  scrollbar.
        _minYOffset = h - browserPane.dataHeight();
        if ( _minYOffset >= 0 ) {
            _scrollable = false;
        }
        else {
            _scrollable = true;
        }
    }
    
    @Override
    public void paintComponent( Graphics g ) {
        Dimension d = getSize();
        //  Draw the scrollbar.
        if ( _scrollable ) {
            g.setColor( Color.LIGHT_GRAY );
            g.fillRect( d.width - SCROLLBAR_WIDTH, 0, SCROLLBAR_WIDTH, d.height );
            g.setColor( Color.BLACK );
            g.drawRect( d.width - SCROLLBAR_WIDTH, 0, SCROLLBAR_WIDTH, d.height );
            _scrollBar.setValues( -_yOffset, d.height, 0, browserPane.dataHeight() ); 
        }
        g.setColor( Color.BLACK );
        g.drawRect( 0, 0, d.width  - 1, d.height - 1 );
    }
    
    /*
     * The timeout interval is used to animate the browser, giving scroll operations
     * "momentum".  The tricky bit is getting the momentum to decay with time in
     * a way that is visually pleasing.  Using the actual scrollbar does none of
     * this - it behaves conventionally (no momentum).
     */
    protected void timeoutIntervalEvent() {
        if ( _scrollBar.getValue() > _scrollBar.getMaximum() - _scrollBar.getVisibleAmount() )
            scrollToEnd();
        if ( _offsetMotion != 0 && !_scrolling ) {
            //  Make sure we haven't scrolled too far in either direction.  The
            //  _yOffset measures how far from the top we start drawing browser
            //  items, so it should always be zero or negative.
            if ( _yOffset > 0 ) {
                _yOffset = 0;
                _offsetMotion = 0;
            }
            //  The _yOffset must also not be so large that we are displaying
            //  useless white space at the bottom of the browser.
            else if ( _yOffset < _minYOffset ) {
                _yOffset = _minYOffset;
                _offsetMotion = 0;
            }
            //  Otherwise, adjust the _yOffset for "momentum" motion, and decay 
            //  the amount we adjust.
            else if ( _momentumOn ) {
                _yOffset += _offsetMotion;
                --_decayCount;
                if ( _decayCount < 1 ) {
                    if ( _offsetMotion > 0 ) {
                        _decayCount = _decayStartCount - _offsetMotion;
                        --_offsetMotion;
                    }
                    else if ( _offsetMotion < 0 ) {
                        _decayCount = _decayStartCount + _offsetMotion;
                        ++_offsetMotion;
                    }
                }
            }
            else {
                _yOffset += _offsetMotion;
                _offsetMotion = 0;
            }
            browserPane.yOffset( _yOffset );
            this.updateUI();
        }
        //  This is needed incase closing a browser node makes the data smaller
        //  than the screen.
        if ( _yOffset == 0 )
            this.updateUI();
    }
    
    @Override
    public void mouseMoved( MouseEvent e ) {
        _lastY = e.getY();
        _lastX = e.getX();
        _scrolling = false;
    }
    
    @Override
    public void mouseDragged( MouseEvent e ) {
        if ( _scrollable ) {
            _offsetMotion = e.getY() - _lastY;
            _decayCount = 10;
            _decayStartCount = 10;
            _lastY = e.getY();
        }
    }
    
    @Override
    public void mouseWheelMoved( MouseWheelEvent e ) {
        if ( _scrollable ) {
            _offsetMotion = 2 * e.getScrollAmount() * e.getWheelRotation();
            _decayCount = 10;
            _decayStartCount = 10;
        }
    }
    
    @Override
    public void adjustmentValueChanged( AdjustmentEvent e ) {
        //  If the value has changed, this is an actual scroll event requiring
        //  a redraw and momentum operations.
        if ( _yOffset != -e.getValue() ) {
            //  Momentum just doesn't look very good...so I'm making the
            //  adjustment directly.
            _yOffset = -e.getValue();
            browserPane.yOffset( _yOffset );
            this.updateUI();
            //  This stuff could be used instead if momentum was desired.
            //_offsetMotion = -e.getValue() - _yOffset;
            //_decayCount = 10;
            //_decayStartCount = 10;
        }
    }
    
    void momentumOn( boolean newVal ) {
        _momentumOn = newVal;
    }
    
    /*
     * Give access to the browser pane, which is actually the top level node to
     * the tree of nodes that are displayed.
     */
    public BrowserNode browserTopNode() {
        return browserPane;
    }
    
    protected NodeBrowserPane browserPane;
    protected JScrollBar _scrollBar;

    protected int _yOffset;
    protected int _offsetMotion;
    protected int _decayCount;
    protected int _decayStartCount;
    protected int _minYOffset;
    protected boolean _scrollable;
    protected int _lastY;
    protected int _lastX;
    protected boolean _scrolling;
    protected boolean _momentumOn;
    
    static protected int SCROLLBAR_WIDTH = 16;
    
}
