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

import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;

import java.util.ArrayList;

public class SendLinkImpl
    implements SendLink
{

    private SendQueue sendQ;
    private MessageAddress addr;
    private MessageTransportRegistry registry;

    SendLinkImpl(SendQueue sendQ, 
		 MessageAddress addr)
    {
	this.sendQ = sendQ;
	this.addr = addr;
	this.registry = MessageTransportRegistry.getRegistry();
    }


    public void sendMessage(Message message) {
	sendQ.sendMessage(message);
    }

    // Default is no-op, all the real work is done in FlushAspect
    public void flushMessages(ArrayList droppedMessages) {
    }

    public MessageAddress getAddress() {
	return addr;
    }

    public void release() {
	sendQ = null;
	registry = null;
    }

    public boolean okToSend(Message message) {
	MessageAddress target = message.getTarget();
	if (target == null || target.toString().equals("")) {
	    System.err.println("**** Malformed message: "+message);
	    Thread.dumpStack();
	    return false;
	} else {
	    return true;
	}
    }

	
    /**
     * Redirects the request to the MessageTransportRegistry. */
    public synchronized void registerClient(MessageTransportClient client) {
	// Should throw an exception of client != this.client
	registry.registerClient(client);
    }


    /**
     * Redirects the request to the MessageTransportRegistry. */
    public synchronized void unregisterClient(MessageTransportClient client) {
	// Should throw an exception of client != this.client
	registry.unregisterClient(client);

	// NB: The proxy (as opposed to the client) CANNOT be
	// unregistered here.  If it were, messageDelivered callbacks
	// wouldn't be delivered and flush could block forever.
	// Unregistering the proxy can only happen as part of
	// releasing the service (see release());
    }
    
   
    /**
     * Redirects the request to the MessageTransportRegistry. */
    public String getIdentifier() {
	return registry.getIdentifier();
    }

    /**
     * Redirects the request to the MessageTransportRegistry. */
    public boolean addressKnown(MessageAddress a) {
	return registry.addressKnown(a);
    }

}

