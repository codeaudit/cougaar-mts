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
import java.util.HashMap;

public class FlushAspect extends StandardAspect
{

    private HashMap delegates;

    public FlushAspect() {
	super();
	delegates = new HashMap();
    }


    private ServiceDelegate findServiceDelegate(Message message) {
	MessageAddress addr = message.getOriginator();
	return (ServiceDelegate) delegates.get(addr);
    }

    private void registerServiceDelegate(ServiceDelegate delegate,
					 MessageAddress addr)
    {
	delegates.put(addr, delegate);
    }

    private void unregisterServiceDelegate(ServiceDelegate delegate,
					   MessageAddress addr)
    {
	delegates.remove(addr);
    }

    public Object getDelegate(Object delegate, 
			      LinkProtocol protocol, 
			      Class type) 
    {
	if (type == MessageTransportServiceDelegate.class) {
	    return new ServiceDelegate((MessageTransportServiceDelegate) delegate);
	} else if (type == DestinationLink.class) {
	    return new LinkDelegate((DestinationLink) delegate);
	} else {
	    return null;
	}
    }


    
    public class LinkDelegate extends DestinationLinkDelegateImplBase {
	public LinkDelegate(DestinationLink link) {
	    super(link);
	}

	public void forwardMessage(Message message) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException
	{
	    ServiceDelegate delegate = findServiceDelegate(message);
	    try {
		link.forwardMessage(message);
		if (delegate != null) delegate.messageDelivered(message);
	    } catch (NameLookupException name_ex) {
		if (delegate != null) delegate.messageFailed(message);
		throw name_ex;
	    } catch (UnregisteredNameException unreg_ex) {
		if (delegate != null) delegate.messageFailed(message);
		throw unreg_ex;
	    } catch (CommFailureException comm_ex) {
		if (delegate != null) delegate.messageFailed(message);
		throw comm_ex;
	    } catch (MisdeliveredMessageException misd_ex) {
		if (delegate != null) delegate.messageFailed(message);
		throw misd_ex;
	    }
	}

	public boolean retryFailedMessage(Message message, int count) {
	    ServiceDelegate delegate = findServiceDelegate(message);
	    if (delegate != null)
		return delegate.retryFailedMessage(message, count);
	    else
		return link.retryFailedMessage(message, count);
	}

    }

    public class ServiceDelegate extends ServiceProxyDelegateImplBase {
	
	private int outstandingMessages;
	private ArrayList droppedMessages;
	private boolean flushing;

	public ServiceDelegate (MessageTransportServiceDelegate delegate) {
	    super(delegate);
	    outstandingMessages = 0;
	    flushing = false;
	    registerServiceDelegate(this, getAddress());
	}
	
	public void release() {
	    droppedMessages = null;
	    unregisterServiceDelegate(this, getAddress());
	    super.release();
	}

	private void showPending(String text) {
	    String msgs = 
		outstandingMessages == 1 ? " message" : " messages";
	    System.out.println("%%% " + getAddress() + ": " + text +
			       ", "  + outstandingMessages +  msgs +
			       " now pending");
	}

	synchronized void messageDelivered(Message m) {
	    --outstandingMessages;
	    if (Debug.debugFlush()) showPending("Message delivered");
	    if (outstandingMessages <= 0) notify();
	}


	/**
	 * Callback from DestinationQueueImpl which tells us that an
	 * unsuccessful attempt was made to deliver the message.  If this
	 * proxy is flushing, drop the message and notify the caller
	 * of that fact by return true.  Otherwise do nothing, at least
	 * for now.
	 */
	synchronized void messageFailed(Message message) {
	    if (!flushing) return; // do nothing in this case

	    --outstandingMessages;
	    if (Debug.debugFlush()) showPending("Message dropped");
	    if (droppedMessages == null) droppedMessages = new ArrayList();
	    droppedMessages.add(message);
	    if (outstandingMessages <= 0) notify();

	}

	synchronized boolean retryFailedMessage(Message message, int count) {
	    return !flushing;
	}


	public boolean okToSend(Message message) {
	    synchronized (this) {
		if (flushing) {
		    System.err.println("***** sendMessage during flush!");
		    Thread.dumpStack();
		    return false;
		} 
	    }
	    
	    return delegate.okToSend(message);
	}

	public synchronized ArrayList flushMessages() {
	    flushing = true;
	    droppedMessages = new ArrayList();
	    while (outstandingMessages > 0) {
		if (Debug.debugFlush()) {
		    System.out.println("%%% " + getAddress() + 
				       ": Waiting on " + 
				       outstandingMessages +
				       " messages");
		}
		try { wait(); } catch (InterruptedException ex) {}
	    }
	    if (Debug.debugFlush()) {
		System.out.println("%%% " + getAddress() + 
				   ": All messages flushed.");
	    }
	    flushing = false;
	    return droppedMessages;
	}
    }

}



    
