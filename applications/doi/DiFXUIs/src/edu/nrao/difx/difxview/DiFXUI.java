/*
 * This is the top-level of the DiFX GUI.  It launches a single window containing
 * multiple panels for DifX control.
 */
package edu.nrao.difx.difxview;

import mil.navy.usno.widgetlib.MessageDisplayPanel;
import edu.nrao.difx.difxutilities.BareBonesBrowserLaunch;

import java.awt.Toolkit;
import java.awt.Dimension;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import edu.nrao.difx.difxdatamodel.*;
import edu.nrao.difx.difxcontroller.*;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;

/**
 *
 * @author  jspitzak (USNO)
 * @author  mguerra (NRAO)
 * @author  Helge Rottmann (MPIfR)
 * @version 1.0
 */
public class DiFXUI extends JFrame implements WindowListener {

    //  This thing collects messages for us.  It is static so we can use it in
    //  the "main" method.
    private static MessageDisplayPanel messageCtr = null;
    
    private static final long serialVersionUID = 1;
    // Allow only one controller and data model instance
    static DiFXDataModel mDataModel;
    static DiFXController mController;
    // Keep a copy of the current running job
    JobManagerUI mCurrentJM;

    public DiFXUI() {        
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
        messageCtr = _messageCenter;
        //  Set the message center to absorb "logging" messages as well.  The
        //  "global" string tells it to capture everything (you can specify more
        //  restrictive names if you are perverse enough to delve into the Java
        //  logging system).
        messageCtr.captureLogging( "global" );
        
        // Create multi cast thread and data model
        mDataModel = new DiFXDataModel();
        mDataModel.messageDisplayPanel( _messageCenter );
        mController = new DiFXController();
        
        //mitigateErrorManager.initialize( messageCenter, mDataModel, mController );
        
        _queueBrowser.dataModel( mDataModel );
        _hardwareMonitor.dataModel( mDataModel );
        mDataModel.notifyListeners();
        _hardwareMonitor.controller( mController );

        /*
         * By default, set the main frame to take over the screen and subwindow
         * sizes to appropriate values.  The user should be able to change these
         * and have the changes stick the next time they start the application.
         */
        _topSplitPane.setResizeWeight( 0.5 );
        _mainSplitPane.setDividerLocation( 0.75 );

        /*  
         * Set ourselves up to intercept window operations (close, iconize, etc).
         */
        addWindowListener(this);
        
        _this = this;
		this.addComponentListener(new java.awt.event.ComponentAdapter() 
		{
			public void componentResized(ComponentEvent e)
			{
				_this.newSize();
			}
			public void componentMoved(ComponentEvent e)
			{
				_this.newSize();
			}
		});
//        java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.SEVERE, "WHAT IS THIS??");
    }
    
    /*
     * This is a function swiped from the following web link:
     * http://blog.darevay.com/2011/06/jsplitpainintheass-a-less-abominable-fix-for-setdividerlocation/
     * It tries to fix the idiotic behavior of "setDividerLocation" for JSplitPanes.
     * It kind of works....mostly....sometimes....
     */
//    public static JSplitPane setDividerLocation(final JSplitPane splitter,
//            final double proportion) {
//        if (splitter.isShowing()) {
//            if (splitter.getWidth() > 0 && splitter.getHeight() > 0) {
//                splitter.setDividerLocation(proportion);
//            } else {
//                splitter.addComponentListener(new ComponentAdapter() {
//
//                    @Override
//                    public void componentResized(ComponentEvent ce) {
//                        splitter.removeComponentListener(this);
//                        setDividerLocation(splitter, proportion);
//                    }
//                });
//            }
//        } else {
//            splitter.addHierarchyListener(new HierarchyListener() {
//
//                @Override
//                public void hierarchyChanged(HierarchyEvent e) {
//                    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0
//                            && splitter.isShowing()) {
//                        splitter.removeHierarchyListener(this);
//                        setDividerLocation(splitter, proportion);
//                    }
//                }
//            });
//        }
//        return splitter;
//    }

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
    
    @Override
    public void setBounds( int x, int y, int w, int h ) {
        super.setBounds( x, y, w, h );
        newSize();
    }
    
    public void newSize() {
        int w = this.getWidth();
        int h = this.getHeight();
        if ( _mainSplitPane != null ) {
            _mainSplitPane.setBounds( 0, 25, w, h - 47 );
            //  Maintain the relative sizes of the subwindows
            if ( _topSplitPane != null && _messageCenter != null ) {
                double wFraction = (double)( _topSplitPane.getHeight() ) / 
                        ( (double)( _topSplitPane.getHeight() ) + (double)( _messageCenter.getHeight() ) );
                //System.out.println( "wFraction is " + wFraction );
                _mainSplitPane.setDividerLocation( wFraction );
            }
            if ( _queueBrowser != null && _hardwareMonitor != null ) {
                double wFraction = (double)( _queueBrowser.getWidth() ) /
                        ( (double)( _queueBrowser.getWidth() ) + (double)( _hardwareMonitor.getWidth() ) );
                //System.err.println( "fraction is " + wFraction );
                //topSplitPane.setDividerLocation( (int)( wFraction * (double)w ) );
            }
            _mainSplitPane.updateUI();
        }
        if ( _menuBar != null )
            _menuBar.setBounds( 0, 0, w, 25 );
    }

    private void initComponents() {
        
        this.setLayout( null );

        _mainSplitPane = new javax.swing.JSplitPane();
        _topSplitPane = new javax.swing.JSplitPane();
        _queueBrowser = new edu.nrao.difx.difxview.QueueBrowserPanel();
        _queueBrowser.addTearOffListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                queueBrowserTearOffEvent();
            }
        } );
        _messageCenter = new mil.navy.usno.widgetlib.MessageDisplayPanel();

        setDefaultCloseOperation( javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE );
        setTitle( "USNO DiFX GUI" );
        setName("DiFXUI");
        setSize( new java.awt.Dimension(1400, 800) );
        
        _hardwareMonitor = new HardwareMonitorPanel();
        _hardwareMonitor.addTearOffListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                hardwareMonitorTearOffEvent();
            }
        } );

        _mainSplitPane.setBorder( javax.swing.BorderFactory.createEmptyBorder( 1, 1, 1, 1 ) );
        _mainSplitPane.setDividerLocation(400);
        _mainSplitPane.setDividerSize(3);
        _mainSplitPane.setOrientation( JSplitPane.VERTICAL_SPLIT );

        _topSplitPane.setDividerLocation(650);
        _topSplitPane.setDividerSize(3);
        _topSplitPane.setRightComponent( _hardwareMonitor );
        _topSplitPane.setLeftComponent( _queueBrowser );

        _mainSplitPane.setLeftComponent( _topSplitPane );
        _mainSplitPane.setRightComponent( _messageCenter );

        //  Menu bar and components
        _menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu( "File" );
        fileMenu.add( new JSeparator() );
        JMenuItem quitItem = new JMenuItem( "Quit" );
        quitItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                exitOperation();
            }
        } );
        fileMenu.add( quitItem );
        _menuBar.add( fileMenu );
        JMenu settingsMenu = new JMenu( "Settings" );
        JMenuItem showSettingsItem = new JMenuItem( "Show Settings" );
        showSettingsItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                showSettingsAction();
            }
        } );
        settingsMenu.add( showSettingsItem );
        _menuBar.add( settingsMenu );
        JMenu windowMenu = new JMenu( "Window" );
        JMenu arrangeMenu = new JMenu( "Monitor Arrangement" );
        _horizontalItem = new JCheckBoxMenuItem( "Horizontal" );
        _horizontalItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                arrangeHorizontal();
            }
        } );
        _horizontalItem.setSelected( true );
        arrangeMenu.add( _horizontalItem );
        _verticalItem = new JCheckBoxMenuItem( "Vertical" );
        _verticalItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                arrangeVertical();
            }
        } );
        _verticalItem.setSelected( false );
        arrangeMenu.add( _verticalItem );
        windowMenu.add( arrangeMenu );
        windowMenu.add( new JSeparator() );
        JMenuItem resourceItem = new JMenuItem( "Resource Manager" );
        resourceItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                resourceManagerButtonActionPerformed( e );
            }
        });
        windowMenu.add( resourceItem );
        JMenuItem modulesItem = new JMenuItem( "Units/Modules" );
        modulesItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                modulesButtonActionPerformed( e );
            }
        });
        windowMenu.add( modulesItem );
        JMenuItem projectItem = new JMenuItem( "Project Manager" );
        projectItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                projectManagerButtonActionPerformed( e );
            }
        });
        windowMenu.add( projectItem );
        JMenuItem jobManagerItem = new JMenuItem( "Job Manager" );
        jobManagerItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                jobManagerButtonActionPerformed( e );
            }
        });
        windowMenu.add( jobManagerItem );
        JMenuItem queueManagerItem = new JMenuItem( "Queue Manager" );
        queueManagerItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                queueManagerButtonActionPerformed( e );
            }
        });
        windowMenu.add( queueManagerItem );
        _menuBar.add( windowMenu );
        JMenu helpMenu = new JMenu( "Help" );
        JMenuItem aboutItem = new JMenuItem( "Version, etc." );
        aboutItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                aboutAction( e );
            }
        } );
        helpMenu.add( aboutItem );
        helpMenu.add( new JSeparator() );
        JMenuItem helpIndexItem = new JMenuItem( "Help Index" );
        helpIndexItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                helpAction( "index" );
            }
        } );
        helpMenu.add( helpIndexItem );
        _menuBar.add( helpMenu );
        this.add( _menuBar );
        this.add( _mainSplitPane );
        this.newSize();
    }                  

    private void exitOperation() {
        mController.stopController();
        System.exit(0);
    }
    
    /*
     * Bring up a window containing settings for the GUI.
     */
    private void showSettingsAction() {
    }
    
    /*
     * This is called when the "Horizontal" arrangement button is pushed.
     */
    protected void arrangeHorizontal() {
        //  You can't "unselect" these buttons (they act like radio buttons).
        if ( !_horizontalItem.isSelected() ) {
            _horizontalItem.setSelected( true );
            return;
        }
        //  Turn off the other button.
        _verticalItem.setSelected( false );
        double wFraction = (double)( _queueBrowser.getWidth() ) /
                           ( (double)( _queueBrowser.getWidth() ) + (double)( _hardwareMonitor.getWidth() ) );
        _topSplitPane.setOrientation( JSplitPane.HORIZONTAL_SPLIT );
        _topSplitPane.setDividerLocation( wFraction );
    }
    
    /*
     * Similar function for the "Vertical" arrangement button.
     */
    protected void arrangeVertical() {
        //  You can't "unselect" these buttons (they act like radio buttons).
        if ( !_verticalItem.isSelected() ) {
            _verticalItem.setSelected( true );
            return;
        }
        //  Turn off the other button.
        _horizontalItem.setSelected( false );
        double wFraction = (double)( _queueBrowser.getWidth() ) /
                           ( (double)( _queueBrowser.getWidth() ) + (double)( _hardwareMonitor.getWidth() ) );
        _topSplitPane.setOrientation( JSplitPane.VERTICAL_SPLIT );
        _topSplitPane.setDividerLocation( wFraction );
    }
    
    /*
     * Called when the "tear off" button on the hardware monitor is pushed.  This
     * can happen when the button is pushed to "re-attach" as well.
     */
    protected void hardwareMonitorTearOffEvent() {
        if ( _hardwareMonitor.tearOffState() ) {
            if ( _hardwareMonitorWindow == null )
                _hardwareMonitorWindow = new JFrame();
            _hardwareMonitorWindow.setSize( _hardwareMonitor.getWidth(), _hardwareMonitor.getHeight() );
            _hardwareMonitorWindow.add( _hardwareMonitor );
            _hardwareMonitorWindow.setVisible( true );
            if ( _queueBrowser.tearOffState() ) {
                _dividerLocation = (double)_mainSplitPane.getDividerLocation() / (double)this.getHeight();
                _mainSplitPane.remove( _topSplitPane );
            }
        }
        else {
            if ( _queueBrowser.tearOffState() ) {
                _mainSplitPane.setLeftComponent( _topSplitPane );
                _mainSplitPane.setDividerLocation( _dividerLocation );
            }
            _topSplitPane.setRightComponent( _hardwareMonitor );
            _hardwareMonitorWindow.setVisible( false );
        }
    }
    
    /*
     * Called when the "tear off" button on the queue browser is pushed.  This
     * can happen when the button is pushed to "re-attach" as well.
     */
    protected void queueBrowserTearOffEvent() {
        if ( _queueBrowser.tearOffState() ) {
            if ( _queueBrowserWindow == null )
                _queueBrowserWindow = new JFrame();
            _queueBrowserWindow.setSize( _queueBrowser.getWidth(), _queueBrowser.getHeight() );
            _queueBrowserWindow.add( _queueBrowser );
            _queueBrowserWindow.setVisible( true );
            if ( _hardwareMonitor.tearOffState() ) {
                _dividerLocation = (double)_mainSplitPane.getDividerLocation() / (double)this.getHeight();
                _mainSplitPane.remove( _topSplitPane );
            }
        }
        else {
            if ( _hardwareMonitor.tearOffState() ) {
                _mainSplitPane.setLeftComponent( _topSplitPane );
                _mainSplitPane.setDividerLocation( _dividerLocation );
            }
            _topSplitPane.setLeftComponent( _queueBrowser );
            _queueBrowserWindow.setVisible( false );
        }
    }

    private void projectManagerButtonActionPerformed(java.awt.event.ActionEvent evt) {                                                     

        // Display the GUI
        ProjectManagerUI thePM = ProjectManagerUI.instance(mDataModel, mController);
        // singleton, attach listener in the ProjectManagerUI class not here.
        // thePM.attachListenerCallback();
        thePM.setVisible(true);
        mDataModel.notifyListeners();

    }                                                    

    private void resourceManagerButtonActionPerformed(java.awt.event.ActionEvent evt) {                                                      

        // Display the GUI
        ResourceManagerUI theRM = ResourceManagerUI.instance(mDataModel, mController);
        // singleton, attach listener in the ResourceManagerUI class not here.
        //theRM.attachListenerCallback();
        theRM.setVisible(true);
        mDataModel.notifyListeners();

    }                                                     

    private void jobManagerButtonActionPerformed(java.awt.event.ActionEvent evt) {                                                 

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
    }                                                

    private void modulesButtonActionPerformed(java.awt.event.ActionEvent evt) {                                              

        // Display the GUI
        ModuleManagerUI theMM = ModuleManagerUI.instance(mDataModel, mController);
        // singleton, attach listener in the ResourceManagerUI class not here.
        // theMM.attachListenerCallback();
        theMM.setVisible(true);
        mDataModel.notifyListeners();

    }                                             

    private void queueManagerButtonActionPerformed(java.awt.event.ActionEvent evt)                                                   
    {                                                       
        // Display the GUI
        QueueManagerUI theQM = QueueManagerUI.instance(mDataModel, mController);
        // singleton, attach listener in the QueueManagerUI class not here.
        // theQM.attachListenerCallback();
        theQM.setVisible(true);
        mDataModel.notifyListeners();

    }
    
    /*
     * Pop up a window containing information about this software.  The event is
     * passed so that the window pops up near the mouse.
     */
    protected void aboutAction( ActionEvent e ) {
    }
    
    /*
     * Launch a browser containing help on a given topic.  The topic is described
     * by the passed string.
     */
    protected void helpAction( String topic ) {
        //BareBonesBrowserLaunch.openURL( "file:///Users/jspitzak/fltk2/documentation/index.html" );
        BareBonesBrowserLaunch.openURL( "http://www.nyt.com" );
    }


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
                DiFXUI view = new DiFXUI();

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
                    
    protected HardwareMonitorPanel _hardwareMonitor;
    protected JSplitPane _mainSplitPane;
    protected MessageDisplayPanel _messageCenter;
    protected QueueBrowserPanel _queueBrowser;
    protected JSplitPane _topSplitPane;
    protected DiFXUI _this;
    protected JMenuBar _menuBar;
    protected JCheckBoxMenuItem _horizontalItem;
    protected JCheckBoxMenuItem _verticalItem;
    protected JFrame _hardwareMonitorWindow;
    protected JFrame _queueBrowserWindow;
    protected double _dividerLocation;

}