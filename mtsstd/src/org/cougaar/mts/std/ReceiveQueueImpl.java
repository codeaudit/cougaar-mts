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
 * The default, and for now only, implementation of ReceiveQueue.  The
 * implementation of <strong>deliverMessage</strong> simply adds the
 * message to the queue.  This kind of queue includes its own thread,
 * which invokes <strong>dispatch</strong> as each message is popped
 * off the queue (see MessageQueue).  This, in turn, forward the
 * message on to the appropriate DestinationLink.
 * */
public class ReceiveQueueImpl extends MessageQueue implements ReceiveQueue
{
    private MessageTransportRegistry registry;

    ReceiveQueueImpl(String name, MessageTransportRegistry registry) {
	super(name);
	this.registry = registry;
    }


    private void sendOne(Message message, MessageAddress dest) {
	ReceiveLink link = registry.findLocalReceiveLink(dest);
	if (link != null) {
	    link.deliverMessage(message);
	} else {
	    throw new RuntimeException("Misdelivered message "
				       + message +
				       " sent to "+this);
	}
    }

    /**
     * Forward the message on to the appropriate ReceiveLink, or links
     * in the case of a MulticastMessageAddress.  The lookup is
     * handled by the MessageTransportRegistry. */
    private void sendMessageToClient(Message message) {
	if (message == null) return;
	MessageAddress addr = message.getTarget();
	if (addr instanceof MulticastMessageAddress) {
	    Iterator i = registry.findLocalMulticastReceivers((MulticastMessageAddress)addr); 
	    while (i.hasNext()) {
		MessageAddress dest = (MessageAddress) i.next();
		sendOne(message, dest);
	    }

	} else {
	    sendOne(message, addr);
	}
    }

    /**
     * This is the callback from the internal thread.  */
    void dispatch(Message message) {
	sendMessageToClient(message);
    }


    /**
     * The implementation of this ReceiveQueue method simply adds the
     * message to the internal queue (a CircularQueue).  */
    public void deliverMessage(Message message) {
	add(message);
    }

    public boolean matches(String name) {
	return name.equals(getName());
    }

}
