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
 * A factory which makes MessageDeliverers.  It uses the standard
 * find-or-make approach, where the name is used for finding.  Since
 * this factory is a subclass of AspectFactory, aspects can be
 * attached to a MessageDeliverer when it's first instantiated.  */
public class MessageDelivererFactory extends AspectFactory
{
    private ArrayList deliverers = new ArrayList();
    private MessageTransportRegistryService registry;

    MessageDelivererFactory(ServiceBroker sb)
    {
	super(sb);
	registry = (MessageTransportRegistryService)
	    sb.getService(this, MessageTransportRegistryService.class, null);
    }

    /**
     * Find a MessageDeliverer with the given name, or make a new one
     * of type MessageDelivererImpl if there isn't one by the given
     * name.  In the latter case, attach all relevant aspects as part
     * of the process of creating the queue.  The final object
     * returned is the outermost aspect delegate, or the
     * MessageDelivererImpl itself if there are no aspects.  */
    MessageDeliverer getMessageDeliverer(String name) {
	Iterator i = deliverers.iterator();
	while (i.hasNext()) {
	    MessageDeliverer candidate = (MessageDeliverer) i.next();
	    if (candidate != null && candidate.matches(name)) return candidate;
	}
	// No match, make a new one
	MessageDeliverer d = new MessageDelivererImpl(name, registry);
	d = (MessageDeliverer) attachAspects(d, MessageDeliverer.class);
	deliverers.add(d);
	return d;
    }
}
