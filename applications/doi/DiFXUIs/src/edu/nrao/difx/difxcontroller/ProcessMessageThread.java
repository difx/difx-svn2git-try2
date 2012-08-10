/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.nrao.difx.difxcontroller;

import edu.nrao.difx.difxdatamodel.DiFXDataModel;

import edu.nrao.difx.difxview.SystemSettings;
import java.net.*;

import edu.nrao.difx.xmllib.difxmessage.*;
import java.io.ByteArrayInputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author mguerra
 */
public class ProcessMessageThread extends Thread //implements Runnable
{

   private boolean mDone = false;

   private BlockingQueue<ByteArrayInputStream> _messageQueue;

   private DiFXDataModel       _difxDataModel;
   private JAXBPacketProcessor mThePacketProcessor;
   
   SystemSettings _settings;

   // Constructor, give the thread a name
   public ProcessMessageThread( SystemSettings systemSettings )
   {
      _settings           = systemSettings;
      _messageQueue       = new LinkedBlockingQueue<ByteArrayInputStream>();
      mThePacketProcessor = new JAXBPacketProcessor( systemSettings.jaxbPackage() );
   }

   // Stop thread
   public void shutDown()
   {
        mDone = true;
   }

   // Methods specific to Queue
   public boolean add( ByteArrayInputStream pack )
   {
      // no null entries allowed
      try
      {
         return ( _messageQueue.offer( pack ) );
      }
      catch (NullPointerException e)
      {
         return false;
      }
   }

//   public DatagramPacket remove() throws InterruptedException
   public ByteArrayInputStream remove() throws InterruptedException
   {
      return ( _messageQueue.take() );
   }

   public void difxDataModel( DiFXDataModel newDataModel )
   {
      _difxDataModel = newDataModel;
   }

   // Process a datagram - unmarshall into DifxMessage and send to controller.
   // The controller is responsible for updating the data model.
   public synchronized void processMessage( ByteArrayInputStream packet)
   {
      //System.out.printf("**************** Process message queue process message packet. \n");
      //java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.INFO, "got a packet" );
      
      // Process message into DiFXMessage
      DifxMessage difxMsg = mThePacketProcessor.ConvertToJAXB(packet);
      if ( difxMsg != null ) {
//          if ( _difxDataModel != null)
//          {
//             _difxDataModel.serviceDataModel(difxMsg);
//          }
//          else
//          {
//             System.out.printf("**************** Process message queue DiFX Data Model not defined. \n");
//          }

          
                  Header header = difxMsg.getHeader();
        //System.out.println( header.getFrom() );
        //System.out.println( "         " + header.getType() );

        if (header.getType().equalsIgnoreCase("DifxStatusMessage")) {
            //java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.INFO, "DifxStatusMessage");
            _difxDataModel.processDifxStatusMessage(difxMsg);

        } else if (header.getType().equalsIgnoreCase("Mark5StatusMessage")) {
            //java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.INFO, "Mark5StatusMessage");
            _difxDataModel.processMark5StatusMessage(difxMsg);

        } else if (header.getType().equalsIgnoreCase("DifxLoadMessage")) {
            //java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.INFO, "DifxLoadMessage");
            _difxDataModel.processDifxLoadMessage(difxMsg);

        } else if (header.getType().equalsIgnoreCase("DifxAlertMessage")) {
            //java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.INFO, "DifxAlertMessage");
            _difxDataModel.processDifxAlertMessage(difxMsg);

        } else {
            if ( !_settings.suppressWarnings() ) {
                java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.WARNING, "unknown DiFX message: \""
                        + header.getType() + "\"");
            }
        }

        // clean up
        header = null;

          
          
          
          
      }
      else
      {
         System.out.printf("**************** Process message queue DifxMessage not defined. \n");
      }

      // clean up
      difxMsg = null;

      //System.out.println("**************** Process message queue process message packet complete. \n");
   }

   // Print a datagram packet to console
   public void printPacket(DatagramPacket packet)
   {
      System.out.println("**************** Process message packet data from: " + packet.getAddress().toString() +
              ":" + packet.getPort() + " with length: " +
              packet.getLength());

      System.out.write(packet.getData(), 0, packet.getLength());
      System.out.println();
   }

   // Implement the thread interface
   @Override
   public void run()
   {
      synchronized (this)
      {
         // Element to take from queue
         ByteArrayInputStream packet = null;

         // Loop forever, dequeue and process datagram packets
         while (!mDone)
         {
            try
            {
               // Check queue for the packet
               if (_messageQueue != null)
               {
                  // Wait and get packet from queue
                  packet = _messageQueue.take();

                  // process the message packet
                  if (packet != null)
                  {
                     processMessage( packet );
                  }
                  else
                  {
                     System.out.printf( "******************************** Process message queue take returned null packet ==> Should never occur. \n");
                  }

                  // no need to throttle the queue loop

                  // clean up
                  packet = null;

                  if (Thread.currentThread().isInterrupted() == true)
                  {
                     System.out.println( "**************** Process message thread interrupted." );
                     mDone = true;
                  }

               } // -- if (_messageQueue != null)
            }
            catch (InterruptedException exception)
            {
               Thread.interrupted();
               System.out.println( "**************** Process message thread caught interrupt - done." );
               mDone = true;
            }

         } // -- while (!mDone)

         System.out.println( "**************** Process message thread done. \n" );
      }
   }
}
