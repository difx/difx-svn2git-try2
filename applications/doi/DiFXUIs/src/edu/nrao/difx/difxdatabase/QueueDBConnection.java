/*
 * This extension of the generic DBConnection class includes convenience functions
 * for interacting with the DiFX queue data base.
 * 
 * Exceptions are intercepted here but generally ignored as the parent class
 * produces messages associated with them.
 */
package edu.nrao.difx.difxdatabase;

import java.sql.ResultSet;

import edu.nrao.difx.difxview.SystemSettings;

/**
 *
 * @author jspitzak
 */
public class QueueDBConnection extends DBConnection {
    
    public QueueDBConnection( SystemSettings settings ) {
        super( "jdbc:mysql://" + settings.dbHost() + ":" + settings.jdbcPort() + "/mysql",
                settings.jdbcDriver(), settings.dbSID(), settings.dbPWD() );
        try {
            this.connectToDB();
        } catch ( Exception e ) {
        }
        _settings = settings;
    }
    
    /*
     * Generate a list of all experiments in the data base.
     */
    public ResultSet experimentList() {
        try {
            return this.selectData( "select * from " + _settings.dbName() + ".Experiment" );
        } catch ( Exception e ) {
            return null;
        }
    }
    
    /*
     * Generate a list of all passes in the data base.
     */
    public ResultSet passList() {
        try {
            return this.selectData( "select * from " + _settings.dbName() + ".Pass" );
        } catch ( Exception e ) {
            return null;
        }
    }
    
    /*
     * Generate a list of all jobs in the data base.
     */
    public ResultSet jobList() {
        try {
            return this.selectData( "select * from " + _settings.dbName() + ".Job" );
        } catch ( Exception e ) {
            return null;
        }
    }
    
    /*
     * Generate a list of all pass types in the data base.
     */
    public ResultSet passTypeList() {
        try {
            return this.selectData( "select * from " + _settings.dbName() + ".PassType" );
        } catch ( Exception e ) {
            return null;
        }
    }
    
    /*
     * Generate a list of all job status types in the data base.
     */
    public ResultSet jobStatusList() {
        try {
            return this.selectData( "select * from " + _settings.dbName() + ".JobStatus" );
        } catch ( Exception e ) {
            return null;
        }
    }
    
    public ResultSet slotList() {
        try {
            return this.selectData( "select * from " + _settings.dbName() + ".Slot" );
        } catch ( Exception e ) {
            return null;
        }
    }
    
    public ResultSet moduleList() {
        try {
            return this.selectData( "select * from " + _settings.dbName() + ".Module" );
        } catch ( Exception e ) {
            return null;
        }
    }
    
    public ResultSet experimentAndModuleList() {
        try {
            return this.selectData( "select * from " + _settings.dbName() + ".ExperimentAndModule" );
        } catch ( Exception e ) {
            return null;
        }
    }
    
    public ResultSet experimentStatusList() {
        try {
            return this.selectData( "select * from " + _settings.dbName() + ".ExperimentStatus" );
        } catch ( Exception e ) {
            return null;
        }
    }
    
    /*
     * Delete the given experiment from the database.  The experiment is identified
     * by its unique ID.
     */
    public void deleteExperiment( Integer id ) {
        //  We can't handle null ID's.
        if ( id == null )
            return;
        try {
            //  Handle the possibility that the segment is null.
            this.updateData( "delete from " + _settings.dbName() + 
                    ".Experiment"
                    + " where id = \"" + id.toString() + "\"" );
        } catch ( Exception e ) {
            java.util.logging.Logger.getLogger( "global" ).log( java.util.logging.Level.SEVERE, null, e );
        }
    }
    SystemSettings _settings;
    
    /*
     * Create a new experiment.  The data base will automatically assign a unique
     * ID number, but we give it name, number (used to be called "segment"), and
     * initial status.  Return whether this operation was successful or not.
     */
    public boolean newExperiment( String name, Integer number ) {
        if ( !this.connected() )
            return false;
        try {
        int updateCount = this.updateData( "insert into " + _settings.dbName() + 
                        ".Experiment (code, number) values( \"" + name + "\", \"" + number.toString() + "\" )" );
        if ( updateCount > 0 )
            return true;
        else
            return false;
        } catch ( Exception e ) {
            java.util.logging.Logger.getLogger( "global" ).log( java.util.logging.Level.SEVERE, null, e );
            return false;
        }
    }
    
    /*
     * Update an element of a specified experiment (identified by ID).  It will change
     * a specific field to a specific value - both are strings.  Return the number of items
     * updated.
     */
    public int updateExperiment( Integer id, String param, String setting ) {
        if ( id == null )
            return 0;
        try {
            //  Deal with a possible null segment.
            return this.updateData( "update " + _settings.dbName()
                    +  ".Experiment set " + param + " = \"" + setting + "\""
                    + " where id = \"" + id.toString() + "\"" );
        } catch ( Exception e ) {
            java.util.logging.Logger.getLogger( "global" ).log( java.util.logging.Level.SEVERE, null, e );
            return 0;
        }
    }
    
}
