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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TimerTask;
import org.cougaar.core.component.Container;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;

/**
 * An abstract class which manages a circular queue of messages, and
 * runs its own thread to pop messages off that queue.  The method
 * <strong>dispatch</strong>, provided by instantiable subclasses, is
 * invoked on each message as it's popped off. */
abstract class MessageQueue 
    extends BoundComponent
    implements Runnable
{

    // Simplified queue
    private static class SimpleQueue extends LinkedList {
	Object next() {
	    return removeFirst();
	}
    }

    
    private TimerTask restartTask;
    private SimpleQueue queue;
    private Schedulable thread;
    private String name;
    private AttributedMessage pending;
    private Object pending_lock;


    MessageQueue(String name, Container container) {
	this.name = name;
	pending_lock = new Object();
	queue = new SimpleQueue();
    }




    public void load() {
	super.load();
	thread = threadService.getThread(this, this, name);
    }


    String getName() {
	return name;
    }


    void removeMessagesFrom(MessageAddress address, ArrayList removed) {
	synchronized (queue) {
	    Iterator itr = queue.iterator();
	    while (itr.hasNext()) {
		AttributedMessage msg = (AttributedMessage) itr.next();
		if (msg.getOriginator().equals(address)) {
		    removed.add(msg);
		    itr.remove();
		}
	    }
	}
	synchronized (pending_lock) {
	    if (pending != null && pending.getOriginator().equals(address)) {
		removed.add(pending);
		pending = null;
	    }
	}
    }

    public void run() {
	boolean process_queue = true;
	synchronized (pending_lock) {
	    if (pending != null) {
		// Retry the last failed dispatch before looking at the
		// queue. 
		if (!dispatch(pending)) {
		    process_queue = false;
		} else {
		    pending = null;
		}
	    }
	}
	while (process_queue) {
	    AttributedMessage message;
	    synchronized (queue) {
		if (queue.isEmpty()) break;
		message = (AttributedMessage) queue.next(); // from top
	    }


	    if (message == null || dispatch(message)) continue;
	    
	    // Dispatch failed.  Save the message as pending and stop
	    // walking through the queue.  Presumably the dispatch
	    // body has scheduled a restart.
	    pending = message;
	    break;
	}
    }

    void scheduleRestart(int delay) {
	// Some dedicated task needs to call restart at the right
	// time.  For now use a static java.util.Timer.
	restartTask = new TimerTask() {
		public void run() { restart(); }
	    };
	threadService.schedule(restartTask, delay);
    }

    void restart() {
	thread.start();
    }

    /** 
     * Enqueue a message. */
    void add(AttributedMessage message) {
	synchronized (queue) {
	    queue.add(message);
	}
	thread.start();
    }


    /**
     * Process a dequeued message.  Return value indicates success or
     * failure or the dispatch.  Failed dispatches will be tried again
     * before any further queue entries are dispatched. */
    abstract boolean dispatch(AttributedMessage m);

    /**
     * Number of messages waiting in the queue.
     */
    public int size (){
	return queue.size();
    }
      
}
