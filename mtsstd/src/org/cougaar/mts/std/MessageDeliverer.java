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

import org.cougaar.core.component.Service;

/**
 * The first or second station for an outgoing message on the receive
 * side is a MessageDeliverer (first if no serialization, second
 * otherwise).  <p> In theory a given Message Transport subsystem can
 * have multiple MessageDeliverers.  For this release we only make
 * one, instantiated as a MessageDelivererImpl.  Either way, the
 * MessageDeliverers are instantiated by a MessageDelivererFactory,
 * accessible as the MTS-internal <ff>MessageDeliver</ff> service,
 * 
 * <p> The <strong>deliverMessage</strong> method is used to pass the
 * messages onto the next stop, a ReceiveLink.  The LinkProtocol is
 * responsible for calling the MessageDeliverer's deliverMessage after
 * it reaches the destination node.  
 * 
 * <p> The previous station is MessageReader if the Java serialization
 * was used or DestinationLink on the sender side otherwise. The next
 * station is ReceiveLink.
 *
 * @see MessageDelivererFactory
 * @see SendLink
 * @see SendQueue
 * @see Router
 * @see DestinationQueue
 * @see DestinationLink
 * @see MessageWriter
 * @see MessageReader
 * @see ReceiveLink
 *
 * Javadoc contributions from George Mount.
 */

public interface MessageDeliverer extends Service
{
  /**
   * Called by the LinkProtocol on the receiving side. Chooses
   * the correct ReceiveLink associated with the target
   * address and calls the deliverMessage.
   *
   * @param message The mesage to be delivered.
   * @param dest The target MessageTransportClient address.
   * @see LinkProtocol
   * @see ReceiveLink#deliverMessage(AttributedMessage)
   */
    MessageAttributes deliverMessage(AttributedMessage message,
				     MessageAddress dest)
	throws MisdeliveredMessageException;

  /**
   * Returns true iff this MessageDeliverer is associated with the
   * given name. Currently there is only one MessageDeliverer per
   * node. Used by MessageDelivererFactory should there be multiple
   * MessageDeliverers.
   *
   * @param name The MessageDeliverer name to compare against the one
   *             associated with this MessageDeliverer
   */
    boolean matches(String name);

}
