/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.nrao.difx.difxdatamodel;

import edu.nrao.difx.difxview.SystemSettings;
import mil.navy.usno.widgetlib.MessageDisplayPanel;
import edu.nrao.difx.xmllib.difxmessage.*;

import java.awt.Toolkit;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.event.EventListenerList;
/**
 *
 * @author mguerra
 */
public class DiFXDataModel {

    private List<Mark5Unit> mMark5Units = new ArrayList<Mark5Unit>(24);
    private List<ProcessorNode> mProcessorNodes = new ArrayList<ProcessorNode>(10);
    //  Listeners for different types of incoming data
    EventListenerList _hardwareMessageListeners;
    EventListenerList _jobMessageListeners;
    // internal message collection/display
//    private MessageDisplayPanel _messageDisplayPanel;
    
    // Contains all settings used to run the GUI
    SystemSettings _systemSettings;

    public DiFXDataModel( SystemSettings newSettings ) {
        _hardwareMessageListeners = new EventListenerList();
        _jobMessageListeners = new EventListenerList();
        _systemSettings = newSettings;
    }

    public void addHardwareMessageListener( AttributedMessageListener a ) {
        _hardwareMessageListeners.add( AttributedMessageListener.class, a );
    }

    public void addJobMessageListener( AttributedMessageListener a ) {
        _jobMessageListeners.add( AttributedMessageListener.class, a );
    }

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
     * Returns the mark5 unit that contains the module with the given vsn
     * @param vsn the module vsn to search for
     * @return the Mark5Unit object that corresponds to the found unit
     * @return null in case no matching mark5 unit can be found
     */
    public synchronized Mark5Unit getMark5UnitViaVSN(String vsn) {
        Iterator it = mMark5Units.iterator();
        while (it.hasNext() == true) {
            Mark5Unit element = (Mark5Unit) it.next();
            if (element.getBankAVSN() != null
                    && element.getBankBVSN() != null) {
                if (element.getBankAVSN().equalsIgnoreCase(vsn)
                        || element.getBankBVSN().equalsIgnoreCase(vsn)) {
                    // found, return job
                    return element;
                }
            }
            element = null;
        }

        // not found
        return null;
    }

    /**
     * Returns the list of processinf nodes
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

    // Process the DifxMessage into the Data Model
    //    This method is always called via the DiFX Controller
    public synchronized void serviceDataModel(DifxMessage difxMsg) {
        // -- Convert a DifxMessage into a DiFXObject

        // Determine the type of message and send to the appropriate processor
        //BLAT
        Header header = difxMsg.getHeader();
        //System.out.println( header.getFrom() );
        //System.out.println( "         " + header.getType() );

        if (header.getType().equalsIgnoreCase("DifxStatusMessage")) {
            //java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.INFO, "DifxStatusMessage");
            processDifxStatusMessage(difxMsg);

        } else if (header.getType().equalsIgnoreCase("Mark5StatusMessage")) {
            //java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.INFO, "Mark5StatusMessage");
            processMark5StatusMessage(difxMsg);

        } else if (header.getType().equalsIgnoreCase("DifxLoadMessage")) {
            //java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.INFO, "DifxLoadMessage");
            processDifxLoadMessage(difxMsg);

        } else if (header.getType().equalsIgnoreCase("DifxAlertMessage")) {
            //java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.INFO, "DifxAlertMessage");
            processDifxAlertMessage(difxMsg);

        } else {
            if ( !_systemSettings.suppressWarnings() ) {
                java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.WARNING, "unknown DiFX message: \""
                        + header.getType() + "\"");
            }
        }

        // clean up
        header = null;
    }

    private synchronized void processDifxStatusMessage(DifxMessage difxMsg) {
        
        Object[] listeners = _jobMessageListeners.getListenerList();
        int numListeners = listeners.length;
        for ( int i = 0; i < numListeners; i+=2 ) {
            if ( listeners[i] == AttributedMessageListener.class )
                ((AttributedMessageListener)listeners[i+1]).update( difxMsg );
        }

    }

    private synchronized void processMark5StatusMessage(DifxMessage difxMsg) {
        // -- catch some exceptions and keep the program from terminating. . .
        try {
            // Create DiFXObject from DifxMessage

            //  Why are these "doi" sourced messages considered "Mark5 Status" messages???
            //  They come from the GUI!  Beyond that, why do we DO this??  We produce
            //  This stupid message, then we capture it??  
            if (difxMsg.getHeader().getFrom().substring(0, 3).equalsIgnoreCase("doi")) {
                // create alert text
                String lostString = "";
                if (difxMsg.getBody().getMark5Status().getState().equalsIgnoreCase("lost")) {
                    lostString = " connection and not responding.";
                    Toolkit.getDefaultToolkit().beep();
                    Thread.sleep(250);
                    Toolkit.getDefaultToolkit().beep();
                }
                String alertMessage = difxMsg.getHeader().getIdentifier() + " "
                        + difxMsg.getBody().getMark5Status().getState()
                        + lostString;

                DifxMessage difxAlertMsg = CreateDiFXAlertMessage(alertMessage);
                this.serviceDataModel(difxAlertMsg);
            }
            else {//if (difxMsg.getHeader().getFrom().substring(0, 5).equalsIgnoreCase("mark5")) {
                // Create object to update the data model
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
                updateDataModel(mark5);

                // clean up
                existingM5 = null;
                mark5 = null;
            }

            //  Dispatch a message to each of the listeners interested in the
            //  receipt of a hardware-related message.
            Object[] listeners = _hardwareMessageListeners.getListenerList();
            int numListeners = listeners.length;
            for ( int i = 0; i < numListeners; i+=2 ) {
                if ( listeners[i] == AttributedMessageListener.class )
                    ((AttributedMessageListener)listeners[i+1]).update( difxMsg );
            }

        } catch (Exception e) {
            System.err.println("uncaught exception: " + e);
        }
    }

    /*
     * DiFX load messages come from CPUs, either "cluster nodes" (i.e. processors)
     * or the Mark5 systems.  At the moment (and this is gross, and will be fixed)
     * we are determining whether the source of a message is a cluster node or a
     * Mark5 unit by looking at its name.  Anything that is not a Mark5 is assumed
     * to be a cluster node.
     */
    private synchronized void processDifxLoadMessage(DifxMessage difxMsg) {
        // -- catch some exceptions and keep the program from terminating. . .
        try {
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
                updateDataModel(proc);

                // clean up
                existingProc = null;
                proc = null;
            }
            
            
            //  Dispatch a message to each of the listeners interested in the
            //  receipt of a hardware-related message.
            Object[] listeners = _hardwareMessageListeners.getListenerList();
            int numListeners = listeners.length;
            for ( int i = 0; i < numListeners; i+=2 ) {
                if ( listeners[i] == AttributedMessageListener.class )
                    ((AttributedMessageListener)listeners[i+1]).update( difxMsg );
            }

        } catch (Exception e) {
            System.err.println("uncaught exception: " + e);
        }
    }

    private synchronized void processDifxAlertMessage(DifxMessage difxMsg) {
        
        //  Dispatch a message to each of the listeners interested in alerts.  Most
        //  alerts have information about jobs, but some are from hardware.  The
        //  hardware alerts *appear* to only come from mk5daemon, so we key on it
        //  when deciding where to send them.
        if ( difxMsg.getHeader().getIdentifier().trim().equals( "mk5daemon" ) ) {
            Object[] listeners = _hardwareMessageListeners.getListenerList();
            int numListeners = listeners.length;
            for ( int i = 0; i < numListeners; i+=2 ) {
                if ( listeners[i] == AttributedMessageListener.class )
                    ((AttributedMessageListener)listeners[i+1]).update( difxMsg );
            }
        }
        else if ( difxMsg.getHeader().getIdentifier().trim().equals( "doi" ) ) {
        }
        else {
            Object[] listeners = _jobMessageListeners.getListenerList();
            int numListeners = listeners.length;
            for ( int i = 0; i < numListeners; i+=2 ) {
                if ( listeners[i] == AttributedMessageListener.class )
                    ((AttributedMessageListener)listeners[i+1]).update( difxMsg );
            }
        }
        
        //  Send this alert to the internal messaging system, unless we don't
        //  have access to it - in which case we simply use the logging
        //  system.
        if ((difxMsg.getBody().getDifxAlert().getSeverity() >= 0)
                && (difxMsg.getBody().getDifxAlert().getSeverity() <= 4)) {

            if ( _systemSettings.messageCenter() != null) {
                if (difxMsg.getBody().getDifxAlert().getSeverity() < 3) {
                    _systemSettings.messageCenter().error(0, difxMsg.getHeader().getFrom() + " : "
                            + difxMsg.getHeader().getIdentifier(),
                            difxMsg.getBody().getDifxAlert().getAlertMessage().toString());
                } else if (difxMsg.getBody().getDifxAlert().getSeverity() < 4) {
                    _systemSettings.messageCenter().warning(0, difxMsg.getHeader().getFrom() + " : "
                            + difxMsg.getHeader().getIdentifier(),
                            difxMsg.getBody().getDifxAlert().getAlertMessage().toString());
                } else {
                    _systemSettings.messageCenter().message(0, difxMsg.getHeader().getFrom() + " : "
                            + difxMsg.getHeader().getIdentifier(),
                            difxMsg.getBody().getDifxAlert().getAlertMessage().toString());
                }
            } else {
                if (difxMsg.getBody().getDifxAlert().getSeverity() < 3) {
                    java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.SEVERE,
                            difxMsg.getBody().getDifxAlert().getAlertMessage().toString());
                } else if (difxMsg.getBody().getDifxAlert().getSeverity() < 4) {
                    java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.WARNING,
                            difxMsg.getBody().getDifxAlert().getAlertMessage().toString());
                } else {
                    java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.INFO,
                            difxMsg.getBody().getDifxAlert().getAlertMessage().toString());
                }
            }
        }
            
    }

    public DifxMessage CreateDiFXAlertMessage(String message) {
        ObjectFactory factory = new ObjectFactory();

        // Create header
        Header header = factory.createHeader();
        header.setFrom("doi");
        header.setTo("doi");
        header.setMpiProcessId("-1");
        header.setIdentifier("doi");
        header.setType("DifxAlertMessage");

        // Create alert informational message
        DifxAlert alertMessage = factory.createDifxAlert();
        alertMessage.setAlertMessage(message);
        alertMessage.setSeverity(4);

        // -- Create the XML defined messages and process through the system
        Body body = factory.createBody();
        body.setDifxAlert(alertMessage);

        DifxMessage difxMsg = factory.createDifxMessage();
        difxMsg.setHeader(header);
        difxMsg.setBody(body);

        return difxMsg;
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
        } else if (difxObj.getObjType().equalsIgnoreCase("processor")) {
            // -- test, update existing or insert new object
            boolean exists = false;

            // find the processor
            Iterator it = mProcessorNodes.iterator();
            while ((!exists) && it.hasNext()) {
                ProcessorNode element = (ProcessorNode) it.next();
                if (element.getObjName().equalsIgnoreCase(difxObj.getObjName())) {
                    // update the data model
                    element.updateObject((ProcessorNode) difxObj);
                    exists = true;
                }
                element = null;
            }

            // insert new object
            if (!exists) {
                addProcessorNode((ProcessorNode) difxObj);
            }
        } else {
            System.out.println("DiFXDataModel.java(2247): " + difxObj.getObjType());
        }

    }

}
