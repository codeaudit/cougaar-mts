/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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


/**
 * The second station for an outgoing message is a SendQueue.  In theory a
 * given Message Transport subsystem can have multiple SendQueues.
 * For this release we only make one, instantiated as a SendQueueImpl.
 * Either way, the SendQueues are instantiated by a SendQueueFactory,
 * accessible to SendLinkImpl as the internal MTS service
 * <ff>SendQueue</ff>.
 * <p>
 * The <strong>sendMessage</strong> method is used to queue messages
 * in preparation for passing them onto the next station, a Router.
 * Ordinarily this would only be called from a SendLinkImpl.  The
 * processing of queued messages is assumed to take place in its own
 * thread, not the caller's thread.  In other words, sendMessage is
 * the final call in the original client's call sequence.
 * <p>
 * In a system with multiple SendQueues, the <strong>matches</strong>
 * method would be used by the SendQueueFactory to avoid making any
 * particular queue more than once.  
 * <p>
 * The previous stop is SendLink. The next stop is Router.
 *
 * @see SendQueueFactory
 * @see SendLink
 * @see Router
 * @see DestinationQueue
 * @see DestinationLink
 * @see MessageWriter
 * @see MessageReader
 * @see MessageDeliverer
 * @see ReceiveLink
 */
public interface SendQueue 
{
    /**
     * Used by SendLinkImpls to queue outgoing messages.
     *
     * @param message A message to add to the queue.
     * @see SendLink#sendMessage(AttributedMessage)
     *
     * Javadoc contributions from George Mount.
     */
    void sendMessage(AttributedMessage message); 

    /**
     * Used by a SendQueueFactory in its find-or-make algorithm to
     * avoid duplicating SendQueues.  
     *
     * @param name The name of the queue.
     */
    boolean matches(String name);

    /**
     * Number of messages waiting in the queue.
     */
    int size();

}
