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

abstract public class NameSupportDelegateImplBase implements NameSupport 
{
    protected NameSupport nameSupport;

    protected NameSupportDelegateImplBase (NameSupport nameSupport) {
	this.nameSupport = nameSupport;
    }


    public MessageAddress getNodeMessageAddress() {
	return nameSupport.getNodeMessageAddress();
    }

    public void registerAgentInNameServer(Object proxy, 
					  MessageAddress address, 
					  String transportType)
    {
	nameSupport.registerAgentInNameServer(proxy, address, transportType);
    }

    public void unregisterAgentInNameServer(Object proxy, 
					    MessageAddress address, 
					    String transportType)
    {
	nameSupport.unregisterAgentInNameServer(proxy, address, transportType);
    }

    public void registerMTS(MessageAddress address) {
	nameSupport.registerMTS(address);
    }

    public Object lookupAddressInNameServer(MessageAddress address, 
					    String transportType)
    {
	return nameSupport.lookupAddressInNameServer(address, transportType);
    }


    public Iterator lookupMulticast(MulticastMessageAddress address) {
	return nameSupport.lookupMulticast(address);
    }

    public void addToTopology(MessageAddress addr, String category) {
	nameSupport.addToTopology(addr, category);
    }

    public void removeFromTopology(MessageAddress addr) {
	nameSupport.removeFromTopology(addr);
    }

    public Iterator lookupInTopology(Attributes match, String attribute) {
	return nameSupport.lookupInTopology(match, attribute);
    }

    public Iterator lookupInTopology(Attributes match, String[] ret_attr) {
	return nameSupport.lookupInTopology(match, ret_attr);
    }

}
