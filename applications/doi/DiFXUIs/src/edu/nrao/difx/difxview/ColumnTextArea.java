/*
 * This class formats text for display as part of a column display.
 */
package edu.nrao.difx.difxview;

import javax.swing.JPanel;

import java.awt.Graphics;
import java.awt.Dimension;

public class ColumnTextArea extends JPanel {
    
    public ColumnTextArea() {
        super();
        _text = "";
        _margin = 2;
        _justify = CENTER;
    }
    
    public ColumnTextArea( String newText ) {
        super();
        _text = newText;
    }
    
    public void justify( int newVal ) {
        _justify = newVal;
    }
    
    public void margin( int newVal ) {
        _margin = newVal;
    }
    
    public void setText( String newText ) {
        _text = newText;
    }
    
    public String getText() {
        return _text;
    }
    
    @Override
    public void paintComponent( Graphics g ) {
        super.paintComponent( g );
        Dimension d = this.getSize();
        int height = g.getFontMetrics().getHeight();
        int descent = g.getFontMetrics().getDescent();
        int width = g.getFontMetrics().stringWidth( _text );
        if ( _justify == LEFT )
            g.drawString( _text, _margin, ( d.height - height ) / 2 + ( height - descent ) );
        else if ( _justify == RIGHT )
            g.drawString( _text, d.width - _margin - width, ( d.height - height ) / 2 + ( height - descent ) );
        else
            g.drawString( _text, _margin + ( d.width - width ) / 2, ( d.height - height ) / 2 + ( height - descent ) );
    }
    
    static int CENTER = 0;
    static int LEFT = 1;
    static int RIGHT = 2;
    int _justify;
    String _text;
    int _margin;
}
