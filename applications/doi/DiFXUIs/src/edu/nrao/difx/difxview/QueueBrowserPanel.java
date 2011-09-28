/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.nrao.difx.difxview;

import mil.navy.usno.widgetlib.NodeBrowserScrollPane;
import mil.navy.usno.widgetlib.BrowserNode;
import javax.swing.JPanel;
import javax.swing.JLabel;

import java.awt.Font;
import java.awt.Color;

import java.util.List;
import java.util.Iterator;

import edu.nrao.difx.difxdatamodel.*;
import edu.nrao.difx.difxcontroller.*;

import edu.nrao.difx.xmllib.difxmessage.DifxMessage;
import edu.nrao.difx.xmllib.difxmessage.DifxStatus;
import edu.nrao.difx.xmllib.difxmessage.DifxStatus.Weight;

public class QueueBrowserPanel extends JPanel {

    public QueueBrowserPanel() {
        setLayout( null );
        _browserPane = new NodeBrowserScrollPane();
        this.add( _browserPane );
        _browserPane.setBackground( Color.WHITE );
        _mainLabel = new JLabel( "Queue Browser" );
        _mainLabel.setBounds( 5, 0, 150, 20 );
        _mainLabel.setFont( new Font( "Dialog", Font.BOLD, 12 ) );
        add( _mainLabel );
        ProjectNode project1 = new ProjectNode( "Project 1" );
        _browserPane.addNode( project1 );
        ProjectNode project2 = new ProjectNode( "Project 2" );
        _browserPane.addNode( project2 );
        JobNode job1 = new JobNode( "Job 1" );
        project1.addChild( job1 );
        JobNode job2 = new JobNode( "Job 2" );
        project1.addChild( job2 );
        JobNode job3 = new JobNode( "Job 3" );
        project2.addChild( job3 );
        JobNode job4 = new JobNode( "Job 4" );
        project2.addChild( job4 );
        JobNode job5 = new JobNode( "Job 5" );
        project2.addChild( job5 );
        JobNode job6 = new JobNode( "Job 6" );
        project2.addChild( job6 );
        JobNode job7 = new JobNode( "Job 7" );
        project2.addChild( job7 );
        JobNode job8 = new JobNode( "Job 8" );
        project2.addChild( job8 );
        JobNode job9 = new JobNode( "Job 9" );
        project2.addChild( job9 );
        JobNode joba = new JobNode( "Job a" );
        project2.addChild( joba );
        JobNode jobb = new JobNode( "Job b" );
        project2.addChild( jobb );
        JobNode jobc = new JobNode( "Job c" );
        project2.addChild( jobc );
        JobNode jobd = new JobNode( "Job d" );
        project2.addChild( jobd );
        JobNode jobe = new JobNode( "Job e" );
        project2.addChild( jobe );
//        ProjectNode project3 = new ProjectNode( "Project 3" );
//        _browserPane.addNode( project3 );
//        ProjectNode project4 = new ProjectNode( "Big Project" );
//        _browserPane.addNode( project4 );
//        for ( int i = 0; i < 50; ++i ) {
//            JobNode job = new JobNode( "Another Job " + i );
//            project4.addChild( job );
//        }

    }
    
    /*
     * This method allows me to control resize behavior.  Otherwise I have to
     * leave it up to the layouts, which is a disaster.
     */
    @Override
    public void setBounds(int x, int y, int width, int height) {
        _browserPane.setBounds( 0, 70, width, height - 70 );
        super.setBounds( x, y, width, height );
    }

    /*
     * Set the data model, which provides us with data from DiFX.
     */
    public void dataModel( DiFXDataModel newModel ) {
        _mDataModel = newModel;
        // create a listener that calls our local function
//        _mListener = new MessageListener() {
//            @Override
//            public void update() {
//                serviceDataUpdate();
//            }
//        };
//        // hand DataModel a call back listener
//        _mDataModel.attachListener( _mListener );
        _mDataModel.addJobMessageListener( new AttributedMessageListener() {
            @Override
            public void update( DifxMessage difxMsg ) {
                serviceUpdate( difxMsg );
            }
        } );
    }
    
    /*
     * This method services a data message from the data model.  It adds information
     * about any projects or jobs to appropriate lists, creating new nodes on the lists
     * as new items appear.
     */
    protected void serviceDataUpdate() {
        
        //  This would be unlikely...
        if ( _mDataModel == null )
            return;
        
        //  Get all jobs the data model knows about.
        List<Job> jobs = _mDataModel.getJobs();
        
        //  Change the displayed properties for each job.
        if ( jobs == null ) {
            System.out.println( "no jobs" );
        }
        else {
            //  Run through each unit in the list of Mark5 modules and change their 
            //  displayed properties.
            for ( Iterator<Job> iter = jobs.iterator(); iter.hasNext(); ) {
                Job thisJob = iter.next();
                System.out.println( thisJob.getJobID() ); 
                //  Find the node in our browser that represents this unit.
                JobNode processor = null;
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
        
        //System.out.println( "\n\nNEW JOB MESSAGE!!!!!!" );
        //System.out.println( difxMsg.getHeader().getIdentifier() );
        if ( difxMsg.getHeader().getIdentifier().equals( "mk5daemon" ) ) {
            if ( difxMsg.getBody().getDifxAlert() != null )
                System.out.println( "this is an alert" );
            System.out.println( difxMsg.getHeader().getFrom() );
        }
        
        //  The identifier provides us with the job name.  Lacking anything else
        //  to go on, we use the job name to locate the job in our current list of
        //  jobs.
        JobNode thisJob = null;
        //  Loop through each "project"
        for ( Iterator<BrowserNode> projectIter = _browserPane.browserTopNode().children().iterator(); 
                projectIter.hasNext() && thisJob == null; ) {
            ProjectNode testProject = (ProjectNode)projectIter.next();
            //  Within each project, look at all jobs...
            for ( Iterator<BrowserNode> jobIter = testProject.children().iterator(); 
                jobIter.hasNext() && thisJob == null; ) {
                JobNode testJob = (JobNode)jobIter.next();
                if ( testJob.name().equals( difxMsg.getHeader().getIdentifier() ) )
                    thisJob = testJob;
            }
        }
        
        //  If we didn't find this job, create an entry for it in the "unaffiliated"
        //  project (which we might have to create if it doesn't exist!).

        if ( thisJob == null ) {
            if ( _unaffiliated == null ) {
                _unaffiliated = new ProjectNode( "Unaffiliated" );
                _browserPane.addNode( _unaffiliated );
            }
            thisJob = new JobNode( difxMsg.getHeader().getIdentifier() );
            _unaffiliated.addChild( thisJob );
        }

        //  Send the message to the job node.
        thisJob.consumeMessage( difxMsg );
        
    }  

    private NodeBrowserScrollPane _browserPane;
    private JLabel _mainLabel;
    DiFXDataModel  _mDataModel;
    DiFXController _mController;
    MessageListener _mListener;
    protected ProjectNode _unaffiliated;
    
}
