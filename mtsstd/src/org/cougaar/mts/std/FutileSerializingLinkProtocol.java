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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import org.cougaar.core.service.LoggingService;

/**
 * Debugging aid - fails by design after serializing.
 */
class FutileSerializingLinkProtocol 
    extends LinkProtocol
    
{

    private HashMap links;

    public FutileSerializingLinkProtocol() 
    {
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
	// we know everybody
	return true;
    }




    class Link implements DestinationLink {
	private AttributedMessage lastMessage;
	private MessageAddress address;
	private int count;

	Link(MessageAddress address) {
	    this.address = address;
	}

	public MessageAddress getDestination() {
	    return address;
	}


	public int cost(AttributedMessage msg) {
	    if (lastMessage != msg) {
		lastMessage = msg;
		count = 1;
		return 400;
	    } else if (count < 3) {
		count++;
		return 400;
	    }  else {
		lastMessage = null;
		return Integer.MAX_VALUE;
	    }
	}
	


	private void serialize(AttributedMessage message) {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    ObjectOutputStream oos = null;

	    try {
		oos = new ObjectOutputStream(baos);
		oos.writeObject(message);
	    } catch (java.io.IOException ioe) {
		if (loggingService.isErrorEnabled())
		    loggingService.error(null, ioe);
		return;
	    }

	    try {
		oos.close();
	    } catch (java.io.IOException ioe2) {
		if (loggingService.isErrorEnabled())
		    loggingService.error(null, ioe2);
	    }

	    if (loggingService.isInfoEnabled())
		loggingService.info("Serialized " + message);
	}

	public MessageAttributes forwardMessage(AttributedMessage message) 
	    throws MisdeliveredMessageException
	{
	    serialize(message);
	    throw new MisdeliveredMessageException(message);
	}

	public boolean retryFailedMessage(AttributedMessage message, 
					  int retryCount) 
	{
	    return true;
	}

    
	public Class getProtocolClass() {
	    return FutileSerializingLinkProtocol.class;
	}


	public Object getRemoteReference() {
	    return null;
	}

	public void addMessageAttributes(MessageAttributes attrs) {
	    
	}

	

    }

}
