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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A simple aspect which implements the message watching functionality
 * by attaching delegates to SendQueue and ReceiveQueue objects.  The
 * delegates are implemented as inner classes.  */
public class WatcherAspect 
    extends StandardAspect
    implements MessageWatcherService
{
    private ArrayList watchers;

    public WatcherAspect() {
	this.watchers = new ArrayList();
    }


    public Object getDelegate(Object delegate, int cutpoint) {
	switch (cutpoint) {
	case SendQueue:
	    return new SendQueueDelegate((SendQueue) delegate);

	case ReceiveQueue:
	    return new ReceiveQueueDelegate((ReceiveQueue) delegate);

	default:
	    return null;
	}
    }


    public void addMessageTransportWatcher(MessageTransportWatcher watcher) {
	watchers.add(watcher);
    }


    private void notifyWatchersOfSend(Message message) {
	Iterator itr = watchers.iterator();
	while (itr.hasNext()) {
	    ((MessageTransportWatcher) itr.next()).messageSent(message);
	}
    }

    private void notifyWatchersOfReceive(Message m) {
	Iterator itr = watchers.iterator();
	while ( itr.hasNext() ) {
	    ((MessageTransportWatcher)itr.next()).messageReceived(m);
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


    public class ReceiveQueueDelegate implements ReceiveQueue
    {
	private ReceiveQueue server;
	
	public ReceiveQueueDelegate (ReceiveQueue server)
	{
	    this.server = server;
	}
	
	public void deliverMessage(Message message) {
	    server.deliverMessage(message);
	    notifyWatchersOfReceive(message);
	}
	
	public boolean matches(String name) {
	    return server.matches(name);
	}

	public int size() {
	    return server.size();
	}
    }
}


    
