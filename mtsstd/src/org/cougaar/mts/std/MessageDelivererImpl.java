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
    public void deliverMessage(Message message) {
	if (message == null) return;
	try {
	    MessageAddress addr = message.getTarget();
	    if (addr instanceof MulticastMessageAddress) {
		Object lock = registry.getLock();
		synchronized (lock) {
		
		    Iterator i = registry.findLocalMulticastReceiveLinks((MulticastMessageAddress)addr); 
		    while (i.hasNext()) {
			ReceiveLink link = (ReceiveLink) i.next();
			link.deliverMessage(message);
		    }
		}
	    } else {
		Object lock = registry.getLock();
		synchronized (lock) {
		    ReceiveLink link = registry.findLocalReceiveLink(addr);
		    if (link != null) {
			link.deliverMessage(message);
		    } else {
			throw new RuntimeException("Misdelivered message "
						   + message +
						   " sent to "+this);
		    }
		}
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }


}