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
 * A factory which makes ReceiveQueues.  It uses the standard
 * find-or-make approach, where the name is used for finding.  Since
 * this factory is a subclass of AspectFactory, aspects can be
 * attached to a ReceiveQueue when it's first instantiated.  */
public class ReceiveQueueFactory extends AspectFactory
{
    private ArrayList queues = new ArrayList();
    private MessageTransportRegistry registry;

    ReceiveQueueFactory(MessageTransportRegistry registry,
			ArrayList aspects)
    {
	super(aspects);
	this.registry = registry;
    }

    /**
     * Find a ReceiveQueue with the given name, or make a new one of
     * type ReceiveQueueImpl if there isn't one by the given name.  In
     * the latter case, attach all relevant aspects as part of the
     * process of creating the queue.  The final object returned is
     * the outermost aspect delegate, or the ReceiveQueueImpl itself if
     * there are no aspects.  */
    ReceiveQueue getReceiveQueue(String name) {
	Iterator i = queues.iterator();
	while (i.hasNext()) {
	    ReceiveQueue candidate = (ReceiveQueue) i.next();
	    if (candidate != null && candidate.matches(name)) return candidate;
	}
	// No match, make a new one
	ReceiveQueue queue = new ReceiveQueueImpl(name, registry);
	queue = (ReceiveQueue) attachAspects(queue, ReceiveQueue);
	queues.add(queue);
	return queue;
    }
}
