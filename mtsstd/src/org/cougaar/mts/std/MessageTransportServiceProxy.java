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
import org.cougaar.core.society.MessageStatistics;


/**
 * Currently the only implementation of MessageTransportService.  It
 * does almost nothing by itself - its work is accomplished by
 * redirecting calls to the MessageTransportRegistry and the
 * SendQueue.  */
public class MessageTransportServiceProxy 
    implements MessageTransportService
{
    private MessageTransportRegistry registry;
    private SendQueue sendQ;
    private MessageTransportClient client;
    private int outstandingMessages;
    private boolean flushing;
    private MessageAddress addr;

    public MessageTransportServiceProxy(MessageTransportClient client,
					MessageTransportRegistry registry,
					SendQueue queue) 
    {
	this.sendQ = queue;
	this.registry = registry;
	this.client = client;
	this.addr = client.getMessageAddress();
	outstandingMessages = 0;
	flushing = false;
    }



    

    /**
     * Any non-null target passes this check. */
    private boolean checkMessage(Message message) {
	MessageAddress target = message.getTarget();
	// message is ok as long as the target is not empty or null
	return target != null && !target.toString().equals("");
    }




    /**
     * Redirects the sendMessage to the SendQueue. */
    public void sendMessage(Message m) {
	synchronized (this) {
	    if (flushing) {
		System.err.println("***** sendMessage during flush!");
		Thread.dumpStack();
		return;
	    }
	}

	if (checkMessage(m)) {
	    sendQ.sendMessage(m);
	    synchronized (this) { 
		++outstandingMessages; 
		if (Debug.DEBUG_FLUSH) {
		    System.out.println("%%% " + addr +
				       ": Message sent, " 
				       + outstandingMessages + 
				       " messages pending");
		}
	    }
	} else {
	    System.err.println("**** Malformed message: "+m);
	    Thread.dumpStack();
	    return;
	}
    }


    synchronized void messageDelivered(Message m) {
	--outstandingMessages;
	if (Debug.DEBUG_FLUSH) {
	    System.out.println("%%% " + addr + 
			       ": Message delivered, " 
			       + outstandingMessages + 
			       " messages pending");
	}
	if (outstandingMessages <= 0) notify();
    }


    public synchronized void flushMessages() {
	flushing = true;
	while (outstandingMessages > 0) {
	    if (Debug.DEBUG_FLUSH) {
		System.out.println("%%% " + addr + 
				   ": Waiting on " + 
				   outstandingMessages +
				   " messages");
	    }
	    try { wait(); } catch (InterruptedException ex) {}
	}
	if (Debug.DEBUG_FLUSH) {
	    System.out.println("%%% " + addr + ": All messages flushed.");
	}
	flushing = false;
    }

    /**
     * Redirects the request to the MessageTransportRegistry. */
    public synchronized void registerClient(MessageTransportClient client) {
	// Should throw an exception of client != this.client
	registry.registerClient(client);
	registry.registerServiceProxy(this, client.getMessageAddress());
    }


    /**
     * Redirects the request to the MessageTransportRegistry. */
    public synchronized void unregisterClient(MessageTransportClient client) {
	// Should throw an exception of client != this.client
	registry.unregisterClient(client);
    }
    
    public boolean isFlushing() {
	return flushing;
    }

    void droppedMessage(Message message) {
	// notify agent?
	System.err.println("#### Dropping message " + message);
    }

   
    /**
     * Redirects the request to the MessageTransportRegistry. */
    public String getIdentifier() {
	return registry.getIdentifier();
    }

    /**
     * Redirects the request to the MessageTransportRegistry. */
    public boolean addressKnown(MessageAddress a) {
	return registry.addressKnown(a);
    }

}

