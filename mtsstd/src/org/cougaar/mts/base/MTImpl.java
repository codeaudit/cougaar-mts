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
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject; // not used but needed by ANT and build process -- DO NOT REMOVE

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.SimpleMessageAttributes;
import org.cougaar.core.service.LoggingService;

import org.cougaar.mts.std.AttributedMessage;

/** Actual RMI remote object providing the implementation of
 * MessageTransport client.  The transient tags shouldn't really be
 * necessary since this object should never be serialized.  But leave
 * them in anyway, for documentation if nothing else.
 **/
public class MTImpl extends RemoteObject implements MT 
{
    private static transient MessageAttributes DummyReturn;
    static {
	DummyReturn = new SimpleMessageAttributes();
	DummyReturn.setAttribute(MessageAttributes.DELIVERY_ATTRIBUTE,
				 MessageAttributes.DELIVERY_STATUS_OLD_INCARNATION);
    }

    private MessageAddress address;

    private transient ServiceBroker sb;
    private transient MessageDeliverer deliverer;
    private transient MessageTransportRegistryService registry;
    private transient LoggingService loggingService;
    private transient SocketFactory socfac;
    private transient HashMap incarnation_numbers = new HashMap();

    public MTImpl(MessageAddress addr,  
		  ServiceBroker sb,
		  SocketFactory socfac) 
	throws RemoteException 
    {
	// super(0, socfac, socfac);
	super();
	this.socfac = socfac;
	this.sb = sb;
	this.address = addr;
	this.deliverer = (MessageDeliverer) 
	    sb.getService(this,  MessageDeliverer.class, null);
	this.loggingService = (LoggingService)
	    sb.getService(this, LoggingService.class, null);
	this.registry = (MessageTransportRegistryService)
	    sb.getService(this, MessageTransportRegistryService.class, null);
    }

    public SocketFactory getSocketFactory() {
	return socfac;
    }

    private void decacheDestinationLink(MessageAddress sender) {
	// Find the RMI DestinationLink for this address and force a
	// decache.
	ArrayList links = registry.getDestinationLinks(sender);
	if (links != null) {
	    Iterator itr = links.iterator();
	    while (itr.hasNext()) {
		Object next = itr.next();
		if (next instanceof RMILinkProtocol.Link) {
		    RMILinkProtocol.Link link = (RMILinkProtocol.Link) next;
		    if (link.getDestination().equals(sender)) {
			if (loggingService.isDebugEnabled())
			    loggingService.debug("Decaching reference for " +
						 sender);
			link.incarnationChanged();
			return;
		    }
		}
	    }
	}
    }

    public MessageAttributes rerouteMessage(AttributedMessage message) 
	throws MisdeliveredMessageException
    {
	MessageAddress sender = message.getOriginator();
	String sender_string = sender.getAddress();
	Long incarnation = (Long)
	    message.getAttribute(AttributeConstants.INCARNATION_ATTRIBUTE);
	Long old = (Long) incarnation_numbers.get(sender_string);
	if (incarnation == null) {
	    if (loggingService.isDebugEnabled())
		loggingService.debug("No incarnation number on message " +
				     message);
	    
	} else if (old == null || old.longValue() < incarnation.longValue()) {
	    // First message from this sender or new incarnation 
	    incarnation_numbers.put(sender_string, incarnation);
	    decacheDestinationLink(sender);
	} else if (old.longValue() > incarnation.longValue()) {
	    // Bogus message from old incarnation.  Pretend normal
	    // delivery but don't process it.
	    return DummyReturn;
	}
	return deliverer.deliverMessage(message, message.getTarget());
    }

    public MessageAddress getMessageAddress() {
	return address;
    }

    public String toString() {
      return "MT for "+getMessageAddress();
    }
}
