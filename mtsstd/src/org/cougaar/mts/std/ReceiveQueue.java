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


/**
 * The fifth stop for an outgoing message, and the first on the
 * receive side, is a ReceiveQueue.  In theory a given Message
 * Transport subsystem can have multiple ReceiveQueues.  For this
 * release we only make one, instantiated as a ReceiveQueueImpl.
 * Either way, the ReceiveQueues are instantiated by a
 * ReceiveQueueFactory.
 *
 * The <strong>deliverMessage</strong> method is used to queue
 * messages in preparation for passing them onto the next stop, a
 * ReceiveLink.
 *
 * In a system with multiple ReceiveQueues, the
 * <strong>matches</strong> method would be used by the
 * ReceiveQueueFactory to avoid making any particular queue more than
 * once.  */

public interface ReceiveQueue 
{
     /**
     * Used to queue a message that's just been received. */
   public void deliverMessage(Message message);

    /**
     * Used by a ReceiveQueueFactory in its find-or-make algorithm to
     * avoid duplicating ReceiveQueues.  */
    public boolean matches(String name);

    /**
     * Number of messages waiting in the queue.
     */
    public int size();

}
