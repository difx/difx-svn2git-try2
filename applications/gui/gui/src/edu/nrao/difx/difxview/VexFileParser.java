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
            //  See if the line looks like the start of a new section (or the
            //  end of the file).
            if ( line == null || line.charAt( 0 ) == '$' ) {
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
                else if ( sectionName.equalsIgnoreCase( "$EOP" ) )
                    parseEOPData( sectionData );
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
                }
            }
        }
    }

    /*
     * Extract "freq" data from a list of data lines.
     */
    protected void parseFreqData( ArrayList<String> data ) {
        //  At the moment all we are intersted in is the bandwidth, which is the 
        //  4th column in "chan_def" lines.  We plow through three column separators
        //  (":" characters) to get to it.  If the line we are on cannot produce
        //  these separators, we ignore the line.
        for ( Iterator<String> iter = data.iterator(); iter.hasNext(); ) {
            String thisLine = iter.next();
            //  Get beyond the third ":" character.
            boolean okay = true;
            int count = 0;
            while ( okay && count < 3 ) {
                ++count;
                if ( thisLine.indexOf( ':' ) != -1 )
                    thisLine = thisLine.substring( thisLine.indexOf( ':' ) + 1 );
                else
                    okay = false;
            }
            //  If the line looks good, extract the next number - the bandwidth.
            if ( okay )
                _bandwidth = Double.valueOf( thisLine.trim().substring( 0, thisLine.trim().indexOf( ' ' ) ) );
        }
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
                    String dataStr = skipEq( thisLine );
                    currentScan.start = new GregorianCalendar();
                    currentScan.start.set( Calendar.YEAR, Integer.parseInt( dataStr.substring( 0, 4 )));
                    currentScan.start.set( Calendar.DAY_OF_YEAR, Integer.parseInt( dataStr.substring( 5, 8 )));
                    currentScan.start.set( Calendar.HOUR_OF_DAY, Integer.parseInt( dataStr.substring( 9, 11 )));
                    currentScan.start.set( Calendar.MINUTE, Integer.parseInt( dataStr.substring( 12, 14 )));
                    currentScan.start.set( Calendar.SECOND, Integer.parseInt( dataStr.substring( 15, 17 )));
                }
            }
            else if ( thisLine.length() > 4 && thisLine.substring( 0, 4 ).equalsIgnoreCase( "MODE" ) ) {
                if ( currentScan != null ) {
                    currentScan.mode = skipEq( thisLine );
                }
            }
            else if ( thisLine.length() > 6 && thisLine.substring( 0, 6 ).equalsIgnoreCase( "SOURCE" ) ) {
                if ( currentScan != null ) {
                    currentScan.source = skipEq( thisLine );
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
            else if ( thisLine.length() > 13 && thisLine.substring( 0, 19 ).equalsIgnoreCase( "SITE_POSITION_EPOCH" ) ) {
                if ( currentSite != null ) {
                    currentSite.positionEpoch = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
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
                }
            }
        }
    }

    /*
     * Extract "source" data from a list of data lines.  These data contain all
     * of the source information for the observations.
     */
    protected void parseSourceData( ArrayList<String> data ) {
        Source currentSource = null;
        for ( Iterator<String> iter = data.iterator(); iter.hasNext(); ) {
            String thisLine = iter.next();
            //  Find the "scan" string indicating the start of a scan.
            if ( thisLine.length() > 3 && thisLine.substring( 0, 3 ).equalsIgnoreCase( "DEF" ) ) {
                //  Create a new scan.
                currentSource = new Source();
                currentSource.def = thisLine.substring( thisLine.indexOf( ' ' ) ).trim();
            }
            else if ( thisLine.length() > 11 && thisLine.substring( 0, 11 ).equalsIgnoreCase( "SOURCE_NAME" ) ) {
                if ( currentSource != null ) {
                    currentSource.name = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
                }
            }
            else if ( thisLine.length() > 11 && thisLine.substring( 0, 11 ).equalsIgnoreCase( "SOURCE_TYPE" ) ) {
                if ( currentSource != null ) {
                    currentSource.type = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
                }
            }
            else if ( thisLine.length() > 8 && thisLine.substring( 0, 8 ).equalsIgnoreCase( "IAU_NAME" ) ) {
                if ( currentSource != null ) {
                    currentSource.IAU_name = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
                }
            }
            else if ( thisLine.length() > 2 && thisLine.substring( 0, 2 ).equalsIgnoreCase( "RA" ) ) {
                if ( currentSource != null ) {
                    currentSource.ra = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
                }
            }
            else if ( thisLine.length() > 3 && thisLine.substring( 0, 3 ).equalsIgnoreCase( "DEC" ) ) {
                if ( currentSource != null ) {
                    currentSource.dec = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
                }
            }
            else if ( thisLine.length() > 15 && thisLine.substring( 0, 15 ).equalsIgnoreCase( "REF_COORD_FRAME" ) ) {
                if ( currentSource != null ) {
                    currentSource.refCoordFrame = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
                }
            }
            else if ( thisLine.length() >= 6 && thisLine.substring( 0, 6 ).equalsIgnoreCase( "ENDDEF" ) ) {
                if ( currentSource != null ) {
                    //  Add the current scan to the list of scans.
                    if ( _sourceList == null )
                        _sourceList = new ArrayList<Source>();
                    _sourceList.add( currentSource );
                }
            }
        }
    }
    
    /*
     * Extract "EOP" data from a list of data lines.
     */
    protected void parseEOPData( ArrayList<String> data ) {
        EOP currentEOP = null;
        for ( Iterator<String> iter = data.iterator(); iter.hasNext(); ) {
            String thisLine = iter.next();
            //  Find the "scan" string indicating the start of a scan.
            if ( thisLine.length() > 3 && thisLine.substring( 0, 3 ).equalsIgnoreCase( "DEF" ) ) {
                //  Create a new scan.
                currentEOP = new EOP();
                currentEOP.num_eop_points = 1;
                currentEOP.ut1_utc = new ArrayList<String>();
                currentEOP.x_wobble = new ArrayList<String>();
                currentEOP.y_wobble = new ArrayList<String>();
                currentEOP.def = thisLine.substring( thisLine.indexOf( ' ' ) ).trim();
            }
            else if ( thisLine.length() > 7 && thisLine.substring( 0, 7 ).equalsIgnoreCase( "TAI-UTC" ) ) {
                if ( currentEOP != null ) {
                    currentEOP.tai_utc = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
                }
            }
            else if ( thisLine.length() > 6 && thisLine.substring( 0, 6 ).equalsIgnoreCase( "A1-TAI" ) ) {
                if ( currentEOP != null ) {
                    currentEOP.a1_tai = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
                }
            }
            else if ( thisLine.length() > 13 && thisLine.substring( 0, 13 ).equalsIgnoreCase( "EOP_REF_EPOCH" ) ) {
                if ( currentEOP != null ) {
                    currentEOP.eop_ref_epoch = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
                }
            }
            else if ( thisLine.length() > 14 && thisLine.substring( 0, 14 ).equalsIgnoreCase( "NUM_EOP_POINTS" ) ) {
                if ( currentEOP != null ) {
                    currentEOP.num_eop_points = Integer.parseInt( thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim() );
                }
            }
            else if ( thisLine.length() > 14 && thisLine.substring( 0, 12 ).equalsIgnoreCase( "EOP_INTERVAL" ) ) {
                if ( currentEOP != null ) {
                    currentEOP.eop_interval = thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim();
                }
            }
            else if ( thisLine.length() > 7 && thisLine.substring( 0, 7 ).equalsIgnoreCase( "UT1-UTC" ) ) {
                if ( currentEOP != null ) {
                    int startIndex = thisLine.indexOf( '=' ) + 1;
                    int endIndex = thisLine.indexOf( ':' );
                    if ( endIndex < 0 )
                        endIndex = thisLine.length();
                    for ( int i = 0; i < currentEOP.num_eop_points && startIndex < endIndex; ++i ) {
                        currentEOP.ut1_utc.add( thisLine.substring( startIndex, endIndex ).trim() );
                        if ( endIndex < thisLine.length() ) {
                            startIndex = endIndex + 1;
                            endIndex = thisLine.substring( startIndex, thisLine.length() ).indexOf( ':' );
                            if ( endIndex < 0 )
                                endIndex = thisLine.length();
                            else
                                endIndex += startIndex;
                        }
                    }
                }
            }
            else if ( thisLine.length() > 8 && thisLine.substring( 0, 8 ).equalsIgnoreCase( "X_WOBBLE" ) ) {
                if ( currentEOP != null ) {
                    int startIndex = thisLine.indexOf( '=' ) + 1;
                    int endIndex = thisLine.indexOf( ':' );
                    if ( endIndex < 0 )
                        endIndex = thisLine.length();
                    for ( int i = 0; i < currentEOP.num_eop_points && startIndex < endIndex; ++i ) {
                        currentEOP.x_wobble.add( thisLine.substring( startIndex, endIndex ).trim() );
                        if ( endIndex < thisLine.length() ) {
                            startIndex = endIndex + 1;
                            endIndex = thisLine.substring( startIndex, thisLine.length() ).indexOf( ':' );
                            if ( endIndex < 0 )
                                endIndex = thisLine.length();
                            else
                                endIndex += startIndex;
                        }
                    }
//                    currentEOP.x_wobble.add( thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim() );
                }
            }
            else if ( thisLine.length() > 8 && thisLine.substring( 0, 8 ).equalsIgnoreCase( "Y_WOBBLE" ) ) {
                if ( currentEOP != null ) {
                    int startIndex = thisLine.indexOf( '=' ) + 1;
                    int endIndex = thisLine.indexOf( ':' );
                    if ( endIndex < 0 )
                        endIndex = thisLine.length();
                    for ( int i = 0; i < currentEOP.num_eop_points && startIndex < endIndex; ++i ) {
                        currentEOP.y_wobble.add( thisLine.substring( startIndex, endIndex ).trim() );
                        if ( endIndex < thisLine.length() ) {
                            startIndex = endIndex + 1;
                            endIndex = thisLine.substring( startIndex, thisLine.length() ).indexOf( ':' );
                            if ( endIndex < 0 )
                                endIndex = thisLine.length();
                            else
                                endIndex += startIndex;
                        }
                    }
//                    currentEOP.y_wobble.add( thisLine.substring( thisLine.indexOf( '=' ) + 1 ).trim() );
                }
            }
            else if ( thisLine.length() >= 6 && thisLine.substring( 0, 6 ).equalsIgnoreCase( "ENDDEF" ) ) {
                if ( currentEOP != null ) {
                    //  Add the current scan to the list of scans.
                    if ( _eopList == null )
                        _eopList = new ArrayList<EOP>();
                    _eopList.add( currentEOP );
                }
            }
        }
    }
    
    /*
     * Skip to the character immediately following an "=" sign, which is a common
     * thing to have to do...
     */
    protected String skipEq( String inString ) {
        return inString.substring( inString.indexOf( '=' ) + 1 ).trim();
    }

    public double revision() { return _revision; }
    public double bandwidth() { return _bandwidth; }
    public ArrayList<Scan> scanList() { return _scanList; }
    public ArrayList<Station> stationList() { return _stationList; }
    public ArrayList<Site> siteList() { return _siteList; }
    public ArrayList<Antenna> antennaList() { return _antennaList; }
    public ArrayList<Source> sourceList() { return _sourceList; }
    public ArrayList<EOP> eopList() { return _eopList; }
    
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
        String positionEpoch;
        String horizon_map_az;
        String horizon_map_el;
        String occupation_code;
    }
    
    public class Source {
        String def;
        String name;
        String type;
        String IAU_name;
        String ra;
        String dec;
        String refCoordFrame;
    }
    
    public class EOP {
        String def;
        String tai_utc;
        String a1_tai;
        String eop_ref_epoch;
        Integer num_eop_points;
        String eop_interval;
        ArrayList<String> ut1_utc;
        ArrayList<String> x_wobble;
        ArrayList<String> y_wobble;
    }
    
    static String deleteEOPData( String inStr ) {
        String outStr = "";
        int pos = 0;
        int endPos = 0;
        boolean done = false;
        boolean inEOP = false;
        while ( !done ) {
            endPos = inStr.indexOf( '\n', pos );
            if ( endPos == -1 ) {
                done = true;
            }
            else {
                if ( !inEOP && inStr.substring( pos, endPos ).trim().regionMatches( true, 0, "$EOP;", 0, 5 ) ) {
                    inEOP = true;
                }
                else if ( inStr.substring( pos, endPos ).trim().length() > 0 && 
                          inStr.substring( pos, endPos ).trim().charAt( 0 ) == '$' ) {
                    inEOP = false;
                }
                if ( !inEOP )
                    outStr += inStr.substring( pos, endPos + 1 );
                pos = endPos + 1;
            }
        }
        return outStr;
    }
    
    protected double _revision;
    protected double _bandwidth;
    protected int _pos;
    protected int _length;
    protected String _str;
    protected ArrayList<Scan> _scanList;
    protected ArrayList<Station> _stationList;
    protected ArrayList<Antenna> _antennaList;
    protected ArrayList<Site> _siteList;
    protected ArrayList<Source> _sourceList;
    protected ArrayList<EOP> _eopList;
    
}