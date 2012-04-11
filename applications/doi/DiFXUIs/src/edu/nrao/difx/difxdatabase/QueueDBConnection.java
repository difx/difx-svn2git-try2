/*
 * Use the generic DBConnection class in convenience functions
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
public class QueueDBConnection {
    
    public QueueDBConnection( SystemSettings settings ) {
        _settings = settings;
        //  Don't connect to the database if the user is not using it!
        if ( !_settings.useDataBase() ) {
            _db = null;
            return;
        }
        _db = new DBConnection( "jdbc:mysql://" + _settings.dbHost() + ":" + _settings.jdbcPort() + "/mysql",
                _settings.jdbcDriver(), settings.dbSID(), _settings.dbPWD() );
        try {
            _db.connectToDB();
        } catch ( Exception e ) {
        }
    }
    
    /*
     * Return whether we are actually connected to the database.
     */
    public boolean connected() {
        if ( _db == null )
            return false;
        return _db.connected();
    }
    
    /*
     * Generate a list of all experiments in the data base.
     */
    public ResultSet experimentList() {
        try {
            return _db.selectData( "select * from " + _settings.dbName() + ".Experiment" );
        } catch ( Exception e ) {
            return null;
        }
    }
    
    /*
     * Generate a list of all passes in the data base.
     */
    public ResultSet passList() {
        try {
            return _db.selectData( "select * from " + _settings.dbName() + ".Pass" );
        } catch ( Exception e ) {
            return null;
        }
    }
    
    /*
     * Generate a list of all jobs in the data base.
     */
    public ResultSet jobList() {
        try {
            return _db.selectData( "select * from " + _settings.dbName() + ".Job" );
        } catch ( Exception e ) {
            return null;
        }
    }
    
    /*
     * Generate a list of all pass types in the data base.
     */
    public ResultSet passTypeList() {
        try {
            return _db.selectData( "select * from " + _settings.dbName() + ".PassType" );
        } catch ( Exception e ) {
            return null;
        }
    }
    
    /*
     * Generate a list of all job status types in the data base.
     */
    public ResultSet jobStatusList() {
        try {
            return _db.selectData( "select * from " + _settings.dbName() + ".JobStatus" );
        } catch ( Exception e ) {
            return null;
        }
    }
    
    public ResultSet slotList() {
        try {
            return _db.selectData( "select * from " + _settings.dbName() + ".Slot" );
        } catch ( Exception e ) {
            return null;
        }
    }
    
    public ResultSet moduleList() {
        try {
            return _db.selectData( "select * from " + _settings.dbName() + ".Module" );
        } catch ( Exception e ) {
            return null;
        }
    }
    
    public ResultSet experimentAndModuleList() {
        try {
            return _db.selectData( "select * from " + _settings.dbName() + ".ExperimentAndModule" );
        } catch ( Exception e ) {
            return null;
        }
    }
    
    public ResultSet experimentStatusList() {
        try {
            return _db.selectData( "select * from " + _settings.dbName() + ".ExperimentStatus" );
        } catch ( Exception e ) {
            return null;
        }
    }
    
    /*
     * Generate a list of all jobs in the data base with the given limitation.
     */
    public ResultSet jobListLimited( String limitation ) {
        try {
            return _db.selectData( "select * from " + _settings.dbName() + ".Job where " + limitation );
        } catch ( Exception e ) {
            return null;
        }
    }
    
    /*
     * Generate a list of all jobs in the data base in the given Pass.
     */
    public ResultSet jobListByPassId( int passId ) {
        try {
            return jobListLimited( "passID = " + passId );
        } catch ( Exception e ) {
            return null;
        }
    }
    
    /*
     * Create a new experiment.  The data base will automatically assign a unique
     * ID number, but we give it name, number (used to be called "segment"), 
     * initial status, directory, and vex filename.  Return whether this operation 
     * was successful or not.
     */
    public boolean newExperiment( String name, Integer number, Integer statusId, String directory, String vexFileName ) {
        if ( !this.connected() )
            return false;
        try {
        int updateCount = _db.updateData( "insert into " + _settings.dbName() + 
                        ".Experiment (code, number, statusID, directory, vexfile) values("
                + " \"" + name + "\","
                + " \"" + number.toString() + "\","
                + " \"" + statusId.toString() + "\","
                + " \"" + directory + "\","
                + " \"" + vexFileName + "\" )" );
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
     * Create a new pass.  The pass is given a name, type, and experiment ID, and
     * (hopefully soon) a directory and vex file name.
     */
    public boolean newPass( String name, Integer typeId, Integer experimentId ) {
        if ( !this.connected() )
            return false;
        try {
        int updateCount = _db.updateData( "insert into " + _settings.dbName() + 
                        ".Pass (experimentID, passName, passTypeID) values("
                + " \"" + experimentId.toString() + "\","
                + " \"" + name + "\","
                + " \"" + typeId.toString() + "\" )" );
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
     * Create a new job.
     */
    public boolean newJob( String name, Integer passId, Integer jobNumber, Double jobStart,
            Double jobDuration, String inputFile, String difxVersion, Integer numAntennas,
            Integer numForeign, Integer statusId ) {
        if ( !this.connected() )
            return false;
        try {
        int updateCount = _db.updateData( "insert into " + _settings.dbName() + 
                        ".Job (passID, jobNumber, jobStart, jobDuration, inputFile, difxVersion, numAntennas, numForeign, statusID) values("
                + " \"" + passId.toString() + "\","
                + " \"" + jobNumber.toString() + "\","
                + " \"" + jobStart.toString() + "\","
                + " \"" + jobDuration.toString() + "\","
                + " \"" + inputFile + "\","
                + " \"" + difxVersion + "\","
                + " \"" + numAntennas.toString() + "\","
                + " \"" + numForeign.toString() + "\","
                + " \"" + statusId.toString() + "\" )" );
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
     * Delete the given experiment from the database.  The experiment is identified
     * by its unique ID.
     */
    public void deleteExperiment( Integer id ) {
        //  We can't handle null ID's.
        if ( id == null )
            return;
        try {
            _db.updateData( "delete from " + _settings.dbName() + 
                    ".Experiment"
                    + " where id = \"" + id.toString() + "\"" );
        } catch ( Exception e ) {
            java.util.logging.Logger.getLogger( "global" ).log( java.util.logging.Level.SEVERE, null, e );
        }
    }
    
    /*
     * Delete the given pass from the database.  The pass is identified
     * by its unique ID.
     */
    public void deletePass( Integer id ) {
        //  We can't handle null ID's.
        if ( id == null )
            return;
        try {
            _db.updateData( "delete from " + _settings.dbName() + 
                    ".Pass"
                    + " where id = \"" + id.toString() + "\"" );
        } catch ( Exception e ) {
            java.util.logging.Logger.getLogger( "global" ).log( java.util.logging.Level.SEVERE, null, e );
        }
    }
    
    /*
     * Delete the given job from the database.  The job is identified
     * by its unique ID.
     */
    public void deleteJob( Integer id ) {
        //  We can't handle null ID's.
        if ( id == null )
            return;
        try {
            _db.updateData( "delete from " + _settings.dbName() + 
                    ".Job"
                    + " where id = \"" + id.toString() + "\"" );
        } catch ( Exception e ) {
            java.util.logging.Logger.getLogger( "global" ).log( java.util.logging.Level.SEVERE, null, e );
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
            return _db.updateData( "update " + _settings.dbName()
                    +  ".Experiment set " + param + " = \"" + setting + "\""
                    + " where id = \"" + id.toString() + "\"" );
        } catch ( Exception e ) {
            java.util.logging.Logger.getLogger( "global" ).log( java.util.logging.Level.SEVERE, null, e );
            return 0;
        }
    }
    
    /*
     * Update an element of a specified pass (identified by ID).  It will change
     * a specific field to a specific value - both are strings.  Return the number of items
     * updated.
     */
    public int updatePass( Integer id, String param, String setting ) {
        if ( id == null )
            return 0;
        try {
            return _db.updateData( "update " + _settings.dbName()
                    +  ".Pass set " + param + " = \"" + setting + "\""
                    + " where id = \"" + id.toString() + "\"" );
        } catch ( Exception e ) {
            java.util.logging.Logger.getLogger( "global" ).log( java.util.logging.Level.SEVERE, null, e );
            return 0;
        }
    }
    
    /*
     * Update an element of a specified job (identified by ID).  It will change
     * a specific field to a specific value - both are strings.  Return the number of items
     * updated.
     */
    public int updateJob( Integer id, String param, String setting ) {
        if ( id == null )
            return 0;
        try {
            return _db.updateData( "update " + _settings.dbName()
                    +  ".Job set " + param + " = \"" + setting + "\""
                    + " where id = \"" + id.toString() + "\"" );
        } catch ( Exception e ) {
            java.util.logging.Logger.getLogger( "global" ).log( java.util.logging.Level.SEVERE, null, e );
            return 0;
        }
    }
    
    DBConnection _db;
    SystemSettings _settings;
    
}
