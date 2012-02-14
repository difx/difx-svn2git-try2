/*
 * The VexFileParser class is used to pull data out of a .vex file and store it
 * in an organized way such that other classes can obtain parts of the data
 * easily.  It also allows changes to those data.
 * 
 * "File" is a bit of a misnomer, as the class actually operates on a String of
 * data (presumably the contents of a .vex file).  It can also produce a string
 * of data in .vex format.
 * 
 * Vex files are pretty complex, and not all aspects of them are implemented here.
 * Basically, the class is limited to what is necessary for the GUI project, at
 * least for now.
 * 
 * For the most part, we assume that the .vex data are properly formatted.
 */
package edu.nrao.difx.difxview;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 *
 * @author jspitzak
 */
public class VexFileParser {
    
    /*
     * Accept a string of data as the contents of a .vex file.  Parse the contents
     * and organize it.
     */
    public void data( String str ) {
        _str = str;
        _length = _str.length();
        
        //  See if the first line contains the revision.  This might help us in
        //  accommodating future changes.
        if ( _str.regionMatches( true, 0, "VEX_REV", 0, 7 ) ) {
            int loc = _str.indexOf( '=' );
            try {
                int endln = _str.indexOf( '\n' );
                _revision = Double.parseDouble( _str.substring( loc + 1, endln - 1 ) );
            } catch ( NumberFormatException e ) {
                _revision = -1.0; //  negative revision indicates we don't know it.
            }
        }
        
        //  Run through every line of the data and locate "sections".
        boolean endOfFile = false;
        String sectionName = "nothing";
        ArrayList<String> sectionData = new ArrayList<String>();
        while ( !endOfFile ) {
            String line = nextLine();
            if ( line == null )
                endOfFile = true;
            else {
                //  See if the line looks like the start of a new section.
                if ( line.charAt( 0 ) == '$' ) {
                    //  Figure out what to do with the collected data based on the
                    //  section type (of the previous section!).  Any sections we
                    //  don't know about we simply ignore.
                    if ( sectionName.equalsIgnoreCase( "$STATION" ) )
                        parseStationData( sectionData );
                    else if ( sectionName.equalsIgnoreCase( "$ANTENNA" ) )
                        parseAntennaData( sectionData );
                    else if ( sectionName.equalsIgnoreCase( "$FREQ" ) )
                        parseFreqData( sectionData );
                    else if ( sectionName.equalsIgnoreCase( "$SCHED" ) )
                        parseSchedData( sectionData );
                    else if ( sectionName.equalsIgnoreCase( "$SITE" ) )
                        parseSiteData( sectionData );
                    else if ( sectionName.equalsIgnoreCase( "$SOURCE" ) )
                        parseSourceData( sectionData );
                    //  Clear the data list and record this new section.
                    sectionName = line;
                    sectionData.clear();
                }
                else
                    //  Tack this string on the list associated with our current
                    //  section.
                    sectionData.add( line );
            }
        }
    }
    
    /*
     * Return a string containing the "next" line in the data.  Lines terminate in
     * semicolons (omitted).  Any whitespace they start with is trimmed.  Comments
     * are ignored - they are "*" characters that are the first non-whitespace
     * characters.  They continue until a newline character is reached.
     */
    protected String nextLine() {
        boolean found = false;
        String line = null;
        //  Get the start of a line...
        while ( !found ) {
            //  Find the first non-whitespace character before the file ends.
            while ( _pos < _length && ( _str.charAt( _pos ) == ' ' || _str.charAt( _pos ) == '\t' || _str.charAt( _pos ) == '\n' ) )
                ++_pos;
            //  End of the string?
            if ( _pos >= _length )
                return null;
            //  Is the character a "*"?  That means the start of a comment.
            if ( _str.charAt( _pos ) == '*' ) {
                //  Get to the end of this line.
                while ( _pos < _length && _str.charAt( _pos ) != '\n' )
                    ++_pos;
            }
            //  Otherwise, this is the start of a line.
            else {
                //  Locate the semicolon that terminates it.
                int endln = _str.indexOf( ';', _pos );
                //  If there wasn't one, the string must have terminated.
                if ( endln == -1 ) {
                    _pos = _length + 1;
                    return null;
                }
                //  Otherwise, we're in good shape.
                found = true;
                line = _str.substring( _pos, endln );
                _pos = endln + 1;
            }
        }
        return line;
    }
    
    /*
     * Reset the position used in the "nextLine()" function to the start of the
     * data.
     */
    protected void rewind() {
        _pos = 0;
    }
    
    /*
     * Extract "station" data from a list of data lines.
     */
    protected void parseStationData( ArrayList<String> data ) {
        Station currentStation = null;
        for ( Iterator<String> iter = data.iterator(); iter.hasNext(); ) {
            String thisLine = iter.next();
            //  Find the "scan" string indicating the start of a scan.
            if ( thisLine.length() > 3 && thisLine.substring( 0, 3 ).equalsIgnoreCase( "DEF" ) ) {
                //  Create a new scan.
                currentStation = new Station();
                currentStation.name = thisLine.substring( thisLine.indexOf( ' ' ) ).trim().toUpperCase();
                currentStation.dasList = new ArrayList<String>();
            }
            else if ( thisLine.length() > 3 && thisLine.substring( 0, 3 ).equalsIgnoreCase( "REF" ) ) {
                //  Trim off the "ref = $" crap.
                thisLine = thisLine.substring( thisLine.indexOf( '$' ) + 1 );
                if ( thisLine.length() > 3 && thisLine.substring( 0, 3 ).equalsIgnoreCase( "DAS" ) ) {
                    if ( currentStation != null ) {
                        String newDas = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
                        currentStation.dasList.add( newDas );
                    }
                }
                else if ( thisLine.length() > 4 && thisLine.substring( 0, 4 ).equalsIgnoreCase( "SITE" ) ) {
                    if ( currentStation != null ) {
                        currentStation.site = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
                    }
                }
                else if ( thisLine.length() > 7 && thisLine.substring( 0, 7 ).equalsIgnoreCase( "ANTENNA" ) ) {
                    if ( currentStation != null ) {
                        currentStation.antenna = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
                    }
                }
            }
            else if ( thisLine.length() >= 6 && thisLine.substring( 0, 6 ).equalsIgnoreCase( "ENDDEF" ) ) {
                if ( currentStation != null ) {
                    //  Add the current scan to the list of scans.
                    if ( _stationList == null )
                        _stationList = new ArrayList<Station>();
                    _stationList.add( currentStation );
//                    System.out.println( ">" + currentStation.name );
//                    System.out.println( ">" + currentStation.site );
//                    System.out.println( ">" + currentStation.antenna );
//                    for ( Iterator<String> jter = currentStation.dasList.iterator(); jter.hasNext(); ) {
//                        System.out.println( ">" + jter.next() );
//                    }
                }
            }
        }
    }

    /*
     * Extract "antenna" data from a list of data lines.
     */
    protected void parseAntennaData( ArrayList<String> data ) {
        Antenna currentAntenna = null;
        for ( Iterator<String> iter = data.iterator(); iter.hasNext(); ) {
            String thisLine = iter.next();
            //  Find the "scan" string indicating the start of a scan.
            if ( thisLine.length() > 3 && thisLine.substring( 0, 3 ).equalsIgnoreCase( "DEF" ) ) {
                //  Create a new scan.
                currentAntenna = new Antenna();
                currentAntenna.name = thisLine.substring( thisLine.indexOf( ' ' ) ).trim().toUpperCase();
                currentAntenna.motion = new ArrayList<String>();
            }
            else if ( thisLine.length() > 9 && thisLine.substring( 0, 9 ).equalsIgnoreCase( "AXIS_TYPE" ) ) {
                if ( currentAntenna != null ) {
                    currentAntenna.axis_type = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
                }
            }
            else if ( thisLine.length() > 11 && thisLine.substring( 0, 11 ).equalsIgnoreCase( "AXIS_OFFSET" ) ) {
                if ( currentAntenna != null ) {
                    currentAntenna.axis_offset = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
                }
            }
            else if ( thisLine.length() > 12 && thisLine.substring( 0, 12 ).equalsIgnoreCase( "ANTENNA_DIAM" ) ) {
                if ( currentAntenna != null ) {
                    currentAntenna.diameter = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
                }
            }
            else if ( thisLine.length() > 14 && thisLine.substring( 0, 14 ).equalsIgnoreCase( "ANTENNA_MOTION" ) ) {
                if ( currentAntenna != null ) {
                    currentAntenna.motion.add( thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim() );
                }
            }
            else if ( thisLine.length() > 15 && thisLine.substring( 0, 15 ).equalsIgnoreCase( "POINTING_SECTOR" ) ) {
                if ( currentAntenna != null ) {
                    currentAntenna.pointing_sector = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
                }
            }
            else if ( thisLine.length() >= 6 && thisLine.substring( 0, 6 ).equalsIgnoreCase( "ENDDEF" ) ) {
                if ( currentAntenna != null ) {
                    //  Add the current scan to the list of scans.
                    if ( _antennaList == null )
                        _antennaList = new ArrayList<Antenna>();
                    _antennaList.add( currentAntenna );
//                    System.out.println( ">" + currentAntenna.name );
//                    System.out.println( ">" + currentAntenna.diameter );
//                    System.out.println( ">" + currentAntenna.axis_type );
//                    System.out.println( ">" + currentAntenna.axis_offset );
//                    System.out.println( ">" + currentAntenna.pointing_sector );
//                    for ( Iterator<String> jter = currentAntenna.motion.iterator(); jter.hasNext(); ) {
//                        System.out.println( ">" + jter.next() );
//                    }
                }
            }
        }
    }

    /*
     * Extract "freq" data from a list of data lines.
     */
    protected void parseFreqData( ArrayList<String> data ) {
        //System.out.println( "frequency data" );
    }

    /*
     * Extract "sched" data from a list of data lines.  These data include all of
     * the scans.
     */
    protected void parseSchedData( ArrayList<String> data ) {
        Scan currentScan = null;
        for ( Iterator<String> iter = data.iterator(); iter.hasNext(); ) {
            String thisLine = iter.next();
            //  Find the "scan" string indicating the start of a scan.
            if ( thisLine.length() >= 4 && thisLine.substring( 0, 4 ).equalsIgnoreCase( "SCAN" ) ) {
                //  Create a new scan.
                currentScan = new Scan();
                currentScan.name = thisLine.substring( 5 );
                currentScan.station = new ArrayList<ScanStation>();
            }
            else if ( thisLine.length() >= 7 && thisLine.substring( 0, 7 ).equalsIgnoreCase( "ENDSCAN" ) ) {
                if ( currentScan != null ) {
                    //  Add the current scan to the list of scans.
                    if ( _scanList == null )
                        _scanList = new ArrayList<Scan>();
                    _scanList.add( currentScan );
                }
            }
            else if ( thisLine.length() > 5 && thisLine.substring( 0, 5 ).equalsIgnoreCase( "START" ) ) {
                //  Convert the start time to a Java Calendar format.
                if ( currentScan != null ) {
                    currentScan.start = new GregorianCalendar();
                    currentScan.start.set( Calendar.YEAR, Integer.parseInt( thisLine.substring( 8, 12 )));
                    currentScan.start.set( Calendar.DAY_OF_YEAR, Integer.parseInt( thisLine.substring( 13, 16 )));
                    currentScan.start.set( Calendar.HOUR_OF_DAY, Integer.parseInt( thisLine.substring( 17, 19 )));
                    currentScan.start.set( Calendar.MINUTE, Integer.parseInt( thisLine.substring( 20, 22 )));
                    currentScan.start.set( Calendar.SECOND, Integer.parseInt( thisLine.substring( 23, 25 )));
                }
            }
            else if ( thisLine.length() > 4 && thisLine.substring( 0, 4 ).equalsIgnoreCase( "MODE" ) ) {
                if ( currentScan != null ) {
                    currentScan.mode = thisLine.substring( 7 );
                }
            }
            else if ( thisLine.length() > 6 && thisLine.substring( 0, 6 ).equalsIgnoreCase( "SOURCE" ) ) {
                if ( currentScan != null ) {
                    currentScan.source = thisLine.substring( 9 );
                }
            }
            else if ( thisLine.length() > 7 && thisLine.substring( 0, 7 ).equalsIgnoreCase( "STATION" ) ) {
                if ( currentScan != null ) {
                    ScanStation newStation = new ScanStation();
                    //  The "station" line contains the name of the station...
                    int sPos = thisLine.indexOf( '=', 7 ) + 1;
                    int ePos = thisLine.indexOf( ':', sPos );
                    newStation.name = thisLine.substring( sPos, ePos ).trim();
                    //  The delay-from-start time (in seconds)...
                    sPos = ePos + 1;
                    ePos = thisLine.indexOf( 's', sPos + 1 );
                    newStation.delay = Integer.parseInt( thisLine.substring( sPos, ePos ).trim() );
                    //  The duration time (in seconds)...
                    sPos = thisLine.indexOf( ':', ePos ) + 1;
                    ePos = thisLine.indexOf( 's', sPos + 1 );
                    newStation.duration = Integer.parseInt( thisLine.substring( sPos, ePos ).trim() );
                    //  And the rest of the line that we currently don't know what to do with.
                    sPos = thisLine.indexOf( ':', ePos ) + 1;
                    newStation.otherStuff = thisLine.substring( sPos );
                    //  Make a copy of the whole string.
                    newStation.wholeString = thisLine;
                    //  Add this station to the list of stations associated with this
                    //  scan.
                    currentScan.station.add( newStation );
                }
            }
        }
    }

    /*
     * Extract "site" data from a list of data lines.  This includes the location
     * of antennas involved.
     */
    protected void parseSiteData( ArrayList<String> data ) {
        Site currentSite = null;
        for ( Iterator<String> iter = data.iterator(); iter.hasNext(); ) {
            String thisLine = iter.next();
            //  Find the "scan" string indicating the start of a scan.
            if ( thisLine.length() > 3 && thisLine.substring( 0, 3 ).equalsIgnoreCase( "DEF" ) ) {
                //  Create a new scan.
                currentSite = new Site();
                currentSite.name = thisLine.substring( thisLine.indexOf( ' ' ) ).trim().toUpperCase();
            }
            else if ( thisLine.length() > 9 && thisLine.substring( 0, 9 ).equalsIgnoreCase( "SITE_TYPE" ) ) {
                if ( currentSite != null ) {
                    currentSite.type = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
                }
            }
            else if ( thisLine.length() > 9 && thisLine.substring( 0, 9 ).equalsIgnoreCase( "SITE_NAME" ) ) {
                if ( currentSite != null ) {
                    currentSite.site_name = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
                }
            }
            else if ( thisLine.length() > 7 && thisLine.substring( 0, 7 ).equalsIgnoreCase( "SITE_ID" ) ) {
                if ( currentSite != null ) {
                    currentSite.id = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
                }
            }
            else if ( thisLine.length() > 13 && thisLine.substring( 0, 13 ).equalsIgnoreCase( "SITE_POSITION" ) ) {
                if ( currentSite != null ) {
                    currentSite.position = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
                }
            }
            else if ( thisLine.length() > 14 && thisLine.substring( 0, 14 ).equalsIgnoreCase( "HORIZON_MAP_AZ" ) ) {
                if ( currentSite != null ) {
                    currentSite.horizon_map_az = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
                }
            }
            else if ( thisLine.length() > 14 && thisLine.substring( 0, 14 ).equalsIgnoreCase( "HORIZON_MAP_EL" ) ) {
                if ( currentSite != null ) {
                    currentSite.horizon_map_el = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
                }
            }
            else if ( thisLine.length() > 15 && thisLine.substring( 0, 15 ).equalsIgnoreCase( "OCCUPATION_CODE" ) ) {
                if ( currentSite != null ) {
                    currentSite.occupation_code = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
                }
            }
            else if ( thisLine.length() >= 6 && thisLine.substring( 0, 6 ).equalsIgnoreCase( "ENDDEF" ) ) {
                if ( currentSite != null ) {
                    //  Add the current scan to the list of scans.
                    if ( _siteList == null )
                        _siteList = new ArrayList<Site>();
                    _siteList.add( currentSite );
//                    System.out.println( ">" + currentSite.name );
//                    System.out.println( ">" + currentSite.type );
//                    System.out.println( ">" + currentSite.site_name );
//                    System.out.println( ">" + currentSite.id );
//                    System.out.println( ">" + currentSite.position );
//                    System.out.println( ">" + currentSite.horizon_map_az );
//                    System.out.println( ">" + currentSite.horizon_map_el );
//                    System.out.println( ">" + currentSite.occupation_code );
                }
            }
        }
    }

    /*
     * Extract "source" data from a list of data lines.  These data contain all
     * of the source information for the observations.
     */
    protected void parseSourceData( ArrayList<String> data ) {
        //System.out.println( "source data" );
    }

    public double revision() { return _revision; }
    public ArrayList<Scan> scanList() { return _scanList; }
    public ArrayList<Station> stationList() { return _stationList; }
    
    public class ScanStation {
        String name;
        int delay;
        int duration;
        String otherStuff;
        String wholeString;
    }
    
    public class Scan {
        String name;
        Calendar start;
        String mode;
        String source;
        ArrayList<ScanStation> station;
    }
    
    public class Station {
        String name;
        String site;
        String antenna;
        ArrayList<String> dasList;
    }
    
    public class Antenna {
        String name;
        String diameter;
        String axis_type;
        String axis_offset;
        String pointing_sector;
        ArrayList<String> motion;
    }
    
    public class Site {
        String name;
        String type;
        String site_name;
        String id;
        String position;
        String horizon_map_az;
        String horizon_map_el;
        String occupation_code;
    }
    
    protected double _revision;
    protected int _pos;
    protected int _length;
    protected String _str;
    protected ArrayList<Scan> _scanList;
    protected ArrayList<Station> _stationList;
    protected ArrayList<Antenna> _antennaList;
    protected ArrayList<Site> _siteList;
    
}
