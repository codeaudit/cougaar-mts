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
		       AspectSupport aspectSupport)
    {
	super(aspectSupport);
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

