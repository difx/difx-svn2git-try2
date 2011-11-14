/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.nrao.difx.difxview;

import mil.navy.usno.widgetlib.NodeBrowserScrollPane;
import mil.navy.usno.widgetlib.BrowserNode;
import mil.navy.usno.widgetlib.TearOffPanel;

import javax.swing.JLabel;

import java.awt.Font;
import java.awt.Color;

import java.util.List;
import java.util.Iterator;

import edu.nrao.difx.difxdatamodel.*;
import edu.nrao.difx.difxcontroller.*;

import edu.nrao.difx.xmllib.difxmessage.DifxMessage;

/**
 *
 * @author jspitzak
 */
public class HardwareMonitorPanel extends TearOffPanel {

    public HardwareMonitorPanel( SystemSettings settings ) {
        _settings = settings;
        _settings.hardwareMonitor( this );
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
    }
         
    /*
     * Set the data model, which provides us with data from DiFX.
     */
    public void dataModel( DiFXDataModel newModel ) {
        _mDataModel = newModel;
        // create a listener that calls our local function
//        _mListener = new MessageListener() {
//            @Override
//            public void update() {
//                serviceDataUpdate();
//            }
//        };
//        // hand DataModel a call back listener
//        _mDataModel.attachListener( _mListener );
        _mDataModel.addHardwareMessageListener( new AttributedMessageListener() {
            @Override
            public void update( DifxMessage data ) {
                serviceUpdate( data );
            }
        } );
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
    
    /*
     * A message has been received from the named processor/mark5.  Update the
     * data associated with it.
     */
    protected void serviceUpdate( DifxMessage difxMsg ) {
        
        //  We've got the entire message to deal with here...maybe we want to do
        //  more than this with it?
        String nodeName = difxMsg.getHeader().getFrom();
        
        //  First see if the data model considers this a "processor" by locating
        //  it in the data model's list of processors.
        List<ProcessorNode> processorNodes = _mDataModel.getProcessorNodes();
        if ( processorNodes != null ) {
            for ( Iterator<ProcessorNode> iter = processorNodes.iterator(); iter.hasNext(); ) {
                ProcessorNode thisProcessor = iter.next();
                if ( nodeName.equals( thisProcessor.getObjName() ) ) {
                    //  Okay, we've found it.  Now locate it in OUR list of processors, if
                    //  we know about it.
                    ClusterNode processor = null;
                    for ( Iterator<BrowserNode> iter2 = _clusterNodes.children().iterator(); iter2.hasNext(); ) {
                        BrowserNode thisModule = iter2.next();
                        if ( thisModule.name().equals( thisProcessor.getObjName() ) )
                            processor = (ClusterNode)thisModule;
                    }
                    //  If there was no node representing this unit in our list, create one.
                    if ( processor == null ) {
                        processor = new ClusterNode( thisProcessor.getObjName() );
                        processor.difxController( _mController );
                        _clusterNodes.addChild( processor );
                    }
                    //  If this is an "alert" relating to this processor, send it to the
                    //  processing node.  Otherwise, update the processor with new data
                    //  from the data base.
                    if ( difxMsg.getBody().getDifxAlert() != null )
                        processor.newAlert( difxMsg );
                    else
                        processor.setData( thisProcessor );
                    //  We are done if we've made it here - bail out.
                    return;
                }
            }
        }
        
        //  If a processor was not found, try the list of Mark5 units.
        List<Mark5Unit> mark5Units = _mDataModel.getMark5Units();        
        if ( mark5Units != null ) {
            for ( Iterator<Mark5Unit> iter = mark5Units.iterator(); iter.hasNext(); ) {
                Mark5Unit thisMark5 = iter.next();
                if ( nodeName.equals( thisMark5.getObjName() ) ) {
                    //  Found our node name in the Mark 5 list.  Now locate it in our
                    //  lists.  First,  to accomodate a seeming bug in mk5daemon, or somewhere else
                    //  up the messaging pipeline, we try our list of processors.  If a processor
                    //  is rebooted, messages relating to this action (i.e. "state = rebooting")
                    //  come as if from a mark5, rather than a processor.  So we check the name of 
                    //  each "mark5" against the list of known processors first, and if found we 
                    //  assume the mark5 is actually a processor.
                    ClusterNode processor = null;
                    for ( Iterator<BrowserNode> iter2 = _clusterNodes.children().iterator(); iter2.hasNext(); ) {
                        BrowserNode thisModule = iter2.next();
                        if ( thisModule.name().equals( thisMark5.getObjName() ) ) {
                            processor = (ClusterNode)thisModule;
                            processor.setData( thisMark5 );
                            return;
                        }
                    }
                    //  Now we may continue knowing this is a Mark 5.
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
                    //  If this is an "alert" relating to this mark5, send it to the
                    //  processing node.  Otherwise, update the mark5 with new data
                    //  from the data base.
                    if ( difxMsg.getBody().getDifxAlert() != null )
                        mk5Module.newAlert( difxMsg );
                    else
                        mk5Module.setData( thisMark5 );
                    return;
                }
            }
        }

    }
    
    public BrowserNode clusterNodes() { return _clusterNodes; }
    public BrowserNode mk5Modules() { return _mk5Modules; }

    
    private NodeBrowserScrollPane _browserPane;
    protected BrowserNode _clusterNodes;
    protected BrowserNode _mk5Modules;
    private JLabel _mainLabel;
    DiFXDataModel  _mDataModel;
    DiFXController _mController;
    MessageListener _mListener;
    protected SystemSettings _settings;
    
}
