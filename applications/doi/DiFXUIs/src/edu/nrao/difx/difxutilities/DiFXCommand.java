/*
 * Base class for commands sent via XML to the DiFX host
 */
package edu.nrao.difx.difxutilities;

import edu.nrao.difx.difxview.SystemSettings;

import edu.nrao.difx.xmllib.difxmessage.ObjectFactory;
import edu.nrao.difx.xmllib.difxmessage.Header;
import edu.nrao.difx.xmllib.difxmessage.Body;

import edu.nrao.difx.difxcontroller.JAXBDiFXProcessor;

import edu.nrao.difx.xmllib.difxmessage.DifxMessage;

/**
 *
 * @author jspitzak
 */
public class DiFXCommand {
    
    /*
     * Construct the skeleton of a generic message.
     */
    public DiFXCommand( SystemSettings settings ) {
        _settings = settings;
        _factory = new ObjectFactory();
        _header = _factory.createHeader();
        _header.setFrom( "doi" );
        _header.setTo( _settings.difxControlAddress() );
        _header.setMpiProcessId( "0" );
        _header.setIdentifier( "doi" );
        _body = _factory.createBody();
        _difxMsg = _factory.createDifxMessage();
    }
    
    /*
     * Used to set the "identifier" if something other than the default is needed.
     */
    public void identifier( String newVal ) {
        _header.setIdentifier( newVal );
    }
    
    /*
     * Similarly, change "from".
     */
    public void from( String newVal ) {
        _header.setFrom( newVal );
    }
    
    /*
     * Convert the message to XML and send it.
     */
    public void send() {
        _difxMsg.setHeader( _header );
        _difxMsg.setBody( _body );
        JAXBDiFXProcessor xmlProc = new JAXBDiFXProcessor( _difxMsg );
        String xmlString = xmlProc.ConvertToXML();
        if ( xmlString != null )
            SendMessage.writeToSocket( xmlString, _settings );
    }
    
    public ObjectFactory factory() { return _factory; }
    public Body body() { return _body; }
    public Header header() { return _header; }
    
    protected SystemSettings _settings;
    protected Header _header;
    protected Body _body;
    protected DifxMessage _difxMsg;
    protected ObjectFactory _factory;
    
}
