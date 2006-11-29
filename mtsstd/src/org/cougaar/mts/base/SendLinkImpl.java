/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.mts.base;
import java.util.ArrayList;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.AgentState;
import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.service.LoggingService;
import org.cougaar.mts.std.AttributedMessage;
import org.cougaar.util.UnaryPredicate;


/**
 * The only implementation of {@link SendLink}, instantiated once per
 * MessageTransportClient.  It's main job is simply to place outgoing
 * messages on the (singleton) {@link SendQueue}.
 */
final public class SendLinkImpl
    implements SendLink
{
    static final String VERSION = "version";

    private SendQueue sendq;
    private SendQueueProviderService sendq_factory;
    private DestinationQueueProviderService destq_factory;
    private MessageAddress addr;
    private MessageTransportRegistryService registry;
    private LoggingService loggingService;
    private Long incarnation;
    private Object flush_lock = new Object();

    SendLinkImpl(MessageAddress addr, long incarnation, ServiceBroker sb)
    {
	this.addr = addr;
	this.incarnation = new Long(incarnation);
	registry = (MessageTransportRegistryService)
	    sb.getService(this, MessageTransportRegistryService.class, null);
	sendq_factory = (SendQueueProviderService)
	    sb.getService(this, SendQueueProviderService.class, null);
	sendq = sendq_factory.getSendQueue(addr);
	destq_factory = (DestinationQueueProviderService)
	    sb.getService(this, 
			  DestinationQueueProviderService.class, 
			  null);
	loggingService = (LoggingService)
	    sb.getService(this, LoggingService.class, null);
    }


    // This should be locked vis-a-vis flushMessages
    public void sendMessage(AttributedMessage message) {
	synchronized (flush_lock) {
	    MessageAddress orig = message.getOriginator();
	    if (!addr.equals(orig)) {
		loggingService.error("SendLink saw a message whose originator (" +orig+ ") didn't match the MessageTransportClient address (" +addr+ ")");
	    }
	    message.setAttribute(AttributeConstants.INCARNATION_ATTRIBUTE,
				 incarnation);
	    sendq.sendMessage(message);
	}
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
	synchronized (flush_lock) {
	    sendq_factory.removeMessages(flushPredicate, droppedMessages);
	    destq_factory.removeMessages(flushPredicate, droppedMessages);
	}
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

