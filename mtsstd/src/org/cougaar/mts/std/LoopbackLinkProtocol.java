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

import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;


/**
 * This protocol handles all intra-node message traffic.  It can act
 * as its own DestinationLink, since this transport only sees traffic
 * for one destination.  The cost function is minimal (0) for local
 * traffic, and maximal (Integer.MAX_VALUE) for any other traffic. */
class LoopbackLinkProtocol 
    extends LinkProtocol
    implements DestinationLink
{

    private DestinationLink link;

    public LoopbackLinkProtocol(String id, AspectSupport aspectSupport) {
	super(aspectSupport);
    }

    public DestinationLink getDestinationLink(MessageAddress address) {
	if (link == null) {
	    link = (DestinationLink) attachAspects(this, 
						   DestinationLink.class,
						   this);
	}
	return link;
    }

    public void registerClient(MessageTransportClient client) {
	// Does nothing because the Database of local clients is held
	// by MessageTransportServerImpl
    }

    public void unregisterClient(MessageTransportClient client) {
	// Does nothing because the Database of local clients is held
	// by MessageTransportServerImpl
    }

    public boolean addressKnown(MessageAddress address) {
	// true iff the address is local
	return registry.isLocalClient(address);
    }


    public void registerMTS(MessageAddress address) {
    }
   



    // DestinationLink interface

    public int cost(Message msg) {
	MessageAddress addr = msg.getTarget();
	if (registry.isLocalClient(addr)) {
	    return 0;
	} else {
	    return Integer.MAX_VALUE;
	}
    }
	


    public void forwardMessage(Message message) 
	throws MisdeliveredMessageException
    {
	deliverer.deliverMessage(message, message.getTarget());
    }



}
