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
 * The final station in the message transport system before an
 * incoming message is delivered to an Agent.  One ReceiveLink is
 * instantiated for each MessageTransportClient.  <p> The previous
 * station is MessageDeliverer.
 *
 * @see org.cougaar.core.service.MessageTransportService#registerClient(MessageTransportClient)
 * @see SendLink
 * @see SendQueue
 * @see Router
 * @see DestinationQueue
 * @see DestinationLink
 * @see MessageWriter
 * @see MessageReader
 * @see MessageDeliverer
 *
 * Javadoc contributions from George Mount.
 */
public interface ReceiveLink
{
    /** 
     * Deliver the message to the final recipient (an Agent). 
     * The message is unwrapped by the implementation before
     * calling MessageTransportClient receiveMessage.
     *
     * @param message The message to be delivered.
     * @see MessageTransportClient#receiveMessage
     */
    MessageAttributes deliverMessage(AttributedMessage message);

    /**
     * Returns the client associated with this ReceiveLink
     */
    MessageTransportClient getClient();

}
