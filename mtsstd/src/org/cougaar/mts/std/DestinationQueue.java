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
 * The fourth station for an outgoing message is a DestinationQueue. In
 * this implementation, the DestinationQueueFactory instantiates one
 * DestinationQueue per destination address and is accessible to
 * certain impl classes as the MTS-internal service
 * DestinationQueueProviderService. 
 * <p>
 * The dispatching thread associated with this queue will send
 * dequeued messages on to the 'best' DestinationLink, handling
 * retries if an exception occurs.
 * <p>
 * The <strong>holdMessage</strong> method is used to queue messages
 * in preparation for passing them onto the next stop, a
 * transport-specific DestinationLink.  Ordinarily this would only be
 * called from a Router.
 * <p>
 * The <strong>matches</strong> method is used by the
 * DestinationQueueFactory to avoid making more than one queue for a
 * given destinationAddress.  
 * <p>
 * The previous station is Router. The next station is DestinationLink.
 *
 * @see DestinationQueueFactory
 * @see SendLink
 * @see SendQueue
 * @see Router
 * @see DestinationLink
 * @see MessageWriter
 * @see MessageReader
 * @see MessageDeliverer
 * @see ReceiveLink
 * 
 * Javadoc contributions by George Mount.
 */

public interface DestinationQueue 
{
    /**
     * Adds the message to the queue.  Since the queue runs its own
     * thread, this call is typically the last element in the Router
     * thread's call sequence. */
    void holdMessage(AttributedMessage message);

    /**
     * Handles the next message popped off the queue. Finds the
     * most appropriate DestinationLink based on the LinkSelectionPolicy
     * and calls its <tt>addMessageAttributes</tt> before
     * <tt>forwardMessage</tt> to dispatch.
     *
     * @see LinkSelectionPolicy
     * @see Router#routeMessage(AttributedMessage)
     * @see DestinationLink#addMessageAttributes(MessageAttributes)
     * @see DestinationLink#forwardMessage(AttributedMessage)
     */
    void dispatchNextMessage(AttributedMessage message);

    /**
     * Returns true iff this queue was created for the given
     * destination.  Used by the DestinationQueueFactory. */
    boolean matches(MessageAddress address);

    /**
     * Number of messages waiting in the queue.
     */
    int size();

    /**
     * Returns the address for which this DestinationQueue was created.
     */
    MessageAddress getDestination();

    /**
     * Return a snapshot of the current contents of the queue.
     */
    AttributedMessage[] snapshot();

}
