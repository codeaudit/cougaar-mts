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

import org.cougaar.core.component.ServiceBroker;

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
    private MessageTransportRegistryService registry;

    SendQueueFactory(ServiceBroker sb)
    {
	super(sb);
	registry = (MessageTransportRegistryService)
	    sb.getService(this, MessageTransportRegistryService.class, null);

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
	SendQueue queue = new SendQueueImpl(name, sb, router, registry);
	queue = (SendQueue) attachAspects(queue, SendQueue.class);
	queues.add(queue);
	return queue;
    }
}
