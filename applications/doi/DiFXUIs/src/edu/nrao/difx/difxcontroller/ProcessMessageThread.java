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
public class ProcessMessageThread implements Runnable
{

   private String  mThreadName;
   private boolean mDone = false;

   private BlockingQueue<ByteArrayInputStream> _messageQueue;

   private DiFXDataModel       _difxDataModel;
   private JAXBPacketProcessor mThePacketProcessor;

   // Constructor, give the thread a name
   public ProcessMessageThread(String name, SystemSettings systemSettings )
   {
      mThreadName         = name;
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
      if (difxMsg != null)
      {
         // service data model - update the internal data
         //serviceDataModel(difxMsg);
          if ( _difxDataModel != null)
          {
             _difxDataModel.serviceDataModel(difxMsg);
          }
          else
          {
             System.out.printf("**************** Process message queue DiFX Data Model not defined. \n");
          }

      }
      else
      {
         System.out.printf("**************** Process message queue DifxMessage not defined. \n");
      }

      // clean up
      difxMsg = null;

      //System.out.println("**************** Process message queue process message packet complete. \n");
   }

//   // Service Data Model. . .send the message to be processed
//   protected synchronized void serviceDataModel(DifxMessage difxMsg)
//   {
//      if ( _difxDataModel != null)
//      {
//         _difxDataModel.serviceDataModel(difxMsg);
//      }
//      else
//      {
//         System.out.printf("**************** Process message queue DiFX Data Model not defined. \n");
//      }
//   }

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
                     System.out.printf("******************************** Process message queue take returned null packet ==> Should never occur. \n");
                  }

                  // no need to throttle the queue loop

                  // clean up
                  packet = null;

                  if (Thread.currentThread().isInterrupted() == true)
                  {
                     System.out.printf("**************** Process message thread %s interrupted. \n", mThreadName);
                     mDone = true;
                  }

               } // -- if (_messageQueue != null)
            }
            catch (InterruptedException exception)
            {
               Thread.interrupted();
               System.out.printf("**************** Process message %s caught interrupt - done. \n", mThreadName);
               mDone = true;
            }

         } // -- while (!mDone)

         System.out.printf("**************** Process message thread %s done. \n", mThreadName);
      }
   }
}
