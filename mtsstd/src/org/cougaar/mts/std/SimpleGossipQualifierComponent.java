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

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.qos.metrics.Constants;
import org.cougaar.core.qos.metrics.CredibilityQualifier;
import org.cougaar.core.qos.metrics.DeltaValueQualifier;
import org.cougaar.core.qos.metrics.MetricNotificationQualifier;

public class SimpleGossipQualifierComponent
    extends BoundComponent 
{
    public void load() {
	super.load();

	ServiceBroker sb = getServiceBroker();
	NodeIdentificationService nis = (NodeIdentificationService)
	    sb.getService(this, NodeIdentificationService.class, null);
	String node = nis.getMessageAddress().getAddress();

	ServiceProvider sp = new Provider(node);
	sb.addService(GossipQualifierService.class, sp);
    }


    private static class Provider implements ServiceProvider {
	private GossipQualifierService impl;

	Provider(String node) {
	    impl = new Impl(node);
	}

	public Object getService(ServiceBroker sb, 
				 Object requestor, 
				 Class serviceClass) 
	{
	    if (serviceClass == GossipQualifierService.class) {
		return impl;
	    } else {
		return null;
	    }
	}

	public void releaseService(ServiceBroker sb, 
				   Object requestor, 
				   Class serviceClass, 
				   Object service)
	{
	}
    }

    private static class Impl implements GossipQualifierService, Constants
    {
	String my_addr;
	String host_key;
	String ipflow_key;
	String node;
	String node_key;

	Impl(String node) {
	    this.node = node;
	    try {
		my_addr = java.net.InetAddress.getLocalHost().getHostAddress();
	    } catch (java.net.UnknownHostException ex) {
		my_addr = "127.0.0.1";
	    }
	    host_key = "Host_" + my_addr;
	    ipflow_key = "Ip_Flow_" + my_addr;
	    node_key = "Node_" + node;
	}

	public MetricNotificationQualifier getNotificationQualifier(String key)
	{
	    String classname = null;
	    if (key.matches("^Agent_.*_((Spoke)|(Heard))Time$")) {
		// These values are milliseconds.
		return new DeltaValueQualifier(5000.0);
	    } else {
		return new CredibilityQualifier(SYS_DEFAULT_CREDIBILITY);
	    }
	}


	public boolean shouldForwardRequest(String key) {
	    return 
		!key.startsWith(host_key) &&
		!key.startsWith(ipflow_key) &&
		!key.startsWith("Site_Flow") &&
		!key.startsWith(node_key);
	}

    }

}

