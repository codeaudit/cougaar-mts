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
import org.cougaar.core.component.Component;
import org.cougaar.core.component.Container;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;

/**
 * Root class of Components of the MTS.
 */
abstract public class BoundComponent
    extends GenericStateModelAdapter
    implements Component
{

    private BindingSite bindingSite;
    private MessageTransportRegistryService registry;
    private NameSupport nameSupport;
    private AspectSupport aspectSupport;

    protected LoggingService loggingService;
    protected ThreadService threadService;

    protected BoundComponent() {

    }

    public void load() {
	super.load();
	getLoggingService();
	getThreadService();
	getRegistry();
	getNameSupport();
	getAspectSupport();
    }

    protected MessageTransportRegistryService getRegistry() {
	if (registry == null) {
	    ServiceBroker sb = getServiceBroker();
	    registry =
		(MessageTransportRegistryService)
		sb.getService(this,
			      MessageTransportRegistryService.class,
			      null);
	}
	return registry;
    }


    protected NameSupport getNameSupport() {
	if (nameSupport == null) {
	    ServiceBroker sb = getServiceBroker();
	    nameSupport =
		(NameSupport) sb.getService(this, NameSupport.class,  null);
	}
	return nameSupport;
    }

    protected AspectSupport getAspectSupport() {
	if (aspectSupport == null) {
	    ServiceBroker sb = getServiceBroker();
	    aspectSupport =
		(AspectSupport) sb.getService(this, AspectSupport.class,  null);
	}
	return aspectSupport;
    }


    protected LoggingService getLoggingService() {
	if (loggingService == null) {
	    ServiceBroker sb = getServiceBroker();
	    loggingService =
		(LoggingService) sb.getService(this, LoggingService.class,  
					       null);
	}
	return loggingService;
    }


    protected ThreadService getThreadService() {
	if (threadService == null) {
	    ServiceBroker sb = getServiceBroker();
	    threadService =
		(ThreadService) sb.getService(this, ThreadService.class,  
					       null);
	}
	return threadService;
    }




    public final void setBindingSite(BindingSite bs) {
	this.bindingSite = bs;
    }


    public ServiceBroker getServiceBroker() {
	return bindingSite.getServiceBroker();
    }


}
