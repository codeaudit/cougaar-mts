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

import org.cougaar.core.component.Container;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
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
	MessageAddress primalAddress = address.getPrimary();
	synchronized (queue) {
	    Iterator itr = queue.iterator();
	    while (itr.hasNext()) {
		AttributedMessage msg = (AttributedMessage) itr.next();
		if (msg.getOriginator().getPrimary().equals(primalAddress)) {
		    removed.add(msg);
		    itr.remove();
		}
	    }
	}
	synchronized (pending_lock) {
	    if (pending != null && pending.getOriginator().getPrimary().equals(primalAddress)) {
		removed.add(pending);
		pending = null;
	    }
	}
    }



    private static final long HOLD_TIME = 500;

    // Process the last failed message, if any, followed by as many
    // items as possible from the queue, with a max time as given by
    // HOLD_TIME.
    public void run() {
	long endTime= System.currentTimeMillis() + HOLD_TIME;

	// Retry the last failed dispatch before looking at the
	// queue.
	synchronized (pending_lock) {
	    if (pending != null) {
		if (dispatch(pending)) {
		    pending = null;
		} else {
		    // The dispatch code has already scheduled the
		    // thread to run again later
		    return;
		}
	    }
	}

	// Now process the queued items. 
	AttributedMessage message;
	while (System.currentTimeMillis() <= endTime) {
	    synchronized (queue) {
		if (queue.isEmpty()) break; // done for now
		message = (AttributedMessage) queue.next(); // from top
	    }


	    if (message == null || dispatch(message)) {
		// Processing succeeded, continue popping the queue
		continue;
	    } else {
		// Remember the failed message.  The dispatch code
		// has already scheduled the thread to run again later
		pending = message;
		return;
	    }
	}
	restartIfNotEmpty();

    }

    public AttributedMessage[] snapshot() 
    {
        AttributedMessage head;
        synchronized (pending_lock) {
            head = pending;
        }
        synchronized (queue) {
            int size = queue.size();
            if (head != null) size++;
	    AttributedMessage[] ret = new AttributedMessage[size];
            int i = 0;
            if (head != null) ret[i++] = head;
            for (Iterator iter = queue.iterator(); i < size; i++) {
                ret[i] = (AttributedMessage) iter.next();
            }
            return ret;
        }
    }

    // Restart the thread immediately if the queue is not empty.
    private void restartIfNotEmpty() {
	synchronized (queue) {
	    if (!queue.isEmpty()) thread.start();
	}
    }

    void scheduleRestart(int delay) {
	thread.schedule(delay);
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
