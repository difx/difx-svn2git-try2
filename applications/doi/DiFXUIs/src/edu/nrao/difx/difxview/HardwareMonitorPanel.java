/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.nrao.difx.difxview;

import mil.navy.usno.widgetlib.NodeBrowserScrollPane;
import mil.navy.usno.widgetlib.BrowserNode;
import mil.navy.usno.widgetlib.TearOffPanel;

import javax.swing.JLabel;

import java.math.BigDecimal;

import java.awt.Font;
import java.awt.Color;

import java.util.List;
import java.util.Iterator;

import edu.nrao.difx.difxdatamodel.*;
import edu.nrao.difx.difxcontroller.DiFXMessageProcessor;
import edu.nrao.difx.difxcontroller.AttributedMessageListener;

import edu.nrao.difx.xmllib.difxmessage.DifxMessage;

import edu.nrao.difx.difxdatamodel.Mark5Unit;
import edu.nrao.difx.difxdatamodel.ProcessorNode;
import java.util.ArrayList;

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
        _browserPane.setBounds( 0, 60, width, height - 60 );
        super.setBounds( x, y, width, height );
    }


    private void initComponents() {
        setLayout( null );
        _browserPane = new NodeBrowserScrollPane();
        this.add( _browserPane );
        _browserPane.setBackground( Color.WHITE );
        _mainLabel = new JLabel( "Hardware Monitor" );
        _mainLabel.setBounds( 5, 0, 150, 20 );
        _mainLabel.setFont( new Font( "Dialog", Font.BOLD, 14 ) );
        add( _mainLabel );
        _clusterNodes = new ClusterNodesHeader( "Processor Nodes" );
        _clusterNodes.backgroundColor( new Color( 255, 204, 153 ) );
        _browserPane.addNode( _clusterNodes );
        _mk5Modules = new Mark5NodesHeader( "Mark5 Modules" );
        _mk5Modules.backgroundColor( new Color( 255, 204, 153 ) );
        _browserPane.addNode( _mk5Modules );
    }
         
    /*
     * Sign up for callbacks from the DiFX message processor for different types
     * of messages.
     */
    public void difxMessageProcessor( DiFXMessageProcessor processor ) {
        processor.addDifxAlertMessageListener(new AttributedMessageListener() {
            @Override
            public void update( DifxMessage difxMsg ) {
                processDifxAlertMessage( difxMsg );
            }
        } );
        processor.addDifxLoadMessageListener(new AttributedMessageListener() {
            @Override
            public void update( DifxMessage difxMsg ) {
                processDifxLoadMessage( difxMsg );
            }
        } );
        processor.addMark5StatusMessageListener(new AttributedMessageListener() {
            @Override
            public void update( DifxMessage difxMsg ) {
                processMark5StatusMessage( difxMsg );
            }
        } );
    }
    
    protected void processDifxAlertMessage( DifxMessage difxMsg ) {
        //  Hardware alerts *appear* to only come from mk5daemon, so we key on it
        //  when deciding whether to use them.
        if ( difxMsg.getHeader().getIdentifier().trim().equals( "mk5daemon" ) )
            serviceUpdate( difxMsg );
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
        List<ProcessorNode> processorNodes = getProcessorNodes();
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
                        processor = new ClusterNode( thisProcessor.getObjName(), _settings );
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
        List<Mark5Unit> mark5Units = getMark5Units();        
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
                        mk5Module = new Mark5Node( thisMark5.getObjName(), _settings );
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
    protected SystemSettings _settings;
    
    //  REDUNDANT STUFF BELOW HERE (WE THINK)
    private List<Mark5Unit> mMark5Units = new ArrayList<Mark5Unit>(24);
    private List<ProcessorNode> mProcessorNodes = new ArrayList<ProcessorNode>(10);

        /**
     * Returns the list of mark5 units
     * @return
     */
    public List<Mark5Unit> getMark5Units() {
        return this.mMark5Units;
    }

    /**
     * Appends a mark5 unit to the list of mark5 units
     * @param m5Unit
     */
    public void addMark5Unit(Mark5Unit m5Unit) {
        mMark5Units.add(m5Unit);
    }

    /**
     * Returns a Mark5Unit object based on its name.
     * @param m5Name the name of the mark5 unit to return
     * @return the Mark5Unit object
     * @return null in case no matching mark5  units can be found
     */
    public synchronized Mark5Unit getMark5Unit(String m5Name) {
        Iterator it = mMark5Units.iterator();
        while (it.hasNext() == true) {
            Mark5Unit element = (Mark5Unit) it.next();
            if (element.getObjName().equalsIgnoreCase(m5Name)) {
                // found, return job
                return element;
            }
            element = null;
        }

        // not found
        return null;
    }

    /**
     * Returns the list of processor nodes
     * @return
     */
    public List<ProcessorNode> getProcessorNodes() {
        return this.mProcessorNodes;
    }

    /**
     * Appends a processing node to the current list of processing nodes
     * @param procNode
     */
    public void addProcessorNode(ProcessorNode procNode) {
        mProcessorNodes.add(procNode);
    }

    /**
     * Returns a ProcessorNode object based on its node node
     * @param procNodeName the name of the processing node
     * @return the ProcessorNode object that matches the given node name
     * @return null in case no matching processing node can be found
     */
    public synchronized ProcessorNode getProcessorNode(String procNodeName) {
        Iterator it = mProcessorNodes.iterator();
        while (it.hasNext() == true) {
            ProcessorNode element = (ProcessorNode) it.next();
            if (element.getObjName().equalsIgnoreCase(procNodeName)) {
                // found, return job
                return element;
            }
            element = null;
        }

        // not found
        return null;
    }

    public synchronized void processMark5StatusMessage(DifxMessage difxMsg) {
        // -- catch some exceptions and keep the program from terminating. . .
        //try {
            Mark5Unit mark5 = new Mark5Unit();
            mark5.setObjType("mark5");
            mark5.setObjName(difxMsg.getHeader().getFrom());
            mark5.setMsgSrcId(difxMsg.getHeader().getIdentifier());  // job id or mark5Daemon

            Mark5Unit existingM5 = getMark5Unit(difxMsg.getHeader().getFrom());
            if (existingM5 != null) {
                mark5.updateObject(existingM5);
            }

            // Update the rest of the fields with difx message
            if (mark5.getState().equalsIgnoreCase(difxMsg.getBody().getMark5Status().getState()) == true) {
                mark5.setStateChanged(false);
            } else {
                mark5.setStateChanged(true);
            }
            mark5.setState(difxMsg.getBody().getMark5Status().getState());
            mark5.setBankAVSN(difxMsg.getBody().getMark5Status().getBankAVSN());
            mark5.setBankBVSN(difxMsg.getBody().getMark5Status().getBankBVSN());
            mark5.setStatusWord(difxMsg.getBody().getMark5Status().getStatusWord());
            mark5.setActiveBank(difxMsg.getBody().getMark5Status().getActiveBank());
            mark5.setScanNumber(difxMsg.getBody().getMark5Status().getScanNumber());
            mark5.setScanName(difxMsg.getBody().getMark5Status().getScanName());
            mark5.setPosition(difxMsg.getBody().getMark5Status().getPosition());
            mark5.setPlayRate(difxMsg.getBody().getMark5Status().getPlayRate());
            mark5.setDataMJD(new BigDecimal(difxMsg.getBody().getMark5Status().getDataMJD().trim()));
            mark5.setCurrentJob(difxMsg.getHeader().getIdentifier());
            mark5.setStatusTimeStampUTC(); // current wall time UTC

            // Now update the actual data structures contained in the data model
            updateDataModel( mark5 );

            // clean up
            existingM5 = null;
            mark5 = null;

            serviceUpdate( difxMsg );

//        } catch (Exception e) {
//            System.err.println("uncaught exception: " + e);
//        }
    }

    /*
     * DiFX load messages come from CPUs, either "cluster nodes" (i.e. processors)
     * or the Mark5 systems.  At the moment (and this is gross, and will be fixed)
     * we are determining whether the source of a message is a cluster node or a
     * Mark5 unit by looking at its name.  Anything that is not a Mark5 is assumed
     * to be a cluster node.
     */
    public synchronized void processDifxLoadMessage(DifxMessage difxMsg) {
        // -- catch some exceptions and keep the program from terminating. . .
//        try {
            // Create DiFXObject from DifxMessage
            if (difxMsg.getHeader().getFrom().substring(0, 5).equalsIgnoreCase("mark5")) {
                // Create object to update the data model
                Mark5Unit mark5 = new Mark5Unit();
                mark5.setObjType("mark5");
                mark5.setObjName(difxMsg.getHeader().getFrom());
                mark5.setMsgSrcId(difxMsg.getHeader().getIdentifier());

                // -- Copy the existing fields not updated via this message
                Mark5Unit existingMark5 = getMark5Unit(difxMsg.getHeader().getFrom());
                if (existingMark5 != null) {
                    mark5.updateObject(existingMark5);
                }
                //System.out.println( difxMsg.getBody().getSeqNumber() );

                // Update the rest of the fields with difx message
                mark5.setCpuLoad(difxMsg.getBody().getDifxLoad().getCpuLoad());
                mark5.setTotalMem(difxMsg.getBody().getDifxLoad().getTotalMemory());
                mark5.setUsedMem(difxMsg.getBody().getDifxLoad().getUsedMemory());
                mark5.setMemLoad((float) difxMsg.getBody().getDifxLoad().getUsedMemory()
                        / difxMsg.getBody().getDifxLoad().getTotalMemory());
                mark5.setNetRxRate(difxMsg.getBody().getDifxLoad().getNetRXRate());
                mark5.setNetTxRate(difxMsg.getBody().getDifxLoad().getNetTXRate());
                mark5.setNumCores(difxMsg.getBody().getDifxLoad().getNCore());
                mark5.setCpuLoad( mark5.getCpuLoad() / (float)(mark5.getNumCores()) );
                //mark5.setStatusTimeStampUTC();

                // Now update the actual data structures contained in the data model
                updateDataModel(mark5);

                // clean up
                existingMark5 = null;
                mark5 = null;
            } 
            //else if (difxMsg.getHeader().getFrom().substring(0, 3).equalsIgnoreCase("swc")) {
            else {
                // Create object to update the data model
                ProcessorNode proc = new ProcessorNode();
                proc.setObjType("processor");
                proc.setObjName(difxMsg.getHeader().getFrom());
                proc.setMsgSrcId(difxMsg.getHeader().getIdentifier());

                // -- Copy the existing fields not updated via this message
                ProcessorNode existingProc = getProcessorNode(difxMsg.getHeader().getFrom());
                if (existingProc != null) {
                    proc.updateObject(existingProc);
                }

                // Update the rest of the fields with difx message
                proc.setState("Online"); // assume processor is  
                proc.setCpuLoad(difxMsg.getBody().getDifxLoad().getCpuLoad());
                proc.setTotalMem(difxMsg.getBody().getDifxLoad().getTotalMemory());
                proc.setUsedMem(difxMsg.getBody().getDifxLoad().getUsedMemory());
                proc.setMemLoad((float) difxMsg.getBody().getDifxLoad().getUsedMemory()
                        / difxMsg.getBody().getDifxLoad().getTotalMemory());
                proc.setNetRxRate(difxMsg.getBody().getDifxLoad().getNetRXRate());
                proc.setNetTxRate(difxMsg.getBody().getDifxLoad().getNetTXRate());
                proc.setNumCores(difxMsg.getBody().getDifxLoad().getNCore());
                proc.setCpuLoad( proc.getCpuLoad() / (float)(proc.getNumCores()) );
                proc.setStatusTimeStampUTC();

                // Now update the actual data structures contained in the data model
                boolean exists = false;
                Iterator it = mProcessorNodes.iterator();
                while ( ( !exists ) && it.hasNext() ) {
                    ProcessorNode element = (ProcessorNode)it.next();
                    if ( element.getObjName().equalsIgnoreCase( proc.getObjName() ) ) {
                        element.updateObject( proc );
                        exists = true;
                    }
                    element = null;
                }
                if ( !exists ) {
                    addProcessorNode( proc );
                }

                // clean up
                existingProc = null;
                proc = null;
            }
            
            
            serviceUpdate( difxMsg );

//        } catch (Exception e) {
//            System.err.println("uncaught exception: " + e);
//        }
    }

    // Update the data structure in the Data Model
    private synchronized void updateDataModel(DiFXObject difxObj) {

        if (difxObj.getObjType().equalsIgnoreCase("mark5")) {
            // -- update existing or insert new mark5
            boolean exists = false;

            // find the mark5
            Iterator it = mMark5Units.iterator();
            while ((!exists) && it.hasNext()) {
                Mark5Unit element = (Mark5Unit) it.next();
                if (element.getObjName().equalsIgnoreCase(difxObj.getObjName())) {
                    // update the data model - mark5 and re-associate to job/module
                    element.updateObject((Mark5Unit) difxObj);
                    exists = true;
                }
                element = null;
            }

            // insert new object
            if (!exists) {
                addMark5Unit((Mark5Unit) difxObj);
            }
        } else {
            System.out.println("DiFXDataModel.java(2247): " + difxObj.getObjType());
        }

    }


}
