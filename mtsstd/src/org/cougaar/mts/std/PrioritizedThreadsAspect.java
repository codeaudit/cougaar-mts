/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.mts.std;
import java.io.FileInputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.ThreadControlService;
import org.cougaar.core.service.ThreadListenerService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.thread.ThreadListener;
import org.cougaar.mts.base.DestinationQueue;
import org.cougaar.mts.base.StandardAspect;

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
