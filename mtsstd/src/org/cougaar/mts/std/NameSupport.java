/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.core.mts;

import org.cougaar.core.society.MulticastMessageAddress;
import org.cougaar.core.society.MessageAddress;

import java.util.Iterator;
import javax.naming.directory.Attributes;

/**
 * This is utility class which hides the grimy details of dealing with
 * NameServers from the rest of the message transport subsystem.  */
public interface NameSupport {
    public static final String MTS_DIR =  "MessageTransports";
    public static final String AGENT_DIR =  "Agents";
    public static final String TOPOLOGY_DIR = "Topology";

    public static final String STATUS_ATTR = "Status";
    public static final String HOST_ATTR = "Host";
    public static final String NODE_ATTR = "Node";
    public static final String AGENT_ATTR = "Agent";
    public static final String REGISTERED_STATUS = "registered";
    public static final String UNREGISTERED_STATUS = "unregistered";

    public MessageAddress  getNodeMessageAddress();

    public void registerAgentInNameServer(Object proxy, 
					  MessageAddress address, 
					  String transportType);

    public void unregisterAgentInNameServer(Object proxy, 
					    MessageAddress address, 
					    String transportType);

    public void registerMTS(MessageAddress address);

    public Object lookupAddressInNameServer(MessageAddress address, 
					    String transportType);

    public Iterator lookupMulticast(MulticastMessageAddress address);

    public void addToTopology(MessageAddress addr);
    public void removeFromTopology(MessageAddress addr);
    public Iterator lookupInTopology(Attributes match, String attribute);
    public Iterator lookupInTopology(Attributes match, String[] ret_attr);

}
