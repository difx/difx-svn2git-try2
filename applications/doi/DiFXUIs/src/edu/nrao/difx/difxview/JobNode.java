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
import java.awt.Component;

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
        _columnColor = Color.LIGHT_GRAY;
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
        _experiment = new ColumnTextArea();
        _experiment.justify( ColumnTextArea.RIGHT );
        _experiment.setText( "" );
        showExperiment( false );
        this.add( _experiment );
        _pass = new ColumnTextArea();
        _pass.justify( ColumnTextArea.RIGHT );
        _pass.setText( "" );
        showPass( false );
        this.add( _pass );
        _priority = new ColumnTextArea();
        _priority.justify( ColumnTextArea.RIGHT );
        _priority.setText( "" );
        showPriority( false );
        this.add( _priority );
        _queueTime = new ColumnTextArea();
        _queueTime.justify( ColumnTextArea.RIGHT );
        _queueTime.setText( "" );
        showQueueTime( true );
        this.add( _queueTime );
        _correlationStart = new ColumnTextArea();
        _correlationStart.justify( ColumnTextArea.RIGHT );
        _correlationStart.setText( "" );
        showCorrelationStart( false );
        this.add( _correlationStart );
        _correlationEnd = new ColumnTextArea();
        _correlationEnd.justify( ColumnTextArea.RIGHT );
        _correlationEnd.setText( "" );
        showCorrelationEnd( false );
        this.add( _correlationEnd );
        _jobStart = new ColumnTextArea();
        _jobStart.justify( ColumnTextArea.RIGHT );
        _jobStart.setText( "" );
        showJobStart( true );
        this.add( _jobStart );
        _jobDuration = new ColumnTextArea();
        _jobDuration.justify( ColumnTextArea.RIGHT );
        _jobDuration.setText( "" );
        showJobDuration( true );
        this.add( _jobDuration );
        _inputFile = new ColumnTextArea();
        _inputFile.justify( ColumnTextArea.RIGHT );
        _inputFile.setText( "" );
        showInputFile( true );
        this.add( _inputFile );
        _outputFile = new ColumnTextArea();
        _outputFile.justify( ColumnTextArea.RIGHT );
        _outputFile.setText( "" );
        showOutputFile( false );
        this.add( _outputFile );
        _outputSize = new ColumnTextArea();
        _outputSize.justify( ColumnTextArea.RIGHT );
        _outputSize.setText( "" );
        showOutputSize( false );
        this.add( _outputSize );
        _difxVersion = new ColumnTextArea();
        _difxVersion.justify( ColumnTextArea.RIGHT );
        _difxVersion.setText( "" );
        showDifxVersion( true );
        this.add( _difxVersion );
        _speedUpFactor = new ColumnTextArea();
        _speedUpFactor.justify( ColumnTextArea.RIGHT );
        _speedUpFactor.setText( "" );
        showSpeedUpFactor( true );
        this.add( _speedUpFactor );
        _numAntennas = new ColumnTextArea();
        _numAntennas.justify( ColumnTextArea.RIGHT );
        _numAntennas.setText( "" );
        showNumAntennas( true );
        this.add( _numAntennas );
        _numForeignAntennas = new ColumnTextArea();
        _numForeignAntennas.justify( ColumnTextArea.RIGHT );
        _numForeignAntennas.setText( "" );
        showNumForeignAntennas( true );
        this.add( _numForeignAntennas );
        _dutyCycle = new ColumnTextArea();
        _dutyCycle.justify( ColumnTextArea.RIGHT );
        _dutyCycle.setText( "" );
        showDutyCycle( true );
        this.add( _dutyCycle );
        _status = new ColumnTextArea();
        _status.justify( ColumnTextArea.RIGHT );
        _status.setText( "" );
        showStatus( true );
        this.add( _status );
                boolean active = false;
        _statusId = new ColumnTextArea();
        _statusId.justify( ColumnTextArea.RIGHT );
        _statusId.setText( "" );
        showStatusId( false );
        this.add( _statusId );
        _active = new ActivityMonitorLight();
        showActive( true );
        this.add( _active );
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
        widthExperiment( 100 );
        widthPass( 100 );
        widthPriority( 100 );
        widthQueueTime( 170 );
        widthCorrelationStart( 100 );
        widthCorrelationEnd( 100 );
        widthJobStart( 80 );
        widthJobDuration( 80 );
        widthInputFile( 400 );
        widthOutputFile( 100 );
        widthOutputSize( 100 );
        widthDifxVersion( 100 );
        widthSpeedUpFactor( 100 );
        widthNumAntennas( 50 );
        widthNumForeignAntennas( 50 );
        widthDutyCycle( 100 );
        widthStatus( 100 );
        widthActive( 100 );
        widthStatusId( 100 );
    }
    
    @Override
    public void positionItems() {
        _colorColumn = false;
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
        if ( _experiment.isVisible() )
            setTextArea( _experiment, _widthExperiment );
        if ( _pass.isVisible() )
            setTextArea( _pass, _widthPass );
        if ( _priority.isVisible() )
            setTextArea( _priority, _widthPriority );
        if ( _queueTime.isVisible() )
            setTextArea( _queueTime, _widthQueueTime );
        if ( _correlationStart.isVisible() )
            setTextArea( _correlationStart, _widthCorrelationStart );
        if ( _correlationEnd.isVisible() )
            setTextArea( _correlationEnd, _widthCorrelationEnd );
        if ( _jobStart.isVisible() )
            setTextArea( _jobStart, _widthJobStart );
        if ( _jobDuration.isVisible() )
            setTextArea( _jobDuration, _widthJobDuration );
        if ( _inputFile.isVisible() )
            setTextArea( _inputFile, _widthInputFile );
        if ( _outputFile.isVisible() )
            setTextArea( _outputFile, _widthOutputFile );
        if ( _outputSize.isVisible() )
            setTextArea( _outputSize, _widthOutputSize );
        if ( _difxVersion.isVisible() )
            setTextArea( _difxVersion, _widthDifxVersion );
        if ( _speedUpFactor.isVisible() )
            setTextArea( _speedUpFactor, _widthSpeedUpFactor );
        if ( _numAntennas.isVisible() )
            setTextArea( _numAntennas, _widthNumAntennas );
        if ( _numForeignAntennas.isVisible() )
            setTextArea( _numForeignAntennas, _widthNumForeignAntennas );
        if ( _dutyCycle.isVisible() )
            setTextArea( _dutyCycle, _widthDutyCycle );
        if ( _status.isVisible() )
            setTextArea( _status, _widthStatus );
        if ( _active.isVisible() )
            setTextArea( _active, _widthActive );
        if ( _statusId.isVisible() )
            setTextArea( _statusId, _widthStatusId );
    }
    
    /*
     * Private function used repeatedly in positionItems().
     */
    protected void setTextArea( Component area, int xSize ) {
        area.setBounds( _xOff, 1, xSize, _ySize - 2);
        _xOff += xSize;
        if ( _colorColumn )
            area.setBackground( _columnColor );
        else
            area.setBackground( Color.WHITE );
        _colorColumn = !_colorColumn;
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
    
    public void experiment( String newVal ) { _experiment.setText( newVal ); }
    public String experiment() { return _experiment.getText(); }
    public void pass( String newVal ) { _pass.setText( newVal ); }
    public String pass() { return _pass.getText(); }
    public void priority( int newVal ) { _priority.setText( String.format( "%10d", newVal ) ); }
    public int priority() { return new Integer( _priority.getText() ).intValue(); }
    public void queueTime( String newVal ) { _queueTime.setText( newVal ); }
    public String queueTime() { return _queueTime.getText(); }
    public void correlationStart( String newVal ) { _correlationStart.setText( newVal ); }
    public String correlationStart() { return _correlationStart.getText(); }
    public void correlationEnd( String newVal ) { _correlationEnd.setText( newVal ); }
    public String correlationEnd() { return _correlationEnd.getText(); }
    public void jobStart( double newVal ) { _jobStart.setText( String.format( "%10.3f", newVal ) ); }
    public double jobStart() { return new Double( _jobStart.getText() ).doubleValue(); }
    public void jobDuration( double newVal ) { _jobDuration.setText( String.format( "%10.3f", newVal ) ); }
    public double jobDuration() { return new Double( _jobDuration.getText() ).doubleValue(); }
    public void inputFile( String newVal ) { _inputFile.setText( newVal ); }
    public String inputFile() { return _inputFile.getText(); }
    public void outputFile( String newVal ) { _outputFile.setText( newVal ); }
    public String outputFile() { return _outputFile.getText(); }
    public void outputSize( int newVal ) { _outputSize.setText( String.format( "%10d", newVal ) ); }
    public int outputSize() { return new Integer( _outputSize.getText() ).intValue(); }
    public void difxVersion( String newVal ) { _difxVersion.setText( newVal ); }
    public String difxVersion() { return _difxVersion.getText(); }
    public void speedUpFactor( double newVal ) { _speedUpFactor.setText( String.format( "%10.3f", newVal ) ); }
    public double speedUpFactor() { return new Double( _speedUpFactor.getText() ).doubleValue(); }
    public void numAntennas( int newVal ) { _numAntennas.setText( String.format( "%10d", newVal ) ); }
    public int numAntennas() { return new Integer( _numAntennas.getText() ).intValue(); }
    public void numForeignAntennas( int newVal ) { _numForeignAntennas.setText( String.format( "%10d", newVal ) ); }
    public int numForeignAntennas() { return new Integer( _numForeignAntennas.getText() ).intValue(); }
    public void dutyCycle( String newVal ) { _dutyCycle.setText( newVal ); }
    public String dutyCycle() { return _dutyCycle.getText(); }
    public void status( String newVal ) { _status.setText( newVal ); }
    public String status() { return _status.getText(); }
    public void active( boolean newVal ) { _active.on( newVal ); }
    public boolean active() { return _active.on(); }
    public void statusId( int newVal ) { _statusId.setText( String.format( "%10d", newVal ) ); }
    public int statusId() { return new Integer( _statusId.getText() ).intValue(); }
    
    public void showExperiment( boolean newVal ) { _experiment.setVisible( newVal ); }
    public void showPass( boolean newVal ) { _pass.setVisible( newVal ); }
    public void showPriority( boolean newVal ) { _priority.setVisible( newVal ); }
    public void showQueueTime( boolean newVal ) { _queueTime.setVisible( newVal ); }
    public void showCorrelationStart( boolean newVal ) { _correlationStart.setVisible( newVal ); }
    public void showCorrelationEnd( boolean newVal ) { _correlationEnd.setVisible( newVal ); }
    public void showJobStart( boolean newVal ) { _jobStart.setVisible( newVal ); }
    public void showJobDuration( boolean newVal ) { _jobDuration.setVisible( newVal ); }
    public void showInputFile( boolean newVal ) { _inputFile.setVisible( newVal ); }
    public void showOutputFile( boolean newVal ) { _outputFile.setVisible( newVal ); }
    public void showOutputSize( boolean newVal ) { _outputSize.setVisible( newVal ); }
    public void showDifxVersion( boolean newVal ) { _difxVersion.setVisible( newVal ); }
    public void showSpeedUpFactor( boolean newVal ) { _speedUpFactor.setVisible( newVal ); }
    public void showNumAntennas( boolean newVal ) { _numAntennas.setVisible( newVal ); }
    public void showNumForeignAntennas( boolean newVal ) { _numForeignAntennas.setVisible( newVal ); }
    public void showDutyCycle( boolean newVal ) { _dutyCycle.setVisible( newVal ); }
    public void showStatus( boolean newVal ) { _status.setVisible( newVal ); }
    public void showActive( boolean newVal ) { _active.setVisible( newVal ); }
    public void showStatusId( boolean newVal ) { _statusId.setVisible( newVal ); }
    
    public void widthExperiment( int newVal ) { _widthExperiment = newVal; }
    public void widthPass( int newVal ) { _widthPass = newVal; }
    public void widthPriority( int newVal ) { _widthPriority = newVal; }
    public void widthQueueTime( int newVal ) { _widthQueueTime = newVal; }
    public void widthCorrelationStart( int newVal ) { _widthCorrelationStart = newVal; }
    public void widthCorrelationEnd( int newVal ) { _widthCorrelationEnd = newVal; }
    public void widthJobStart( int newVal ) { _widthJobStart = newVal; }
    public void widthJobDuration( int newVal ) { _widthJobDuration = newVal; }
    public void widthInputFile( int newVal ) { _widthInputFile = newVal; }
    public void widthOutputFile( int newVal ) { _widthOutputFile = newVal; }
    public void widthOutputSize( int newVal ) { _widthOutputSize = newVal; }
    public void widthDifxVersion( int newVal ) { _widthDifxVersion = newVal; }
    public void widthSpeedUpFactor( int newVal ) { _widthSpeedUpFactor = newVal; }
    public void widthNumAntennas( int newVal ) { _widthNumAntennas = newVal; }
    public void widthNumForeignAntennas( int newVal ) { _widthNumForeignAntennas = newVal; }
    public void widthDutyCycle( int newVal ) { _widthDutyCycle = newVal; }
    public void widthStatus( int newVal ) { _widthStatus = newVal; }
    public void widthActive( int newVal ) { _widthActive = newVal; }
    public void widthStatusId( int newVal ) { _widthStatusId = newVal; }
    
    protected JButton _startButton;
    protected JButton _editButton;
    protected JobEditor _editor;
    protected JProgressBar _progress;
    protected int _xOff;
    protected ActivityMonitorLight _networkActivity;
    protected ColumnTextArea _state;
    protected ColumnTextArea _experiment;
    protected int _widthExperiment;
    protected ColumnTextArea _pass;
    protected int _widthPass;
    protected ColumnTextArea _priority;
    protected int _widthPriority;
    protected ColumnTextArea _queueTime;
    protected int _widthQueueTime;
    protected ColumnTextArea _correlationStart;
    protected int _widthCorrelationStart;
    protected ColumnTextArea _correlationEnd;
    protected int _widthCorrelationEnd;
    protected ColumnTextArea _jobStart;
    protected int _widthJobStart;
    protected ColumnTextArea _jobDuration;
    protected int _widthJobDuration;
    protected ColumnTextArea _inputFile;
    protected int _widthInputFile;
    protected ColumnTextArea _outputFile;
    protected int _widthOutputFile;
    protected ColumnTextArea _outputSize;
    protected int _widthOutputSize;
    protected ColumnTextArea _difxVersion;
    protected int _widthDifxVersion;
    protected ColumnTextArea _speedUpFactor;
    protected int _widthSpeedUpFactor;
    protected ColumnTextArea _numAntennas;
    protected int _widthNumAntennas;
    protected ColumnTextArea _numForeignAntennas;
    protected int _widthNumForeignAntennas;
    protected ColumnTextArea _dutyCycle;
    protected int _widthDutyCycle;
    protected ColumnTextArea _status;
    protected int _widthStatus;
    protected ActivityMonitorLight _active;
    protected int _widthActive;
    protected ColumnTextArea _statusId;
    protected int _widthStatusId;
    
    protected boolean _colorColumn;
    protected Color _columnColor;

}
