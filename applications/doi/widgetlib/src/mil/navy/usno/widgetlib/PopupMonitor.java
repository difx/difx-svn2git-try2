/*
 * This class provides a "monitor" popup - a modal dialog window that can be used
 * to monitor some process (a file transfer, or whatever).  The actual process takes
 * place during the (overridden) "run()" function.  The "success( true )" function should
 * be called when the process (what ever it is) completes successfully - at which
 * point the popup window will disappear.  The "errorCondition()" function should
 * be called if the process fails for some reason - the window will remain to
 * display an error message.  When running, there is a "cancel" button that turns
 * into a "dismiss" button when displaying errors.
 * 
 * The window is created with a "delay" value (in milliseconds) - the popup will not 
 * appear until after this time, and only if the process has not completed.  This
 * is used to keep the popup from flashing into and out of existence for fast
 * processes.
 * 
 * The window displays a little green spinning thing to show that it is working,
 * a primary status label (set using "statusLabel()"), an optional progress bar,
 * and an optional second status label (can't be displayed with the progress bar - they
 * occupy the same location).
 */
package mil.navy.usno.widgetlib;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JPanel;

import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.lang.Thread;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Dimension;
import java.awt.Color;

import java.awt.Frame;

/**
 *
 * @author jspitzak
 */
public class PopupMonitor extends JDialog implements WindowListener  {
    
    public PopupMonitor( Frame frame, int x, int y, int w, int h, int delay ) {
        super( frame, "", true );
        _theWindow = this;
        _delay = delay;
        this.setDefaultCloseOperation( JDialog.DO_NOTHING_ON_CLOSE );
        this.setLayout( null );
        this.setBounds( x, y, w, h );
        this.setResizable( false );
        _statusLabel = new JLabel( "" );
        _statusLabel.setBounds( 80, 20, w - 90, 25 );
        this.add( _statusLabel );
        _statusLabel2 = new JLabel( "" );
        _statusLabel2.setBounds( 80, 40, w - 90, 25 );
        _statusLabel2.setVisible( false );
        this.add( _statusLabel2 );
        _spinner = new Spinner();
        _spinner.setBounds( 20, 20, 40, 40 );
        this.add( _spinner );
        _progress = new JProgressBar();
        _progress.setBounds( 80, 50, w - 200, 25 );
        _progress.setVisible( false );
        this.add( _progress );
        _cancelButton = new JButton( "Cancel" );
        _cancelButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                cancelOperation();
            }
        });
        _cancelButton.setBounds( w - 320, 80, 120, 25 );
        this.add( _cancelButton );
        addWindowListener( this );
    }
    
    /*
     * Start things up.
     */
    public void start() {
        _spinner = new Spinner();
        //  This is where we actually run the process.
        run();
        //  Wait for a delay in milliseconds before we actually display a pop-up
        //  window.  This will give the process a chance to finish quietly if it is
        //  very quick.
        try { Thread.sleep( _delay ); } catch ( Exception e ) {}
        if ( _success == false )
            this.setVisible( true );
    }
    
    /*
     * Run process - to be overridden.
     */
    public void run() {
    }
    
    /*
     * Show the progress bar.
     */
    public void showProgress( boolean newVal ) {
        _progress.setVisible( newVal );
    }
    
    /*
     * Show the second status line.
     */
    public void showStatus2( boolean newVal ) {
        _statusLabel2.setVisible( newVal );
    }
    
    /*
     * Set the content of the status label.
     */
    public void status( String newVal ) {
        _statusLabel.setText( newVal );
    }
    
    /*
     * Set the content of the second status label.
     */
    public void status2( String newVal ) {
        _statusLabel2.setText( newVal );
    }
    
    /*
     * Called when things succeed.  This makes the window go away.
     */
    protected void successCondition() {
        _theWindow.setTitle( "DISMISSED" );
        _success = true;
        _theWindow.setVisible( false );
    }
    
    /*
     * Called when things fail.
     */
    protected void errorCondition() {
        _cancelButton.setText( "Dismiss" );
        _dismissActive = true;
    }
    
    /*
     * Set the progress.
     */
    protected void progress( int value, int maximum ) {
        _progress.setMaximum( maximum );
        _progress.setValue( value );
    }
    
    /*
     * Combine a few things - set the first and second status lines (the second one
     * is optional), remove the progress bar, and set the error condition.
     */
    protected void error( String stat1, String stat2 ) {
        showProgress( false );
        if ( stat2 != null )
            showStatus2( true );
        status( stat1 );
        status2( stat2 );
        errorCondition();
    }
    
    /*
     * This tells us whether the "dismiss" state has been set - indicating an
     * error has occurred.
     */
    protected boolean dismissActive() {
        return _dismissActive;
    }
    
    /*
     * Called when the user hits the cancel button.  process
     * and sets the "success" flag to false.  Note that the file transfer might
     * possibly complete before success is made false, but we are assuming the user
     * cancelled deliberately.
     */
    protected void cancelOperation() {
        _theWindow.setTitle( "DISMISSED" );
        _success = false;
        _theWindow.setVisible( false );
    }
    
    /*
     * Return whether the operation was completed successfully.
     */
    public boolean success() { return _success; }
    
    /*
     * Little spinner thing.
     */
    public class Spinner extends JPanel {
        
        public Spinner() {
            super();
            _value = 0;
            _this = this;
            _timeThread = new TimeThread();
            _timeThread.start();
            _colors = new int[12];
            for ( int i = 0; i < 12; ++i )
                _colors[i] = 255;
        }
        
        public void paintComponent( Graphics g ) {
            super.paintComponent( g );
            Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                         RenderingHints.VALUE_ANTIALIAS_ON );
            Dimension d = getSize();
            int startAngle = 359;
            int endAngle = 15;
            _colors[_value] = 0;
            for ( int i = 0; i < 12; ++i ) {
                if ( _cancelButton.getText().equalsIgnoreCase( "Dismiss" ) )
                    g2.setColor( Color.RED );
                else
                    g2.setColor( new Color( _colors[i], 255, _colors[i] ) );
                g2.fillArc( 0, 0, d.width, d.height, startAngle, endAngle );
                startAngle -= 30;
                _colors[i] += 25;
                if ( _colors[i] > 255 )
                    _colors[i] = 255;
            }
        }
        
        class TimeThread extends Thread {
            
            public TimeThread() {
                super();
            }
            
            public void run() {
                _value = 10;
                while ( !_theWindow.getTitle().equalsIgnoreCase( "DISMISSED" ) ) {
                    try { Thread.sleep( 166 );
                    } catch ( Exception e ) {
                    }
                    _value += 1;
                    if ( _value == 12 )
                        _value = 0;
                    _this.updateUI();
                }
            }
        }
        
        protected int _value;
        protected TimeThread _timeThread;
        volatile protected boolean _errorCondition;
        protected Spinner _this;
        protected int[] _colors;
    }
    
    /*
     * Window event methods - we need each of these, even though we are only
     * interested in the "Closing" method.
     */
    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        if ( _dismissActive )
            cancelOperation();
        else {
            int ans = JOptionPane.showConfirmDialog( this,
                    "Do you really wish to cancel the current download?",
                    "Cancel Operation?",
                    JOptionPane.YES_NO_OPTION );
            if ( ans == JOptionPane.YES_OPTION )
                cancelOperation();
        }
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    protected boolean _success;
    protected JLabel _statusLabel;
    protected JLabel _statusLabel2;
    protected JButton _cancelButton;
    protected JProgressBar _progress;
    protected boolean _dismissActive;
    protected Spinner _spinner;
    protected PopupMonitor _theWindow;
    protected int _delay;
    
}
