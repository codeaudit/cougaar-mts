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


/**
 * The fourth stop for outgoing messages. Each LinkProtocol has its
 * own DestinationLink implementation class.  DestinationLinks are
 * made by the protocols, acting as factories.  */
public interface DestinationLink
{

    /**
     * This method is used to request the associated transport to do
     * its thing with the given message.  Only called during
     * processing of messages in DestinationQueueImpl.  */
    void forwardMessage(AttributedMessage message) 
	throws UnregisteredNameException, 
	NameLookupException, 
	CommFailureException,
	MisdeliveredMessageException;

    /**
     * This method returns a simple measure of the cost of sending the
     * given message via the associated transport. Only called during
     * processing of messages in DestinationQueueImpl. */
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

}
