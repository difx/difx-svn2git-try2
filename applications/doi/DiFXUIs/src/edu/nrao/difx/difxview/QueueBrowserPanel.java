/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.nrao.difx.difxview;

import mil.navy.usno.widgetlib.NodeBrowserScrollPane;
import mil.navy.usno.widgetlib.BrowserNode;
import mil.navy.usno.widgetlib.TearOffPanel;
import mil.navy.usno.widgetlib.MessageDisplayPanel;
import mil.navy.usno.widgetlib.ActivityMonitorLight;

import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JSeparator;
import javax.swing.JOptionPane;

import java.awt.Font;
import java.awt.Color;
import java.awt.Insets;
import java.awt.Frame;
import java.awt.Component;
import java.awt.Point;

import java.io.File;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.Timer;

import java.util.List;
import java.util.Iterator;
import java.util.Date;
import java.text.SimpleDateFormat;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import edu.nrao.difx.difxdatamodel.*;
import edu.nrao.difx.difxcontroller.*;

import edu.nrao.difx.difxutilities.DiFXCommand_mkdir;
import edu.nrao.difx.difxutilities.DiFXCommand_vex2difx;

import edu.nrao.difx.xmllib.difxmessage.DifxMessage;

import edu.nrao.difx.difxdatabase.DBConnection;
import edu.nrao.difx.difxdatabase.QueueDBConnection;

import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;

import java.sql.ResultSet;

public class QueueBrowserPanel extends TearOffPanel {

    public QueueBrowserPanel( SystemSettings systemSettings, MessageDisplayPanel messageDisplay ) {
        _systemSettings = systemSettings;
        _systemSettings.queueBrowser( this );
        _messageDisplay = messageDisplay;
        setLayout( null );
        _browserPane = new NodeBrowserScrollPane( 20 );
        this.add( _browserPane );
        addKeyListener( new KeyEventListener() );
        _browserPane.setBackground( Color.WHITE );
        _mainLabel = new JLabel( "Queue Browser" );
        _mainLabel.setBounds( 5, 0, 150, 20 );
        _mainLabel.setFont( new Font( "Dialog", Font.BOLD, 14 ) );
        add( _mainLabel );
        //  The popup menu for the "New" button.
        _newMenu = new JPopupMenu();
        JMenuItem newExperimentItem = new JMenuItem( "Experiment" );
        newExperimentItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                newExperiment();
            }
        });
        _newMenu.add( newExperimentItem );
        _newButton = new JButton( "New..." );
        _newButton.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _newMenu.show( _newButton, 0, 25 );
            }
        });
        this.add( _newButton );
        //  The menu for the "select" button.
        _selectMenu = new JPopupMenu();
        JMenuItem selectAllItem = new JMenuItem( "Select All" );
        selectAllItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                selectAll();
            }
        });
        _selectMenu.add( selectAllItem );
        JMenuItem unselectAllItem = new JMenuItem( "Unselect All" );
        unselectAllItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                unselectAll();
            }
        });
        _selectMenu.add( unselectAllItem );
        _selectMenu.add( new JSeparator() );
        JMenuItem runSelectedItem = new JMenuItem( "Run Selected" );
        runSelectedItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                runSelected();
            }
        });
        _selectMenu.add( runSelectedItem );
        runSelectedItem.setEnabled( false );
        JMenuItem deleteSelectedItem = new JMenuItem( "Delete Selected" );
        deleteSelectedItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                deleteSelected();
            }
        });
        _selectMenu.add( deleteSelectedItem );
        _selectButton = new JButton( "Select" );
        _selectButton.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _selectMenu.show( _selectButton, 0, 25 );
            }
        });
        this.add( _selectButton );
        //  The menu for the "show" button.
        _showMenu = new JPopupMenu();
        _showSelectedItem = new JCheckBoxMenuItem( "Selected" );
        _showSelectedItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                showItemChange();
            }
        });
        _showSelectedItem.setSelected( _systemSettings.queueBrowserSettings().showSelected );
        _showMenu.add( _showSelectedItem );
        _showUnselectedItem = new JCheckBoxMenuItem( "Unselected" );
        _showUnselectedItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                showItemChange();
            }
        });
        _showUnselectedItem.setSelected( _systemSettings.queueBrowserSettings().showUnselected );
        _showMenu.add( _showUnselectedItem );
        _showCompletedItem = new JCheckBoxMenuItem( "Completed" );
        _showCompletedItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                showItemChange();
            }
        });
        _showCompletedItem.setSelected( _systemSettings.queueBrowserSettings().showCompleted );
        _showMenu.add( _showCompletedItem );
        _showIncompleteItem = new JCheckBoxMenuItem( "Incomplete" );
        _showIncompleteItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                showItemChange();
            }
        });
        _showIncompleteItem.setSelected( _systemSettings.queueBrowserSettings().showIncomplete );
        _showMenu.add( _showIncompleteItem );
        _showMenu.add( new JSeparator() );
        JMenuItem expandAllItem = new JMenuItem( "Expand All" );
        expandAllItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                expandAll();
            }
        });
        _showMenu.add( expandAllItem );
        JMenuItem collapseAllItem = new JMenuItem( "Collapse All" );
        collapseAllItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                collapseAll();
            }
        });
        _showMenu.add( collapseAllItem );
        _showButton = new JButton( "Show..." );
        _showButton.addActionListener(new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                _showMenu.show( _showButton, 0, 25 );
            }
        });
        this.add( _showButton );
        _updateButton = new JButton( "DB Update" );
        _updateButton.setToolTipText( "Update queue data from the DiFX database." );
        _updateButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                updateNow( true );
            }
        });
        this.add( _updateButton );
        _autoButton = new JButton( "" );
        _autoButton.setToolTipText( "Auto update DiFX queue." );
        _autoButton.setMargin( new Insets( 2, 4, 2, 4 ) );
        this.add( _autoButton );
        _autoActiveLight = new ActivityMonitorLight();
        _autoActiveLight.setBounds( 4, 4, 16, 21 );
        _autoActiveLight.onColor( Color.GREEN );
        _autoButton.add( _autoActiveLight );
        _autoButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                autoButtonAction();
            }
        });
        

        //  Set up a repeating timeout that occurs every 10th of a second.
        Action updateDatabaseAction = new AbstractAction() {
            @Override
            public void actionPerformed( ActionEvent e ) {
                databaseTimeoutEvent();
            }
        };
        
        //  Create a header line of all jobs.
        _header = new JobNodesHeader();
        _browserPane.addNode( _header );

        //  This thread is used to update from the database (updates can hang if
        //  the database can't be located).
        _updateLoop = new UpdateLoop();
        _updateLoop.start();
        
        new Timer( 1000, updateDatabaseAction ).start();

    }
    
    /*
     * This method allows me to control resize behavior.  Otherwise I have to
     * leave it up to the layouts, which is a disaster.
     */
    @Override
    public void setBounds(int x, int y, int width, int height) {
        _browserPane.setBounds( 0, 60, width, height - 60 );
        super.setBounds( x, y, width, height );
        _newButton.setBounds( 5, 30, 100, 25 );
        _selectButton.setBounds( 110, 30, 100, 25 );
        _showButton.setBounds( 215, 30, 100, 25 );
        _updateButton.setBounds( width - 120, 30, 110, 25 );
        _autoButton.setBounds( width - 145, 30, 25, 25 );
    }

    /*
     * Set the data model, which provides us with data from DiFX.
     */
    public void dataModel( DiFXDataModel newModel ) {
        _dataModel = newModel;
        _dataModel.addJobMessageListener( new AttributedMessageListener() {
            @Override
            public void update( DifxMessage difxMsg ) {
                serviceUpdate( difxMsg );
            }
        } );
    }
    
    protected class KeyEventListener extends KeyAdapter {    
        public void keyPressed( KeyEvent e ) {
            System.out.println( "push " + e.getKeyCode() );
        }    
        public void keyReleased( KeyEvent e ) {
            System.out.println( "release " + e.getKeyCode() );
        }
    }

    /*
     * Add a new experiment to the browser.
     */
    public void addExperiment( ExperimentNode newExperiment ) {
        _browserPane.addNode( newExperiment );
    }
    
    /*
     * Add a new job to the header.  This is so changes in header column widths
     * apply to the job.
     */
    public void addJob( JobNode newJob ) {
        _header.addJob( newJob );
    }
    
    /*
     * Allow the user to produce a new experiment by bringing up the Experiment
     * Editor.
     */
    protected void newExperiment() {
        //  If the user is currently using the data base, try to connect to it.
        //  Failure, or no attept to connect, will leave "db" as null, indicating
        //  we should try creating an experiment without using the data base.
        QueueDBConnection db = null;
        if ( _systemSettings.useDataBase() ) {
            db = new QueueDBConnection( _systemSettings );
            if ( !db.connected() )
                db = null;
        }

        //  Generate an ID number, which is also used to generate an initial name.
        //  We do this using the database if possible, or by looking at other
        //  experiments if not.
        Integer newExperimentId = 1;
        if ( db != null ) {
            //  Scan the database for the highest experiment ID in existence.  Then
            //  assume the data base software will assign an ID number for this experiment
            //  that is 1 higher than this number.
            ResultSet dbExperimentList = db.experimentList();
            try {
                //  Parse out the ID numbers.  We don't care about the names.
                while ( dbExperimentList.next() ) {
                    int newId = dbExperimentList.getInt( "id" );
                    if ( newId >= newExperimentId )
                        newExperimentId = newId + 1;
                }
            } catch ( Exception e ) {
                    java.util.logging.Logger.getLogger( "global" ).log( java.util.logging.Level.SEVERE, null, e );
            }
        }
        else {
            //  Look at all existing experiments that are NOT in the database and
            //  get their ID numbers.  Find the largest and increment by 1.
            BrowserNode experimentList = _browserPane.browserTopNode();
            if ( experimentList.children().size() > 1 ) {
                Iterator<BrowserNode> iter = experimentList.childrenIterator();
                iter.next();
                for ( ; iter.hasNext(); ) {
                    ExperimentNode thisExperiment = (ExperimentNode)(iter.next());
                    if ( thisExperiment.id() != null && thisExperiment.id() >= newExperimentId )
                        newExperimentId = thisExperiment.id() + 1;
                }
            }
        }
        
        //  Open a window where the user can specify details of the new experiment.
        Point pt = _newButton.getLocationOnScreen();
        ExperimentEditor win =
                new ExperimentEditor( pt.x + 25, pt.y + 25, _systemSettings );
                //new ExperimentEditor( (Frame)comp, pt.x + 25, pt.y + 25, _systemSettings );
        win.setTitle( "Create New Experiment" );
        win.number( 0 );
        win.name( "Experiment_" + newExperimentId.toString() );
        win.id( newExperimentId );
        if ( db != null )
            win.inDataBase( true );
        else
            win.inDataBase( false );
        String creationDate = (new SimpleDateFormat( "yyyy-mm-dd HH:mm:ss" )).format( new Date() );
        win.created( creationDate );
        win.status( "unknown" );
        win.directory( _systemSettings.workingDirectory() + "/" + win.name() );
        win.vexFileName( win.name() + ".vex" );
        win.addVexFileName( "one" );
        win.addVexFileName( "two" );
        win.addVexFileName( "three" );
        win.keepDirectory( false );
        win.passName( "Production Pass" );
        win.createPass( _systemSettings.defaultNames().createPassOnExperimentCreation );
        win.newExperimentMode( true );
        win.visible();
    }
        
    protected void selectAll() {};
    protected void unselectAll() {};
    protected void runSelected() {};
    protected void deleteSelected() {};
    protected void showItemChange() {};
    protected void expandAll() {};
    protected void collapseAll() {};
                
    /*
     * The user has hit the "auto" button.  This activates the "auto" light and
     * causes auto updates to occur.  Or it turns them off.
     */
    public void autoButtonAction() {
        if ( _systemSettings.dbAutoUpdate() ) {
            _systemSettings.dbAutoUpdate( false );
        }
        else {
            _systemSettings.dbAutoUpdate( true );
        }
    }
    
    /*
     * Timeout event for reading the database.
     */
    public void databaseTimeoutEvent() {
        //  See if we are doing "auto" updates of the queue.
        if ( _systemSettings.dbAutoUpdate() ) {
            //  Auto updates will be performed at the interval specified unless they
            //  have just been turned on (in which case an immediate update will be
            //  performed).
            if ( _timeoutCounter == 0 )
                updateNow( true );
            //  Otherwise we just check the status of each job that we know about.  If
            //  a job has been removed we will detect that, too.
            else
                checkQueueStatusFromDatabase();
            ++_timeoutCounter;
            if ( _timeoutCounter >= _systemSettings.dbAutoUpdateInterval() )
                _timeoutCounter = 0;
        }
        else
            _timeoutCounter = 0;
    }
    
    protected synchronized void updateNow( boolean newVal ) { _updateNow = newVal; }
    
    /*
     * Internal thread used to keep the database queries from tying up the graphics
     * update.
     */
    class UpdateLoop extends Thread {
        
        public void run() {
            
            while ( true ) {
                try {
                    Thread.sleep( 100 );
                    if ( _updateNow ) {
                       updateQueueFromDatabase();
                       updateNow( false );
                    }
                } catch( java.lang.InterruptedException e ) {
                }
            }
            
        }
        
    }

    /*
     * Update our list of experiments, passes, and nodes from the database.  This
     * pulls everything off the database and uses it to change our current list.
     */
    void updateQueueFromDatabase() {
        
        //  Get a new connection to the database.  Bail out if this doesn't work.
        QueueDBConnection db = new QueueDBConnection( _systemSettings );
        if ( !db.connected() )
            return;
        
        //  Get lists of all experiments, passes, and jobs in the database.
        ResultSet dbExperimentList = db.experimentList();
        ResultSet dbPassList = db.passList();
        ResultSet dbJobList = db.jobList();
        ResultSet dbPassTypeList = db.passTypeList();
        ResultSet dbJobStatusList = db.jobStatusList();
        ResultSet dbSlotList = db.slotList();
        ResultSet dbModule = db.moduleList();
        ResultSet dbExperimentAndModule = db.experimentAndModuleList();
        ResultSet dbExperimentStatus = db.experimentStatusList();
        
        //  Getting this far indicates a successful update from the queue.  
        //  Flash the indicator light!
        _autoActiveLight.on( true );

        //  We need to track the addition and deletion of items in the data base.
        //  To make this possible, we set a "found" flag to false in each item we
        //  already know about - if we don't "find" any one item again, we'll
        //  remove it.
        BrowserNode experimentList = _browserPane.browserTopNode();
        if ( experimentList.children().size() > 1 ) {
            //  The first item in the browser list is not actually an experiment -
            //  it is the header.  We skip it.
            Iterator<BrowserNode> iter = experimentList.childrenIterator();
            iter.next();
            for ( ; iter.hasNext(); ) {
                QueueBrowserNode thisExperiment = (QueueBrowserNode)(iter.next());
                thisExperiment.found( false );
                //  Within each experiment, flag passes....
                for ( Iterator<BrowserNode> pIter = thisExperiment.childrenIterator(); pIter.hasNext(); ) {
                    QueueBrowserNode thisPass = (QueueBrowserNode)(pIter.next());
                    thisPass.found( false );
                    //  Within each pass, flag each job...
                    for ( Iterator<BrowserNode> jIter = thisPass.childrenIterator(); jIter.hasNext(); )
                        ((QueueBrowserNode)(jIter.next())).found( false );
                }
            }
        }
        
        //  Database operations generate exceptions here and there....
        try {
            //  Run through each experiment in the data base and see if we know about
            //  it already in our list.  If we do, set the "found" flag.  If we don't,
            //  add it to the list.
            while ( dbExperimentList.next() ) {
                String name = dbExperimentList.getString( "code" );
                Integer id = dbExperimentList.getInt( "id" );
                Integer number = dbExperimentList.getInt( "number" );
                Integer status = dbExperimentList.getInt( "statusID" );
                String dateCreated = dbExperimentList.getString( "dateCreated" );
                System.out.println( name + "  " + id.toString() + " " + number.toString()
                        + "  " + status.toString() + "  " + dateCreated );
                //  Find a match in our experiment list.
                ExperimentNode thisExperiment = null;
                experimentList = _browserPane.browserTopNode();
                if ( experimentList.children().size() > 1 ) {
                    Iterator<BrowserNode> iter = experimentList.childrenIterator();
                    iter.next();
                    for ( ; iter.hasNext(); ) {
                        ExperimentNode testExperiment = (ExperimentNode)(iter.next());
                        //  We should be able to use the ID to match experiments, as it is
                        //  supposed to be unique.
                        if ( testExperiment.inDataBase() && testExperiment.idMatch( id ) )
                            thisExperiment = testExperiment;
                    }
                }
                //  Create a new experiment if we didn't find the named one.
                if ( thisExperiment == null ) {
                    thisExperiment = new ExperimentNode( name, _systemSettings );
                    thisExperiment.id( id );
                    thisExperiment.inDataBase( true );
                    thisExperiment.creationDate( dateCreated );
                    //thisExperiment.segment( segment );
                    _browserPane.addNode( thisExperiment );
                }
                //  Flag the experiment as "found" so we don't eliminate it.
                thisExperiment.found( true );
            }
            //  Next run through the list of passes, locating them in the experiments.
            while ( dbPassList.next() ) {
                String name = dbPassList.getString( "passName" );
                Integer id = dbPassList.getInt( "id" );
                Integer experimentId = dbPassList.getInt( "experimentID" );
                Integer passTypeID = dbPassList.getInt( "passTypeID" );
                String passType = null;
                dbPassTypeList.beforeFirst();
                while ( dbPassTypeList.next() )
                    if ( passTypeID == dbPassTypeList.getInt( "id" ) )
                        passType = dbPassTypeList.getString( "type" );
                PassNode thisPass = null;
                ExperimentNode thisExperiment = null;
                experimentList = _browserPane.browserTopNode();
                if ( experimentList.children().size() > 1 ) {
                    Iterator<BrowserNode> iter = experimentList.childrenIterator();
                    iter.next();
                    for ( ; iter.hasNext(); ) {
                        ExperimentNode testExperiment = (ExperimentNode)(iter.next());
                        //  Match the experiment ID.
                        if ( testExperiment.idMatch( experimentId ) ) {
                            thisExperiment = testExperiment;
                            //  Then find the pass in the experiment.
                            for ( Iterator<BrowserNode> pIter = testExperiment.childrenIterator(); pIter.hasNext(); ) {
                                PassNode testPass = (PassNode)(pIter.next());
                                //  Match the pass ID.
                                if ( id == testPass.id() )
                                    thisPass = testPass;
                            }
                        }
                    }
                }
                //  If this pass wasn't encountered in the list of experiments, see
                //  if it is floating outside the experiment list or if it needs to
                //  be added somewhere.
                if ( thisPass == null ) {
                    //  Was the experiment for this pass identified at least?
                    if ( thisExperiment != null ) {
                        //  Okay, it must be a new pass in the experiment - add it.
                        thisPass = new PassNode( name, _systemSettings );
                        thisPass.type( passType );
                        thisPass.id( id );
                        thisPass.experimentNode( thisExperiment );
                        thisExperiment.addChild( thisPass );                        
                    }
                    else {
                        //  TODO:  accomodate passes outside of the experiment structure
                        //  Right.  This is a "floating" pass outside the experiment
                        //  list.  See if it already exists in our list of such
                        //  things.
                        
                        //  It wasn't found, so add it.
                    }
                }
                thisPass.found( true );
            }
            //  Find each job from the data base in our lists.
            while ( dbJobList.next() ) {
                Integer id = dbJobList.getInt( "id" );
                Integer passId = dbJobList.getInt( "passID" );
                //  Locate this job within the proper pass.  Both are identified
                //  by ID's, so screwed up ID's will likely kill us.
                PassNode thisPass = null;
                JobNode thisJob = null;
                ExperimentNode thisExperiment = null;
                experimentList = _browserPane.browserTopNode();
                if ( experimentList.children().size() > 1 ) {
                    Iterator<BrowserNode> iter = experimentList.childrenIterator();
                    iter.next();
                    boolean noJobMatch = true;
                    for ( ; iter.hasNext() && noJobMatch; ) {
                        ExperimentNode testExperiment = (ExperimentNode)(iter.next());
                        for ( Iterator<BrowserNode> pIter = testExperiment.childrenIterator(); pIter.hasNext() && noJobMatch; ) {
                            PassNode testPass = (PassNode)(pIter.next());
                            if ( passId == testPass.id() ) {
                                thisPass = testPass;
                                thisExperiment = testExperiment;
                                for ( Iterator<BrowserNode> jIter = thisPass.childrenIterator(); jIter.hasNext() && noJobMatch; ) {
                                    JobNode testJob = (JobNode)(jIter.next());
                                    if ( id.intValue() == testJob.id().intValue() ) {
                                        noJobMatch = false;
                                        thisJob = testJob;
                                        thisJob.found( true );
                                    }
                                }
                            }
                        }
                    }
                }
                //  Add the job if we haven't found it.
                if ( thisJob == null ) {
                    //  Did we find the pass?
                    if ( thisPass != null ) {
                        //  Generate a job name, either from the input file if that
                        //  works or from the pass name and job number.
                        File tryFile = new File( dbJobList.getString( "inputFile" ) );
                        Integer jobNumber = dbJobList.getInt( "jobNumber" );
                        String jobName = null;
                        if ( tryFile != null ) {
                            jobName = tryFile.getName().substring( 0, tryFile.getName().lastIndexOf( "." ) );
                        }
                        if ( jobName == null )
                            jobName = thisPass.name() + "_" + jobNumber.toString();
                        thisJob = new JobNode( jobName, _systemSettings );
                        thisJob.id( id );
                        thisJob.experiment( thisExperiment.name() );
                        thisJob.pass( thisPass.name() );
                        thisJob.passNode( thisPass );
                        thisPass.addChild( thisJob );
                        _header.addJob( thisJob ); 
                    }
                    else {
                        //  Floating job - figure out what to do with this, if anything.
                    }
                }
                //  Fill in all information about the job if it was found.
                if ( thisJob != null ) {
                    thisJob.found( true );
                    thisJob.inputFile( dbJobList.getString( "inputFile" ) );
                    thisJob.priority( dbJobList.getInt("priority") );
                    thisJob.queueTime( dbJobList.getString( "queueTime" ) );
                    thisJob.correlationStart( dbJobList.getString( "correlationStart" ) );
                    thisJob.correlationEnd( dbJobList.getString( "correlationEnd" ) );
                    thisJob.jobStart( dbJobList.getDouble( "jobStart" ) );
                    thisJob.jobDuration( dbJobList.getDouble( "jobDuration" ) ); 
                    thisJob.inputFile( dbJobList.getString( "inputFile" ) );
                    thisJob.outputFile( dbJobList.getString( "outputFile" ) );
                    thisJob.outputSize( dbJobList.getInt( "outputSize" ) );
                    thisJob.difxVersion( dbJobList.getString( "difxVersion" ) );
                    thisJob.speedUpFactor( dbJobList.getDouble( "speedupFactor" ) );
                    thisJob.numAntennas( dbJobList.getInt( "numAntennas" ) );
                    thisJob.numForeignAntennas( dbJobList.getInt( "numForeign" ) );
                    thisJob.dutyCycle( dbJobList.getString( "dutyCycle" ) );
                    thisJob.status( "unknown" );
                    thisJob.active( false );
                    thisJob.statusId( dbJobList.getInt( "statusID" ) );
                    Integer statusIdInt = dbJobList.getInt( "statusID" );
                    dbJobStatusList.beforeFirst();
                    if ( dbJobStatusList.next() ) {
                        thisJob.status( dbJobStatusList.getString( "status" ) );
                        thisJob.active( dbJobStatusList.getBoolean( "active" ) );
                    }
                }
            }
        } catch ( Exception e ) {
            System.out.println( e );
            e.printStackTrace();
        }

        //  Eliminate any items we have failed to find in the data base,
        //  with the exception of those that aren't actually in the data base.
        experimentList = _browserPane.browserTopNode();
        if ( experimentList.children().size() > 1 ) {
            Iterator<BrowserNode> iter = experimentList.childrenIterator();
            iter.next();
            for ( ; iter.hasNext(); ) {
                ExperimentNode thisExperiment = (ExperimentNode)(iter.next());
                if ( !thisExperiment.found() && thisExperiment.inDataBase() )
                    _browserPane.browserTopNode().removeChild( thisExperiment );
                else {
                    //  Eliminate passes under each experiment...
                    for ( Iterator<BrowserNode> pIter = thisExperiment.childrenIterator(); pIter.hasNext(); ) {
                        PassNode thisPass = (PassNode)(pIter.next());
                        if ( !thisPass.found() && thisPass.inDataBase() )
                            thisExperiment.removeChild( thisPass );
                        else {
                            //  Eliminate jobs under each pass.
                            for ( Iterator<BrowserNode> jIter = thisPass.childrenIterator(); jIter.hasNext(); ) {
                                JobNode thisJob = (JobNode)(jIter.next());
                                if ( !thisJob.found() && thisJob.inDataBase() )
                                    thisPass.removeChild( thisJob );
                            }
                        }
                    }
                }
            }
        }
        
    }
    
    public Iterator<BrowserNode> experimentsIterator() {
        return _browserPane.browserTopNode().children().iterator();
    }
    
    /*
     * Check the status of all known jobs in the database and update any information
     * that changes.  Most items related to a job do not change in the database, so
     * we don't need to check much.  This function will also figure out if a job has
     * been de-queued, i.e. removed from the database.
     */
    protected void checkQueueStatusFromDatabase() {
    }
    
    /*
     * This method services a data message from the data model.  It adds information
     * about any projects or jobs to appropriate lists, creating new nodes on the lists
     * as new items appear.
     * 
     * IT LOOKS LIKE THIS FUNCTION IS NOT USED!!!!
     */
    protected void serviceDataUpdatePoop() {
        
        //  This would be unlikely...
        if ( _dataModel == null )
            return;
        
        //  Get all jobs the data model knows about.
        List<Job> jobs = _dataModel.getJobs();
        
        //  Change the displayed properties for each job.
        if ( jobs != null ) {
            //  Run through each unit in the list of Mark5 modules and change their 
            //  displayed properties.
            for ( Iterator<Job> iter = jobs.iterator(); iter.hasNext(); ) {
                Job thisJob = iter.next();
                System.out.println( thisJob.getJobID() ); 
                //  Find the node in our browser that represents this unit.
                ExperimentNode processor = null;
//                for ( Iterator<BrowserNode> iter2 = _clusterNodes.children().iterator(); iter2.hasNext(); ) {
//                    BrowserNode thisModule = iter2.next();
//                    if ( thisModule.name().equals( thisProcessor.getObjName() ) )
//                        processor = (ClusterNode)thisModule;
//                }
//                //  If there was no node representing this unit, create one.
//                if ( processor == null ) {
//                    processor = new ClusterNode( thisProcessor.getObjName() );
//                    _clusterNodes.addChild( processor );
//                }
//                //  Update the processor with new data.
//                processor.setData( thisProcessor );
            }
        }
        
    }
    
    /*
     * Parse a difx message relayed to us from the data model.  This (presumably)
     * contains some information about a job.
     */
    public void serviceUpdate( DifxMessage difxMsg ) {
        
        //  See if this message looks like it is for a job.  Only proceed if it
        //  does.
        if ( JobNode.testJobMessage( difxMsg ) ) {
        
            //  The identifier provides us with the job name.  Lacking anything else
            //  to go on, we use the job name to locate the job in our current list of
            //  jobs.
            JobNode thisJob = null;
            //  Loop through each "experiment" and "pass" to find this job.
            Iterator<BrowserNode> projectIter = _browserPane.browserTopNode().children().iterator();
            projectIter.next();
            for ( ; projectIter.hasNext() && thisJob == null; ) {
                ExperimentNode testExperiment = (ExperimentNode)projectIter.next();
                PassNode thisPass = null;
                if ( testExperiment.children().size() > 0 ) {
                    for ( Iterator<BrowserNode> iter = testExperiment.childrenIterator(); iter.hasNext(); ) {
                        PassNode testPass = (PassNode)(iter.next());
                        //  Within each project, look at all jobs...
                        for ( Iterator<BrowserNode> jobIter = testPass.children().iterator(); 
                            jobIter.hasNext() && thisJob == null; ) {
                            JobNode testJob = (JobNode)jobIter.next();
                            if ( testJob.name().equals( difxMsg.getHeader().getIdentifier() ) )
                                thisJob = testJob;
                        }
                    }
                }
            }

            //  If we didn't find this job, create an entry for it in the "unaffiliated"
            //  project (which we might have to create if it doesn't exist!).

            if ( thisJob == null ) {
                if ( _unaffiliated == null ) {
                    _unaffiliated = new ExperimentNode( "Jobs Outside Queue", _systemSettings );
                    _browserPane.addNode( _unaffiliated );
                    _unknown = new PassNode( "", _systemSettings );
                    _unknown.experimentNode( _unaffiliated );
                    _unknown.setHeight( 0 );
                    _unaffiliated.addChild( _unknown );
                }
                thisJob = new JobNode( difxMsg.getHeader().getIdentifier(), _systemSettings );
                _unknown.addChild( thisJob );
                thisJob.passNode( _unknown );
                _header.addJob( thisJob );
            }

            //  Send the message to the job node.
            thisJob.consumeMessage( difxMsg );
        
        }
        
    }  
    
    protected NodeBrowserScrollPane _browserPane;
    protected JLabel _mainLabel;
    protected JButton _updateButton;
    DiFXDataModel  _dataModel;
    DiFXController _controller;
    MessageListener _mListener;
    protected ExperimentNode _unaffiliated;
    protected PassNode _unknown;
    protected SystemSettings _systemSettings;
    protected MessageDisplayPanel _messageDisplay;
    protected int _timeoutCounter;
    protected JButton _autoButton;
    protected ActivityMonitorLight _autoActiveLight;
    protected JobNodesHeader _header;
    protected boolean _updateNow;
    protected UpdateLoop _updateLoop;
    protected JButton _newButton;
    protected JPopupMenu _newMenu;
    protected JButton _selectButton;
    protected JPopupMenu _selectMenu;
    protected JButton _showButton;
    protected JPopupMenu _showMenu;
    protected JCheckBoxMenuItem _showSelectedItem;
    protected JCheckBoxMenuItem _showUnselectedItem;
    protected JCheckBoxMenuItem _showCompletedItem;
    protected JCheckBoxMenuItem _showIncompleteItem;
    
}
