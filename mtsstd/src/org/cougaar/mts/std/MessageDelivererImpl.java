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



import java.util.Iterator;

/**
 * The default, and for now only, implementation of MessageDeliverer.
 * The implementation of <strong>deliverMessage</strong> forwards each
 * message on to the appropriate DestinationLink. */
public class MessageDelivererImpl implements MessageDeliverer
{
    private MessageTransportRegistry registry;
    private String name;

    MessageDelivererImpl(String name, MessageTransportRegistry registry) {
	this.registry = registry;
	this.name = name;
    }

    public boolean matches(String name) {
	return this.name.equals(name);
    }

    /**
     * Forward the message on to the appropriate ReceiveLink, or links
     * in the case of a MulticastMessageAddress.  The lookup is
     * handled by the MessageTransportRegistry. */
    public void deliverMessage(Message message, MessageAddress addr) 
	throws MisdeliveredMessageException
    {
	if (message == null) return;
	synchronized (registry) {
	    // This is locked to prevent the receiver from
	    // unregistering between the lookup and the delivery.
	    ReceiveLink link = registry.findLocalReceiveLink(addr);
	    if (link != null) {
		link.deliverMessage(message);
		return;
	    }
	}
	throw new MisdeliveredMessageException(message);
    }


}
