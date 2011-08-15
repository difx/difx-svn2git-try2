/*
 * Extend the ProcessorNode class to include items that are specific to Mark5
 * Unit control and display.
 */
package edu.nrao.difx.difxdatamodel;

import edu.nrao.difx.xmllib.difxmessage.*;

import java.math.BigDecimal;

public class Mark5Unit extends ProcessorNode {//DiFXObject {

    public enum Mark5Commands {

        GETSVN, GETLOAD, GETDIR, RESETMARK5, STARTMARK5A, STOPMARK5A,
        CLEAR, REBOOT, POWEROFF, COPY
    };
    // -- status fields
    protected boolean stateChanged;
    protected String bankAVSN;
    protected String bankBVSN;
    protected String statusWord;
    protected String activeBank;
    protected int scanNumber;
    protected String scanName;
    protected long position;
    protected float playRate;
    protected BigDecimal dataMJD;
    protected String currentJob;

    public Mark5Unit() {
        super();
        stateChanged = false;
        bankAVSN = "";
        bankBVSN = "";
        statusWord = "";
        activeBank = "";
        scanNumber = 0;
        scanName = "";
        position = 0l;
        playRate = 0.0f;
        dataMJD = new BigDecimal(0.0);
        currentJob = "";
    }

    public boolean isStateChanged() {
        return stateChanged;
    }

    public void setStateChanged(boolean val) {
        stateChanged = val;
    }

    public String getBankAVSN() {
        return bankAVSN;
    }

    public void setBankAVSN(String val) {
        bankAVSN = val;
    }

    public String getBankBVSN() {
        return bankBVSN;
    }

    public void setBankBVSN(String val) {
        bankBVSN = val;
    }

    public String getStatusWord() {
        return statusWord;
    }

    public void setStatusWord(String val) {
        statusWord = val;
    }

    public String getActiveBank() {
        return activeBank;
    }

    public void setActiveBank(String val) {
        activeBank = val;
    }

    public int getScanNumber() {
        return scanNumber;
    }

    public void setScanNumber(int val) {
        scanNumber = val;
    }

    public String getScanName() {
        return scanName;
    }

    public void setScanName(String val) {
        scanName = val;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long val) {
        position = val;
    }

    public float getPlayRate() {
        return playRate;
    }

    public void setPlayRate(float val) {
        playRate = val;
    }

    public BigDecimal getDataMJD() {
        return dataMJD;
    }

    public void setDataMJD(BigDecimal val) {
        dataMJD = val;
    }

    public String getCurrentJob() {
        return currentJob;
    }

    public void setCurrentJob(String val) {
        currentJob = val;
    }

    public DifxMessage CreateDiFXCommandMessage(Mark5Commands cmd) {
        ObjectFactory factory = new ObjectFactory();

        // Create header
        Header header = factory.createHeader();
        header.setFrom("doi");
        header.setTo(getObjName());
        header.setMpiProcessId("-1");
        header.setIdentifier("doi");
        header.setType("DifxCommand");

        // Create mark5 command
        DifxCommand mark5Command = factory.createDifxCommand();

        switch (cmd) {
            case GETSVN:
                mark5Command.setCommand("GetVSN");
                break;
            case GETLOAD:
                mark5Command.setCommand("GetLoad");
                break;
            case GETDIR:
                mark5Command.setCommand("GetDir");
                break;
            case RESETMARK5:
                mark5Command.setCommand("ResetMark5");
                break;
            case STARTMARK5A:
                mark5Command.setCommand("StartMark5A");
                break;
            case STOPMARK5A:
                mark5Command.setCommand("StopMark5A");
                break;
            case CLEAR:
                mark5Command.setCommand("Clear");
                break;
            case REBOOT:
                mark5Command.setCommand("Reboot");
                break;
            case POWEROFF:
                mark5Command.setCommand("Poweroff");
                break;
            case COPY:
                mark5Command.setCommand("Copy");
                break;
            default: {
                mark5Command.setCommand("Invalid");
            }
        }

        // -- Create the XML defined messages and process through the system
        Body body = factory.createBody();
        body.setDifxCommand(mark5Command);

        DifxMessage difxMsg = factory.createDifxMessage();
        difxMsg.setHeader(header);
        difxMsg.setBody(body);

        return difxMsg;
    }

    public DifxMessage CreateDiFXCommandMessage(String parms) {
        ObjectFactory factory = new ObjectFactory();

        // Create header
        Header header = factory.createHeader();
        header.setFrom("doi");
        header.setTo("swc000");
        header.setMpiProcessId("-1");
        header.setIdentifier(getObjName());
        header.setType("DifxCommand");

        // Create mark5 copy command, append parameters
        DifxCommand mark5Command = factory.createDifxCommand();
        mark5Command.setCommand("Copy" + parms);

        // -- Create the XML defined messages and process through the system
        Body body = factory.createBody();
        body.setDifxCommand(mark5Command);

        DifxMessage difxMsg = factory.createDifxMessage();
        difxMsg.setHeader(header);
        difxMsg.setBody(body);

        return difxMsg;
    }

    @Override
    public void updateObject(DiFXObject newData) {
        bankAVSN = ((Mark5Unit) newData).getBankAVSN();
        bankBVSN = ((Mark5Unit) newData).getBankBVSN();
        statusWord = ((Mark5Unit) newData).getStatusWord();
        activeBank = ((Mark5Unit) newData).getActiveBank();
        scanNumber = ((Mark5Unit) newData).getScanNumber();
        scanName = ((Mark5Unit) newData).getScanName();
        position = ((Mark5Unit) newData).getPosition();
        playRate = ((Mark5Unit) newData).getPlayRate();
        dataMJD = ((Mark5Unit) newData).getDataMJD();
        currentJob = ((Mark5Unit) newData).getCurrentJob();
        super.updateObject(newData);
    }

    @Override
    public boolean isEqual(DiFXObject objToCompare) {
        // -- compare config and status
        return (bankAVSN.equals(((Mark5Unit) objToCompare).getBankAVSN())
                && bankBVSN.equals(((Mark5Unit) objToCompare).getBankBVSN())
                && statusWord.equals(((Mark5Unit) objToCompare).getStatusWord())
                && activeBank.equals(((Mark5Unit) objToCompare).getActiveBank())
                && scanNumber == ((Mark5Unit) objToCompare).getScanNumber()
                && scanName.equals(((Mark5Unit) objToCompare).getScanName())
                && position == ((Mark5Unit) objToCompare).getPosition()
                && playRate == ((Mark5Unit) objToCompare).getPlayRate()
                && dataMJD.equals(((Mark5Unit) objToCompare).getDataMJD())
                && currentJob.equals(((Mark5Unit) objToCompare).getCurrentJob())
                && super.isEqual(objToCompare));
    }

    @Override
    public boolean isVerified() {
        return ((state.equalsIgnoreCase("IDLE") || state.equalsIgnoreCase("CLOSE")) && enabled);
    }
}
