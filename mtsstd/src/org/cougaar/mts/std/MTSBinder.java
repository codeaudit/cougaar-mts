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


import org.cougaar.core.component.BinderSupport;
import org.cougaar.core.component.BinderFactory;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceBroker;

public final class MTSBinder 
    extends BinderSupport
{
    // All MTS binders share service broker which has special handling
    // to ensure that they all use the same ThreadService proxy
    // (though with unique aspect delegates)
    private static ServiceBroker sb;

    private synchronized static void ensureSharedServiceBroker(ServiceBroker broker) {
	if (sb == null) sb = new SharedThreadServiceBrokerWithAspects(broker);
    }

    private class MTSBinderProxy implements BindingSite {
	public ServiceBroker getServiceBroker() {
	    return sb;
	}


	public void requestStop() {
	     MTSBinder.this.requestStop();
	}

    }


    public MTSBinder(BinderFactory bf, Object child) {
	super(bf, child);
    }


    protected final BindingSite getBinderProxy() {
	ensureSharedServiceBroker(getServiceBroker());
	return new MTSBinderProxy();
    }


}
