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
import org.cougaar.core.component.ServiceProvider;

/**
 * A factory which makes ReceiveLinks.  The caching for ReceiveLinks
 * is in the registry, not here, since the links need to be cleaned up
 * when agents unregister. 
 * 
 * This Factory is a subclass of AspectFactory, so aspects can be *
 * attached to a ReceiveLink when it's first instantiated.  */
public class ReceiveLinkFactory 
    extends AspectFactory
    implements ReceiveLinkProviderService, ServiceProvider
{

    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	// Could restrict this request to the registry
	if (serviceClass == ReceiveLinkProviderService.class) {
	    if (requestor instanceof MessageTransportRegistry.ServiceImpl) 
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



    /**
     * Make a new ReceiveLinkImpl and attach all relevant aspects.
     * The final object returned is the outermost aspect delegate, or
     * the ReceiveLinkImpl itself if there are no aspects.  */
    public ReceiveLink getReceiveLink(MessageTransportClient client) {
	ReceiveLink link = new ReceiveLinkImpl(client, getServiceBroker());
	return (ReceiveLink) attachAspects(link, ReceiveLink.class);
    }
}

