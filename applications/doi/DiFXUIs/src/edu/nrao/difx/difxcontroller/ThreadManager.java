/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.nrao.difx.difxcontroller;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import edu.nrao.difx.difxview.SystemSettings;

/**
 *
 * @author mguerra
 */
public class ThreadManager {

   private ReadMessageThread     mReadThread;
   private ProcessMessageThread  mProcessThread;
   private UpdateViewThread      mUpdateThread;
   private ExecutorService       mThreadExecutor;

   public ThreadManager( SystemSettings systemSettings )
   {
      // create thread manager with multisocket, message queue, and update view threads, and
      // an executor to manage the threads.
      mReadThread     = new ReadMessageThread( "ReadMessageThread", systemSettings );
      mProcessThread  = new ProcessMessageThread("ProcessMessageThread", systemSettings );
      mUpdateThread   = new UpdateViewThread("UpdateViewThread");
      mThreadExecutor = Executors.newFixedThreadPool(4);
   }

   public ProcessMessageThread getProcessThread()
   {
      return mProcessThread;
   }

   public ReadMessageThread getReadThread()
   {
      return mReadThread;
   }

   public UpdateViewThread getUpdateThread()
   {
      return mUpdateThread;
   }

   public void startThreads() throws InterruptedException
   {
      System.out.println("Thread manager threads starting.");

      // Start the threads
      mThreadExecutor.execute(mUpdateThread);
      mThreadExecutor.execute(mProcessThread);

      // Assign the preccess message thread to the read thread
      mReadThread.addQueue(mProcessThread);

      // Start read thread, and wait to synch up
      mThreadExecutor.execute(mReadThread);
      Thread.sleep(1);

      System.out.println("Thread manager threads started. \n");
   }

   public void stopThreads() {
      mThreadExecutor.shutdown();
      try {
         if (!mThreadExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS))
         {
            mThreadExecutor.shutdownNow();
            if (!mThreadExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS))
            {
               System.err.println("Thread manager pool did not terminate");
            }
         }
      } 
      catch (InterruptedException e)
      {
         mThreadExecutor.shutdownNow();
         Thread.currentThread().interrupt();
      }

      System.out.println("Thread manager threads stopped \n");
   }

}
