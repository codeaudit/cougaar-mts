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



/**
 * The third stop for an outgoing message is a DestinationQueue. In
 * this implementation, the DestinationQueueFactory instantiates one
 * DestinationQueue per destination address.  
 * 
 * The dispatching thread associated with this queue will send
 * dequeued messages on to the 'best' DestinationLink, handling
 * retries if an exception occurs.
 *
 * The <strong>holdMessage</strong> method is used to queue messages
 * in preparation for passing them onto the next stop, a
 * transport-specific DestionLink.  Ordinarily this would only be
 * called from a Router.
 *
 * The <strong>matches</strong> method is used by the
 * DestinationQueueFactory to avoid making more than one queue for a
 * given destinationAddress.  */

public interface DestinationQueue 
{
    /**
     * Adds the message to the queue. */
    public void holdMessage(Message message);

    /**
     * Handles the next message popped off the queue */
    public void dispatchNextMessage(Message message);

    /**
     * Returns true iff this queue was created for the given
     * destination.  Used by the DestinationQueueFactory. */
    public boolean matches(MessageAddress address);

    /**
     * Number of messages waiting in the queue.
     */
    public int size();
}
