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

import org.cougaar.core.service.*;

import org.cougaar.core.node.*;

import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;



/**
 * The third stop for an outgoing message is a DestinationQueue. In
 * this implementation, the DestinationQueueFactory instantiates one
 * DestinationQueue per destination address.  
 * 
 * The dispatching thread associated with this queue will send
 * dequeued messages on to the 'best' DestinationLink, handling
 * retries if an exception occurs.
 *
 * The <strong>holdMessage</strong> method is used to queue messages
 * in preparation for passing them onto the next stop, a
 * transport-specific DestionLink.  Ordinarily this would only be
 * called from a Router.
 *
 * The <strong>matches</strong> method is used by the
 * DestinationQueueFactory to avoid making more than one queue for a
 * given destinationAddress.  */

public interface DestinationQueue 
{
    /**
     * Adds the message to the queue. */
    void holdMessage(Message message);

    /**
     * Handles the next message popped off the queue */
    void dispatchNextMessage(Message message);

    /**
     * Returns true iff this queue was created for the given
     * destination.  Used by the DestinationQueueFactory. */
    boolean matches(MessageAddress address);

    /**
     * Number of messages waiting in the queue.
     */
    int size();
}
