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

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

/**
 * This protocol handles all intra-node message traffic.  It can act
 * as its own DestinationLink, since this transport only sees traffic
 * for one destination.  The cost function is minimal (0) for local
 * traffic, and maximal (Integer.MAX_VALUE) for any other traffic. */
class FutileSerializingLinkProtocol 
    extends LinkProtocol
    
{

    private HashMap links;

    public FutileSerializingLinkProtocol(String id, 
					 AspectSupport aspectSupport) 
    {
	super(aspectSupport);
	links = new HashMap();
    }

    public synchronized DestinationLink getDestinationLink(MessageAddress address) {
	DestinationLink link = (DestinationLink) links.get(address);
	if (link == null) {
	    link = new Link();
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


    public void registerMTS(MessageAddress address) {
    }
   



    class Link implements DestinationLink {
	private Message lastMessage;
	private int count;

	public int cost(Message msg) {
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
	


	private void serialize(Message message) {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    ObjectOutputStream oos = null;

	    try {
		oos = new ObjectOutputStream(baos);
		oos.writeObject(message);
	    } catch (java.io.IOException ioe) {
		ioe.printStackTrace();
		return;
	    }

	    try {
		oos.close();
	    } catch (java.io.IOException ioe2) {
		ioe2.printStackTrace();
	    }

	    System.out.println("Serialized " + message);
	}

	public void forwardMessage(Message message) 
	    throws MisdeliveredMessageException
	{
	    serialize(message);
	    throw new MisdeliveredMessageException(message);
	}

	public boolean retryFailedMessage(Message message, int retryCount) {
	    return true;
	}

    
	public Class getProtocolClass() {
	    return FutileSerializingLinkProtocol.class;
	}
    }

}
