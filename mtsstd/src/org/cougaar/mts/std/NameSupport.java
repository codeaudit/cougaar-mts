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

import org.cougaar.core.service.*;

import org.cougaar.core.node.*;

import org.cougaar.core.mts.MulticastMessageAddress;
import org.cougaar.core.mts.MessageAddress;

import java.util.Iterator;
import javax.naming.directory.Attributes;

/**
 * This is utility class which hides the grimy details of dealing with
 * NameServers from the rest of the message transport subsystem.  */
public interface NameSupport {
    String MTS_DIR =  "MessageTransports";
    String AGENT_DIR =  "Agents";
    String TOPOLOGY_DIR = "Topology";

    String STATUS_ATTR = "Status";
    String HOST_ATTR = "Host";
    String NODE_ATTR = "Node";
    String AGENT_ATTR = "Agent";
    String INCARNATION_ATTR = "Incarnation";
    String CATEGORY_ATTR = "Category";

    String NODE_CATEGORY = "node";
    String AGENT_CATEGORY = "agent";
    String SYSTEM_CATEGORY = "system";

    String REGISTERED_STATUS = "registered";
    String UNREGISTERED_STATUS = "unregistered";

    MessageAddress  getNodeMessageAddress();

    void registerAgentInNameServer(Object proxy, 
					  MessageAddress address, 
					  String transportType);

    void unregisterAgentInNameServer(Object proxy, 
					    MessageAddress address, 
					    String transportType);

    void registerMTS(MessageAddress address);

    Object lookupAddressInNameServer(MessageAddress address, 
					    String transportType);

    Iterator lookupMulticast(MulticastMessageAddress address);

    void addToTopology(MessageAddress addr, String category);
    void removeFromTopology(MessageAddress addr);
    Iterator lookupInTopology(Attributes match, String attribute);
    Iterator lookupInTopology(Attributes match, String[] ret_attr);

}
