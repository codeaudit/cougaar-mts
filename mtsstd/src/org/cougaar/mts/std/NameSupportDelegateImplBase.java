/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

import java.net.URI;
import java.util.Iterator;

import org.cougaar.core.service.wp.Callback;

abstract public class NameSupportDelegateImplBase implements NameSupport 
{
    private NameSupport nameSupport;

    protected NameSupportDelegateImplBase (NameSupport nameSupport) {
	this.nameSupport = nameSupport;
    }


    public MessageAddress getNodeMessageAddress() {
	return nameSupport.getNodeMessageAddress();
    }

    public void registerAgentInNameServer(URI reference, 
					  MessageAddress address, 
					  String protocol)
    {
	nameSupport.registerAgentInNameServer(reference, address, protocol);
    }

    public void unregisterAgentInNameServer(URI reference, 
					    MessageAddress address, 
					    String protocol)
    {
	nameSupport.unregisterAgentInNameServer(reference, address, protocol);
    }


    public void lookupAddressInNameServer(MessageAddress address, 
					  String protocol,
					  Callback callback)
    {
	nameSupport.lookupAddressInNameServer(address, protocol, callback);
    }

    public URI lookupAddressInNameServer(MessageAddress address, 
					 String protocol)
    {
	return nameSupport.lookupAddressInNameServer(address, protocol);
    }

    public URI lookupAddressInNameServer(MessageAddress address, 
					 String protocol,
					 long timeout)
    {
	return nameSupport.lookupAddressInNameServer(address, protocol,
						     timeout);
    }


    public Iterator lookupMulticast(MulticastMessageAddress address) {
	return nameSupport.lookupMulticast(address);
    }


}
