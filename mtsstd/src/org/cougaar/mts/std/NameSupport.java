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

import org.cougaar.core.component.Service;

import java.util.Iterator;
import javax.naming.directory.Attributes;

/**
 * This is utility class which hides the grimy details of dealing with
 * NameServers from the rest of the message transport subsystem.  */
public interface NameSupport extends Service
{
    String MTS_DIR =  "MessageTransports";
    String AGENT_DIR =  "Agents";

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

    /** @deprecated replaced by the topology services */
    void addToTopology(MessageAddress addr, String category);
    /** @deprecated replaced by the topology services */
    void removeFromTopology(MessageAddress addr);
    /** @deprecated replaced by the topology services */
    Iterator lookupInTopology(Attributes match, String attribute);
    /** @deprecated replaced by the topology services */
    Iterator lookupInTopology(Attributes match, String[] ret_attr);

}
