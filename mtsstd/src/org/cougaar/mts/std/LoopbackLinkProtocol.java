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
