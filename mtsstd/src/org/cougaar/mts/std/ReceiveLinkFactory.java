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

import java.util.HashMap;

/**
 * A factory which makes ReceiveLinks.  It uses the standard
 * find-or-make approach, where a MessageTransportClient is used for
 * finding.  Since this factory is a subclass of AspectFactory,
 * aspects can be attached to a ReceiveLink when it's first
 * instantiated.  */
public class ReceiveLinkFactory extends AspectFactory
{
    private HashMap links;
    private MessageTransportRegistry registry;
	
    ReceiveLinkFactory(MessageTransportRegistry registry,
		       java.util.ArrayList aspects)
    {
	super(aspects);
	links = new HashMap();
	this.registry = registry;
    }


    /**
     * Find a ReceiveLink for the given client, or make a new one of
     * type ReceiveLinkImpl if there isn't one by the given name.  In
     * the latter case, attach all relevant aspects as part of the
     * process of creating the queue.  The final object returned is
     * the outermost aspect delegate, or the ReceiveLinkImpl itself if
     * there are no aspects.  */
    ReceiveLink getReceiveLink(MessageTransportClient client) {
	ReceiveLink link = (ReceiveLink) links.get(client);
	if (link == null) {
	    link = new ReceiveLinkImpl(client);
	    link = ( ReceiveLink) attachAspects(link, ReceiveLink.class);
	    links.put(client, link);
	}
	return link;
    }
}

