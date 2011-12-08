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

import java.awt.Font;
import java.awt.Color;
import java.awt.Insets;

import java.io.File;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.Timer;

import java.util.List;
import java.util.Iterator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import edu.nrao.difx.difxdatamodel.*;
import edu.nrao.difx.difxcontroller.*;

import edu.nrao.difx.xmllib.difxmessage.DifxMessage;

import edu.nrao.difx.difxdatabase.DBConnection;

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
        _updateButton = new JButton( "Update" );
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
        

//        ProjectNode project1 = new ProjectNode( "Project 1" );
//        _browserPane.addNode( project1 );
//        ProjectNode project2 = new ProjectNode( "Project 2" );
//        _browserPane.addNode( project2 );
//        ExperimentNode job1 = new ExperimentNode( "Job 1" );
//        project1.addChild( job1 );
//        ExperimentNode job2 = new ExperimentNode( "Job 2" );
//        project1.addChild( job2 );
//        ExperimentNode job3 = new ExperimentNode( "Job 3" );
//        project2.addChild( job3 );
//        ExperimentNode job4 = new ExperimentNode( "Job 4" );
//        project2.addChild( job4 );
//        ExperimentNode job5 = new ExperimentNode( "Job 5" );
//        project2.addChild( job5 );
//        ExperimentNode job6 = new ExperimentNode( "Job 6" );
//        project2.addChild( job6 );
//        ExperimentNode job7 = new ExperimentNode( "Job 7" );
//        project2.addChild( job7 );
//        ExperimentNode job8 = new ExperimentNode( "Job 8" );
//        project2.addChild( job8 );
//        ExperimentNode job9 = new ExperimentNode( "Job 9" );
//        project2.addChild( job9 );
//        ExperimentNode joba = new ExperimentNode( "Job a" );
//        project2.addChild( joba );
//        ExperimentNode jobb = new ExperimentNode( "Job b" );
//        project2.addChild( jobb );
//        ExperimentNode jobc = new ExperimentNode( "Job c" );
//        project2.addChild( jobc );
//        ExperimentNode jobd = new ExperimentNode( "Job d" );
//        project2.addChild( jobd );
//        ExperimentNode jobe = new ExperimentNode( "Job e" );
//        project2.addChild( jobe );
//        ProjectNode project3 = new ProjectNode( "Project 3" );
//        _browserPane.addNode( project3 );
//        ProjectNode project4 = new ProjectNode( "Big Project" );
//        _browserPane.addNode( project4 );
//        for ( int i = 0; i < 50; ++i ) {
//            ExperimentNode job = new ExperimentNode( "Another Job " + i );
//            project4.addChild( job );
//        }

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
        
//        for ( Integer i = 0; i < 30; ++i )
//            createDummyJob( ("sample job " + i.toString() ) );

        
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

        //  Create a new database connection using the current system settings.
        DBConnection dbConnection = new DBConnection( _systemSettings.dbURL(), _systemSettings.jdbcDriver(),
                _systemSettings.dbSID(), _systemSettings.dbPWD() );

        try {
            dbConnection.connectToDB();

            //  Grab all of the job information from the database.
            ResultSet jobInfo = dbConnection.selectData( "select * from " + _systemSettings.dbName() + ".Job" );

            //  Getting this far indicates a successful update from the queue.  
            //  Flash the indicator light!
            _autoActiveLight.on( true );
            
            //  For each job, parse out everything we need to know about it.
            while ( jobInfo.next() ) {

                //  Jobs have names, and are also identified by the "pass" they are
                //  in.  The pass is then contained in an "experiment".  All three
                //  items are used to identify the job uniquely.
                String passName = "unknown";
                String experimentName = "unknown";
                String passType = "unknown";
                Integer passId = jobInfo.getInt( "passID" );
                ResultSet passInfo = dbConnection.selectData( "select * from " + _systemSettings.dbName() + ".Pass where id="
                        + passId.toString() );
                Integer experimentId = -1;
                if ( passInfo.next() ) {
                    passName = passInfo.getString( "passName" );
                    experimentId = passInfo.getInt( "experimentID" );
                    ResultSet expInfo = dbConnection.selectData( "select code from " + _systemSettings.dbName() + ".Experiment where id="
                            + experimentId.toString() );
                    if ( expInfo.next() )
                        experimentName = expInfo.getString( "code" );
                    Integer passTypeId = passInfo.getInt( "passTypeID" );
                    ResultSet passTypeInfo = dbConnection.selectData( "select type from " + _systemSettings.dbName() + ".PassType where id="
                            + passTypeId.toString() );
                    if ( passTypeInfo.next() )
                        passType = passTypeInfo.getString( "type" );
                }
                Integer jobNumber = jobInfo.getInt( "jobNumber" );
                
                //  Construct a job name.  We first try to do this using the input file
                //  name.  Failing that, we do it using the pass name and job number.
                String jobName = null;
                File tryFile = new File( jobInfo.getString( "inputFile" ) );
                if ( tryFile != null ) {
                    jobName = tryFile.getName().substring( 0, tryFile.getName().lastIndexOf( "." ) );
                }
                if ( jobName == null )
                    jobName = passName + "_" + jobNumber.toString();
                
                //  Try to match this job to the existing hierarchy of experiments,
                //  passes, and job names.  If any of these things do not exist, we
                //  will assume this is a new job and create them.  First, the
                //  experiment....
                ExperimentNode thisExperiment = null;
                BrowserNode experimentList = _browserPane.browserTopNode();
                if ( experimentList.children().size() > 1 ) {
                    //  The first item in the browser list is not actually an experiment -
                    //  it is the header.  We skip it.
                    Iterator<BrowserNode> iter = experimentList.childrenIterator();
                    iter.next();
                    //  Now look at the rest.
                    for ( ; iter.hasNext(); ) {
                        ExperimentNode testExperiment = (ExperimentNode)(iter.next());
                        if ( testExperiment.name().contentEquals( experimentName ) )
                            thisExperiment = testExperiment;
                     }
                }
                //  Create a new experiment if we didn't find the named one.
                if ( thisExperiment == null ) {
                    thisExperiment = new ExperimentNode( experimentName );
                    _browserPane.addNode( thisExperiment );
                }
                
                //  Now find the pass in the experiment...
                PassNode thisPass = null;
                if ( thisExperiment.children().size() > 0 ) {
                    for ( Iterator<BrowserNode> iter = thisExperiment.childrenIterator(); iter.hasNext(); ) {
                        PassNode testPass = (PassNode)(iter.next());
                        if ( testPass.name().contentEquals( passName ) )
                            thisPass = testPass;
                     }
                }
                //  Create a new pass if we didn't find the named one.
                if ( thisPass == null ) {
                    thisPass = new PassNode( passName );
                    thisPass.type( passType );
                    thisPass.experimentNode( thisExperiment );
                    thisExperiment.addChild( thisPass );
                }
                
                //  Finally, find the job in the pass...
                JobNode thisJob = null;
                if ( thisPass.children().size() > 0 ) {
                    for ( Iterator<BrowserNode> iter = thisPass.childrenIterator(); iter.hasNext(); ) {
                        JobNode testJob = (JobNode)(iter.next());
                        if ( testJob.name().contentEquals( jobName ) && 
                                testJob.inputFile().contentEquals( jobInfo.getString( "inputFile" ) ) )
                            thisJob = testJob;
                     }
                }
                //  Create a new job if we didn't find the named one.
                if ( thisJob == null ) {
                    thisJob = new JobNode( jobName, _systemSettings );
                    thisJob.experiment( thisExperiment.name() );
                    thisJob.pass( thisPass.name() );
                    thisJob.passNode( thisPass );
                    thisPass.addChild( thisJob );
                    _header.addJob( thisJob );
                }
    
                thisJob.priority( jobInfo.getInt("priority") );
                thisJob.queueTime( jobInfo.getString( "queueTime" ) );
                thisJob.correlationStart( jobInfo.getString( "correlationStart" ) );
                thisJob.correlationEnd( jobInfo.getString( "correlationEnd" ) );
                thisJob.jobStart( jobInfo.getDouble( "jobStart" ) );
                thisJob.jobDuration( jobInfo.getDouble( "jobDuration" ) ); 
                thisJob.inputFile( jobInfo.getString( "inputFile" ) );
                thisJob.outputFile( jobInfo.getString( "outputFile" ) );
                thisJob.outputSize( jobInfo.getInt( "outputSize" ) );
                thisJob.difxVersion( jobInfo.getString( "difxVersion" ) );
                thisJob.speedUpFactor( jobInfo.getDouble( "speedupFactor" ) );
                thisJob.numAntennas( jobInfo.getInt( "numAntennas" ) );
                thisJob.numForeignAntennas( jobInfo.getInt( "numForeign" ) );
                thisJob.dutyCycle( jobInfo.getString( "dutyCycle" ) );
                thisJob.status( "unknown" );
                thisJob.active( false );
                thisJob.statusId( jobInfo.getInt( "statusID" ) );
                Integer statusIdInt = jobInfo.getInt( "statusID" );
                ResultSet jobStatusInfo = dbConnection.selectData( "select status, active from " + _systemSettings.dbName() + ".JobStatus where id="
                        + statusIdInt.toString() );
                if ( jobStatusInfo.next() ) {
                    thisJob.status( jobStatusInfo.getString( "status" ) );
                    thisJob.active( jobStatusInfo.getBoolean( "active" ) );
                }

            }
            
            //  Turn the auto active light to green so we know database updates
            //  are working.
            _autoActiveLight.onColor( Color.GREEN );

        } catch ( java.sql.SQLException e ) {
            _messageDisplay.error( 0, "QueueBrowswer.updateQueueFromDatabase()",
                    ( "SQLException: " + e.getMessage() ) );
            _messageDisplay.error( 0, "QueueBrowswer.updateQueueFromDatabase()",
                    "Check Database Configuration parameters in the Settings Menu" );
            _autoActiveLight.onColor( Color.RED );
        } catch ( ClassNotFoundException e ) {
            String message =
                    "Failed to find database driver [" + e.getMessage() + "]";
            _messageDisplay.error( 0, "QueueBrowswer.updateQueueFromDatabase()", message );
            _messageDisplay.error( 0, "QueueBrowswer.updateQueueFromDatabase()",
                    "Check Database Configuration parameters in the Settings Menu" );
            _autoActiveLight.onColor( Color.RED );
        } catch ( Exception e ) {
            String message =
                    "Failed to connect to database [" + e.getMessage() + "]";
            _messageDisplay.error( 0, "QueueBrowswer.updateQueueFromDatabase()", message );
            _messageDisplay.error( 0, "QueueBrowswer.updateQueueFromDatabase()",
                    "Check Database Configuration parameters in the Settings Menu" );
            _autoActiveLight.onColor( Color.RED );
        }

    }
    
    public Iterator<BrowserNode> experimentsIterator() {
        return _browserPane.browserTopNode().children().iterator();
    }
    
    /*
     * Check the status of all known jobs in the database and update any information
     * that changes.  Most items related to a job do not change in the database, so
     * we don't need check much.  This function will also figure out if a job has
     * been de-queued, i.e. removed from the database.
     */
    protected void checkQueueStatusFromDatabase() {
    }
    
    /*
     * This method services a data message from the data model.  It adds information
     * about any projects or jobs to appropriate lists, creating new nodes on the lists
     * as new items appear.
     */
    protected void serviceDataUpdate() {
        
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
                    _unaffiliated = new ExperimentNode( "Jobs Outside Queue" );
                    _browserPane.addNode( _unaffiliated );
                    _unknown = new PassNode( "" );
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
    
    public void createDummyJob( String name ) {
        if ( _unaffiliated == null ) {
            _unaffiliated = new ExperimentNode( "Jobs Outside Queue" );
            _browserPane.addNode( _unaffiliated );
            _unknown = new PassNode( "" );
            _unknown.experimentNode( _unaffiliated );
            _unknown.setHeight( 0 );
            _unaffiliated.addChild( _unknown );
        }
        JobNode thisJob = new JobNode( name, _systemSettings );
        _unknown.addChild( thisJob );
        thisJob.passNode( _unknown );
        _header.addJob( thisJob );
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
    
}
