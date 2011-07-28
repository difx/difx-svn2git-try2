/*
 * A message node contains the data for a message, which may have different
 * levels of severity.
 */
package edu.nrao.difx.difxview;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Container;

import java.awt.event.MouseEvent;

import java.text.SimpleDateFormat;

/**
 *
 * @author jspitzak
 */
public class MessageNode extends BrowserNode {
    
    public static int ERROR = 0;
    public static int WARNING = 1;
    public static int INFO = 2;
    
    MessageNode( long time, int severity, String source, String message ) {
        super( "" );
        this.setLevel( 0 );
        //  Background and highlight colors are identical...i.e. no highlighting.
        _backgroundColor = Color.BLACK;
        _highlightColor = Color.BLACK;
        this.setHeight( 14 );
        this.setWidth( 1000 );
        _source = source;
        _message = message;
        _time = time;
        _severity = severity;
        _showTime = true;
        _showDate = false;
        _showSource = true;
        _showErrors = true;
        _showWarnings = true;
        _showMessages = true;
        _showChecked = true;
        _showUnchecked = true;
        SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd" );
        _dateString = sdf.format( time );
        sdf = new SimpleDateFormat( "HH:mm:ss.SSS" );
        _timeString = sdf.format( time );
    }
    
    @Override
    public void paintComponent( Graphics g ) {
        //  Use anti-aliasing on the text (looks much better)
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                     RenderingHints.VALUE_ANTIALIAS_ON );
        super.paintComponent( g );
        //  Draw the text....
        g2.setFont( new Font( "Dialog", Font.PLAIN, 12 ) );
        if ( _severity < 1 )
            g2.setColor( Color.RED );
        else if ( _severity < 2 )
            g2.setColor( Color.YELLOW );
        else
            g2.setColor( Color.WHITE );
        String text = "";
        if ( _showDate ) {
            text += _dateString + " ";
        }
        if ( _showTime ) {
            text += _timeString + " ";
        }
        if ( _showSource )
            text += "(" + _source + ") ";
        g2.drawString( text + _message , 18, 11 );
        //  Draw the star....clicked or unclicked.
        //  (For the moment, at least, the star is a box).
        if ( _starred )
            g2.setColor( Color.YELLOW );
        else
            g2.setColor( Color.DARK_GRAY );
        g2.fillRect( 4, 1, 10, 10 );
        if ( _starred )
            g2.setColor( Color.BLUE );
        else
            g2.setColor( Color.LIGHT_GRAY );
        g2.drawRect( 4, 1, 10, 10 );
        if ( _starred ) {
            g2.drawLine( 4, 1, 14, 11 );
            g2.drawLine( 4, 11, 14, 1 );
        }
    }
    
    
    public int severity() {
        return _severity;
    }
    public void severity( int newVal ) {
        _severity = newVal;
    }
    
    public boolean starred() {
        return _starred;
    }
    public void starred( boolean newVal ) {
        _starred = newVal;
    }
    
    public boolean showDate() {
        return _showDate;
    }
    public void showDate( boolean newVal ) {
        _showDate = newVal;
    }
    
    public boolean showTime() {
        return _showTime;
    }
    public void showTime( boolean newVal ) {
        _showTime = newVal;
    }
    
    public boolean showSource() {
        return _showSource;
    }
    public void showSource( boolean newVal ) {
        _showSource = newVal;
    }
    
    @Override
    public void mouseClicked( MouseEvent e ) {
        //  Check for a click on the "star" to the left of each message.
        if ( e.getX() > 1 && e.getX() < 17 ) {
            _starred = !_starred;
            showIt();
            this.updateUI();
        }
        else {
            Container foo = this.getParent();
            foo.dispatchEvent( e );
        }
   }
    
    public boolean showErrors() {
        return _showErrors;
    }
    public void showErrors( boolean newVal ) {
        _showErrors = newVal;
        showIt();
    }
    
    public boolean showWarnings() {
        return _showWarnings;
    }
    public void showWarnings( boolean newVal ) {
        _showWarnings = newVal;
        showIt();
    }
    
    public boolean showMessages() {
        return _showMessages;
    }
    public void showMessages( boolean newVal ) {
        _showMessages = newVal;
        showIt();
    }
    
    public boolean showChecked() {
        return _showChecked;
    }
    public void showChecked( boolean newVal ) {
        _showChecked = newVal;
        showIt();
    }
    
    public boolean showUnchecked() {
        return _showUnchecked;
    }
    public void showUnchecked( boolean newVal ) {
        _showUnchecked = newVal;
        showIt();
    }
    public void applyFilter( boolean filterSource, boolean filterMessage, String filter ) {
        _filterSource = filterSource;
        _filterMessage = filterMessage;
        //  Special case - if the filter is "*", we save null as an indication
        //  that there really is no filter.
        if ( filter == null )
            _filter = null;
        else if ( filter.equals( "*" ) )
            _filter = null;
        else
            _filter = filter;
        showIt();
    }
    
    /*
     * Figure out whether we want to show this item based on the above settings.
     */
    public void showIt() {
        if ( _severity == ERROR ) {
            _showThis = _showErrors;            
        }
        else if ( _severity == WARNING ) {
            _showThis = _showWarnings;
        }
        else if ( _severity == INFO ) {
            _showThis = _showMessages;
        }
        if ( _starred ) {
            _showThis &= _showChecked;
        }
        else {
            _showThis &= _showUnchecked;
        }
        if ( _filter != null ) {
            if ( _filterSource ) {
                if ( _source.indexOf( _filter ) > -1 )
                    _showThis &= true;
                else
                    _showThis = false;
            }
            if ( _filterMessage ) {
                if ( _message.indexOf( _filter ) > -1 )
                    _showThis &= true;
                else
                    _showThis = false;
            }
        }
    }
    
    protected String _source;
    protected String _message;
    protected long _time;
    protected int _severity;
    protected boolean _showTime;
    protected boolean _showDate;
    protected boolean _showSource;
    protected boolean _showErrors;
    protected boolean _showWarnings;
    protected boolean _showMessages;
    protected boolean _showChecked;
    protected boolean _showUnchecked;
    protected String _dateString;
    protected String _timeString;
    protected boolean _starred;
    protected boolean _filterSource;
    protected boolean _filterMessage;
    protected String _filter;
    
}
