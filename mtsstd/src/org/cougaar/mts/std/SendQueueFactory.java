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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A factory which makes SendQueues.  It uses the standard
 * find-or-make approach, where the name is used for finding.  Since
 * this factory is a subclass of AspectFactory, aspects can be
 * attached to a SendQueue when it's first instantiated.  */
public class SendQueueFactory extends AspectFactory
{
    private ArrayList queues = new ArrayList();
    private MessageTransportRegistry registry;

    SendQueueFactory(MessageTransportRegistry registry,
		     ArrayList aspects)
    {
	super(aspects);
	this.registry = registry;
    }

    /**
     * Find a SendQueue with the given name, or make a new one of type
     * SendQueueImpl if there isn't one by the given name.  In the
     * latter case, attach all relevant aspects as part of the process
     * of creating the queue.  The final object returned is the
     * outermost aspect delegate, or the SendQueueImpl itself if there
     * are no aspects.  */
    SendQueue getSendQueue(String name, Router router) {
	Iterator i = queues.iterator();
	while (i.hasNext()) {
	    SendQueue candidate = (SendQueue) i.next();
	    if (candidate != null && candidate.matches(name)) return candidate;
	}
	// No match, make a new one
	SendQueue queue = new SendQueueImpl(name, router, registry);
	queue = (SendQueue) attachAspects(queue, SendQueue.class);
	queues.add(queue);
	return queue;
    }
}
