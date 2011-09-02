/*
 * A message node contains the data for a message, which may have different
 * levels of severity.  It uses the "draw()" function to display itself.
 */
package edu.nrao.difx.difxview;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Container;

import java.text.SimpleDateFormat;

/**
 *
 * @author jspitzak
 */
public class MessageNode {
    
    public static int ERROR = 0;
    public static int WARNING = 1;
    public static int INFO = 2;
    
    MessageNode( long time, int severity, String source, String message ) {
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
    
    /*
     * Draw this message at the given x and y location.
     */
    public void draw( Graphics g, int x, int y ) {
        if ( _severity < 1 )
            g.setColor( Color.RED );
        else if ( _severity < 2 )
            g.setColor( Color.YELLOW );
        else
            g.setColor( Color.WHITE );
        String text = "";
        if ( _showDate ) {
            text += _dateString + " ";
        }
        if ( _showTime ) {
            text += _timeString + " ";
        }
        if ( _showSource )
            text += "(" + _source + ") ";
        g.drawString( text + _message , x, y );
    }
    
    
    public int severity() {
        return _severity;
    }
    public void severity( int newVal ) {
        _severity = newVal;
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
    
    public boolean showThis() { return _showThis; }
    
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
    protected boolean _filterSource;
    protected boolean _filterMessage;
    protected String _filter;
    protected boolean _showThis;
    
}
