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

import org.cougaar.core.component.Container;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.service.LoggingService;

/**
 * 
 * @property org.cougaar.message.transport.policy Sets the message
 * transport policy to instance o the specified class.
 */
public class LinkSelectionPolicyServiceProvider
    implements ServiceProvider
{

    private final static String POLICY_PROPERTY =
	"org.cougaar.message.transport.policy";

    private LinkSelectionPolicy policy;
    private LoggingService loggingService;

    LinkSelectionPolicyServiceProvider(ServiceBroker sb,
				       Container container) 
    {
	loggingService = (LoggingService)
	    sb.getService(this, LoggingService.class, null);
	LinkSelectionProvisionService lsp = (LinkSelectionProvisionService)
	    sb.getService(this, LinkSelectionProvisionService.class, null);
	policy = lsp.getPolicy();
	if (policy == null) policy = createSelectionPolicy(container);
    }


    private LinkSelectionPolicy createSelectionPolicy(Container container) {
	String policy_classname = System.getProperty(POLICY_PROPERTY);
	LinkSelectionPolicy selectionPolicy = null;
	if (policy_classname == null) {
	    selectionPolicy = new MinCostLinkSelectionPolicy();
	} else {
	    try {
		Class policy_class = Class.forName(policy_classname);
		selectionPolicy = 
		    (LinkSelectionPolicy) policy_class.newInstance();
		if (loggingService.isDebugEnabled())
		    loggingService.debug("Created " +  policy_classname);
	    } catch (Exception ex) {
		if (loggingService.isErrorEnabled())
		    loggingService.error(null, ex);
		selectionPolicy = new MinCostLinkSelectionPolicy();
	    }
	}

	container.add(selectionPolicy);
	return selectionPolicy;

    }



    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	if (serviceClass == LinkSelectionPolicy.class) {
	    if (requestor instanceof DestinationQueueImpl)
		return policy;
	    else if (loggingService.isErrorEnabled())
		loggingService.error("Illegal request for LinkSelectionPolicy from "
				   + requestor);
	}
	return null;
    }


    public void releaseService(ServiceBroker sb, 
			       Object requestor, 
			       Class serviceClass, 
			       Object service)
    {
	if (serviceClass == LinkSelectionPolicy.class) {
	    // ??
	}
    }




}
