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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A factory which makes MessageDeliverers.  In the current design
 * there's onlyone Deliverer */
public class MessageDelivererFactory 
    extends AspectFactory
    implements ServiceProvider
{
    private MessageDeliverer deliverer;

    MessageDelivererFactory(ServiceBroker sb, String id)
    {
	super(sb);
	MessageTransportRegistryService registry = 
	    (MessageTransportRegistryService)
	    sb.getService(this, MessageTransportRegistryService.class, null);

	String name = id+"/Deliverer";
	MessageDeliverer d = new MessageDelivererImpl(name, registry);
	deliverer = 
	    (MessageDeliverer) attachAspects(d, MessageDeliverer.class);
    }


    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	// Could restrict this request to LinkProtocols
	if (serviceClass == MessageDeliverer.class) {
	    if (requestor instanceof LinkProtocol)
		return deliverer;
	    else
		System.err.println("Illegal request for MessageDeliverer from "
				   + requestor);
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
