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
import org.cougaar.core.society.MessageAddress;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A simple aspect which implements the message watching functionality
 * by attaching delegates to SendQueue and MessageDeliverer objects.  The
 * delegates are implemented as inner classes.  */
public class WatcherAspect 
    extends StandardAspect
{
    private ArrayList watchers;

    public WatcherAspect() {
	this.watchers = new ArrayList();
    }


    public Object getDelegate(Object delegate, Class type) 
    {
	if (type == SendQueue.class) {
	    return new SendQueueDelegate((SendQueue) delegate);
	} else if (type == MessageDeliverer.class) {
	    return new MessageDelivererDelegate((MessageDeliverer) delegate);
	} else {
	    return null;
	}
    }


    synchronized void addWatcher(MessageTransportWatcher watcher) {
	watchers.add(watcher);
    }


    synchronized void removeWatcher(MessageTransportWatcher watcher) {
	watchers.remove(watcher);
    }


    private void notifyWatchersOfSend(Message message) {
	Iterator itr = watchers.iterator();
	synchronized (this) {
	    while (itr.hasNext()) {
		MessageTransportWatcher w =(MessageTransportWatcher)itr.next();
		if (Debug.debugWatcher()) {
		    System.err.println("%%% Notifying " + w + " of send");
		}
		w.messageSent(message);
	    }
	}
    }

    private void notifyWatchersOfReceive(Message m) {
	Iterator itr = watchers.iterator();
	synchronized (this) {
	    while ( itr.hasNext() ) {
		MessageTransportWatcher w =(MessageTransportWatcher)itr.next();
		if (Debug.debugWatcher()) {
		    System.err.println("%%% Notifying " + w + " of receive");
		}
		w.messageReceived(m);
	    }
	}
    }


    public class SendQueueDelegate extends SendQueueDelegateImplBase
    {
	public SendQueueDelegate (SendQueue queue) {
	    super(queue);
	}
	
	public void sendMessage(Message message) {
	    queue.sendMessage(message);
	    notifyWatchersOfSend(message);
	}
	
    }


    public class MessageDelivererDelegate 
	extends MessageDelivererDelegateImplBase
    {
	public MessageDelivererDelegate (MessageDeliverer deliverer) {
	    super(deliverer);
	}
	
	public void deliverMessage(Message message, MessageAddress dest) 
	    throws MisdeliveredMessageException
	{
	    deliverer.deliverMessage(message, dest);
	    notifyWatchersOfReceive(message);
	}
	

    }
}


    
