/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.mts;

import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;


/**
 * The default, and for now only, implementation of Router.  The
 * <strong>routeMesageMethod</strong> finds the DestinationQueue for
 * each message's target, and enqueues the outgoing message there.  */
class RouterImpl implements Router
{
    private MessageTransportRegistry registry;
    private DestinationQueueFactory destQFactory;


    RouterImpl(MessageTransportRegistry registry, 
	       DestinationQueueFactory destQFactory)
    {
	this.registry = registry;
	this.destQFactory = destQFactory;
    }

    /** Find or make a DestinationQueue for this message, then add the
     message to that queue.  The factory has a fairly efficient cache,
     so we do not have to cache here.  */
    public void routeMessage(Message message) {
	MessageAddress destination = message.getTarget();
	DestinationQueue queue = destQFactory.getDestinationQueue(destination);
	queue.holdMessage(message);
    }

}
