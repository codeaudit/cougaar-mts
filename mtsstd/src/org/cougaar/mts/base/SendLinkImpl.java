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

package org.cougaar.mts.base;
import java.util.ArrayList;

import org.cougaar.core.component.ServiceBroker;

import org.cougaar.mts.std.AttributedMessage;
import org.cougaar.core.mts.AgentState;
import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.util.UnaryPredicate;


final public class SendLinkImpl
    implements SendLink
{
    static final String VERSION = "version";

    private SendQueue sendq;
    private SendQueueImpl sendq_impl;
    private DestinationQueueProviderService destq_factory;
    private MessageAddress addr;
    private MessageTransportRegistryService registry;
    private LoggingService loggingService;
    private Long incarnation;

    private class BlockingWPCallback implements Callback {
	Object lock;
	AddressEntry entry;
	
	BlockingWPCallback(Object lock)
	{
	    this.lock = lock;
	}
	
	public void execute(Response response) 
	{
	    if (response.isSuccess()) {
		if (loggingService.isInfoEnabled()) {
		    loggingService.info("WP Response: "+response);
		}
		entry = ((Response.Get) response).getAddressEntry();
		synchronized (lock) {
		    lock.notify();
		}
	    } else {
		loggingService.error("WP Error: "+response);
	    }
	}
    }


    SendLinkImpl(MessageAddress addr, ServiceBroker sb)
    {
	this.addr = addr;
	registry = (MessageTransportRegistryService)
	    sb.getService(this, MessageTransportRegistryService.class, null);
	sendq = (SendQueue)
	    sb.getService(this, SendQueue.class, null);
	sendq_impl = (SendQueueImpl)
	    sb.getService(this, SendQueueImpl.class, null);
	destq_factory = (DestinationQueueProviderService)
	    sb.getService(this, 
			  DestinationQueueProviderService.class, 
			  null);
	loggingService = (LoggingService)
	    sb.getService(this, LoggingService.class, null);

	String agentID = addr.getAddress();
	WhitePagesService wp = (WhitePagesService)
	    sb.getService(this, WhitePagesService.class, null);
	long incn = 0;
	try {
	    AddressEntry entry;
	    Object lock = new Object();
	    BlockingWPCallback callback = new BlockingWPCallback(lock);
	    synchronized (lock) {
		wp.get(agentID, VERSION, callback);
		// Callback could be invoked in this thread!  Don't
		// wait in that case.
		while (callback.entry == null) {
		    try { lock.wait(); }
		    catch (InterruptedException ex) {}
		}
	    }
	    entry = callback.entry;
	    if (entry != null) {
		String path = entry.getURI().getPath();
		int end = path.indexOf('/', 1);
		String incn_str = path.substring(1, end);
		incn = Long.parseLong(incn_str);
	    }
	} catch (Exception ex) {
	    if (loggingService.isErrorEnabled())
		loggingService.error("Failed Incarnation",ex);
	}
	incarnation = new Long(incn);
    }


    public void sendMessage(AttributedMessage message) {
	MessageAddress orig = message.getOriginator();
	if (!addr.equals(orig)) {
	    loggingService.error("SendLink saw a message whose originator (" +orig+ ") didn't match the MessageTransportClient address (" +addr+ ")");
	}
	message.setAttribute(AttributeConstants.INCARNATION_ATTRIBUTE,
			     incarnation);
	sendq.sendMessage(message);
    }


    private final UnaryPredicate flushPredicate = new UnaryPredicate() {
	    public boolean execute(Object m) {
		AttributedMessage msg = (AttributedMessage) m;
		MessageAddress primalAddress = addr.getPrimary();
		MessageAddress src = msg.getOriginator().getPrimary();
		return src.equals(primalAddress);
	    }
	};


    public void flushMessages(ArrayList droppedMessages) {
	sendq_impl.removeMessages(flushPredicate, droppedMessages);
	destq_factory.removeMessages(flushPredicate, droppedMessages);
    }

    public MessageAddress getAddress() {
	return addr;
    }

    public void release() {
	registry.removeAgentState(addr);
	sendq = null;
	registry = null;
    }

    public boolean okToSend(AttributedMessage message) {
	if (sendq == null) return false; // client has released the service

	MessageAddress target = message.getTarget();
	if (target == null || target.toString().equals("")) {
	    if (loggingService.isErrorEnabled())
		loggingService.error("Malformed message: "+message);
	    return false;
	} else {
	    return true;
	}
    }

	
    /**
     * Redirects the request to the MessageTransportRegistry. */
    public void registerClient(MessageTransportClient client) {
	// Should throw an exception of client != this.client
	registry.registerClient(client);
    }


    /**
     * Redirects the request to the MessageTransportRegistry. */
    public void unregisterClient(MessageTransportClient client) {
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

    public AgentState getAgentState() {
	return registry.getAgentState(addr);
    }

}
