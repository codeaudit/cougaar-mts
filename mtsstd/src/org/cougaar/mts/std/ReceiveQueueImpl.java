/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
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



    /**
     * Forward the message on to the appropriate ReceiveLink, or links
     * in the case of a MulticastMessageAddress.  The lookup is
     * handled by the MessageTransportRegistry. */
    private void sendMessageToClient(Message message) {
	if (message == null) return;
	try {
	    MessageAddress addr = message.getTarget();
	    if (addr instanceof MulticastMessageAddress) {
		Iterator i = registry.findLocalMulticastReceiveLinks((MulticastMessageAddress)addr); 
		while (i.hasNext()) {
		    ReceiveLink link = (ReceiveLink) i.next();
		    link.deliverMessage(message);
		}
	    } else {
		ReceiveLink link = registry.findLocalReceiveLink(addr);
		if (link != null) {
		    link.deliverMessage(message);
		} else {
		    throw new RuntimeException("Misdelivered message "
					       + message +
					       " sent to "+this);
		}
	    }
	} catch (Exception e) {
		e.printStackTrace();
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
