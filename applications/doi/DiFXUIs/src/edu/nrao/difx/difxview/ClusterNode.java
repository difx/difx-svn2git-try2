/*
 * This is a BrowserNode for a cluster processing module.  In much of the GUI
 * these are referred to as "Processor Nodes", however there was already a
 * ProcessorNode class so I couldn't use that name.
 */
package edu.nrao.difx.difxview;

import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.Component;

import java.text.DecimalFormat;

import edu.nrao.difx.difxdatamodel.*;

import mil.navy.usno.plotlib.PlotWindow;
import mil.navy.usno.plotlib.Plot2DObject;
import mil.navy.usno.plotlib.Track2D;

/**
 *
 * @author jspitzak
 */
public class ClusterNode extends BrowserNode {
    
    public ClusterNode( String name ) {
        super( name );
        _columnColor = new Color( 204, 204, 255 );
        _dec = new DecimalFormat();
    }
    
    @Override
    public void createAdditionalItems() {
        //  This is the data monitor light.
        _networkActivity = new ActivityMonitorLight();
        this.add( _networkActivity );
        _numCPUs = new ColumnTextArea();
        _numCPUs.justify( ColumnTextArea.RIGHT );
        this.add( _numCPUs );
        _numCores = new ColumnTextArea();
        _numCores.justify( ColumnTextArea.RIGHT );
        this.add( _numCores );
        _bogusGHz = new ColumnTextArea();
        _bogusGHz.justify( ColumnTextArea.RIGHT );
        this.add( _bogusGHz );
        _type = new ColumnTextArea();
        _type.justify( ColumnTextArea.RIGHT );
        this.add( _type );
        _typeString = new ColumnTextArea();
        _typeString.justify( ColumnTextArea.CENTER );
        this.add( _typeString );
        _state = new ColumnTextArea();
        _state.justify( ColumnTextArea.CENTER );
        this.add( _state );
        _enabledLight = new ActivityMonitorLight();
        this.add( _enabledLight );
        _cpuLoad = new ColumnTextArea();
        _cpuLoad.justify( ColumnTextArea.RIGHT );
        this.add( _cpuLoad );
        _cpuLoadPlot = new PlotWindow();
        this.add( _cpuLoadPlot );
        _cpuPlot = new Plot2DObject();
        _cpuLoadPlot.add2DPlot( _cpuPlot );
        _cpuPlot.name( "CPU Plot" );
        _cpuPlot.drawBackground( true );
        _cpuPlot.drawFrame( true );
        _cpuPlot.frameColor( Color.GRAY );
        _cpuPlot.clip( true );
        _cpuPlot.addTopGrid( Plot2DObject.X_AXIS, 10.0, Color.BLACK );
        _cpuTrack = new Track2D();
        _cpuTrack.fillCurve( true );
        _cpuPlot.addTrack( _cpuTrack );
        _cpuTrack.color( Color.GREEN );
        _cpuTrack.sizeLimit( 200 );
        _cpuPlot.frame( 0.0, 0.0, 1.0, 1.0 );
        _cpuPlot.backgroundColor( Color.BLACK );
        _usedMem = new ColumnTextArea();
        _usedMem.justify( ColumnTextArea.RIGHT );
        this.add( _usedMem );
        _totalMem = new ColumnTextArea();
        _totalMem.justify( ColumnTextArea.RIGHT );
        this.add( _totalMem );
        _memLoad = new ColumnTextArea();
        _memLoad.justify( ColumnTextArea.RIGHT );
        this.add( _memLoad );
        _memLoadPlot = new PlotWindow();
        this.add( _memLoadPlot );
        _memPlot = new Plot2DObject();
        _memLoadPlot.add2DPlot( _memPlot );
        _memPlot.name( "Mem Plot" );
        _memPlot.drawBackground( true );
        _memPlot.drawFrame( true );
        _memPlot.frameColor( Color.GRAY );
        _memPlot.clip( true );
        _memPlot.addTopGrid( Plot2DObject.X_AXIS, 10.0, Color.BLACK );
        _memTrack = new Track2D();
        _memTrack.fillCurve( true );
        _memPlot.addTrack( _memTrack );
        _memTrack.color( Color.GREEN );
        _memTrack.sizeLimit( 200 );
        _memPlot.frame( 0.0, 0.0, 1.0, 1.0 );
        _memPlot.backgroundColor( Color.BLACK );
        _netRxRate = new ColumnTextArea();
        _netRxRate.justify( ColumnTextArea.RIGHT );
        this.add( _netRxRate );
        _netTxRate = new ColumnTextArea();
        _netTxRate.justify( ColumnTextArea.RIGHT );
        this.add( _netTxRate );
        //  Create a popup menu appropriate to a "job".
        _popup = new JPopupMenu();
        JMenuItem menuItem;
        menuItem = new JMenuItem( "Show Monitor for \"" + _label.getText() + "\"" );
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                monitorAction( e );
            }
        });
        _popup.add( menuItem );
    }
    
    @Override
    public void positionItems() {
        _xOff = _level * 20;
        //  The network activity light is positioned to the left of the name
        if ( _showNetworkActivity ) {
            _networkActivity.setBounds( _xOff, 6, 10, 10 );
            _xOff += 14;
        }
        //  We always show the name of the node
        _label.setBounds( _xOff, 0, 180, _ySize );
        _xOff += 180;
        //  Then everything else.  For items that are simply values (as opposed
        //  to plots) we show labels backed by alternating colors so the columns
        //  will be easy to follow.
        _colorColumn = false;
        if( _showNumCPUs )
            setTextArea( _numCPUs, 70 );
        if ( _showNumCores )
            setTextArea( _numCores, 70 );
        if ( _showBogusGHz )
            setTextArea( _bogusGHz, 70 );
        if ( _showType )
            setTextArea( _type, 70 );
        if ( _showTypeString )
            setTextArea( _typeString, 70 );
        if ( _showState ) {
            _state.setBounds( _xOff, 1, 70, _ySize - 2);
            if ( _state.getText().equals( "Lost" ) )
                _state.setBackground( Color.RED );
            else
                _state.setBackground( Color.GREEN );
            _xOff += 70;
            _colorColumn = false;
        }
        if ( _showEnabled ) {
            _enabledLight.setBounds( _xOff + 30, 6, 10, 10 );
            _xOff += 70;
            _colorColumn = false;
        }
        if ( _showCpuLoad )
            setTextArea( _cpuLoad, 70 );
        if ( _showCpuLoadPlot ) {
            _cpuLoadPlot.setBounds( _xOff, 1, 70, _ySize - 2 );
            _cpuPlot.resizeBasedOnWindow( 70, _ySize - 2 );
            _xOff += 70;
            _colorColumn = false;
        }
        if ( _showUsedMem )
            setTextArea( _usedMem, 70 );
        if ( _showTotalMem )
            setTextArea( _totalMem, 70 );
        if ( _showMemLoad )
            setTextArea( _memLoad, 70 );
        if ( _showMemLoadPlot ) {
            _memLoadPlot.setBounds( _xOff, 1, 70, _ySize - 2 );
            _memPlot.resizeBasedOnWindow( 70, _ySize - 2 );
            _xOff += 70;
            _colorColumn = false;
        }
        if ( _showNetRxRate )
            setTextArea( _netRxRate, 70 );
        if ( _showNetTxRate )
            setTextArea( _netTxRate, 70 );
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
     * Show the "monitor" window.  If one has not been created yet, create it first.
     */
    public void monitorAction( ActionEvent e ) {
        if ( _monitor == null ) {
            _monitor = new ProcessorMonitorWindow();
            _monitor.setTitle( _label.getText() );
        }
        _monitor.setVisible( true );
    }
    
    public void showNetworkActivity( boolean newVal ) {
        _showNetworkActivity = newVal;
        _networkActivity.setVisible( _showNetworkActivity );            
    }
    
    public void showNumCPUs( boolean newVal ) {
        _showNumCPUs = newVal;
        _numCPUs.setVisible( newVal );
    }
    
    public void showNumCores( boolean newVal ) {
        _showNumCores = newVal;
        _numCores.setVisible( newVal );
    }
    
    public void showBogusGHz( boolean newVal ) {
        _showBogusGHz = newVal;
        _bogusGHz.setVisible( newVal );
    }

    public void showType( boolean newVal ) {
        _showType = newVal;
        _type.setVisible( newVal );
    }

    public void showTypeString( boolean newVal ) {
        _showTypeString = newVal;
        _typeString.setVisible( newVal );
    }

    public void showState( boolean newVal ) {
        _showState = newVal;
        _state.setVisible( newVal );
    }

    public void showEnabled( boolean newVal ) {
        _showEnabled = newVal;
        _enabledLight.setVisible( newVal );
    }

    public void showCpuLoad( boolean newVal ) {
        _showCpuLoad = newVal;
        _cpuLoad.setVisible( newVal );
    }

    public void showCpuLoadPlot( boolean newVal ) {
        _showCpuLoadPlot = newVal;
        _cpuLoadPlot.setVisible( newVal );            
    }

    public void showUsedMem( boolean newVal ) {
        _showUsedMem = newVal;
        _usedMem.setVisible( newVal );
    }

    public void showTotalMem( boolean newVal ) {
        _showTotalMem = newVal;
        _totalMem.setVisible( newVal );
    }

    public void showMemLoad( boolean newVal ) {
        _showMemLoad = newVal;
        _memLoad.setVisible( newVal );
    }

    public void showMemLoadPlot( boolean newVal ) {
        _showMemLoadPlot = newVal;
        _memLoadPlot.setVisible( newVal );            
    }

    public void showNetRxRate( boolean newVal ) {
        _showNetRxRate = newVal;
        _netRxRate.setVisible( newVal );
    }

    public void showNetTxRate( boolean newVal ) {
        _showNetTxRate = newVal;
        _netTxRate.setVisible( newVal );
    }

     /*
     * Use the data contained in a "ProcessorNode" (from the difxdatamodel) to
     * set fields here.  These are all individually formatted.
     */
    public void setData( ProcessorNode dataNode ) {
        _networkActivity.data();
        _numCPUs.setText( "" + dataNode.getNumCPUs() );
        _numCores.setText( "" + dataNode.getNumCores() );
        _bogusGHz.setText( "" + dataNode.getBogusGHz() );
        _type.setText( "" + dataNode.getType() );
        _typeString.setText( "" + dataNode.getTypeString() );
        if ( dataNode.isStatusCurrent() )
            _state.setText( "" + dataNode.getState() );
        else
            _state.setText( "Lost" );
        _cpuLoad.setText( String.format( "%10.1f", 100.0 * dataNode.getCpuLoad() ) );
        _cpuPlot.limits( (double)(_cpuTrackSize - 100), (double)(_cpuTrackSize), 0.0, 100.0 );
        _cpuTrack.add( (double)(_cpuTrackSize), 100.0 * dataNode.getCpuLoad() );
        _cpuTrackSize += 1;
        _cpuLoadPlot.updateUI();
        _enabledLight.on( dataNode.getEnabled() );
        _memLoad.setText( String.format( "%10.1f", 100.0 * dataNode.getMemLoad() ) );
        _memPlot.limits( (double)(_memTrackSize - 100), (double)(_memTrackSize), 0.0, 100.0 );
        _memTrack.add( (double)(_memTrackSize), 100.0 * dataNode.getMemLoad() );
        _memTrackSize += 1;
        _memLoadPlot.updateUI();
        _totalMem.setText( String.format( "%10d", dataNode.getTotalMem() ) );
        _usedMem.setText( String.format( "%10d", dataNode.getUsedMem() ) );
        //  Convert transmit and receive rates to Mbits/second (instead of Bytes/sec).
        double newRx = 8.0 * (double)(dataNode.getNetRxRate()) / 1024.0 / 1024.0;
        double newTx = 8.0 * (double)(dataNode.getNetTxRate()) / 1024.0 / 1024.0;
        _netRxRate.setText( String.format( "%10.3f", newRx ) );
        _netTxRate.setText( String.format( "%10.3f", newTx ) );
        //  Use the same data to update the monitor window.
        if ( _monitor != null ) {
            _monitor.setNumCPUs( dataNode.getNumCPUs() );
            _monitor.setNumCores( dataNode.getNumCores() );
            _monitor.setBogusGHz( dataNode.getBogusGHz() );
            _monitor.setType( dataNode.getType() );
            _monitor.setTypeString( dataNode.getTypeString() );
            if ( dataNode.isStatusCurrent() )
                    _monitor.setState( dataNode.getState() );
            else
                    _monitor.setState( "Lost" );
            _monitor.setCpuLoad( (float)100.0 * dataNode.getCpuLoad() );
            _monitor.setSysEnabled( dataNode.getEnabled() );
            _monitor.setMemLoad( (float)100.0 * dataNode.getMemLoad() );
            _monitor.setTotalMem( dataNode.getTotalMem() );
            _monitor.setUsedMem( dataNode.getUsedMem() );
            _monitor.setNetRxRate( newRx );
            _monitor.setNetTxRate( newTx );
        }
   }
    
    ProcessorMonitorWindow _monitor;
    ActivityMonitorLight _networkActivity;
    boolean _showNetworkActivity;
    ColumnTextArea _numCPUs;
    boolean _showNumCPUs;
    ColumnTextArea _numCores;
    boolean _showNumCores;
    ColumnTextArea _bogusGHz;
    boolean _showBogusGHz;
    ColumnTextArea _type;
    boolean _showType;
    ColumnTextArea _typeString;
    boolean _showTypeString;
    ColumnTextArea _state;
    boolean _showState;
    boolean _showEnabled;
    ActivityMonitorLight _enabledLight;
    ColumnTextArea _cpuLoad;
    boolean _showCpuLoad;
    PlotWindow _cpuLoadPlot;
    Plot2DObject _cpuPlot;
    Track2D _cpuTrack;
    int _cpuTrackSize;
    boolean _showCpuLoadPlot;
    ColumnTextArea _usedMem;
    boolean _showUsedMem;
    ColumnTextArea _totalMem;
    boolean _showTotalMem;
    ColumnTextArea _memLoad;
    boolean _showMemLoad;
    PlotWindow _memLoadPlot;
    Plot2DObject _memPlot;
    Track2D _memTrack;
    int _memTrackSize;
    boolean _showMemLoadPlot;
    ColumnTextArea _netRxRate;
    boolean _showNetRxRate;
    ColumnTextArea _netTxRate;
    boolean _showNetTxRate;
    
    Color _columnColor;
    boolean _colorColumn;
    int _xOff;
    DecimalFormat _dec;

}
