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

import java.io.PrintStream;
import java.util.Observable;
import java.util.Observer;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.qos.metrics.MetricImpl;
import org.cougaar.core.qos.metrics.MetricsService;
import org.cougaar.core.qos.metrics.MetricsUpdateService;

public class MetricsTestAspect 
    extends StandardAspect 
    implements Observer
{
    
    MetricsUpdateService update;
    MetricsService svc;
    long lastUpdate =0;

    public Object getDelegate(Object delegatee, Class type)  {
	if (type == SendQueue.class) {
	    return new DummySendQueue((SendQueue) delegatee);
	} else {
	    return null;
	}
    }


    public void load() {
	super.load();
	ServiceBroker sb = getServiceBroker();
	update = (MetricsUpdateService)
	    sb.getService(this, MetricsUpdateService.class, null);
	svc = (MetricsService)
	    sb.getService(this, MetricsService.class, null);

	String path = System.getProperty("org.cougaar.metrics.callback");
	if (path != null) {
	    svc.subscribeToValue(path, this);
	    System.out.println("Subscribed to " +path);
	}
    }

    public void update(Observable o, Object arg) {
	long now = System.currentTimeMillis();
	long updateDelta = now-lastUpdate;
	long value = ((Metric) arg).longValue();
	long valueDelta = value;
	    
	System.out.println("Update Time=" +updateDelta +
			   " Value =" + arg);
    }


    private class DummySendQueue extends SendQueueDelegateImplBase {
	DummySendQueue(SendQueue delegatee) {
	    super(delegatee);
	}

	public void sendMessage(AttributedMessage message) {
	     runTest();
	    super.sendMessage(message);
	}

    }

    public void runTest() {
	String path = System.getProperty("org.cougaar.metrics.query");
	if (path != null) {
	    Metric val = svc.getValue(path);
	    System.out.println(path+ "=" +val);
	}

	String key = System.getProperty("org.cougaar.metrics.key");
	if (key != null) {
	    Metric m = new MetricImpl(new Long(System.currentTimeMillis()),
				      0.3,
				      "", "MetricsTestAspect");
	    System.out.println("Published " +key+ "=" +m);
	    update.updateValue(key, m);
	    lastUpdate=System.currentTimeMillis();
	}

    }

}
