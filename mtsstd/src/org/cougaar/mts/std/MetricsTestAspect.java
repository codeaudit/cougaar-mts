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
import org.cougaar.core.qos.metrics.MetricsService;
import org.cougaar.core.qos.metrics.MetricsUpdateService;
import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.qos.metrics.MetricImpl;

import java.util.Observer;
import java.util.Observable;

public class MetricsTestAspect 
    extends StandardAspect 
    implements Observer
{

    private double x = 0.0;

    public Object getDelegate(Object delegatee, Class type)  {
	if (type == SendQueue.class) {
	    return new DummySendQueue((SendQueue) delegatee);
	} else {
	    return null;
	}
    }


    public void load() {
	super.load();

	String path = System.getProperty("org.cougaar.metrics.test");
	ServiceBroker sb = getServiceBroker();
	MetricsService svc = (MetricsService)
	    sb.getService(this, MetricsService.class, null);
	svc.subscribeToValue(path, this);
	System.out.println("Subscribed to " +path);
    }

    public void update(Observable o, Object arg) {
	System.out.println("Updated with " +arg);
    }


    private class DummySendQueue extends SendQueueDelegateImplBase {
	DummySendQueue(SendQueue delegatee) {
	    super(delegatee);
	}

	public void sendMessage(AttributedMessage message) {
	    // runTest();
	    super.sendMessage(message);
	}

    }

    public void runTest() {
	String path = System.getProperty("org.cougaar.metrics.test");
	ServiceBroker sb = getServiceBroker();
	MetricsService svc = (MetricsService)
	    sb.getService(this, MetricsService.class, null);
	Metric val = svc.getValue(path);
	System.out.println(path+ "=" +val);
	
	String key = System.getProperty("org.cougaar.metrics.key");
	MetricsUpdateService update = (MetricsUpdateService)
	    sb.getService(this, MetricsUpdateService.class, null);
	// String type = "ProcessStats";
	x = x + 500000;
	Metric m = new MetricImpl(new Double(x), 0.3,
				  "", "MetricsTestAspect");
	// update.updateValue(key, type, m);
	update.updateValue(key, m);

    }

}
