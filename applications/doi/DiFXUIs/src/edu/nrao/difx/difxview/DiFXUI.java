/*
 * This is the top-level of the DiFX GUI.  It launches a single window containing
 * multiple panels for DifX control.
 */
package edu.nrao.difx.difxview;

import mil.navy.usno.widgetlib.MessageDisplayPanel;

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
    //private static MessageDisplayPanel messageCtr = null;
    
    //private static final long serialVersionUID = 1;
    // Allow only one controller and data model instance
//    static DiFXDataModel _dataModel;
//    static DiFXController _difxController;
    // Keep a copy of the current running job
    //JobManagerUI mCurrentJM;

    public DiFXUI( String settingsFile ) {
        
        //  Produce system settings using the settings file that came from command
        //  line arguments (which might be null, indicating we should use default
        //  values).
        _systemSettings = new SystemSettings( settingsFile );

        //  With system settings finalized, we may now connect to the database.
        //connectToDB();
        //readResourcesConfig( _systemSettings.resourcesFile() );

        _systemSettings.setLookAndFeel();
        
        //  This function builds all of the GUI components.
        initComponents();
        
        //  Set the message center to absorb "logging" messages.  The
        //  "global" string tells it to capture everything (you can specify more
        //  restrictive names if you are perverse enough to delve into the Java
        //  logging system).
        _messageCenter.captureLogging( "global" );
        
        //  Create a "data model" for processing incoming data transmissions
        _dataModel = new DiFXDataModel( _systemSettings );
        _dataModel.messageDisplayPanel( _messageCenter );
        
        //  The data model needs to know when changes are made to database settings.
        _systemSettings.databaseChangeListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _dataModel.updataDatabaseFromSystemSettings();
            }
        } );
        
        //  The DiFX Controller runs threads (why, I'm not sure...)
        _difxController = new DiFXController( _systemSettings );
                try {
                    _difxController.startController();
                } catch (InterruptedException ex) {
                    // threads failed so log an error
                    java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.SEVERE, null, ex);
                }

                // initialize controller with model, view
                _difxController.initialize( _dataModel, this );
        
        //mitigateErrorManager.initialize( messageCenter, _dataModel, _difxController );
        
        _queueBrowser.dataModel( _dataModel );
        _hardwareMonitor.dataModel( _dataModel );
        _dataModel.notifyListeners();
        _hardwareMonitor.controller( _difxController );

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
        
        /*
         * This stuff is used to trap resize events.
         */
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

    //  Commented out Oct 11, 2011
    //  Called from the main() function.  Not sure why.  Seems fine without it.
//    private static void serviceDataModel() {
//        // Add code to update the resource objects
//        // System.out.printf("***************** Static - DiFX Manager service data model. \n");
//        if (_dataModel != null) {
//            // Flag the resources that can not communicate
//            _dataModel.determineLostResources();
//
//            // Determine state of all queued jobs  -- done for each mark5status
//            // _dataModel.determineStateOfAllJobs();
//        }
//        // System.out.printf("***************** Static - DiFX Manager service data model complete. \n");
//    }

//    private static void readResourcesConfig(String fileToOpen) {
//        //System.out.printf("***************** DiFX Manager read resources config file data. \n");
//        if (_dataModel != null) {
//            // read resource config data
//            _dataModel.readResourcesConfig(fileToOpen);
//        }
//    }

//    private static void readSystemConfig(String fileToOpen) {
//        //System.out.printf("***************** DiFX Manager read resources config file data. \n");
//        if (_dataModel != null) {
//            // read resource config data
//            _dataModel.readSystemConfig(fileToOpen);
//        }
//    }

//    private static void connectToDB() {
//        //System.out.printf("***************** DiFX Manager read resources config file data. \n");
//        if (_dataModel != null) {
//            // read resource config data
//            _dataModel.setDBConnection();
//        }
//    }
    
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
        _messageCenter = new mil.navy.usno.widgetlib.MessageDisplayPanel();
        _queueBrowser = new edu.nrao.difx.difxview.QueueBrowserPanel( _systemSettings, _messageCenter );
        _queueBrowser.addTearOffListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                queueBrowserTearOffEvent();
            }
        } );

        setDefaultCloseOperation( javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE );
        setTitle( "USNO DiFX GUI" );
        setName("DiFXUI");
        setSize( new java.awt.Dimension(1400, 800) );
        
        _hardwareMonitor = new HardwareMonitorPanel( _systemSettings );
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
        JMenuItem helpIndexItem = new JMenuItem( "GUI Documentation Index" );
        helpIndexItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _systemSettings.launchGUIHelp( "index.html" );
            }
        } );
        helpMenu.add( helpIndexItem );
        JMenuItem launchUsersGroupItem = new JMenuItem( "DiFX Users Group" );
        launchUsersGroupItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _systemSettings.launchDiFXUsersGroup();
            }
        } );
        helpMenu.add( launchUsersGroupItem );
        JMenuItem launchWikiItem = new JMenuItem( "DiFX Wiki" );
        launchWikiItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _systemSettings.launchDiFXWiki();
            }
        } );
        helpMenu.add( launchWikiItem );
        JMenuItem launchSVNItem = new JMenuItem( "DiFX svn" );
        launchSVNItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _systemSettings.launchDiFXSVN();
            }
        } );
        helpMenu.add( launchSVNItem );
        _menuBar.add( helpMenu );
        this.add( _menuBar );
        this.add( _mainSplitPane );
        this.newSize();
    }                  

    private void exitOperation() {
        _difxController.stopController();
        System.exit(0);
    }
    
    /*
     * Bring up a window containing settings for the GUI.
     */
    private void showSettingsAction() {
        _systemSettings.setVisible( true );
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
            if ( _hardwareMonitorWindow == null ) {
                _hardwareMonitorWindow = new JFrame();
                _hardwareMonitorWindow.setDefaultCloseOperation( javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE );
            }
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
            if ( _queueBrowserWindow == null ) {
                _queueBrowserWindow = new JFrame();
                _queueBrowserWindow.setDefaultCloseOperation( javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE );
            }
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
        ProjectManagerUI thePM = ProjectManagerUI.instance( _systemSettings, _dataModel, _difxController);
        // singleton, attach listener in the ProjectManagerUI class not here.
        // thePM.attachListenerCallback();
        thePM.setVisible(true);
        _dataModel.notifyListeners();

    }                                                    

    private void resourceManagerButtonActionPerformed(java.awt.event.ActionEvent evt) {                                                      

        // Display the GUI
        ResourceManagerUI theRM = ResourceManagerUI.instance(_dataModel, _difxController);
        // singleton, attach listener in the ResourceManagerUI class not here.
        //theRM.attachListenerCallback();
        theRM.setVisible(true);
        _dataModel.notifyListeners();

    }                                                     

    private void jobManagerButtonActionPerformed(java.awt.event.ActionEvent evt) {                                                 

        // Display the GUI, get current job running
        Queue queue = _dataModel.getQueue();
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

            JobManagerUI theJM = new JobManagerUI(_dataModel, _difxController, jobName, true);
            theJM.attachListenerCallback();
            theJM.setVisible(true);

        }


        // clean up
        queue = null;
    }                                                

    private void modulesButtonActionPerformed(java.awt.event.ActionEvent evt) {                                              

        // Display the GUI
        ModuleManagerUI theMM = ModuleManagerUI.instance(_dataModel, _difxController);
        // singleton, attach listener in the ResourceManagerUI class not here.
        // theMM.attachListenerCallback();
        theMM.setVisible(true);
        _dataModel.notifyListeners();

    }                                             

    private void queueManagerButtonActionPerformed(java.awt.event.ActionEvent evt)                                                   
    {                                                       
        // Display the GUI
        QueueManagerUI theQM = QueueManagerUI.instance(_dataModel, _difxController);
        // singleton, attach listener in the QueueManagerUI class not here.
        // theQM.attachListenerCallback();
        theQM.setVisible(true);
        _dataModel.notifyListeners();

    }
    
    /*
     * Pop up a window containing information about this software.  The event is
     * passed so that the window pops up near the mouse.
     */
    protected void aboutAction( ActionEvent e ) {
        if ( _aboutWindow == null )
            _aboutWindow = new VersionWindow();
        _aboutWindow.setBounds( 150, 20, 300, 100 );
        _aboutWindow.setVisible( true );
    }
    
    /**
     * This is the main entry point of the GUI
     * 
     * @param args the command line arguments
     */
    public static void main( final String args[] ) {

        //  Why did we do this????  Seems fine without it...
        //java.awt.EventQueue.invokeLater(new Runnable() {

            //@Override
            //public void run() {
                
                // Create manager UI using the first command line argument as a system settings
                // file.
                String settingsFile = null;
                if ( args.length > 0 )
                    settingsFile = args[0];
                DiFXUI view = new DiFXUI( settingsFile );
                view.setVisible(true);
                view.setTitle( view.getTitle() + " " + VersionWindow.version() );

//                if (messageCtr != null) {
//                    messageCtr.message( 0, null, "***************** DiFX Manager attach listener. \n");
//                } else {
//                    System.out.println("***************** DiFX Manager attach listener. \n");
//                }

                // attach the listener and implementation of update()...
                //  Commented out Oct 11, 2011
                //  Not sure what purpose this serves.
//                _dataModel.attachListener(new MessageListener() {
//
//                    @Override
//                    public void update() {
//                        //System.out.printf("***************** DiFX Manager service data model and view. \n");
//                        serviceDataModel();
//                        //UpdateView();
//                        //System.out.println("***************** DiFX Manager service data model and view complete. \n");
//                    }
//                });

                // kick start the message threads....start controller
//                try {
//                    _difxController.startController();
//                } catch (InterruptedException ex) {
//                    // threads failed so log an error
//                    java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.SEVERE, null, ex);
//                }
//
//                // initialize controller with model, view
//                _difxController.initialize(_dataModel, view);

                // diplay the error UI
                //MitigateErrorManagerUI theMEM = MitigateErrorManagerUI.instance(_dataModel, _difxController);
                //theMEM.setVisible(true);


            //}
        //});
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
    protected VersionWindow _aboutWindow;
    
    protected SystemSettings _systemSettings;

    protected DiFXDataModel _dataModel;
    protected DiFXController _difxController;
}