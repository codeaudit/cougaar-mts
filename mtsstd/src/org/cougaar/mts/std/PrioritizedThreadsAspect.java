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

import java.io.FileInputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Not an aspect, but the aspect mechanism provides a simple way to
 * load classes on demand.  This is actually a ThreadListener which
 * alters the ThreadService's queuing behavior.
 */
public class PrioritizedThreadsAspect
    extends StandardAspect
    implements ThreadListener
{

    private HashMap priorities;
    private HashMap threads;

    // Maps Threads to their priorities
    private Mapper threadMapper = 
	new Mapper() {
	    public Object map(Object x) {
		return threads.get(x);
	    }
	};


    private Comparator priorityComparator =
	new Comparator() {
	    public boolean equals(Object x) {
		return x == this;
	    }

	    public int compare(Object x, Object y) {
		// Entries placed on the queue before our listener
		// starts won't be found by the mapper.  Deal with
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
	
	threads = new HashMap();
	priorities = new HashMap();
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
		priorities.put(key, new Integer(Integer.parseInt(priority)));
	    }
	}
	

	ServiceBroker sb = getServiceBroker();
	ThreadService threadService = (ThreadService) 
	    sb.getService(this, ThreadService.class, null);
	ThreadListenerService listenerService = (ThreadListenerService) 
	    sb.getService(this, ThreadListenerService.class, null);
	ThreadControlService controlService = (ThreadControlService) 
	    sb.getService(this, ThreadControlService.class, null);


	// Whack the queue
	listenerService.addListener(threadService, this);
	controlService.setQueueComparator(threadService,
					  priorityComparator,
					  threadMapper);

	// we should release the services now.

    }


    // MessageTransportAspect
    public Object getDelegate(Object delegatee, Class type) {
	// not a real aspect, so no delegates
	return null;
    }

    

    // ThreadListener
    public void threadPending(Thread thread, Object consumer) {
	if (consumer instanceof DestinationQueue) {
	    // Note the thread's priority just before it goes on the
	    // queue.  The comparator will use this later. 
	    DestinationQueue q  = (DestinationQueue) consumer;
	    MessageAddress address = q.getDestination();
	    threads.put(thread, priorities.get(address.toString()));
	}
    }

    public void threadStarted(Thread thread, Object consumer) {
    }

    public void threadStopped(Thread thread, Object consumer) {
    }

}
