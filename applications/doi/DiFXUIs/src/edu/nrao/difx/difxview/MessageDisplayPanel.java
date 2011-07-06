/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * MessageDisplayPanel.java
 *
 * Created on Jun 29, 2011, 4:02:24 PM
 */
package edu.nrao.difx.difxview;

import javax.swing.text.StyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.BadLocationException;

import java.util.Date;

import java.awt.Color;

/**
 *
 * @author jspitzak
 */
public class MessageDisplayPanel extends javax.swing.JPanel {
    
    /** Creates new form MessageDisplayPanel */
    public MessageDisplayPanel() {
        initComponents();
        textField.setBackground( Color.black );
        textField.setForeground( Color.white );
        
        doc = textField.getStyledDocument();

        //  Set up a bunch of text "styles". 
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        regular = doc.addStyle("regular", def);

        // Create an italic style
        italic = doc.addStyle("italic", regular);
        StyleConstants.setItalic(italic, true);

        // Create a bold style
        bold = doc.addStyle("bold", regular);
        StyleConstants.setBold(bold, true);

        // Create a small style
        small = doc.addStyle("small", regular);
        StyleConstants.setFontSize(small, 10);

        // Create a large style
        large = doc.addStyle("large", regular);
        StyleConstants.setFontSize(large, 16);

        // Create a superscript style
        superscript = doc.addStyle("superscript", regular);
        StyleConstants.setSuperscript(superscript, true);

        // Create a highlight style
        highlight = doc.addStyle("highlight", regular);
        StyleConstants.setBackground(highlight, Color.yellow);
        
        // Colored text styles
        yellow = doc.addStyle( "yellow", regular );
        StyleConstants.setForeground( yellow, Color.yellow );
        red = doc.addStyle( "red", regular );
        StyleConstants.setForeground( red, Color.red );
        
    }
    
    /*
     * Set a "handler" to sponge up log messages and display them on this
     * message panel.  Logging can be broken into individual logs, which are
     * named by the given string.  You can use "global" as the string to grab
     * everything (which is generally what I do).
     */
    public void captureLogging( String logName ) {
        internalLoggingHandler = new InternalLoggingHandler( this );
        java.util.logging.Logger.getLogger( logName ).addHandler( internalLoggingHandler );
    }
    
    public void message( String newText ) {
        try {
            doc.insertString( doc.getLength(), newText, regular );
        } catch ( BadLocationException e ) {
            e.printStackTrace();
        }
    }

    public void warning( String newText ) {
        try {
            doc.insertString( doc.getLength(), newText, yellow );
        } catch ( BadLocationException e ) {
            e.printStackTrace();
        }
    }

    public void error( String newText ) {
        try {
            doc.insertString( doc.getLength(), newText, red );
        } catch ( BadLocationException e ) {
            e.printStackTrace();
        }
    }

    private InternalLoggingHandler internalLoggingHandler;
    /*
     * This class allows the message panel to sponge up logging messages.  Logging
     * is a bit complex and messy in Java, but for our purposes, you can dump
     * something like the following any place in the code:
     * 
     *      java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.SEVERE, "this is a message" );
     * 
     * This will cause the message to appear in the output console as well as to
     * be captured by this logging handler (and from there printed to the panel).
     * 
     * Logging messages are given a "level", which can be SEVERE, WARNING, INFO,
     * CONFIG, FINE, FINER, or FINEST.  The logging system can be adjusted to
     * ignore levels.  We don't use this system to its full extent - SEVERE log
     * messages are treated as "errors", WARNINGS are treated as "warnings" and
     * everything else is treated as "messages".
     * 
     * You can add a "Throwable" as a third parameter to a "log()" call if you
     * wish.  The "publish()" method below knows what to do with those.  You can
     * also add Objects, but publish() ignores those.
     */
    private class InternalLoggingHandler extends java.util.logging.Handler {
                public InternalLoggingHandler( MessageDisplayPanel newOutput ) {
            output = newOutput;
            includeTime = true;
            includeSource = true;
            includeDate = false;
        }
        public void includeDate( boolean newVal ) {
            includeDate = newVal;
        }
        public void includeTime( boolean newVal ) {
            includeTime = newVal;
        }
        public void includeSource( boolean newVal ) {
            includeSource = newVal;
        }
        @Override
        public void flush() {}        
        @Override
        public void close() {}        
        @Override
        public void publish( java.util.logging.LogRecord newRecord ) {
            //  Build a new message string including components as specified.
            String outText = new String( "" );
            if ( includeTime ) {
                //  This shows the seconds since the start of the current epoch
                //  to millisecond accuracy.
                outText += newRecord.getMillis() / 1000 + ".";
                long rem = newRecord.getMillis() - 1000 * ( newRecord.getMillis() / 1000 );
                if ( rem < 100 )
                    outText += "0";
                if ( rem < 10 )
                    outText += "0";
                if ( rem < 1 )
                    outText += "0";
                outText += rem;
                outText += " ";
            }
            else if ( includeDate ) {
                Date cal = new Date( newRecord.getMillis() );
            }
            if ( includeSource )
                outText += newRecord.getSourceClassName().substring( newRecord.getSourceClassName().lastIndexOf(".") + 1 ) + 
                        "." + newRecord.getSourceMethodName() + "(): ";
            //  Print out the string version of a Throwable, if one is included.
            //  These are sometimes big, but useful to see.
            if ( newRecord.getThrown() != null ) {
                outText += newRecord.getThrown().toString() + " ";
            }
            if ( newRecord.getMessage() != null ) {
                outText += newRecord.getMessage();
            }
            if ( newRecord.getLevel() == java.util.logging.Level.SEVERE )
                output.error( outText + "\n" );
            else if ( newRecord.getLevel() == java.util.logging.Level.WARNING )
                output.warning( outText + "\n" );
            else
                output.message( outText + "\n" );
        }       
        private MessageDisplayPanel output;
        boolean includeTime;
        boolean includeDate;
        boolean includeSource;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        textField = new javax.swing.JTextPane();

        jScrollPane1.setViewportView(textField);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 590, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 174, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextPane textField;
    // End of variables declaration//GEN-END:variables
    private StyledDocument doc;
    Style regular;
    Style italic;
    Style bold;
    Style small;
    Style large;
    Style superscript;
    Style highlight;
    Style yellow;
    Style red;
}