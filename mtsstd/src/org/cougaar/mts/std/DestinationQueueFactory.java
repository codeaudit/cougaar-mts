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
import org.cougaar.core.mts.MessageAddress;

import java.util.HashMap;

/**
 * A factory which makes DestinationQueues.  It uses the standard
 * find-or-make approach, where a target address is used for finding.
 * Since this factory is a subclass of AspectFactory, aspects can be
 * attached to a DestinationQueue when it's first instantiated.  */
public class DestinationQueueFactory extends  AspectFactory
{
    private HashMap queues;
    private MessageTransportRegistryService registry;
    private LinkProtocolFactory protocolFactory;
    private LinkSelectionPolicy selectionPolicy;

    DestinationQueueFactory(ServiceBroker sb,
			    LinkProtocolFactory protocolFactory) 
    {
	super(sb);
	queues = new HashMap();
	registry = (MessageTransportRegistryService)
	    sb.getService(this, MessageTransportRegistryService.class, null);
	this.protocolFactory = protocolFactory;
	selectionPolicy =
	(LinkSelectionPolicy)
	    sb.getService(this, LinkSelectionPolicy.class, null);
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
	    DestinationQueueImpl qimpl = 
		new DestinationQueueImpl(destination,
					 sb,
					 registry,
					 protocolFactory,
					 selectionPolicy);
	    q = (DestinationQueue) attachAspects(qimpl, 
						 DestinationQueue.class);
	    qimpl.setDelegate(q);
	    queues.put(destination, q);
	}
	return q;
    }
}

