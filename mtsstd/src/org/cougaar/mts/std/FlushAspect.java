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
import org.cougaar.core.mts.MessageAddress;

import java.util.ArrayList;
import java.util.HashMap;

public class FlushAspect extends StandardAspect
{

    private HashMap delegates;

    public FlushAspect() {
	super();
	delegates = new HashMap();
    }


    private SendLinkDelegate findSendLink(Message message) {
	MessageAddress addr = message.getOriginator();
	Object result =  delegates.get(addr);
	return (SendLinkDelegate) result;
    }

    private void registerSendLink(SendLinkDelegate link,
				  MessageAddress addr)
    {
	delegates.put(addr, link);
    }

    private void unregisterSendLink(SendLinkDelegate link,
				    MessageAddress addr)
    {
	delegates.remove(addr);
    }

    public Object getDelegate(Object delegate, Class type) 
    {
	if (type == SendLink.class) {
	    return new SendLinkDelegate((SendLink) delegate);
	} else if (type == DestinationLink.class) {
	    return new DestinationLinkDelegate((DestinationLink) delegate);
	} else {
	    return null;
	}
    }


    
    public class DestinationLinkDelegate
	extends DestinationLinkDelegateImplBase 
    {
	public DestinationLinkDelegate(DestinationLink link) {
	    super(link);
	}

	public void forwardMessage(Message message) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException
	{
	    SendLinkDelegate sendLink = findSendLink(message);
	    if (sendLink == null && loggingService.isErrorEnabled()) {
		loggingService.error("Warning: No SendLink for " +
					  message.getOriginator());
	    }
	    try {
		link.forwardMessage(message);
		if (sendLink != null) sendLink.messageDelivered(message);
	    } catch (NameLookupException name_ex) {
		if (sendLink != null) sendLink.messageFailed(message);
		throw name_ex;
	    } catch (UnregisteredNameException unreg_ex) {
		if (sendLink != null) sendLink.messageFailed(message);
		throw unreg_ex;
	    } catch (CommFailureException comm_ex) {
		if (sendLink != null) sendLink.messageFailed(message);
		throw comm_ex;
	    } catch (MisdeliveredMessageException misd_ex) {
		if (sendLink != null) sendLink.messageFailed(message);
		throw misd_ex;
	    }
	}

	public boolean retryFailedMessage(Message message, int count) {
	    SendLinkDelegate sendLink = findSendLink(message);
	    if (sendLink != null)
		return sendLink.retryFailedMessage(message, count);
	    else
		return link.retryFailedMessage(message, count);
	}

    }

    public class SendLinkDelegate extends SendLinkDelegateImplBase {
	
	private int outstandingMessages;
	private ArrayList droppedMessages;
	private boolean flushing;

	public SendLinkDelegate(SendLink link) {
	    super(link);
	    outstandingMessages = 0;
	    flushing = false;
	    registerSendLink(this, getAddress());
	}
	
	public void release() {
	    droppedMessages = null;
	    unregisterSendLink(this, getAddress());
	    super.release();
	}

	private void showPending(String text) {
	    if (!loggingService.isInfoEnabled()) return;
	    String msgs = 
		outstandingMessages == 1 ? " message" : " messages";
	    String msg = "% " + getAddress() + ": " + text +
		", "  + outstandingMessages +  msgs +
		" now pending";
	    loggingService.info(msg);
	}


	public void sendMessage(Message message) {
	    synchronized (this) {
		++outstandingMessages;
		if (Debug.isDebugEnabled(loggingService,FLUSH)) 
		    showPending("Message queued");
	    }
	    link.sendMessage(message);
	}

	synchronized void messageDelivered(Message m) {
	    --outstandingMessages;
	    if (Debug.isDebugEnabled(loggingService,FLUSH)) 
		showPending("Message delivered");
	    if (outstandingMessages <= 0) this.notify();
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
	    if (Debug.isDebugEnabled(loggingService,FLUSH)) showPending("Message dropped");
	    if (droppedMessages == null) droppedMessages = new ArrayList();
	    droppedMessages.add(message);
	    if (outstandingMessages <= 0) this.notify();

	}

	synchronized boolean retryFailedMessage(Message message, int count) {
	    return !flushing;
	}


	public boolean okToSend(Message message) {
	    synchronized (this) {
		if (flushing && loggingService.isErrorEnabled()) {
		    loggingService.error("sendMessage during flush!");
		    return false;
		} 
	    }
	    
	    return link.okToSend(message);
	}

	public synchronized ArrayList flushMessages() {
	    flushing = true;
	    droppedMessages = new ArrayList();
	    while (outstandingMessages > 0) {
		if (Debug.isDebugEnabled(loggingService,FLUSH)) {
		    loggingService.debug(getAddress() + 
					      ": Waiting on " + 
					      outstandingMessages +
					      " messages");
		}
		try { this.wait(); } catch (InterruptedException ex) {}
	    }
	    if (Debug.isDebugEnabled(loggingService,FLUSH)) {
		loggingService.debug(getAddress() + 
					  ": All messages flushed.");
	    }
	    flushing = false;
	    return droppedMessages;
	}
    }

}



    
