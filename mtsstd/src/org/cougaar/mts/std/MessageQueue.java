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

import org.cougaar.core.service.*;

import org.cougaar.core.node.*;

import org.cougaar.core.mts.Message;

import org.cougaar.util.CircularQueue;
import org.cougaar.util.ReusableThreadPool;
import org.cougaar.util.ReusableThread;

/**
 * An abstract class which manages a circular queue of messages, and
 * runs its own thread to pop messages off that queue.  The method
 * <strong>dispatch</strong>, provided by instantiable subclasses, is
 * invoked on each message as it's popped off. */
abstract class MessageQueue implements Runnable
{
    private static final int maxThreadCount = 100;
    private static ReusableThreadPool threadPool = 
	new ReusableThreadPool(20, maxThreadCount);

    private static Thread getThread(MessageQueue queue) {
	return threadPool.getThread(queue, queue.name);
    }


    private CircularQueue queue;
    private Thread thread;
    private String name;
    private boolean useThreadPool;

    MessageQueue(String name) {
	this(name, false);
    }

    MessageQueue(String name, boolean useThreadPool) {
	this.name = name;
	this.useThreadPool = useThreadPool;
	queue = new CircularQueue();
	if (!useThreadPool) {
	    thread = new Thread(this, name);
	    thread.setDaemon(true);
	    thread.start();
	}
    }




    String getName() {
	return name;
    }

    public void run() {
	while (true) {
	    Message m;
	    // wait for a message to handle
	    synchronized (queue) {
		while (queue.isEmpty()) {
		    if (!useThreadPool) {
			try { queue.wait(); }
			catch (InterruptedException ex) {}
		    } else {
			thread = null;
			return;
		    }
		}
		m = (Message) queue.next(); // from top
	    }

	    if (m != null) {
		dispatch(m);
	    }
	}
    }

    /** 
     * Enqueue a message. */
    void add(Message m) {
	synchronized (queue) {
	    queue.add(m);
	    if (!useThreadPool) {
		queue.notify();
	    } else if (thread == null) {
		thread = getThread(this);
		thread.start();
	    }
	}
    }


    /**
     * Process a dequeued message. */
    abstract void dispatch(Message m);

    /**
     * Number of messages waiting in the queue.
     */
    public int size (){
	return queue.size();
    }
      
}
