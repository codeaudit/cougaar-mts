/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.mts;

import org.cougaar.core.naming.NS;
import org.cougaar.core.society.MessageAddress;

import java.rmi.RemoteException;

/**
 * This is utility class which hides the grimy details of dealing with
 * NameServers from the rest of the message transport subsystem.  */
public interface NameSupport {
    public static final String CLUSTERDIR = "clusters" + NS.DirSeparator;
    public static final String MTDIR = "MessageTransports" + NS.DirSeparator;
    MessageAddress  getNodeMessageAddress();

    void registerAgentInNameServer(Object proxy, 
                                   MessageTransportClient client, 
                                   String transportType);

    void unregisterAgentInNameServer(Object proxy, 
				     MessageTransportClient client, 
				     String transportType);

    void registerNodeInNameServer(Object proxy, String transportType);

    Object lookupAddressInNameServer(MessageAddress address, 
                                     String transportType);
}
