/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
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
abstract class MessageQueue extends Thread
{
    private CircularQueue queue;

    MessageQueue(String name) {
	super(name);
	queue = new CircularQueue();
	start();
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
