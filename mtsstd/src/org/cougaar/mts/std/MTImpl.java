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

import java.rmi.RemoteException;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject; // not used but needed by ANT
import java.rmi.server.RemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.service.LoggingService;

/** Actual RMI remote object providing the implementation of
 * MessageTransport client.  The transient tags shouldn't really be
 * necessary since this object should never be serialized.  But leave
 * them in anyway, for documentation if nothing else.1
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
