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

import java.util.ArrayList;

/**
 * SendLink is the first station after the MessageTransportService
 * implementation class.  It's primary job is to validate the
 * originator and add the message to the SendQueue, thereby ending the
 * call sequence from the caller's point of view (sendMessage). <p>
 * @see org.cougaar.core.service.MessageTransportService
 * @see SendQueue
 * @see Router
 * @see DestinationQueue
 * @see DestinationLink
 * @see MessageWriter
 * @see MessageReader
 * @see MessageDeliverer
 * @see ReceiveLink
 *
 * Javadoc contribuions from George Mount.
 */
public interface SendLink
{
    /**
     * Validates the originator and add the message to the SendQueue,
     * thereby ending the call sequence from the caller's point of view.
     * Invoked from the MessageTransportService impl class method of the
     * same name, which will have already wrapped the original message
     * in an AttributedMessage, using the original message target
     * address's attributes.
     *
     * @param message The AttributedMessage to be sent.
     * @see SendQueue#sendMessage(AttributedMessage)
     * @see MessageTransportService#sendMessage(Message)
     */
    void sendMessage(AttributedMessage message);


    /**
     * Causes any queued or pending messages from the address
     * associated with the SendLink to be removed from the queues,
     * The list of flushed messages is returned in the supplied
     * ArrayList.
     *
     * @param messages Dropped messages are added to this list.
     * @see org.cougaar.core.service.MessageTransportService#flushMessages()
     */
    void flushMessages(ArrayList messages);

    /**
     * Releases hooks into the MTS and invalidates the SendLink.
     * Called when the MessageTransportService is released by the 
     * MessageTransportClient associated with this SendLink.
     */
    void release();

    /**
     * Returns the MessageAddress that this SendLink is associated with.
     */
    MessageAddress getAddress();

    /**
     * Returns <tt>true</tt> if it is possible to send message.
     * SendLinkImpl only checks that the target MessageAddress is properly formed
     * and release() has not been called.
     *
     * @param message The message to should be checked for validity.
     * @see org.cougaar.core.service.MessageTransportService#sendMessage(Message)
     */
    boolean okToSend(AttributedMessage message);

    /**
     * Calls the MessageTransportRegistry's registerClient, which in turn
     * calls the registerClient for all LinkProtocols. One and only one client
     * should be registered with a SendLink. Once registered, a ReceiveLink
     * is created so that the client can receive messages.
     *
     * @param client The MTS client to register.
     * @see org.cougaar.core.service.MessageTransportService#registerClient(MessageTransportClient)
     * @see MessageTransportRegistryService#registerClient(MessageTransportClient)
     */
    void registerClient(MessageTransportClient client);

    /**
     * Unregisters the client from all LinkProtocols. The client should
     * be the client that was previously passed in registerClient. The client
     * will no longer be able to receive messages.
     *
     * @param client The MTS client to unregister.
     * @see org.cougaar.core.service.MessageTransportService#unregisterClient(MessageTransportClient)
     * @see MessageTransportRegistryService#unregisterClient(MessageTransportClient)
     */
    void unregisterClient(MessageTransportClient client);

    /**
     * Returns the internal name of the Node's MTS. In the current
     * implementation this is just the Node's name, but no assumption
     * should be made that this wll always be true.  All SendLinks in
     * a given MTS will always return the same value here.
     */
    String getIdentifier();

    /**
     * Returns <tt>true</tt> iff some LinkProtocol recognizes the
     * given address.
     *
     * @param address MessageAddress whose status is to be determined.
     * @return <tt>true</tt> iff one of the registered LinkProtocols 
     *         knows address.
     */
    boolean addressKnown(MessageAddress address);

    /**
     * Returns an AgentState that can be used by aspects to store and retrieve
     * MTS-client-based data.
     */
    AgentState getAgentState();

}

