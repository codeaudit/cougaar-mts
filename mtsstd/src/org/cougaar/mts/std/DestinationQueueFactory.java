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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.cougaar.core.component.Container;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;

/**
 * A factory which makes DestinationQueues.  It uses the standard
 * find-or-make approach, where a target address is used for finding.
 * Since this factory is a subclass of AspectFactory, aspects can be
 * attached to a DestinationQueue when it's first instantiated.  */
public class DestinationQueueFactory 
    extends  AspectFactory
    implements DestinationQueueProviderService, ServiceProvider
{
    private HashMap queues;
    private ArrayList impls;
    private Container container;

    DestinationQueueFactory(Container container) 
    {
	this.container = container;
	queues = new HashMap();
	impls = new ArrayList();
    }

    /**
     * Find a DestinationQueue for the given address, or make a new
     * one of type DestinationQueueImpl if there isn't one yet.  In
     * the latter case, attach all relevant aspects as part of the
     * process of creating the queue.  The final object returned is
     * the outermost aspect delegate, or the DestinationQueueImpl itself if
     * there are no aspects.  */
    public DestinationQueue getDestinationQueue(MessageAddress destination) 
    {
	MessageAddress dest = destination.getPrimary();
	DestinationQueue q = (DestinationQueue) 
	    queues.get(dest);
	if (q == null) {
	    DestinationQueueImpl qimpl = 
		new DestinationQueueImpl(dest, container);
	    q = (DestinationQueue) attachAspects(qimpl, 
						 DestinationQueue.class);
	    qimpl.setDelegate(q);
	    synchronized (queues) {
		queues.put(dest, q);
		impls.add(qimpl);
	    }
	}
	return q;
    }



    public void removeMessagesFrom(MessageAddress address, ArrayList removed) {
	synchronized (queues) {
	    Iterator itr = impls.iterator();
	    while (itr.hasNext()) {
		MessageQueue queue = (MessageQueue) itr.next();
		queue.removeMessagesFrom(address, removed);
	    }
	}
    }


    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	// Restrict this service
	if (serviceClass == DestinationQueueProviderService.class) {
	    if (requestor instanceof RouterImpl ||
		requestor instanceof SendLinkImpl) 
		return this;
	} 
	return null;
    }

    public void releaseService(ServiceBroker sb, 
			       Object requestor, 
			       Class serviceClass, 
			       Object service)
    {
    }


}

