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

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ContainerAPI;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.PropagatingServiceBroker;
import org.cougaar.core.component.ServiceBroker;


/**
 * Default base Aspect class, which will accept any transport at any
 * cutpoint.
 */
abstract public class StandardAspect 
    extends ContainerSupport
    implements MessageTransportAspect
{


    private BindingSite bindingSite;

    protected BindingSite getBindingSite() {
	return bindingSite;
    }

    public Object getDelegate(Object delegate, Class type) 
    {
	return null;
    }


    public Object getReverseDelegate(Object delegate, Class type) 
    {
	return null;
    }
	

    // ContainerAPI

    public void requestStop() {}

    public ContainerAPI getContainerProxy() {
	return this;
    }

    protected String specifyContainmentPoint() {
	return "messagetransportservice.aspect";
    }

    public final void setBindingSite(BindingSite bs) {
        super.setBindingSite(bs);
	this.bindingSite = bs;
        setChildServiceBroker(new PropagatingServiceBroker(bs));
    }


}
