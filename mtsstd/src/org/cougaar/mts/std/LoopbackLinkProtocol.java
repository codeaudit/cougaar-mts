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

import java.util.HashMap;


/**
 * This protocol handles all intra-node message traffic.   */
class LoopbackLinkProtocol 
    extends LinkProtocol
{

    private HashMap links;

    public LoopbackLinkProtocol() {
	super();
	links = new HashMap();
    }

    public synchronized DestinationLink getDestinationLink(MessageAddress address) {
	DestinationLink link = (DestinationLink) links.get(address);
	if (link == null) {
	    link = new Link(address);
	    link = (DestinationLink) attachAspects(link,DestinationLink.class);
	    links.put(address, link);
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
	return getRegistry().isLocalClient(address);
    }


    public void registerMTS(MessageAddress address) {
    }
   


    private class Link implements DestinationLink {
	MessageAddress address;

	Link(MessageAddress address) {
	    this.address = address;
	}

	public int cost(AttributedMessage msg) {
	    MessageAddress addr = msg.getTarget();
	    if (getRegistry().isLocalClient(addr)) {
		return 0;
	    } else {
		return Integer.MAX_VALUE;
	    }
	}
	


	public MessageAttributes forwardMessage(AttributedMessage message) 
	    throws MisdeliveredMessageException
	{
	    return getDeliverer().deliverMessage(message, message.getTarget());
	}

	public boolean retryFailedMessage(AttributedMessage message,
					  int retryCount) 
	{
	    return true;
	}

    
	public Class getProtocolClass() {
	    return LoopbackLinkProtocol.class;
	}


	public MessageAddress getDestination() {
	    return address;
	}



	public Object getRemoteReference() {
	    return null;
	}

	public void addMessageAttributes(MessageAttributes attrs) {
	    
	}

	

    }

}
