/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.mts.std;
import java.util.ArrayList;
import java.util.HashMap;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.mts.base.DestinationQueue;
import org.cougaar.mts.base.DestinationQueueDelegateImplBase;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.UnregisteredNameException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.SendLink;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.SendLinkDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;

public class FlushAspect extends StandardAspect
{

    private HashMap delegates;

    public FlushAspect() {
	super();
	delegates = new HashMap();
    }


    private SendLinkDelegate findSendLink(AttributedMessage message) {
	MessageAddress addr = message.getOriginator();
	Object result =  delegates.get(addr.getPrimary());
	return (SendLinkDelegate) result;
    }

    private void registerSendLink(SendLinkDelegate link,
				  MessageAddress addr)
    {
	delegates.put(addr.getPrimary(), link);
    }

    private void unregisterSendLink(SendLinkDelegate link,
				    MessageAddress addr)
    {
	delegates.remove(addr.getPrimary());
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



    
