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
import org.cougaar.core.mts.MessageAttributes;

/**
 * The fifth station for outgoing messages. Each LinkProtocol has its own
 * DestinationLink implementation class. DestinationLinks are made by the
 * protocols, acting as factories.
 * <p>
 * The previous station is DestinationQueue. If the protocol uses Java
 * serialization, the next station is MessageWriter. If there is no
 * serialization, MessageDeliverer on the receiving side is the next stop.
 * 
 * @see LinkProtocol
 * @see SendLink
 * @see SendQueue
 * @see Router
 * @see DestinationQueue
 * @see MessageWriter
 * @see MessageReader
 * @see MessageDeliverer
 * @see ReceiveLink Javadoc contributions from George Mount.
 */
public interface DestinationLink {

    /**
     * This method is used to request the associated transport to do its thing
     * with the given message. Only called during processing of messages in
     * DestinationQueueImpl.
     * 
     * @see DestinationQueue#dispatchNextMessage(AttributedMessage)
     */
    MessageAttributes forwardMessage(AttributedMessage message)
            throws UnregisteredNameException, NameLookupException, CommFailureException,
            MisdeliveredMessageException;

    /**
     * This method returns a simple measure of the cost of sending the given
     * message via the associated transport. Only called during processing of
     * messages in DestinationQueueImpl.
     * 
     * @see DestinationQueue#dispatchNextMessage(AttributedMessage)
     * @see LinkSelectionPolicy
     * @see MinCostLinkSelectionPolicy
     */
    int cost(AttributedMessage message);

    /**
     * @return the class of corresponding LinkProtocol.
     */
    Class<? extends LinkProtocol> getProtocolClass();

    /**
     * Is this link currently legitimate and functional? The default is true for
     * loopback and would ordinarily be true for remote calls if the stub (or
     * whatever) is accessible. Aspects can alter this behavior, for example to
     * disable unencrypted rmi.
     * <p>
     * This method in invoked on every loaded link before the link selection
     * policy is run; the policy will not see invalid links at all. As a
     * side-effect, {@link #cost} will not always be run (as it was before
     * 11_0).
     * <p>
     * This method is supposed to be a quick triage. More complicated
     * calculations can be done in {@link #cost} as an implicit form of
     * invalidation.
     * 
     * @param message The message we're trying to send
     */
    boolean isValid(AttributedMessage message);

    /**
     * Ask Link whether or not further retries should be attempted.
     */
    boolean retryFailedMessage(AttributedMessage message, int retryCount);

    /**
     * Return the target/destination of this link.
     */
    MessageAddress getDestination();

    /**
     * Return some form of remote reference for the destination, if it has one
     * (rmi server stub, smtp url, CORBA ior, etc)
     */
    Object getRemoteReference();

    /**
     * Allows the DestinationLink to add attributes before forwarding the
     * message.
     * 
     * @see DestinationQueue#dispatchNextMessage(AttributedMessage)
     */
    void addMessageAttributes(MessageAttributes attrs);

}
