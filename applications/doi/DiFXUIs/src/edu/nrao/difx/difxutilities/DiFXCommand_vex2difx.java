/*
 * Run the "vex2difx" command on the DiFX host.
 */
package edu.nrao.difx.difxutilities;

import edu.nrao.difx.difxview.SystemSettings;

import edu.nrao.difx.xmllib.difxmessage.DifxVex2DifxRun;

/**
 *
 * @author jspitzak
 */
public class DiFXCommand_vex2difx extends DiFXCommand {
    
    public DiFXCommand_vex2difx( String passPath, String file, SystemSettings settings ) {
        super( settings );
        this.header().setType( "DifxVex2DifxRun" );
        DifxVex2DifxRun v2d = this.factory().createDifxVex2DifxRun();
        v2d.setUser( settings.difxControlUser() );
        v2d.setNode( settings.difxControlAddress() );
        v2d.setDifxPath( settings.difxPath() );
        v2d.setPassPath( passPath );
        v2d.setFile( file );
        this.body().setDifxVex2DifxRun( v2d );
    }
    
}
