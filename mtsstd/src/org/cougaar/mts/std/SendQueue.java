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
    void sendMessage(Message message); 

    /**
     * Used by a SendQueueFactory in its find-or-make algorithm to
     * avoid duplicating SendQueues.  */
    boolean matches(String name);

    /**
     * Number of messages waiting in the queue.
     */
    int size();

}
