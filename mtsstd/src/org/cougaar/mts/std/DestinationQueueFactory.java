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

import org.cougaar.core.society.MessageAddress;

import java.util.HashMap;

/**
 * A factory which makes DestinationQueues.  It uses the standard
 * find-or-make approach, where a target address is used for finding.
 * Since this factory is a subclass of AspectFactory, aspects can be
 * attached to a DestinationQueue when it's first instantiated.  */
public class DestinationQueueFactory extends  AspectFactory
{
    private HashMap queues;
    private MessageTransportRegistry registry;
    private LinkSenderFactory linkSenderFactory;
    
    DestinationQueueFactory(MessageTransportRegistry registry,
			    LinkSenderFactory linkSenderFactory,
			    java.util.ArrayList aspects) 
    {
	super(aspects);
	queues = new HashMap();
	this.registry = registry;
	this.linkSenderFactory = linkSenderFactory;
    }

    /**
     * Find a DestinationQueue for the given address, or make a new
     * one of type DestinationQueueImpl if there isn't one yet.  In
     * the latter case, attach all relevant aspects as part of the
     * process of creating the queue.  The final object returned is
     * the outermost aspect delegate, or the DestinationQueueImpl itself if
     * there are no aspects.  */
    DestinationQueue getDestinationQueue(MessageAddress destination) {
	    
	DestinationQueue q = (DestinationQueue) queues.get(destination);
	if (q == null) {
	    DestinationQueueImpl qimpl = new DestinationQueueImpl(destination);
	    q = (DestinationQueue) attachAspects(qimpl, 
						 DestinationQueue.class);
	    linkSenderFactory.getLinkSender(destination, qimpl, q);
	    queues.put(destination, q);
	}
	return q;
    }
}

