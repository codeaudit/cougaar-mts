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

import org.cougaar.core.component.Container;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.ThreadServiceProvider;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.util.CircularQueue;

/**
 * An abstract class which manages a circular queue of messages, and
 * runs its own thread to pop messages off that queue.  The method
 * <strong>dispatch</strong>, provided by instantiable subclasses, is
 * invoked on each message as it's popped off. */
abstract class MessageQueue 
    extends BoundComponent
    implements Runnable
{

    private CircularQueue queue;
    private Schedulable thread;
    private String name;


    MessageQueue(String name, Container container) {
	this.name = name;
	queue = new CircularQueue();
    }



    public void load() {
	super.load();

	if (Boolean.getBoolean("org.cougaar.message.transport.threadtest")) {
	    // Each queue Component has its own ThreadService.  These are
	    // children of the MTS ThreadService.
	    ServiceBroker sb = getServiceBroker();
	    ThreadServiceProvider tsp = new ThreadServiceProvider(sb, name);
	    tsp.provideServices(sb);
	
	    ThreadService old = threadService;
	    // Force a recache of the threadService instance variable,
	    // since it's pointing at the parent as a result of
	    // super.load().
	    threadService = (ThreadService) 
		sb.getService(this, ThreadService.class,  null);
	}
    }

    String getName() {
	return name;
    }

    public void run() {
	while (true) {
	    AttributedMessage m;
	    synchronized (queue) {
		if (queue.isEmpty()) {
		    thread = null;
		    return;
		}
		m = (AttributedMessage) queue.next(); // from top
	    }


	    if (m != null) dispatch(m);
	}
    }

    /** 
     * Enqueue a message. */
    void add(AttributedMessage m) {
	synchronized (queue) {
	    queue.add(m);
	    if (thread == null) {
		thread = threadService.getThread(this, this, name);
		thread.start();
	    }
	}
    }


    /**
     * Process a dequeued message. */
    abstract void dispatch(AttributedMessage m);

    /**
     * Number of messages waiting in the queue.
     */
    public int size (){
	return queue.size();
    }
      
}
