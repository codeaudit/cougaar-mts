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


import org.cougaar.core.service.MessageTransportService;

import java.util.ArrayList;
import java.util.Iterator;


/**
 * Currently the only implementation of MessageTransportService.  It
 * does almost nothing by itself - its work is accomplished by
 * redirecting calls to the corresponding SendLink.  */
public class MessageTransportServiceProxy 
    implements MessageTransportService
{
    private SendLink link;
    private MessageTransportClient client;

    public MessageTransportServiceProxy(MessageTransportClient client,
					SendLink link) 
    {
	this.client = client;
	this.link = link;
    }


    void release() {
	client = null;
	link.release();
	link = null;
    }
    



    /**
     * Redirects the sendMessage to the SendQueue. */
    public void sendMessage(Message rawMessage) {
	MessageAttributes attrs = rawMessage.getTarget().getQosAttributes();
	AttributedMessage message = new AttributedMessage(rawMessage, attrs);
	if (link.okToSend(message)) link.sendMessage(message);
    }




    /**
     * Wait for all queued messages for our client to be either
     * delivered or dropped. 
     * @return the list of dropped messages, which could be null.
     */
    public synchronized ArrayList flushMessages() {
	ArrayList droppedMessages = new ArrayList();
	link.flushMessages(droppedMessages);
	ArrayList rawMessages = new ArrayList(droppedMessages.size());
	Iterator itr = droppedMessages.iterator();
	while (itr.hasNext()) {
	    AttributedMessage m = (AttributedMessage) itr.next();
	    rawMessages.add(m.getRawMessage());
	}
	return rawMessages;
    }




    /**
     * Redirects the request to the MessageTransportRegistry. */
    public synchronized void registerClient(MessageTransportClient client) {
	// Should throw an exception of client != this.client
	link.registerClient(client);
    }


    /**
     * Redirects the request to the MessageTransportRegistry. */
    public synchronized void unregisterClient(MessageTransportClient client) {
	// Should throw an exception of client != this.client
	link.unregisterClient(client);

	// NB: The proxy (as opposed to the client) CANNOT be
	// unregistered here.  If it were, messageDelivered callbacks
	// wouldn't be delivered and flush could block forever.
	// Unregistering the proxy can only happen as part of
	// releasing the service (see release());
    }
    
   
    /**
     * Redirects the request to the MessageTransportRegistry. */
    public String getIdentifier() {
	return link.getIdentifier();
    }

    /**
     * Redirects the request to the MessageTransportRegistry. */
    public boolean addressKnown(MessageAddress a) {
	return link.addressKnown(a);
    }

}

