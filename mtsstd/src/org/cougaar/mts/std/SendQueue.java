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
 * The first stop for an outgoing message is a SendQueue.  In theory a
 * given Message Transport subsystem can have multiple SendQueues.
 * For this release we only make one, instantiated as a SendQueueImpl.
 * Either way, the SendQueues are instantiated by a SendQueueFactory.
 *
 * The <strong>sendMessage</strong> method is used to queue messages
 * in preparation for passing them onto the next stop, a Router.
 * Ordinarily this would only be called from a
 * MessageTransportServerProxy.
 *
 * In a system with multiple SendQueues, the <strong>matches</strong>
 * method would be used by the SendQueueFactory to avoid making any
 * particular queue more than once.  */
public interface SendQueue 
{
    /**
     * Used by MessageTransportServerProxy's to queue outgoing
     * messages. */
    public void sendMessage(Message message); 

    /**
     * Used by a SendQueueFactory in its find-or-make algorithm to
     * avoid duplicating SendQueues.  */
    public boolean matches(String name);

    /**
     * Number of messages waiting in the queue.
     */
    public int size();

}
