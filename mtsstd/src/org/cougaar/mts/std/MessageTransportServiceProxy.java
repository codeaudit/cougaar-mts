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
import org.cougaar.core.society.MulticastMessageAddress;

import java.util.ArrayList;
import java.util.Iterator;


/**
 * Currently the only implementation of MessageTransportService.  It
 * does almost nothing by itself - its work is accomplished by
 * redirecting calls to the MessageTransportRegistry and the
 * SendQueue.  */
public class MessageTransportServiceProxy 
    implements MessageTransportService
{
    private MessageTransportRegistry registry;
    private MessageTransportServiceDelegate delegate;
    private MessageTransportClient client;
    private MessageAddress addr;

    public MessageTransportServiceProxy(MessageTransportClient client,
					SendQueue queue,
					AspectSupport aspectSupport) 
    {
	this.registry = MessageTransportRegistry.getRegistry();
	this.client = client;
	this.addr = client.getMessageAddress();
	delegate = new ServiceProxyDelegateImplBase(queue, addr);
	Class c = MessageTransportServiceDelegate.class;
	Object raw = aspectSupport.attachAspects(delegate, c, null);
	delegate = (MessageTransportServiceDelegate) raw;
    }


    void release() {
	registry.unregisterServiceProxy(this, client.getMessageAddress());
	registry = null;
	client = null;
	addr = null;
	delegate.release();
	delegate = null;
    }
    



    /**
     * Redirects the sendMessage to the SendQueue. */
    public void sendMessage(Message message) {
	if (delegate.okToSend(message)) {
	    delegate.sendMessage(message);
	}
    }

    /**
     * Wait for all queued messages for our client to be either
     * delivered or dropped. 
     * @return the list of dropped messages, which could be null.
     */
    public synchronized ArrayList flushMessages() {
	return delegate.flushMessages();
    }




    /**
     * Redirects the request to the MessageTransportRegistry. */
    public synchronized void registerClient(MessageTransportClient client) {
	// Should throw an exception of client != this.client
	registry.registerClient(client);
	registry.registerServiceProxy(this, client.getMessageAddress());
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

