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
import org.cougaar.util.CircularQueue;

/**
 * The default, and for now only, implementation of DestinationQueue.
 * This is a simple passive queue for a particular destination
 * address.  It holds on to a LinkSender and notifies it when messages
 * have been added to the queue.  */
class DestinationQueueImpl extends CircularQueue implements DestinationQueue
{
    private MessageAddress destination;

    DestinationQueueImpl(MessageAddress destination)
    {
	this.destination = destination;
    }



    /**
     * Enqueues the given message and notifies the associated
     * LinkSender. */
    public void holdMessage(Message message) {
	synchronized (this) {
	    super.add(message);
	    notify();
	}
    }

    public boolean matches(MessageAddress address) {
	return destination.equals(address);
    }
    

}
