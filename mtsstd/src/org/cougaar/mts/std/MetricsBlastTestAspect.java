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

public class MetricsBlastTestAspect 
    extends StandardAspect 
    implements Observer
{
    
    MetricsUpdateService update;
    MetricsService svc;
    Thread blasterThread =null;
    
    String key,path;
	
    long callbackCount =0;
    long blastCount=0;
    long lastCallbackDelay=0;
    long lastPrintTime=0;
	

    private void dumpCounters(long now) {
	if (1000 <  (now - lastPrintTime)){
	    System.out.println("blast count=" +blastCount+
			       " callback count=" +callbackCount+
			       " Last delay=" +lastCallbackDelay);
	    lastPrintTime=now;
	}
    }

    public Object getDelegate(Object delegatee, Class type)  {
	return null;

    }

    public void load() {
	super.load();
	ServiceBroker sb = getServiceBroker();
	update = (MetricsUpdateService)
	    sb.getService(this, MetricsUpdateService.class, null);
	svc = (MetricsService)
	    sb.getService(this, MetricsService.class, null);

	path = System.getProperty("org.cougaar.metrics.callback");
	if (path != null) {
	    svc.subscribeToValue(path, this);
	    System.out.println("Subscribed to " +path);


	    key = System.getProperty("org.cougaar.metrics.key");
	    if (key !=null) {
		System.out.println("Blasting to " +key);
		blasterThread = new Thread(new Blaster(), "blaster");
		blasterThread.start();
	    }

	}
    }


    public void update(Observable o, Object arg) {
	callbackCount++;
	long now = System.currentTimeMillis();
	long value = ((Metric) arg).longValue();
	lastCallbackDelay = now - value;
    }

    class Blaster implements Runnable {
	long now;
	long startTime;
	public void run() {
	    // Wait a bit for the Node to initialize
	    try { Thread.sleep(10000); } 
	    catch (InterruptedException xxx) {}
	    // Loop forever turning blaster on and off
	    while (true) {
		System.out.println("Starting Blaster");
		startTime =  System.currentTimeMillis();
		// Blast for 5 seconds and then stop
		while (5000 > (now-startTime)) {
		    now =  System.currentTimeMillis();
		    Metric m = new MetricImpl(new Long(System.currentTimeMillis()),
					      0.3,
					      "", "MetricsTestAspect");
		    update.updateValue(key, m);
		    blastCount++;
		    dumpCounters(now);
		    try { Thread.sleep(0); } 
		    catch (InterruptedException xxx) {}
		}
	    
		System.out.println("Stopped Blaster");
		startTime =  System.currentTimeMillis();
		now =  System.currentTimeMillis();
		// wait and see how long it takes for the updates to stop
		while (10000 > (now-startTime)) {
		    dumpCounters(now);
		    try { Thread.sleep(1000); } 
		    catch (InterruptedException xxx) {}
		    now =  System.currentTimeMillis();
		}
	    }
	}
    }
}
