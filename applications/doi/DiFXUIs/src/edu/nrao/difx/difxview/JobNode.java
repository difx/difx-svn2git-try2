/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.nrao.difx.difxview;

import mil.navy.usno.widgetlib.BrowserNode;
import mil.navy.usno.widgetlib.ActivityMonitorLight;

import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JProgressBar;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.util.List;
import java.util.Iterator;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Color;

import edu.nrao.difx.xmllib.difxmessage.DifxMessage;
import edu.nrao.difx.xmllib.difxmessage.DifxAlert;
import edu.nrao.difx.xmllib.difxmessage.DifxStatus;

/**
 *
 * @author jspitzak
 */
public class JobNode extends BrowserNode {
    
    public JobNode( String name ) {
        super( name );
        this.setHeight( 20 );
        this.visiblePopupButton( true );
    }
    
    @Override
    public void createAdditionalItems() {
//        _startButton = new JButton( "Start" );
//        this.add( _startButton );
//        _editButton = new JButton( "Edit" );
//        _editButton.addActionListener(new ActionListener() {
//            public void actionPerformed( ActionEvent e ) {
//                editAction( e );
//            }
//        });
//        this.add( _editButton );
        //  Create a popup menu appropriate to a "job".
        _networkActivity = new ActivityMonitorLight();
        _networkActivity.warningTime( 0 );
        _networkActivity.alertTime( 0 );
        this.add( _networkActivity );
        _state = new ColumnTextArea();
        _state.justify( ColumnTextArea.CENTER );
        _state.setText( "not started" );
        this.add( _state );
        _progress = new JProgressBar( 0, 100 );
        _progress.setValue( 0 );
        _progress.setStringPainted( true );
        this.add( _progress );
        _popup = new JPopupMenu();
        JMenuItem menuItem;
        menuItem = new JMenuItem( "Show Editor" );
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                editAction( e );
            }
        });
        _popup.add( menuItem );
        JMenuItem menuItem2 = new JMenuItem( "Show Monitor" );
        _popup.add( menuItem2 );
    }
    
    @Override
    public void positionItems() {
        _xOff = _level * 20;
        _networkActivity.setBounds( _xOff, 6, 10, 10 );
        _xOff += 14;
        _label.setBounds( _xOff, 0, 180, _ySize );
        _xOff += 180;
        _popupButton.setBounds( _xOff + 2, 2, 16, _ySize - 4 );//16, _ySize - 4 );
        _xOff += 20;
        _state.setBounds( _xOff + 1, 1, 100, 18 );
        _xOff += 101;
        _progress.setBounds( _xOff + 1, 1, 200, 18 );
        _xOff += 201;
//        _startButton.setBounds( _level * 30 + 150, 0, 70, 20 );
//        _editButton.setBounds( _level * 30 + 230, 0, 70, 20 );
    }
    
    @Override
    public void paintComponent( Graphics g ) {
        //  Use anti-aliasing on the text (looks much better)
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                     RenderingHints.VALUE_ANTIALIAS_ON );
        super.paintComponent( g );
    }
    
/*
     * Show the editor window.  If one has not been created yet, create it first.
     */
    public void editAction( ActionEvent e ) {
        if ( _editor == null )
            _editor = new JobEditor();
        _editor.setVisible( true );
    }
    
    public void consumeMessage( DifxMessage difxMsg ) {
        
        //  Got something...
        _networkActivity.data();
        
        //  See what kind of message this is...try status first.
        if ( difxMsg.getBody().getDifxStatus() != null ) {
            if ( difxMsg.getBody().getDifxStatus().getVisibilityMJD() != null &&
                    difxMsg.getBody().getDifxStatus().getJobstartMJD() != null &&
                    difxMsg.getBody().getDifxStatus().getJobstopMJD() != null )
                _progress.setValue( (int)( 0.5 + 100.0 * ( Double.valueOf( difxMsg.getBody().getDifxStatus().getVisibilityMJD() ) -
                        Double.valueOf( difxMsg.getBody().getDifxStatus().getJobstartMJD() ) ) /
                        ( Double.valueOf( difxMsg.getBody().getDifxStatus().getJobstopMJD() ) -
                        Double.valueOf( difxMsg.getBody().getDifxStatus().getJobstartMJD() ) ) ) );
            else
                _progress.setValue( 0 );
            _state.setText( difxMsg.getBody().getDifxStatus().getState() );
            if ( _state.getText().equalsIgnoreCase( "done" ) || _state.getText().equalsIgnoreCase( "mpidone" ) ) {
                _state.setBackground( Color.GREEN );
                _progress.setValue( 100 );  
            }
            else if ( _state.getText().equalsIgnoreCase( "running" ) )
                _state.setBackground( Color.YELLOW );
            else
                _state.setBackground( Color.LIGHT_GRAY ); 
            //System.out.println( difxMsg.getBody().getDifxStatus().getVisibilityMJD() );
            //System.out.println( difxMsg.getBody().getDifxStatus().getJobstartMJD() );
            //System.out.println( difxMsg.getBody().getDifxStatus().getJobstopMJD() );
            List<DifxStatus.Weight> weightList = difxMsg.getBody().getDifxStatus().getWeight();
            for ( Iterator<DifxStatus.Weight> iter = weightList.iterator(); iter.hasNext(); ) {
                DifxStatus.Weight thisWeight = iter.next();
                //System.out.println( "antenna = " + thisWeight.getAnt() + "   weight = " + thisWeight.getWt() );
            }
        }
        else if ( difxMsg.getBody().getDifxAlert() != null ) {
            //System.out.println( "this is an alert" );
            //System.out.println( difxMsg.getBody().getDifxAlert().getAlertMessage() );
            //System.out.println( difxMsg.getBody().getDifxAlert().getSeverity() );
        }

    }
    
    JButton _startButton;
    JButton _editButton;
    JobEditor _editor;
    JProgressBar _progress;
    int _xOff;
    ActivityMonitorLight _networkActivity;
    ColumnTextArea _state;

}
