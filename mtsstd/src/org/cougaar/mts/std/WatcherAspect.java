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


    public Object getDelegate(Object delegate, Class type) {
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
		((MessageTransportWatcher) itr.next()).messageSent(message);
	    }
	}
    }

    private void notifyWatchersOfReceive(Message m) {
	Iterator itr = watchers.iterator();
	synchronized (this) {
	    while ( itr.hasNext() ) {
		((MessageTransportWatcher)itr.next()).messageReceived(m);
	    }
	}
    }


    public class SendQueueDelegate implements SendQueue
    {
	private SendQueue server;
	
	public SendQueueDelegate (SendQueue server)
	{
	    this.server = server;
	}
	
	public void sendMessage(Message message) {
	    server.sendMessage(message);
	    notifyWatchersOfSend(message);
	}
	
	public boolean matches(String name){
	    return server.matches(name);
	}
	public int size() {
	    return server.size();
	}
    }


    public class MessageDelivererDelegate implements MessageDeliverer
    {
	private MessageDeliverer server;
	
	public MessageDelivererDelegate (MessageDeliverer server)
	{
	    this.server = server;
	}
	
	public void deliverMessage(Message message, MessageAddress dest) 
	    throws MisdeliveredMessageException
	{
	    server.deliverMessage(message, dest);
	    notifyWatchersOfReceive(message);
	}
	
	public boolean matches(String name) {
	    return server.matches(name);
	}

    }
}


    
