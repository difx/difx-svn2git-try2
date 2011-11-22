/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.nrao.difx.difxview;

import mil.navy.usno.widgetlib.BrowserNode;
import mil.navy.usno.widgetlib.ActivityMonitorLight;

import mil.navy.usno.plotlib.PlotWindow;
import mil.navy.usno.plotlib.Plot2DObject;
import mil.navy.usno.plotlib.Track2D;

import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.util.List;
import java.util.Iterator;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;

import java.io.File;

import edu.nrao.difx.xmllib.difxmessage.DifxMessage;
import edu.nrao.difx.xmllib.difxmessage.DifxAlert;
import edu.nrao.difx.xmllib.difxmessage.DifxStatus;
import java.awt.Font;

/**
 *
 * @author jspitzak
 */
public class JobNode extends BrowserNode {
    
    public JobNode( String name, SystemSettings settings ) {
        super( name );
        this.setHeight( 20 );
        //this.visiblePopupButton( false );
        _columnColor = Color.LIGHT_GRAY;
        _settings = settings;
        updateEditorMonitor();
    }
    
    @Override
    public void createAdditionalItems() {
        _selectedButton = new JButton( "\u2606" );
        _selectedButton.setBorderPainted( false );
        _selectedButton.setContentAreaFilled( false );
        _selectedButton.setMargin( new Insets( 0, 0, 2, 0 ) );
        _selectedButton.setForeground( Color.BLACK );
        _selectedButton.setFont( new Font( "Dialog", Font.BOLD, 14 ) );
        _selectedButton.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                selectedButtonAction();
            }
        });
        this.add( _selectedButton );
        //  This field is used to edit the name of the experiment when "rename"
        //  is picked from the popup menu.
        _nameEditor = new JTextField( "" );
        _nameEditor.setVisible( false );
        _nameEditor.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                nameEditorAction();
            }
        });
        this.add( _nameEditor );
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
        showNetworkActivity( true );
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
        JMenuItem menuItem2 = new JMenuItem( "Control/Monitor" );
        menuItem2.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateEditorMonitor();
                _editorMonitor.setVisible( true );
            }
        });
        _popup.add( menuItem2 );
        _popup.add( new JSeparator() );
        _selectMenuItem = new JMenuItem( "Select Job" );
        _selectMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                selectedButtonAction();
            }
        });
        _popup.add( _selectMenuItem );
        JMenuItem menuItem2a = new JMenuItem( "Rename" );
        menuItem2a.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                renameAction();
            }
        });
        _popup.add( menuItem2a );
        JMenuItem menuItem3 = new JMenuItem( "Copy" );
        _popup.add( menuItem3 );
        JMenuItem menuItem4 = new JMenuItem( "Delete" );
        _popup.add( menuItem4 );
        _popup.add( new JSeparator() );
        JMenuItem menuItem8 = new JMenuItem( "Queue" );
        menuItem8.setToolTipText( "Put this job in the runnable queue." );
        _popup.add( menuItem8 );
        JMenuItem menuItem5 = new JMenuItem( "Start" );
        menuItem5.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateEditorMonitor();
                _editorMonitor.startJob();
            }
        });
        _popup.add( menuItem5 );
        JMenuItem menuItem6 = new JMenuItem( "Pause" );
        menuItem6.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateEditorMonitor();
                _editorMonitor.pauseJob();
            }
        });
        _popup.add( menuItem6 );
        JMenuItem menuItem7 = new JMenuItem( "Stop" );
        menuItem7.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateEditorMonitor();
                _editorMonitor.stopJob();
            }
        });
        _popup.add( menuItem7 );
        JMenuItem menuItem9 = new JMenuItem( "Reset" );
        _popup.add( menuItem9 );
    }
    
    @Override
    public void positionItems() {
        _selectedButton.setBounds( 0, 2, 20, 20 );
        _colorColumn = false;
        _xOff = _level * 30;
        _networkActivity.setBounds( _xOff, 6, 10, 10 );
        _xOff += 14;
        _label.setBounds( _xOff, 0, _widthName, _ySize );
        _nameEditor.setBounds( _xOff, 0, _widthName, _ySize );
        _xOff += _widthName;
        _state.setBounds( _xOff + 1, 1, _widthState - 2, 18 );
        _xOff += _widthState;
        _progress.setBounds( _xOff + 1, 1, _widthProgressBar - 2, 18 );
        _xOff += _widthProgressBar;
        if ( _showWeights && _weights != null ) {
            //  The weights are a bit complicated...
            if ( _weights.length > 0 ) {
                int boxSize = _widthWeights / _weights.length / 2;
                for ( int i = 0; i < _weights.length; ++i ) {
                    setTextArea( _antenna[i], boxSize );
                    _antenna[i].setVisible( true );
//                    if ( _showWeightsAsPlots ) {
//                        setTextArea( _weightPlotWindow[i], boxSize );
//                        _weightPlotWindow[i].setVisible( true );
//                        _weight[i].setVisible( false );
//                    }
//                    else {
                        setTextArea( _weight[i], boxSize );
                        _weight[i].setVisible( true );
//                        _weightPlotWindow[i].setVisible( false );
//                    }
                }
            }
            else
                _xOff += _widthWeights;
        }
        else {
            if ( _weights != null ) {
                for ( int i = 0; i < _weights.length; ++i ) {
                    _antenna[i].setVisible( false );
                    _weight[i].setVisible( false );
//                    _weightPlotWindow[i].setVisible( false );
                }
            }
        }
//        _startButton.setBounds( _level * 30 + 150, 0, 70, 20 );
//        _editButton.setBounds( _level * 30 + 230, 0, 70, 20 );
        //if ( _state.isVisible() )
        //    setTextArea( _state, _widthState );
        //if ( _progress.isVisible() )
        //    setTextArea( _progress, _widthProgressBar );
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
     * This function responds to a rename request from the popup menu.  It replaces
     * the "label" text field with an editable field containing the name.
     */
    public void renameAction() {
        _nameEditor.setText( _label.getText() );
        _nameEditor.setVisible( true );
        _label.setVisible( false );
    }
    
    /*
     * This is the callback for the editable name field triggered by a rename
     * request.  It replaces the label containing the name with whatever is in
     * the edited field.  The change must be sent to the database as well!
     */
    public void nameEditorAction() {
        _label.setText( _nameEditor.getText() );
        _label.setVisible( true );
        _nameEditor.setVisible( false );
        //  BLAT DATABASE
        System.out.println( "change job name in database" );
    }
    
    /*
     * Select or unselect this item.  This is the callback for the button.
     */
    public void selectedButtonAction() {
        _selected = !_selected;
        checkSelectionSetting();
    }
    
    public void checkSelectionSetting() {
        if ( _selected ) {
            _selectedButton.setText( "\u2605" );
            _selectedButton.setForeground( new Color( 200, 100, 0 ) );
            _selectMenuItem.setText( "Unselect Job" );
        }
        else {
            _selectedButton.setText( "\u2606" );
            _selectedButton.setForeground( Color.BLACK );
            _selectMenuItem.setText( "Select Job" );
        }
    }
    
    /*
     * Set or get the selected value from the outside.
     */
    public boolean selected() { return _selected; }
    public void selected( boolean newVal ) {
        _selected = newVal;
        checkSelectionSetting();
    }

    /*
     * Show the job editor/monitor window.  If one has not been created yet, create it first.
     */
    public void showEditorMonitor( ActionEvent e ) {
        updateEditorMonitor();
        _editorMonitor.setVisible( true );
    }
    
    /*
     * Internal function used to generate an editor/monitor for this job if one
     * does not exists and update it with current settings, as far as we know them.
     */
    protected void updateEditorMonitor() {
        if ( _editorMonitor == null )
            _editorMonitor = new JobEditorMonitor( this, _settings );
    }
    
    /*
     *   Test if this message is intended for a job or not.
     */
    static boolean testJobMessage( DifxMessage difxMsg ) {
        if ( ( difxMsg.getBody().getDifxStatus() != null ) ||
             ( difxMsg.getBody().getDifxAlert() != null ) )
            return true;
        else
            return false;
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
            List<DifxStatus.Weight> weightList = difxMsg.getBody().getDifxStatus().getWeight();
            //  Create a new list of antennas/weights if one hasn't been created yet.
            if ( _weights == null )
                newWeightDisplay( weightList.size() );
            for ( Iterator<DifxStatus.Weight> iter = weightList.iterator(); iter.hasNext(); ) {
                DifxStatus.Weight thisWeight = iter.next();
                weight( thisWeight.getAnt(), thisWeight.getWt() );
            }
        }
        else if ( difxMsg.getBody().getDifxAlert() != null ) {
            //System.out.println( "this is an alert" );
            //System.out.println( difxMsg.getBody().getDifxAlert().getAlertMessage() );
            //System.out.println( difxMsg.getBody().getDifxAlert().getSeverity() );
        }

    }
    
    /*
     * This function is used to generate antenna/weight display areas.
     */
    protected void newWeightDisplay( int numAntennas ) {
        _weights = new double[ numAntennas ];
        _antennas = new String[ numAntennas ];
        _weight = new ColumnTextArea[ numAntennas ];
        _antenna = new ColumnTextArea[ numAntennas ];
//        _weightPlotWindow = new PlotWindow[ numAntennas ];
//        _weightPlot = new Plot2DObject[ numAntennas ];
//        _weightTrack = new Track2D[ numAntennas ];
//        _weightTrackSize = new int[ numAntennas ];
        //  Give the antennas "default" names.
        for ( Integer i = 0; i < numAntennas; ++i ) {
            _antenna[i] = new ColumnTextArea( i.toString() + ": " );
            _antenna[i].justify( ColumnTextArea.RIGHT );
            this.add( _antenna[i] );
            _weight[i] = new ColumnTextArea( "" );
            this.add( _weight[i] );
            _antennas[i] = i.toString();
//            //  This stuff is used to make a plot of the weight.
//            _weightPlotWindow[i] = new PlotWindow();
//            this.add( _weightPlotWindow[i] );
//            _weightPlot[i] = new Plot2DObject();
//            _weightPlotWindow[i].add2DPlot( _weightPlot[i] );
//            _weightTrack[i] = new Track2D();
//            _weightPlot[i].name( "Weight Plot " + i.toString() );
//            _weightPlot[i].drawBackground( true );
//            _weightPlot[i].drawFrame( true );
//            _weightPlot[i].frameColor( Color.GRAY );
//            _weightPlot[i].clip( true );
//            _weightPlot[i].addTopGrid( Plot2DObject.X_AXIS, 10.0, Color.BLACK );
//            _weightTrack[i] = new Track2D();
//            _weightTrack[i].fillCurve( true );
//            _weightPlot[i].addTrack( _weightTrack[i] );
//            _weightTrack[i].color( Color.GREEN );
//            _weightTrack[i].sizeLimit( 200 );
//            _weightPlot[i].frame( 0.0, 0.0, 1.0, 1.0 );
//            _weightPlot[i].backgroundColor( Color.BLACK );
//            _weightTrackSize[i] = 0;
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
    public void inputFile( String newVal ) { 
        _inputFile.setText( newVal );
        //  Convert to a file to extract the directory path...
        File tryFile = new File( newVal );
        _directoryPath = tryFile.getParent();
    }
    public String inputFile() { return _inputFile.getText(); }
    public void outputFile( String newVal ) { _outputFile.setText( newVal ); }
    public String outputFile() { return _outputFile.getText(); }
    public void outputSize( int newVal ) { _outputSize.setText( String.format( "%10d", newVal ) ); }
    public int outputSize() { return new Integer( _outputSize.getText() ).intValue(); }
    public void difxVersion( String newVal ) { _difxVersion.setText( newVal ); }
    public String difxVersion() { return _difxVersion.getText(); }
    public void speedUpFactor( double newVal ) { _speedUpFactor.setText( String.format( "%10.3f", newVal ) ); }
    public double speedUpFactor() { return new Double( _speedUpFactor.getText() ).doubleValue(); }
    public void numAntennas( int newVal ) {
        _numAntennas.setText( String.format( "%10d", newVal ) );
        newWeightDisplay( newVal );
    }
    public int numAntennas() { return new Integer( _numAntennas.getText() ).intValue(); }
    public void weight( String antenna, String newString ) {
        double newVal = Double.valueOf( newString );
        for ( int i = 0; i < _weights.length; ++i ) {
            if ( _antennas[i].contentEquals( antenna ) ) {
                _weights[i] = newVal;
                _weight[i].setText( newString );
//                _weightPlot[i].limits( (double)(_weightTrackSize[i] - 20), (double)(_weightTrackSize[i]), 0.0, 1.05 );
//                _weightTrack[i].add( (double)(_weightTrackSize[i]), newVal );
//                _weightTrackSize[i] += 1;
//                _weightPlotWindow[i].updateUI();
            }
        }
    }
    public double weight( String antenna ) {
        for ( int i = 0; i < _weights.length; ++i )
            if ( _antennas[i].contentEquals( antenna ) )
                return _weights[i];
        return 0.0;
    }
    public void antennaName( int i, String name ) {
        if ( i < _antennas.length )
            _antennas[i] = name;
    }
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
    public String directoryPath() { return _directoryPath; }
    
    public void showNetworkActivity( boolean newVal ) { _networkActivity.setVisible( newVal ); }
    public void showName( boolean newVal ) { _label.setVisible( newVal ); }
    public void showProgressBar( boolean newVal ) { _progress.setVisible( newVal ); }
    public void showState( boolean newVal ) { _state.setVisible( newVal ); }
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
    public void showWeights( boolean newVal ) { 
        _showWeights = newVal;
        this.updateUI();
    }
//    public void showWeightsAsPlots( boolean newVal ) { 
//        _showWeightsAsPlots = newVal;
//        this.updateUI();
//    }
    
    public void widthName( int newVal ) { _widthName = newVal; }
    public void widthProgressBar( int newVal ) { _widthProgressBar = newVal; }
    public void widthState( int newVal ) { _widthState = newVal; }
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
    public void widthWeights( int newVal ) { _widthWeights = newVal; }
    
    public PassNode passNode() {
        return _passNode;
    }
    public void passNode( PassNode newNode ) {
        _passNode = newNode;
    }
    
    public JobEditorMonitor editorMonitor() { return _editorMonitor; }
    
    protected PassNode _passNode;
    
    protected JButton _startButton;
    protected JButton _editButton;
    protected JobEditorMonitor _editorMonitor;
    protected int _xOff;
    protected int _widthName;
    protected JProgressBar _progress;
    protected int _widthProgressBar;
    protected ActivityMonitorLight _networkActivity;
    protected ColumnTextArea _state;
    protected int _widthState;
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
    protected double[] _weights;
    protected String[] _antennas;
    protected boolean _showWeights;
    protected boolean _showWeightsAsPlots;
    protected int _widthWeights;
    protected ColumnTextArea[] _weight;
    protected ColumnTextArea[] _antenna;
//    protected PlotWindow[] _weightPlotWindow;
//    protected Plot2DObject[] _weightPlot;
//    protected Track2D[] _weightTrack;
    protected int[] _weightTrackSize;
    
    protected JButton _selectedButton;
    protected boolean _selected;
    protected JMenuItem _selectMenuItem;
    
    protected boolean _colorColumn;
    protected Color _columnColor;

    protected JTextField _nameEditor;
    
    protected String _directoryPath;
    
    protected SystemSettings _settings;
}
