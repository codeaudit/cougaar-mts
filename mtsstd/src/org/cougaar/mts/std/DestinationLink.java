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
 * The fifth station for outgoing messages. Each LinkProtocol has its
 * own DestinationLink implementation class.  DestinationLinks are
 * made by the protocols, acting as factories.
 * <p>
 * The previous station is DestinationQueue. If the protocol uses
 * Java serialization, the next station is MessageWriter. 
 * If there is no serialization, MessageDeliverer on the receiving
 * side is the next stop. 
 *
 * @see LinkProtocol
 * @see SendLink
 * @see SendQueue
 * @see Router
 * @see DestinationQueue
 * @see MessageWriter
 * @see MessageReader
 * @see MessageDeliverer
 * @see ReceiveLink
 *
 * Javadoc contributions from George Mount.
 */
public interface DestinationLink
{

    /**
     * This method is used to request the associated transport to do
     * its thing with the given message.  Only called during
     * processing of messages in DestinationQueueImpl.  
     *
     * @see DestinationQueue#dispatchNextMessage(AttributedMessage)
     */
    MessageAttributes forwardMessage(AttributedMessage message) 
	throws UnregisteredNameException, 
	NameLookupException, 
	CommFailureException,
	MisdeliveredMessageException;

    /**
     * This method returns a simple measure of the cost of sending the
     * given message via the associated transport. Only called during
     * processing of messages in DestinationQueueImpl. 
     *
     * @see DestinationQueue#dispatchNextMessage(AttributedMessage)
     * @see LinkSelectionPolicy
     * @see MinCostLinkSelectionPolicy
     */
    int cost(AttributedMessage message);

    
    /**
     * @return the class of corresponding LinkProtocol.
     */
    Class getProtocolClass();


    /**
     * Ask Link whether or not further retries should be attempted.
     */
    boolean retryFailedMessage(AttributedMessage message, int retryCount);


    /**
     * Return the target/destination of this link. 
     */
    MessageAddress getDestination();


    /**
     * Return some form of remote reference for the destination, if it
     * has one (rmi server stub, smtp url, CORBA ior, etc) */
    Object getRemoteReference();

  /**
   * Allows the DestinationLink to add attributes before forwarding 
   * the message.
   *
   * @see DestinationQueue#dispatchNextMessage(AttributedMessage)
   */
    void addMessageAttributes(MessageAttributes attrs);

}
