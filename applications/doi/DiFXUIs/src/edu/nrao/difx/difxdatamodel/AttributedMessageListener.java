/*
 * This is a simple message listener structure that allows us to include a
 * String as data.
 */
package edu.nrao.difx.difxdatamodel;

import java.util.EventListener;

/**
 *
 * @author jspitzak
 */
public interface AttributedMessageListener extends EventListener {
    
    public void update( String data );
    
}
