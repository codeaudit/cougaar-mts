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

import java.util.List;

import org.cougaar.core.mts.AgentState;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.mts.std.AttributedMessage;

/**
 * SendLink is the first station after the MessageTransportService
 * implementation class. Its primary job is to validate the originator and add
 * the message to the SendQueue, thereby ending the call sequence from the
 * caller's point of view (sendMessage).
 * <p>
 * 
 * @see org.cougaar.core.service.MessageTransportService
 * @see SendQueue
 * @see Router
 * @see DestinationQueue
 * @see DestinationLink
 * @see MessageWriter
 * @see MessageReader
 * @see MessageDeliverer
 * @see ReceiveLink Javadoc contribuions from George Mount.
 */
public interface SendLink {
    /**
     * Validates the originator and add the message to the SendQueue, thereby
     * ending the call sequence from the caller's point of view. Invoked from
     * the MessageTransportService impl class method of the same name, which
     * will have already wrapped the original message in an AttributedMessage,
     * using the original message target address's attributes.
     * 
     * @param message The AttributedMessage to be sent.
     * @see SendQueue#sendMessage(AttributedMessage)
     * @see org.cougaar.core.service.MessageTransportService#sendMessage(Message)
     */
    void sendMessage(AttributedMessage message);

    /**
     * Causes any queued or pending messages from the address associated with
     * the SendLink to be removed from the queues, The list of flushed messages
     * is returned in the supplied ArrayList.
     * 
     * @param messages Dropped messages are added to this list.
     * @see org.cougaar.core.service.MessageTransportService#flushMessages()
     */
    void flushMessages(List<Message> messages);

    /**
     * Releases hooks into the MTS and invalidates the SendLink. Called when the
     * MessageTransportService is released by the MessageTransportClient
     * associated with this SendLink.
     */
    void release();

    /**
     * Returns the MessageAddress that this SendLink is associated with.
     */
    MessageAddress getAddress();

    /**
     * Returns <tt>true</tt> if it is possible to send message. SendLinkImpl
     * only checks that the target MessageAddress is properly formed and
     * release() has not been called.
     * 
     * @param message The message to should be checked for validity.
     * @see org.cougaar.core.service.MessageTransportService#sendMessage(Message)
     */
    boolean okToSend(AttributedMessage message);

    /**
     * Calls the MessageTransportRegistry's registerClient, which in turn calls
     * the registerClient for all LinkProtocols. One and only one client should
     * be registered with a SendLink. Once registered, a ReceiveLink is created
     * so that the client can receive messages.
     * 
     * @param client The MTS client to register.
     * @see org.cougaar.core.service.MessageTransportService#registerClient(MessageTransportClient)
     * @see MessageTransportRegistryService#registerClient(MessageTransportClient)
     */
    void registerClient(MessageTransportClient client);

    /**
     * Unregisters the client from all LinkProtocols. The client should be the
     * client that was previously passed in registerClient. The client will no
     * longer be able to receive messages.
     * 
     * @param client The MTS client to unregister.
     * @see org.cougaar.core.service.MessageTransportService#unregisterClient(MessageTransportClient)
     * @see MessageTransportRegistryService#unregisterClient(MessageTransportClient)
     */
    void unregisterClient(MessageTransportClient client);

    /**
     * Returns the internal name of the Node's MTS. In the current
     * implementation this is just the Node's name, but no assumption should be
     * made that this wll always be true. All SendLinks in a given MTS will
     * always return the same value here.
     */
    String getIdentifier();

    /**
     * Returns <tt>true</tt> iff some LinkProtocol recognizes the given address.
     * 
     * @param address MessageAddress whose status is to be determined.
     * @return <tt>true</tt> iff one of the registered LinkProtocols knows
     *         address.
     */
    boolean addressKnown(MessageAddress address);

    /**
     * Returns an AgentState that can be used by aspects to store and retrieve
     * MTS-client-based data.
     */
    AgentState getAgentState();

}
