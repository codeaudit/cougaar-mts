/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.mts.base;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.mts.std.AttributedMessage;


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
