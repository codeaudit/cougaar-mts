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
import java.util.HashMap;
import org.cougaar.core.service.LoggingService;

public class FlushAspect extends StandardAspect
{

    private HashMap delegates;

    public FlushAspect() {
	super();
	delegates = new HashMap();
    }


    private SendLinkDelegate findSendLink(AttributedMessage message) {
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
	} else if (type == DestinationQueue.class) {
	    return new DestinationQueueDelegate((DestinationQueue) delegate);
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

	public MessageAttributes forwardMessage(AttributedMessage message) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException
	{
	    SendLinkDelegate sendLink = findSendLink(message);
	    if (sendLink == null && loggingService.isErrorEnabled()) {
		loggingService.error("Warning: No SendLink for " +
					  message.getOriginator(),
				     new RuntimeException("call stack"));
	    }
	    try {
		MessageAttributes meta = super.forwardMessage(message);
		if (sendLink != null) sendLink.messageDelivered(message);
		return meta;
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

	public boolean retryFailedMessage(AttributedMessage message, int count)
	{
	    SendLinkDelegate sendLink = findSendLink(message);
	    if (sendLink != null)
		return sendLink.retryFailedMessage(message, count);
	    else
		return super.retryFailedMessage(message, count);
	}

    }

    public class DestinationQueueDelegate
	extends DestinationQueueDelegateImplBase {
	public DestinationQueueDelegate(DestinationQueue queue) {
	    super(queue);
	}

	public void dispatchNextMessage(AttributedMessage message) {
	    super.dispatchNextMessage(message);	
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
	
	public  void unregisterClient(MessageTransportClient client) {
	    super.unregisterClient(client);
	    if (loggingService.isDebugEnabled()) {
		loggingService.info("Unregistered " + 
				    getAddress());
	    } 
	}
	public void release() {
	    unregisterSendLink(this, getAddress());
	    if (loggingService.isDebugEnabled()) {
		loggingService.info("Released " + getAddress());
	    }
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


	public void sendMessage(AttributedMessage message) {
	    synchronized (this) {
		++outstandingMessages;
		if (loggingService.isDebugEnabled()) 
		    showPending("Message queued");
	    }
	    super.sendMessage(message);
	}

	synchronized void messageDelivered(AttributedMessage m) {
	    --outstandingMessages;
	    if (loggingService.isDebugEnabled()) 
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
	synchronized void messageFailed(AttributedMessage message) {
	    if (!flushing) return; // do nothing in this case

	    --outstandingMessages;
	    if (loggingService.isDebugEnabled()) 
		showPending("Message dropped");
	    droppedMessages.add(message);
	    if (outstandingMessages <= 0) this.notify();

	}

	synchronized boolean retryFailedMessage(AttributedMessage message,
						int count) 
	{
	    return !flushing;
	}


	public boolean okToSend(AttributedMessage message) {
	    synchronized (this) {
		if (flushing && loggingService.isErrorEnabled()) {
		    loggingService.error("sendMessage during flush!");
		    return false;
		} 
	    }
	    
	    return super.okToSend(message);
	}

	public synchronized void flushMessages(ArrayList droppedMessages) {
	    flushing = true;
	    this.droppedMessages = droppedMessages;
	    while (outstandingMessages > 0) {
		if (loggingService.isDebugEnabled()) {
		    loggingService.debug(getAddress() + 
					      ": Waiting on " + 
					      outstandingMessages +
					      " messages");
		}
		try { this.wait(); } catch (InterruptedException ex) {}
	    }
	    if (loggingService.isDebugEnabled()) {
		loggingService.debug(getAddress() + 
					  ": All messages flushed.");
	    }
	    flushing = false;
	    this.droppedMessages = null;
	}
    }

}



    
