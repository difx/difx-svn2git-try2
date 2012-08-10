  /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.nrao.difx.difxcontroller;

import java.net.*;
import java.io.*;
import javax.xml.bind.*;

import edu.nrao.difx.xmllib.difxmessage.*;
import edu.nrao.difx.xmllib.difxmessage.ObjectFactory;

/**
 *
 * @author mguerra
 */
public class JAXBPacketProcessor
{

   /**
    * JAXBPacketProcessor is a thread that handles the packet processing.
    * The packets contain XML messages and upon receipt they are
    * unmarshalled into JAXB objects. The JAXB object is then stuffed
    * into a MulticastGroupEvent object which is received by
    * registered listeners via a callback method invocation.
    */
   private Unmarshaller   mUnmarshaller;
   private ObjectFactory  mFactory;
   private JAXBContext    mJaxbCtx;
   /**
    * Constructor for this class that accepts a datagram packet as
    * input.
    */
   public JAXBPacketProcessor(String pkg)
   {
      try
      {
         System.out.println(pkg);
         mFactory = new ObjectFactory();
         mJaxbCtx = JAXBContext.newInstance(pkg);
         mUnmarshaller = mJaxbCtx.createUnmarshaller();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   /**
    * Convert a data buffer directly into a JAXB object (i.e. avoid that DatagramPacket
    * stuff).
    */
   public DifxMessage ConvertToJAXB( ByteArrayInputStream is )
   {
      // message to return to client
      DifxMessage difxMsg = mFactory.createDifxMessage();

      try
      {
         difxMsg = (DifxMessage) mUnmarshaller.unmarshal(is);
         is = null;
      }
      catch (javax.xml.bind.JAXBException ex)
      {
          System.out.println( "JAXBException");
          System.out.println( is.toString() );
         // XXXTODO Handle exception
         java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.SEVERE, null, ex); //NOI18N
      }
      catch (Exception e)
      {
          System.out.println( ">>>>PACKET ERROR");
      System.out.println( is.toString() );
      System.out.println( ">>>>>>>>>>>>>>>");
         e.printStackTrace();
      }

      // return unmarshalled message to client
      return difxMsg;
   }
}
