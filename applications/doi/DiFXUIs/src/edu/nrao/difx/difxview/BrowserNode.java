/*
 * This is a generic browser node, developed for the queue browser of the DiFX
 * GUI.  This class contains some limited drawing capability - it will draw
 * itself based on where it lies in the hierarchy (and whether it is "open").
 */
package edu.nrao.difx.difxview;

import java.awt.Color;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Container;
import java.awt.Insets;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 *
 * @author jspitzak
 */
public class BrowserNode extends JPanel implements MouseListener, MouseMotionListener {
    
    public BrowserNode( String name ) {
        _open = true;
        _inBounds = true;
        _showThis = true;
        _ySize = 20;
        _xSize = 500;
        _children = new ArrayDeque<BrowserNode>();
        setBounds( 0, 0, _xSize, _ySize );
        setLayout( null );
        addMouseListener( this );
        addMouseMotionListener( this );
        _mouseIn = false;
        _label = new JLabel( name );
        _label.setFont( new Font( "Dialog", Font.BOLD, 12 ) );
        this.add( _label );
        _popupButton = new JButton( "\u25be" );  //  this is a little down arrow
        _popupButton.setMargin( new Insets( 0, 0, 2, 0 ) );
        _popupButton.setVisible( false );
        _popupButton.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                if ( _popup != null )
                    _popup.show( _popupButton, 0, 0 );
            }
        });
        this.add( _popupButton );
        createAdditionalItems();
        setLevel( 0 );
        _highlightColor = Color.LIGHT_GRAY;
        _backgroundColor = Color.WHITE;
    }
    
    /*
     * This method is meant to be inherited - it adds items to the node (buttons
     * and popup menus, etc).
     */
    public void createAdditionalItems() {
    }
    
    /*
     * Figure out the y position where this object will be drawn, as well
     * as whether it is drawn at all.  This information is also figured out for
     * any child objects.  The y position depends on the size of the parent of
     * this object, as well as any preceding siblings.
     */
    public int setDrawConditions( int yOffset, boolean open ) {
        int height = 0;
        int width = 0;
        //  Add the height of this object if its parent object is "open" (which
        //  indicates it should be drawn).
        if ( open ) {
            Dimension d = getSize();
            height += _ySize;
            width = d.width;
            if ( _showThis )
                this.setVisible( true );
            else
                this.setVisible( false );
        }
        else {
            this.setVisible( false );
        }
        //  Get the heights of all children, which are added to the height of this
        //  object.  These heights will be zero if this object isn't open, but
        //  we need to do this anyway to make the children not visible in that
        //  instance.
        for ( Iterator<BrowserNode> iter = _children.iterator(); iter.hasNext(); ) {
            height += iter.next().setDrawConditions( height, _open && open );
        }
        //  Set the bounds of this object such that it contains all of its
        //  children.  We only bother doing this if the parent object is open, 
        //  thus making this object visible.
        if ( open ) {
            this.setBounds( 0, yOffset, width, height );
        }
        if ( !_showThis )
            height = 0;
        if ( yOffset + height < 0 )
            inBounds( false );
        else
            inBounds( true );
        return height;
    }
    
    /*
     * Set the "level" of this object.  This determines whether it has a button
     * for opening and closing children, and where it is.  It also positions
     * other items in the node.
     */
    public void setLevel( int newLevel ) {
        _level = newLevel;
        positionItems();
    }
    
    /*
     * Adjust the position of items to correspond to the current level.  This
     * method should be overridden by any inheriting classes that add new items.
     */
    public void positionItems() {
        _label.setBounds( _level * 30, 0, 150, _ySize );
    }
    
    /*
     * Set the height of this object.
     */
    public void setHeight( int newVal ) {
        _ySize = newVal;
        Dimension d = this.getSize();
        this.setSize( d.width, _ySize );
        positionItems();
    }
    
    /*
     * Set the width of the object.
     */
    public void setWidth( int newVal ) {
        _xSize = newVal;
        Dimension d = this.getSize();
        this.setSize( _xSize, d.height );
        positionItems();
        //  Do we really want to do this?  This makes all child nodes the same
        //  width....which seems like the right thing to do for my specific purposes
        //  right now, but maybe isn't really.
        for ( Iterator<BrowserNode> iter = _children.iterator(); iter.hasNext(); ) {
            iter.next().setWidth( _xSize );
        }
    }
    
    /*
     * Add a new child object to this object.  This is both "added" to the panel
     * and added to the list of children.
     */
    public void addChild( BrowserNode newChild ) {
        newChild.setLevel( _level + 1 );
        _children.add( newChild );
        this.add( newChild );
    }
    
    /*
     * Remove a child.
     */
    public void removeChild( BrowserNode thisChild ) {
        _children.remove( thisChild );
        this.remove( thisChild );
    }
    
    /*
     * Clear all children.
     */
    public void clearChildren() {
        if ( _children.size() > 0 ) {
            for ( Iterator<BrowserNode> iter = _children.iterator(); iter.hasNext(); ) {
                BrowserNode shoot = iter.next();
                shoot.clearChildren();
                removeChild( shoot );
            }
        }
    }
    
    public void backgroundColor( Color newColor ) {
        _backgroundColor = newColor;
    }
    
    public ArrayDeque<BrowserNode> children() {
        return _children;
    }
    
    public Iterator<BrowserNode> childrenIterator() {
        return _children.iterator();
    }

    @Override
    public void paintComponent( Graphics g ) {
        if ( !_inBounds )
            return;
        super.paintComponent( g );
        Dimension d = this.getSize();
        if ( _mouseIn )
            g.setColor( _highlightColor );
        else
            g.setColor( _backgroundColor );
        g.fillRect( 0, 0, d.width, d.height );
        //  Draw the open/close arrow if there are child nodes.
        if ( _children.size() > 0 ) {
            int xpts[] = new int[3];
            int ypts[] = new int[3];
            int levelOffset = ( _level - 1 ) * 30;
            if (_open) {
                //  "Open" objects have an arrow that points down.
                xpts[0] = levelOffset + 10;
                xpts[1] = levelOffset + 22;
                xpts[2] = levelOffset + 16;
                ypts[0] = levelOffset + 5;
                ypts[1] = levelOffset + 5;
                ypts[2] = levelOffset + 15;
            } else {
                xpts[0] = levelOffset + 11;
                xpts[1] = levelOffset + 11;
                xpts[2] = levelOffset + 21;
                ypts[0] = levelOffset + 4;
                ypts[1] = levelOffset + 16;
                ypts[2] = levelOffset + 10;
            }
            g.setColor( Color.GRAY );
            g.fillPolygon( xpts, ypts, 3 );
        }
    }
    
    @Override
    public void mouseClicked( MouseEvent e ) {
        int levelOffset = ( _level - 1 ) * 30;
        if ( e.getX() > levelOffset + 5 && e.getX() < levelOffset + 25 ) {
            _open = !_open;
            this.updateUI();
        }
        else {
            Container foo = this.getParent();
            foo.dispatchEvent( e );
        }
   }
    
    @Override
    public void mouseEntered( MouseEvent e ) {
        _mouseIn = true;
        this.updateUI();
    }
    
    @Override
    public void mouseExited( MouseEvent e ) {
        _mouseIn = false;
        this.updateUI();
    }
    
    @Override
    public void mousePressed( MouseEvent e ) {
        if ( e.getButton() == MouseEvent.BUTTON3 && _popup != null ) {
            _popup.show( e.getComponent(), e.getX(), e.getY() );
        }
        else {
            Container foo = this.getParent();
            foo.dispatchEvent( e );
        }
    }
    
    @Override
    public void mouseReleased( MouseEvent e ) {
        Container foo = this.getParent();
        foo.dispatchEvent( e );
    }
    
    @Override
    public void mouseMoved( MouseEvent e ) {
        Container foo = this.getParent();
        foo.dispatchEvent( e );
    }
    
    @Override
    public void mouseDragged( MouseEvent e ) {
        Container foo = this.getParent();
        foo.dispatchEvent( e );
    }
    
    public String name() {
        return _label.getText();
    }
    
    /*
     * Set whether this object is "in bounds", i.e. whether it can be displayed.
     */
    public void inBounds( boolean newVal ) {
        _inBounds = newVal;
    }
    
    protected boolean _open;
    protected boolean _showThis;
    protected boolean _inBounds;
    protected int _ySize;
    protected int _xSize;
    protected int _level;
    protected ArrayDeque<BrowserNode> _children;
    protected boolean _mouseIn;
    protected Color _backgroundColor;
    protected Color _highlightColor;
    protected JLabel _label;
    protected JPopupMenu _popup;
    protected JButton _popupButton;

}
