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
 * Unlike SendQueues and ReceiveQueues, DestinationQueues are assumed
 * to be passive holding places for messages.  A LinkSender associated
 * with the DestinationQueue will dequeue the messages and send them
 * on to a DestinarionLink.
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
     * Returns true iff this queue was created for the given
     * destination.  Used by the DestinationQueueFactory. */
    public boolean matches(MessageAddress address);

    /**
     * Returns true iff and the queue has no messages in it. */
    public boolean isEmpty();

    /**
     * Pops the next message off the queue. */
    public Object next();
    /**
     * Number of messages waiting in the queue.
     */
    public int size();
}
