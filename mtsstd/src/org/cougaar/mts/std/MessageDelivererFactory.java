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
 * A factory which makes MessageDeliverers.  It uses the standard
 * find-or-make approach, where the name is used for finding.  Since
 * this factory is a subclass of AspectFactory, aspects can be
 * attached to a MessageDeliverer when it's first instantiated.  */
public class MessageDelivererFactory extends AspectFactory
{
    private ArrayList deliverers = new ArrayList();
    private MessageTransportRegistry registry;

    MessageDelivererFactory(MessageTransportRegistry registry,
			    ArrayList aspects)
    {
	super(aspects);
	this.registry = registry;
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
