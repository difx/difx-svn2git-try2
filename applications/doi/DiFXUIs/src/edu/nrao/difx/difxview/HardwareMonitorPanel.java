/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.nrao.difx.difxview;

import javax.swing.JPanel;
import javax.swing.JLabel;

import java.awt.Font;
import java.awt.Color;

import java.util.List;
import java.util.Iterator;

import edu.nrao.difx.difxdatamodel.*;
import edu.nrao.difx.difxcontroller.*;

/**
 *
 * @author jspitzak
 */
public class HardwareMonitorPanel extends JPanel {

    public HardwareMonitorPanel() {
        initComponents();
    }

    /*
     * This method allows me to control resize behavior.  Otherwise I have to
     * leave it up to the layouts, which is a disaster.
     */
    @Override
    public void setBounds(int x, int y, int width, int height) {
        _browserPane.setBounds( 0, 70, width, height - 70 );
        super.setBounds( x, y, width, height );
    }


    private void initComponents() {
        setLayout( null );
        _browserPane = new NodeBrowserScrollPane();
        this.add( _browserPane );
        _browserPane.setBackground( Color.WHITE );
        _mainLabel = new JLabel( "Hardware Monitor" );
        _mainLabel.setBounds( 5, 0, 150, 20 );
        _mainLabel.setFont( new Font( "Dialog", Font.BOLD, 12 ) );
        add( _mainLabel );
        _clusterNodes = new ClusterNodesHeader( "Processor Nodes" );
        _clusterNodes.backgroundColor( new Color( 255, 204, 153 ) );
        _browserPane.addNode( _clusterNodes );
        _mk5Modules = new Mark5NodesHeader( "Mark5 Modules" );
        _mk5Modules.backgroundColor( new Color( 255, 204, 153 ) );
        _browserPane.addNode( _mk5Modules );
        ActivityMonitorLight foo = new ActivityMonitorLight();
    }
    
    /*
     * Set the data model, which provides us with data from DiFX.
     */
    public void dataModel( DiFXDataModel newModel ) {
        _mDataModel = newModel;
        // create a listener that calls our local function
        _mListener = new MessageListener() {
            @Override
            public void update() {
                serviceDataUpdate();
            }
        };
        // hand DataModel a call back listener
        _mDataModel.attachListener( _mListener );
    }
    
    /*
     * The controller provides us a way of sending commands to DiFX
     */
    public void controller( DiFXController newController ) {
        _mController = newController;
    }
    
    /*
     * This method services a data message from the data model.  It adds information
     * about any hardware to appropriate lists, creating new nodes on the lists
     * as new hardware appears.
     */
    protected void serviceDataUpdate() {
        
        //  This would be unlikely...
        if ( _mDataModel == null )
            return;
        
        //  Get all processors (i.e. cluster nodes) the data model knows about.
        List<ProcessorNode> processorNodes = _mDataModel.getProcessorNodes();
        
        //  Change the displayed properties for each processor.
        if ( processorNodes != null ) {
            //  Run through each unit in the list of Mark5 modules and change their 
            //  displayed properties.
            for ( Iterator<ProcessorNode> iter = processorNodes.iterator(); iter.hasNext(); ) {
                ProcessorNode thisProcessor = iter.next();
                //  Find the node in our browser that represents this unit.
                ClusterNode processor = null;
                for ( Iterator<BrowserNode> iter2 = _clusterNodes.children().iterator(); iter2.hasNext(); ) {
                    BrowserNode thisModule = iter2.next();
                    if ( thisModule.name().equals( thisProcessor.getObjName() ) )
                        processor = (ClusterNode)thisModule;
                }
                //  If there was no node representing this unit, create one.
                if ( processor == null ) {
                    processor = new ClusterNode( thisProcessor.getObjName() );
                    processor.difxController( _mController );
                    _clusterNodes.addChild( processor );
                }
                //  Update the processor with new data.
                processor.setData( thisProcessor );
            }
        }
            
        
        //  Get all of the Mark5 units that the data model knows about.
        List<Mark5Unit> mark5Units = _mDataModel.getMark5Units();        
        
        //  Run through each unit in the list of Mark5 modules and change their 
        //  displayed properties.
        for ( Iterator<Mark5Unit> iter = mark5Units.iterator(); iter.hasNext(); ) {
            Mark5Unit thisMark5 = iter.next();
            //  This is to accomodate a seeming bug in mk5daemon, or somewhere else
            //  up the messaging pipeline.  If a processor is rebooted, messages relating
            //  to this action (i.e. "state = rebooting") come as if from a mark5,
            //  rather than a processor.  So we check the name of each "mark5" against
            //  the list of known processors first, and if found we assume the mark5
            //  is actually a processor.
            ClusterNode processor = null;
            for ( Iterator<BrowserNode> iter2 = _clusterNodes.children().iterator(); iter2.hasNext(); ) {
                BrowserNode thisModule = iter2.next();
                if ( thisModule.name().equals( thisMark5.getObjName() ) ) {
                    processor = (ClusterNode)thisModule;
                    processor.setData( thisMark5 );
                }
            }
            //  Now we may continue knowing this is a Mark 5.
            if ( processor == null ) {
                //  Find the node in our browser that represents this unit.
                Mark5Node mk5Module = null;
                for ( Iterator<BrowserNode> iter2 = _mk5Modules.children().iterator(); iter2.hasNext(); ) {
                    BrowserNode thisModule = iter2.next();
                    if ( thisModule.name().equals( thisMark5.getObjName() ) )
                        mk5Module = (Mark5Node)thisModule;
                }
                //  If there was no node representing this unit, create one.
                if ( mk5Module == null ) {
                    mk5Module = new Mark5Node( thisMark5.getObjName() );
                    mk5Module.difxController( _mController );
                    _mk5Modules.addChild( mk5Module );
                }
                mk5Module.setData( thisMark5 );
            }
        }
    }

    
    private NodeBrowserScrollPane _browserPane;
    protected BrowserNode _clusterNodes;
    protected BrowserNode _mk5Modules;
    private JLabel _mainLabel;
    DiFXDataModel  _mDataModel;
    DiFXController _mController;
    MessageListener _mListener;
    
}
