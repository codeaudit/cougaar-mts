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


    // Should the watchers see the AttributedMessage or its contents?
    private void notifyWatchersOfSend(AttributedMessage message) {
	Message rawMessage = message.getRawMessage();
	Iterator itr = watchers.iterator();
	synchronized (this) {
	    while (itr.hasNext()) {
		MessageTransportWatcher w =(MessageTransportWatcher)itr.next();
		if (Debug.isDebugEnabled(loggingService,WATCHER)) {
		    loggingService.debug("Notifying " + w + " of send");
		}
		w.messageSent(rawMessage);
	    }
	}
    }

    private void notifyWatchersOfReceive(AttributedMessage message) {
	Message rawMessage = message.getRawMessage();
	Iterator itr = watchers.iterator();
	synchronized (this) {
	    while ( itr.hasNext() ) {
		MessageTransportWatcher w =(MessageTransportWatcher)itr.next();
		if (Debug.isDebugEnabled(loggingService,WATCHER)) {
		    loggingService.debug("Notifying " + w + 
					      " of receive");
		}
		w.messageReceived(rawMessage);
	    }
	}
    }


    public class SendQueueDelegate extends SendQueueDelegateImplBase
    {
	public SendQueueDelegate (SendQueue queue) {
	    super(queue);
	}
	
	public void sendMessage(AttributedMessage message) {
	    super.sendMessage(message);
	    notifyWatchersOfSend(message);
	}
	
    }


    public class MessageDelivererDelegate 
	extends MessageDelivererDelegateImplBase
    {
	public MessageDelivererDelegate (MessageDeliverer deliverer) {
	    super(deliverer);
	}
	
	public MessageAttributes deliverMessage(AttributedMessage message, 
						MessageAddress dest) 
	    throws MisdeliveredMessageException
	{
	    MessageAttributes result = super.deliverMessage(message, dest);
	    notifyWatchersOfReceive(message);
	    return result;
	}
	

    }
}


    
