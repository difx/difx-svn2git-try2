/*
 * DiFXManagerUI.java
 *
 * Created on March 3, 2008, 11:33 AM
 */
package edu.nrao.difx.difxview;

import java.awt.Toolkit;
import java.awt.Dimension;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;

import edu.nrao.difx.difxdatamodel.*;
import edu.nrao.difx.difxcontroller.*;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

import javax.swing.JSplitPane;
import javax.swing.UIManager;


/**
 *
 * @author  mguerra (NRAO)
 * @author  Helge Rottmann (MPIfR)
 * @version 1.0
 */
public class DiFXManagerUI extends javax.swing.JFrame implements WindowListener {

    //  This thing collects messages for us.  It is static so we can use it in
    //  the "main" method.
    private static MessageDisplayPanel messageCtr = null;
    
    private static final long serialVersionUID = 1;
    // Allow only one controller and data model instance
    static DiFXDataModel mDataModel;
    static DiFXController mController;
    // Keep a copy of the current running job
    JobManagerUI mCurrentJM;

    /** Creates new form DiFXManagerUI */
    public DiFXManagerUI() {        
        //  This is supposed to set the look and feel.  Using the "cross platform"
        //  look and feel makes the menu look the same everywhere.
        try {
            UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName() );
        }
        catch ( Exception e ) {
            //  This thing throws exceptions, but we ignore them.  Shouldn't hurt
            //  us - if the look and feel isn't set, the default should at least
            //  be visible.
        }
        initComponents();
        
        //  Set the static message collector.  We want it to absorb logging
        //  messages as well.
        messageCtr = messageCenter;
        //  Set the message center to absorb "logging" messages as well.  The
        //  "global" string tells it to capture everything (you can specify more
        //  restrictive names if you are perverse enough to delve into the Java
        //  logging system).
        messageCtr.captureLogging( "global" );
        
        // Create multi cast thread and data model
        mDataModel = new DiFXDataModel();
        mDataModel.messageDisplayPanel( messageCenter );
        mController = new DiFXController();
        
        mitigateErrorManager.initialize( messageCenter, mDataModel, mController );

        /*
         * By default, set the main frame to take over the screen and subwindow
         * sizes to appropriate values.  The user should be able to change these
         * and have the changes stick the next time they start the application.
         */
        mainSplitPane.setResizeWeight( 0.75 );
        topSplitPane.setResizeWeight( 0.75 );
        setDividerLocation( mainSplitPane, 0.75 );
        setDividerLocation( topSplitPane, 0.8 );
        setDividerLocation( bottomSplitPane, 0.6 );
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        //setBounds(0,0,screenSize.width, screenSize.height);

        /*  
         * Set ourselves up to intercept window operations (close, iconize, etc).
         */
        addWindowListener(this);
        
//        java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.SEVERE, "WHAT IS THIS??");
    }
    
    /*
     * This is a function swiped from the following web link:
     * http://blog.darevay.com/2011/06/jsplitpainintheass-a-less-abominable-fix-for-setdividerlocation/
     * It tries to fix the idiotic behavior of "setDividerLocation" for JSplitPanes.
     */
    public static JSplitPane setDividerLocation(final JSplitPane splitter,
            final double proportion) {
        if (splitter.isShowing()) {
            if (splitter.getWidth() > 0 && splitter.getHeight() > 0) {
                splitter.setDividerLocation(proportion);
            } else {
                splitter.addComponentListener(new ComponentAdapter() {

                    @Override
                    public void componentResized(ComponentEvent ce) {
                        splitter.removeComponentListener(this);
                        setDividerLocation(splitter, proportion);
                    }
                });
            }
        } else {
            splitter.addHierarchyListener(new HierarchyListener() {

                @Override
                public void hierarchyChanged(HierarchyEvent e) {
                    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0
                            && splitter.isShowing()) {
                        splitter.removeHierarchyListener(this);
                        setDividerLocation(splitter, proportion);
                    }
                }
            });
        }
        return splitter;
    }

    /*
     * Window event methods - we need each of these, even though we are only
     * interested in the "Closing" method.
     */
    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        exitOperation();
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    private static void serviceDataModel() {
        // Add code to update the resource objects
        // System.out.printf("***************** Static - DiFX Manager service data model. \n");
        if (mDataModel != null) {
            // Flag the resources that can not communicate
            mDataModel.determineLostResources();

            // Determine state of all queued jobs  -- done for each mark5status
            // mDataModel.determineStateOfAllJobs();
        }
        // System.out.printf("***************** Static - DiFX Manager service data model complete. \n");
    }

    private static void updateView() {
        // Add code to update the GUI with resource object
        //System.out.printf("***************** Static - DiFX Manager update the view. \n");
    }

    private static void readResourcesConfig(String fileToOpen) {
        //System.out.printf("***************** DiFX Manager read resources config file data. \n");
        if (mDataModel != null) {
            // read resource config data
            mDataModel.readResourcesConfig(fileToOpen);
        }
    }

    private static void readSystemConfig(String fileToOpen) {
        //System.out.printf("***************** DiFX Manager read resources config file data. \n");
        if (mDataModel != null) {
            // read resource config data
            mDataModel.readSystemConfig(fileToOpen);
        }
    }

    private static void connectToDB() {
        //System.out.printf("***************** DiFX Manager read resources config file data. \n");
        if (mDataModel != null) {
            // read resource config data
            mDataModel.setDBConnection();
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        popupMenu = new javax.swing.JPopupMenu();
        aboutItem = new javax.swing.JMenuItem();
        resourceManagerButton = new javax.swing.JButton();
        modulesButton = new javax.swing.JButton();
        projectManagerButton = new javax.swing.JButton();
        jobManagerButton = new javax.swing.JButton();
        queueManagerButton = new javax.swing.JButton();
        mainSplitPane = new javax.swing.JSplitPane();
        topSplitPane = new javax.swing.JSplitPane();
        jPanel2 = new javax.swing.JPanel();
        queuePanel = new javax.swing.JPanel();
        queueBrowserPanel1 = new edu.nrao.difx.difxview.QueueBrowserPanel();
        bottomSplitPane = new javax.swing.JSplitPane();
        mitigateErrorManager = new edu.nrao.difx.difxview.MitigateErrorManagerPanel();
        messageCenter = new edu.nrao.difx.difxview.MessageDisplayPanel();

        aboutItem.setText("About");
        popupMenu.add(aboutItem);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("NRAO DiFX Manager 2.0");
        setName("DiFXManager"); // NOI18N
        setSize(new java.awt.Dimension(1100, 800));

        resourceManagerButton.setText("Resource Manager");
        resourceManagerButton.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        resourceManagerButton.setMaximumSize(new java.awt.Dimension(57, 19));
        resourceManagerButton.setMinimumSize(new java.awt.Dimension(57, 19));
        resourceManagerButton.setPreferredSize(new java.awt.Dimension(57, 25));
        resourceManagerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resourceManagerButtonActionPerformed(evt);
            }
        });

        modulesButton.setText("Units/Modules");
        modulesButton.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        modulesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                modulesButtonActionPerformed(evt);
            }
        });

        projectManagerButton.setText("Project Manager");
        projectManagerButton.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        projectManagerButton.setMaximumSize(new java.awt.Dimension(57, 19));
        projectManagerButton.setMinimumSize(new java.awt.Dimension(57, 19));
        projectManagerButton.setPreferredSize(new java.awt.Dimension(57, 19));
        projectManagerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                projectManagerButtonActionPerformed(evt);
            }
        });

        jobManagerButton.setText("Job Manager");
        jobManagerButton.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        jobManagerButton.setMaximumSize(new java.awt.Dimension(57, 19));
        jobManagerButton.setMinimumSize(new java.awt.Dimension(57, 19));
        jobManagerButton.setPreferredSize(new java.awt.Dimension(57, 19));
        jobManagerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jobManagerButtonActionPerformed(evt);
            }
        });

        queueManagerButton.setText("Queue Manager");
        queueManagerButton.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        queueManagerButton.setMaximumSize(new java.awt.Dimension(57, 19));
        queueManagerButton.setMinimumSize(new java.awt.Dimension(57, 19));
        queueManagerButton.setPreferredSize(new java.awt.Dimension(57, 19));
        queueManagerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                queueManagerButtonActionPerformed(evt);
            }
        });

        mainSplitPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        mainSplitPane.setDividerLocation(400);
        mainSplitPane.setDividerSize(3);
        mainSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setLocation(new java.awt.Point(0, 25));
        mainSplitPane.setPreferredSize(new java.awt.Dimension(800, 575));
        mainSplitPane.setSize(new java.awt.Dimension(800, 575));

        topSplitPane.setDividerLocation(650);
        topSplitPane.setDividerSize(3);
        topSplitPane.setResizeWeight(1.0);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 395, Short.MAX_VALUE)
        );

        topSplitPane.setRightComponent(jPanel2);

        javax.swing.GroupLayout queuePanelLayout = new javax.swing.GroupLayout(queuePanel);
        queuePanel.setLayout(queuePanelLayout);
        queuePanelLayout.setHorizontalGroup(
            queuePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1332, Short.MAX_VALUE)
            .addGroup(queuePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(queueBrowserPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 1332, Short.MAX_VALUE))
        );
        queuePanelLayout.setVerticalGroup(
            queuePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 395, Short.MAX_VALUE)
            .addGroup(queuePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(queueBrowserPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 395, Short.MAX_VALUE))
        );

        topSplitPane.setLeftComponent(queuePanel);

        mainSplitPane.setLeftComponent(topSplitPane);

        bottomSplitPane.setDividerSize(3);
        bottomSplitPane.setRightComponent(mitigateErrorManager);
        bottomSplitPane.setLeftComponent(messageCenter);

        mainSplitPane.setRightComponent(bottomSplitPane);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(resourceManagerButton, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(modulesButton, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(projectManagerButton, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jobManagerButton, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(queueManagerButton, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(773, Short.MAX_VALUE))
            .addComponent(mainSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1341, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(resourceManagerButton, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(modulesButton, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(projectManagerButton, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jobManagerButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(queueManagerButton, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3)
                .addComponent(mainSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 772, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void exitOperation() {
        mController.stopController();
        System.exit(0);
    }

    private void projectManagerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_projectManagerButtonActionPerformed

        // Display the GUI
        ProjectManagerUI thePM = ProjectManagerUI.instance(mDataModel, mController);
        // singleton, attach listener in the ProjectManagerUI class not here.
        // thePM.attachListenerCallback();
        thePM.setVisible(true);
        mDataModel.notifyListeners();

    }//GEN-LAST:event_projectManagerButtonActionPerformed

    private void resourceManagerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resourceManagerButtonActionPerformed

        // Display the GUI
        ResourceManagerUI theRM = ResourceManagerUI.instance(mDataModel, mController);
        // singleton, attach listener in the ResourceManagerUI class not here.
        //theRM.attachListenerCallback();
        theRM.setVisible(true);
        mDataModel.notifyListeners();

    }//GEN-LAST:event_resourceManagerButtonActionPerformed

    private void jobManagerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jobManagerButtonActionPerformed

        // Display the GUI, get current job running
        Queue queue = mDataModel.getQueue();
        if (queue != null) {
            Job job = queue.getCurrentJob();
            String jobName = null;
            if (job != null) {
                jobName = job.getObjName();
                if (jobName.isEmpty()) {
                    jobName = "Open a Job";
                }
                // clean up -BLAT is this necessary??  I thought the all-powerful Java
                // garbage collector would take care of this!
                job = null;
            }

            JobManagerUI theJM = new JobManagerUI(mDataModel, mController, jobName, true);
            theJM.attachListenerCallback();
            theJM.setVisible(true);

        }


        // clean up
        queue = null;
    }//GEN-LAST:event_jobManagerButtonActionPerformed

    private void modulesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_modulesButtonActionPerformed

        // Display the GUI
        ModuleManagerUI theMM = ModuleManagerUI.instance(mDataModel, mController);
        // singleton, attach listener in the ResourceManagerUI class not here.
        // theMM.attachListenerCallback();
        theMM.setVisible(true);
        mDataModel.notifyListeners();

    }//GEN-LAST:event_modulesButtonActionPerformed

    private void queueManagerButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_queueManagerButtonActionPerformed
    {//GEN-HEADEREND:event_queueManagerButtonActionPerformed
        // Display the GUI
        QueueManagerUI theQM = QueueManagerUI.instance(mDataModel, mController);
        // singleton, attach listener in the QueueManagerUI class not here.
        // theQM.attachListenerCallback();
        theQM.setVisible(true);
        mDataModel.notifyListeners();

}//GEN-LAST:event_queueManagerButtonActionPerformed

    /**
     * This is the main entry point of the GUI
     * 
     * @param args the command line arguments
     */
    public static void main(String args[]) {

        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                // create manager UI
                DiFXManagerUI view = new DiFXManagerUI();

                view.setVisible(true);
                view.setTitle(view.getTitle() + " " + DOISystemConfig.DOIVersion);

                if (messageCtr != null) {
                    messageCtr.message( 0, null, "***************** DiFX Manager attach listener. \n");
                } else {
                    System.out.println("***************** DiFX Manager attach listener. \n");
                }

                //  Read the GUI configuration file, if one exists.
                try {
                    readSystemConfig( DOISystemConfig.getConfigFile() );
                } catch (Exception ex) {
                    java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.SEVERE,
                            ex.getMessage() );
                    //System.exit(1);     
                }
                connectToDB();
                readResourcesConfig(DOISystemConfig.ResourcesFile);

                // attach the listener and implementation of update()...
                mDataModel.attachListener(new MessageListener() {

                    @Override
                    public void update() {
                        //System.out.printf("***************** DiFX Manager service data model and view. \n");
                        serviceDataModel();
                        //UpdateView();
                        //System.out.println("***************** DiFX Manager service data model and view complete. \n");
                    }
                });

                // kick start the message threads....start controller
                try {
                    mController.startController();
                } catch (InterruptedException ex) {
                    // threads failed so log an error
                    java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.SEVERE, null, ex);
                }

                // initialize controller with model, view
                mController.initialize(mDataModel, view);

                // diplay the error UI
                //MitigateErrorManagerUI theMEM = MitigateErrorManagerUI.instance(mDataModel, mController);
                //theMEM.setVisible(true);


            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutItem;
    private javax.swing.JSplitPane bottomSplitPane;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JButton jobManagerButton;
    private javax.swing.JSplitPane mainSplitPane;
    private edu.nrao.difx.difxview.MessageDisplayPanel messageCenter;
    private edu.nrao.difx.difxview.MitigateErrorManagerPanel mitigateErrorManager;
    private javax.swing.JButton modulesButton;
    private javax.swing.JPopupMenu popupMenu;
    private javax.swing.JButton projectManagerButton;
    private edu.nrao.difx.difxview.QueueBrowserPanel queueBrowserPanel1;
    private javax.swing.JButton queueManagerButton;
    private javax.swing.JPanel queuePanel;
    private javax.swing.JButton resourceManagerButton;
    private javax.swing.JSplitPane topSplitPane;
    // End of variables declaration//GEN-END:variables
}
