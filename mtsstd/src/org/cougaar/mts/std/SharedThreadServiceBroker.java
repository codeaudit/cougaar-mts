/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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

import org.cougaar.core.component.*;

/**
 * A ServiceBroker that shares a single ThreadService among all
 * requestors of any given BinderSupport class.
 */
public class SharedThreadServiceBroker
    extends PropagatingServiceBroker
    implements ThreadServiceClient
{

    private ThreadService threadService;
    
    public SharedThreadServiceBroker(ServiceBroker sb) {
	super(sb);
    }



    public ThreadGroup getGroup() {
	return null;
    }


    protected synchronized Object getThreadService() {
	if (threadService == null) {
	    threadService = (ThreadService)
		super.getService(this, ThreadService.class, null);
	}
	return threadService;
    }




    public Object getService(Object requestor, 
			     Class serviceClass, 
			     ServiceRevokedListener srl)
    {
	if (serviceClass == ThreadService.class) {
	    return getThreadService();
	} else {
	    return super.getService(requestor, serviceClass, srl);
	}
    }


}

