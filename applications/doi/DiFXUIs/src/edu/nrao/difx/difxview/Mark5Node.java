/*
 * This is a browser node used to display Mark5 data.  It inherits the "ClusterNode"
 * class, as it shares many of the data display traits with that class.
 */
package edu.nrao.difx.difxview;

import edu.nrao.difx.difxdatamodel.Mark5Unit;

/**
 *
 * @author jspitzak
 */
public class Mark5Node extends ClusterNode {
    
    public Mark5Node( String name ) {
        super( name );
    }
    
    public void setData( Mark5Unit newData ) {
        super.setData( newData );
    }
    
}
