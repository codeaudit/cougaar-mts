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

import org.cougaar.core.society.Message;

import org.cougaar.util.CircularQueue;

/**
 * An abstract class which manages a circular queue of messages, and
 * runs its own thread to pop messages off that queue.  The method
 * <strong>dispatch</strong>, provided by instantiable subclasses, is
 * invoked on each message as it's popped off. */
abstract class MessageQueue implements Runnable
{
    private CircularQueue queue;
    private Thread thread;
    private String name;

    MessageQueue(String name) {
	this.name = name;
	queue = new CircularQueue();
	thread = new Thread(this, name);
	thread.setDaemon(true);
	thread.start();
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
		    try {
			queue.wait();
		    } catch (InterruptedException e) {} // dont care
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
	    queue.notify();
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
