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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.service.ThreadControlService;
import org.cougaar.core.service.ThreadListenerService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.thread.ThreadListener;

/**
 * Not an aspect, but the aspect mechanism provides a simple way to
 * load classes on demand.  This is actually a ThreadListener which
 * alters the ThreadService's queuing behavior.
 */
public class PrioritizedThreadsAspect
    extends StandardAspect
    implements ThreadListener
{

    private HashMap agent_priorities;
    private HashMap thread_priorities;

    private Comparator priorityComparator =
	new Comparator() {
	    public boolean equals(Object x) {
		return x == this;
	    }

	    public int compare(Object o1, Object o2) {
		Object x = thread_priorities.get(o1);
		Object y = thread_priorities.get(o2);

		// Entries placed on the queue before our listener
		// starts won't be found by the map.  Deal with
		// that here.
		if (x == null && y == null) return 0;
		if (x == null) return -1;
		if (y == null) return 1;
		
		// Higher priority should precede lower, so reverse
		// the arguments.
		return ((Comparable) y).compareTo(x);
	    }
	};

    public void load() {
	super.load();
	
	thread_priorities = new HashMap();
	agent_priorities = new HashMap();
	Properties p = new Properties();
	String priorities_file = 
	    System.getProperty("org.cougaar.lib.quo.priorities");
	if (priorities_file != null) {
	    try {
		FileInputStream fis = new FileInputStream(priorities_file);
		p.load(fis);
		fis.close();
	    } catch (java.io.IOException ex) {
		System.err.println(ex);
	    }
	}

	Iterator itr = p.entrySet().iterator();
	while (itr.hasNext()) {
	    Map.Entry entry = (Map.Entry) itr.next();
	    Object key = entry.getKey();
	    String priority = (String) entry.getValue();
	    if (priority != null) {
		agent_priorities.put(key, new Integer(Integer.parseInt(priority)));
	    }
	}
	

	ServiceBroker sb = getServiceBroker();
	ThreadListenerService listenerService = (ThreadListenerService) 
	    sb.getService(this, ThreadListenerService.class, null);
	ThreadControlService controlService = (ThreadControlService) 
	    sb.getService(this, ThreadControlService.class, null);


	// Whack the queue
	listenerService.addListener(this);
	controlService.setQueueComparator(priorityComparator);

	// we should release the services now.

    }


    // MessageTransportAspect
    public Object getDelegate(Object delegatee, Class type) {
	// not a real aspect, so no delegates
	return null;
    }

    

    // ThreadListener
    public void threadQueued(Schedulable thread, Object consumer) {
	if (consumer instanceof DestinationQueue) {
	    // Note the thread's priority just before it goes on the
	    // queue.  The comparator will use this later. 
	    DestinationQueue q  = (DestinationQueue) consumer;
	    MessageAddress address = q.getDestination();
	    Object priority =  agent_priorities.get(address.toString());
	    thread_priorities.put(thread, priority);
	}
    }

    public void threadDequeued(Schedulable thread, Object consumer) {
    }

    public void threadStarted(Schedulable thread, Object consumer) {
    }

    public void threadStopped(Schedulable thread, Object consumer) {
    }

    public void rightGiven(String consumer) {
    }

    public  void rightReturned(String consumer) {
    }

}
