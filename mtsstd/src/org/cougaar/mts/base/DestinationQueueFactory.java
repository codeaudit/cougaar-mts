/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.mts.base;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.cougaar.core.component.Container;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.mts.std.AttributedMessage;
import org.cougaar.mts.std.DestinationQueueMonitorServlet;
import org.cougaar.mts.std.AspectFactory;
import org.cougaar.mts.std.MessageTimeoutAspect;

/**
 * A factory which makes DestinationQueues.  It uses the standard
 * find-or-make approach, where a target address is used for finding.
 * Since this factory is a subclass of AspectFactory, aspects can be
 * attached to a DestinationQueue when it's first instantiated.  */
public class DestinationQueueFactory 
    extends  AspectFactory
    implements DestinationQueueProviderService, 
	       DestinationQueueMonitorService,
	       ServiceProvider
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



    public void removeMessages(UnaryPredicate pred, ArrayList removed) 
    {
	synchronized (queues) {
	    Iterator itr = impls.iterator();
	    while (itr.hasNext()) {
		MessageQueue queue = (MessageQueue) itr.next();
		queue.removeMessages(pred, removed);
	    }
	}
    }

    public MessageAddress[] getDestinations()
    {
	synchronized (queues) {
	    MessageAddress[] ret = new MessageAddress[queues.size()];
	    queues.keySet().toArray(ret);
	    return ret;
	}
    }
 
     public AttributedMessage[] snapshotQueue(MessageAddress destination)
     {
	 DestinationQueue q = null;
	 MessageAddress dest = destination.getPrimary();
	 synchronized (queues) {
	     q = (DestinationQueue) queues.get(dest);
	 }
	 return (q == null ? null : q.snapshot());
     }
 

    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	// Restrict this service
	if (serviceClass == DestinationQueueProviderService.class) {
	    if (requestor instanceof RouterImpl ||
		requestor instanceof SendLinkImpl ||
		requestor instanceof MessageTimeoutAspect) 
		return this;
	} else if (serviceClass == DestinationQueueMonitorService.class) {
	    if (requestor instanceof DestinationQueueMonitorServlet)
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

