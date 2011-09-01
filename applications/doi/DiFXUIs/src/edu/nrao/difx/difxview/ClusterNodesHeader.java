/*
 * This class acts as a container for a list of cluster nodes (used in the
 * HardwareMonitorPanel).  It has a popup menu for activating or removing
 * different data displays and headers for each of them.
 */
package edu.nrao.difx.difxview;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JCheckBoxMenuItem;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import java.util.Iterator;

public class ClusterNodesHeader extends BrowserNode {
    
    public ClusterNodesHeader( String name ) {
        super( name );
        initializeDisplaySettings();
    }
    
    @Override
    public void createAdditionalItems() {
        _numCPUs = new ColumnTextArea( "CPUs" );
        this.add( _numCPUs );
        _numCores = new ColumnTextArea( "Cores" );
        this.add( _numCores );
        _bogusGHz = new ColumnTextArea( "GHz" );
        this.add( _bogusGHz );
        _type = new ColumnTextArea( "Type Code" );
        this.add( _type );
        _typeString = new ColumnTextArea( "Type" );
        this.add( _typeString );
        _state = new ColumnTextArea( "State" );
        this.add( _state );
        _enabled = new ColumnTextArea( "Enabled" );
        this.add( _enabled );
        _cpuLoad = new ColumnTextArea( "CPU Usage" );
        _cpuLoad.setToolTipText( "CPU Usage (%)" );
        this.add( _cpuLoad );
        _cpuLoadPlot = new ColumnTextArea( "CPU Usage" );
        _cpuLoadPlot.setToolTipText( "Plot of CPU Usage (%)" );
        this.add( _cpuLoadPlot );
        _usedMem = new ColumnTextArea( "Used Mem" );
        this.add( _usedMem );
        _totalMem = new ColumnTextArea( "Total Mem");
        this.add( _totalMem );
        _memLoad = new ColumnTextArea( "Mem Usage" );
        _memLoad.setToolTipText( "Memory Usage (%)" );
        this.add( _memLoad );
        _memLoadPlot = new ColumnTextArea( "Mem Usage" );
        _memLoadPlot.setToolTipText( "Plot of Memory Usage (%)" );
        this.add( _memLoadPlot );
        _netRxRate = new ColumnTextArea( "Rx Rate" );
        _netRxRate.setToolTipText( "Receive Rate (Mbits/s)" );
        this.add( _netRxRate );
        _netTxRate = new ColumnTextArea( "Tx Rate" );
        _netTxRate.setToolTipText( "Transmit Rate (Mbits/s)" );
        this.add( _netTxRate );
        //  Create a popup menu that allows us to turn things on and off
        _popup = new JPopupMenu();
        JMenuItem menuItem;
        menuItem = new JMenuItem( _label.getText() + " Display Options:" );
        _popup.add( menuItem );
        _popup.add( new JSeparator() );
        _broadcastMonitor = new JCheckBoxMenuItem( "Broadcast Monitor" );
        _broadcastMonitor.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _broadcastMonitor );
        _showNumCPUs = new JCheckBoxMenuItem( "CPUs" );
        _showNumCPUs.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showNumCPUs );
        _showNumCores = new JCheckBoxMenuItem( "Cores" );
        _showNumCores.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showNumCores );
        _showBogusGHz = new JCheckBoxMenuItem( "GHz" );
        _showBogusGHz.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showBogusGHz );
        _showType = new JCheckBoxMenuItem( "Type (Numeric)" );
        _showType.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showType );
        _showTypeString = new JCheckBoxMenuItem( "Type" );
        _showTypeString.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showTypeString );
        _showState = new JCheckBoxMenuItem( "State" );
        _showState.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showState );
        _showEnabled = new JCheckBoxMenuItem( "Enabled" );
        _showEnabled.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showEnabled );
        _showCpuLoad = new JCheckBoxMenuItem( "CPU Usage" );
        _showCpuLoad.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showCpuLoad );
        _showCpuLoadPlot = new JCheckBoxMenuItem( "CPU Usage Plot" );
        _showCpuLoadPlot.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showCpuLoadPlot );
        _showUsedMem = new JCheckBoxMenuItem( "Used Memory" );
        _showUsedMem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showUsedMem );
        _showTotalMem = new JCheckBoxMenuItem( "Total Memory" );
        _showTotalMem.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showTotalMem );
        _showMemLoad = new JCheckBoxMenuItem( "Memory Usage" );
        _showMemLoad.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showMemLoad );
        _showMemLoadPlot = new JCheckBoxMenuItem( "Memory Usage Plot" );
        _showMemLoadPlot.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showMemLoadPlot );
        _showNetRxRate = new JCheckBoxMenuItem( "Net Receive Rate" );
        _showNetRxRate.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showNetRxRate );
        _showNetTxRate = new JCheckBoxMenuItem( "Net Transmit Rate" );
        _showNetTxRate.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateDisplayedData();
            }
        });
        _popup.add( _showNetTxRate );
    }
    
    @Override
    public void positionItems() {
        super.positionItems();
        _xOff = 220;
        if ( _broadcastMonitor.getState() )
            _xOff += 14;
        if ( _showNumCPUs.getState() )
            setTextArea( _numCPUs, 70 );
        if ( _showNumCores.getState() )
            setTextArea( _numCores, 70 );
        if ( _showBogusGHz.getState() )
            setTextArea( _bogusGHz, 70 );
        if ( _showType.getState() )
            setTextArea( _type, 70 );
        if ( _showTypeString.getState() )
            setTextArea( _typeString, 70 );
        if ( _showState.getState() )
            setTextArea( _state, 70 );
        if ( _showEnabled.getState() )
            setTextArea( _enabled, 70 );
        if ( _showCpuLoad.getState() )
            setTextArea( _cpuLoad, 70 );
        if ( _showCpuLoadPlot.getState() ) {
            //  If the header "CPU Usage" is already displayed for the previous
            //  column, don't repeat it.
            if ( _showCpuLoad.getState() )
                _cpuLoadPlot.setText( "" );
            else
                _cpuLoadPlot.setText( "CPU Usage" );
            setTextArea( _cpuLoadPlot, 70 );
        }
        if ( _showUsedMem.getState() )
            setTextArea( _usedMem, 70 );
        if ( _showTotalMem.getState() )
            setTextArea( _totalMem, 70 );
        if ( _showMemLoad.getState() )
            setTextArea( _memLoad, 70 );
        if ( _showMemLoadPlot.getState() ) {
            //  As with the CPU plot - don't repeat a column header if it is
            //  already there.
            if ( _showMemLoad.getState() )
                _memLoad.setText( "" );
            else
                _memLoad.setText( "Mem Usage" );
            setTextArea( _memLoadPlot, 70 );
        }
        if ( _showNetRxRate.getState() )
            setTextArea( _netRxRate, 70 );
        if ( _showNetTxRate.getState() )
            setTextArea( _netTxRate, 70 );
    }
    
    public void setTextArea( Component area, int xSize ) {
        area.setBounds( _xOff + 1, 1, xSize - 2, _ySize - 2);
        _xOff += xSize;
    }

    @Override
    public void paintComponent( Graphics g ) {
        //  Use anti-aliasing on the text (looks much better)
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                     RenderingHints.VALUE_ANTIALIAS_ON );
        super.paintComponent( g );
    }
    
    public void initializeDisplaySettings() {
        _broadcastMonitor.setState( true );
        _showNumCPUs.setState( false );
        _showNumCores.setState( false );
        _showBogusGHz.setState( false );
        _showType.setState( false );
        _showTypeString.setState( false );
        _showState.setState( true );
        _showEnabled.setState( false );
        _showCpuLoad.setState( false );
        _showCpuLoadPlot.setState( true );
        _showUsedMem.setState( false );
        _showTotalMem.setState( false );
        _showMemLoad.setState( false );
        _showMemLoadPlot.setState( true );
        _showNetRxRate.setState( true );
        _showNetTxRate.setState( true );
    }
    
    @Override
    public void addChild( BrowserNode newNode ) {
        super.addChild( newNode );
        updateDisplayedData();
    }
    
    public void updateDisplayedData() {
        //  Run through the list of all "child" nodes, which are all of the listed
        //  cluster nodes.
        for ( Iterator<BrowserNode> iter = _children.iterator(); iter.hasNext(); ) {
            ClusterNode thisNode = (ClusterNode)(iter.next());
            //  Change the settings on these items to match our current specifications.
            thisNode.showNetworkActivity( _broadcastMonitor.getState() );
            thisNode.showNumCPUs( _showNumCPUs.getState() );
            thisNode.showNumCores( _showNumCores.getState() );
            thisNode.showBogusGHz( _showBogusGHz.getState() );
            thisNode.showType( _showType.getState() );
            thisNode.showTypeString( _showTypeString.getState() );
            thisNode.showState( _showState.getState() );
            thisNode.showEnabled( _showEnabled.getState() );
            thisNode.showCpuLoad( _showCpuLoad.getState() );
            thisNode.showCpuLoadPlot( _showCpuLoadPlot.getState() );
            thisNode.showUsedMem( _showUsedMem.getState() );
            thisNode.showTotalMem( _showTotalMem.getState() );
            thisNode.showMemLoad( _showMemLoad.getState() );
            thisNode.showMemLoadPlot( _showMemLoadPlot.getState() );
            thisNode.showNetRxRate( _showNetRxRate.getState() );
            thisNode.showNetTxRate( _showNetTxRate.getState() );
            thisNode.updateUI();
        }
        //  Update the headers as well.
        _numCPUs.setVisible( _showNumCPUs.getState() );
        _numCores.setVisible( _showNumCores.getState() );
        _bogusGHz.setVisible( _showBogusGHz.getState() );
        _type.setVisible( _showType.getState() );
        _typeString.setVisible( _showTypeString.getState() );
        _state.setVisible( _showState.getState() );
        _enabled.setVisible( _showEnabled.getState() );
        _cpuLoad.setVisible( _showCpuLoad.getState() );
        _cpuLoadPlot.setVisible( _showCpuLoadPlot.getState() );
        _usedMem.setVisible( _showUsedMem.getState() );
        _totalMem.setVisible( _showTotalMem.getState() );
        _memLoad.setVisible( _showMemLoad.getState() );
        _memLoadPlot.setVisible( _showMemLoadPlot.getState() );
        _netRxRate.setVisible( _showNetRxRate.getState() );
        _netTxRate.setVisible( _showNetTxRate.getState() );
        this.updateUI();
        
    }
    
    protected JCheckBoxMenuItem _broadcastMonitor;
    protected JCheckBoxMenuItem _showNumCPUs;
    protected JCheckBoxMenuItem _showNumCores;
    protected JCheckBoxMenuItem _showBogusGHz;
    protected JCheckBoxMenuItem _showType;
    protected JCheckBoxMenuItem _showTypeString;
    protected JCheckBoxMenuItem _showState;
    protected JCheckBoxMenuItem _showEnabled;
    protected JCheckBoxMenuItem _showCpuLoad;
    protected JCheckBoxMenuItem _showCpuLoadPlot;
    protected JCheckBoxMenuItem _showUsedMem;
    protected JCheckBoxMenuItem _showTotalMem;
    protected JCheckBoxMenuItem _showMemLoad;
    protected JCheckBoxMenuItem _showMemLoadPlot;
    protected JCheckBoxMenuItem _showNetRxRate;
    protected JCheckBoxMenuItem _showNetTxRate;
    
    ColumnTextArea _numCPUs;
    ColumnTextArea _numCores;
    ColumnTextArea _bogusGHz;
    ColumnTextArea _type;
    ColumnTextArea _typeString;
    ColumnTextArea _state;
    ColumnTextArea _enabled;
    ColumnTextArea _cpuLoad;
    ColumnTextArea _cpuLoadPlot;
    ColumnTextArea _usedMem;
    ColumnTextArea _totalMem;
    ColumnTextArea _memLoad;
    ColumnTextArea _memLoadPlot;
    ColumnTextArea _netRxRate;
    ColumnTextArea _netTxRate;

    private int _xOff;
    
}
