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
import org.cougaar.core.society.MulticastMessageAddress;

import java.util.ArrayList;
import java.util.Iterator;


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
    private ArrayList droppedMessages;

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


    private boolean isRemoteMulticast(MessageAddress address) {
	return address instanceof MulticastMessageAddress &&
	    !address.equals(MessageAddress.LOCAL);
	    
    }

    private void showPending(String text) {
	String msgs = 
	    outstandingMessages == 1 ? " message" : " messages";
	System.out.println("%%% " + addr + ": " + text +
			   ", "  + outstandingMessages +  msgs +
			   " now pending");
    }

    /**
     * Redirects the sendMessage to the SendQueue. */
    public void sendMessage(Message message) {
	synchronized (this) {
	    if (flushing) {
		System.err.println("***** sendMessage during flush!");
		Thread.dumpStack();
		return;
	    }
	}
	MessageAddress destination = message.getTarget();
	if (isRemoteMulticast(destination)) {
	    System.out.println("!!!!!!! Remote Multicast!");

	    MulticastMessageAddress dst = 
		(MulticastMessageAddress) destination;
	    Iterator itr = registry.findRemoteMulticastTransports(dst);
	    MulticastMessageEnvelope envelope;
	    MessageAddress addr;
	    while (itr.hasNext()) {
		addr = (MessageAddress) itr.next();
                System.out.println("!!!!!! next address =" + addr);
		envelope = new MulticastMessageEnvelope(message, addr);
		sendQ.sendMessage(envelope);
                synchronized (this) { 
                    ++outstandingMessages; 
                    if (Debug.DEBUG_FLUSH) showPending("Message queued");
                }
	    }
	} else if (checkMessage(message)) {
	    if (destination.equals(MessageAddress.LOCAL)) {
		System.out.println("!!!!!!!!!!!! Local Multicast!");
		message = new MulticastMessageEnvelope(message,  destination);
	    }

	    sendQ.sendMessage(message);
	    synchronized (this) { 
		++outstandingMessages; 
		if (Debug.DEBUG_FLUSH) showPending("Message queued");
	    }
	} else {
	    System.err.println("**** Malformed message: "+message);
	    Thread.dumpStack();
	    return;
	}
    }


    // Callback functions, called only from LinkSender.

    /**
     * Callback from LinkSender which tells us that the message has
     * been successfully (?) delivered to the destination Node.
     */
    synchronized void messageDelivered(Message m) {
	--outstandingMessages;
	if (Debug.DEBUG_FLUSH) showPending("Message delivered");
	if (outstandingMessages <= 0) notify();
    }


    /**
     * Callback from LinkSender which tells us that an unsuccessful
     * attempt was made to deliver the message.  If this proxy is
     * flushing, drop the message and notify the LinkSender of that
     * fact by return true.  Otherwise do nothing, at least for now.
     */
    synchronized boolean messageFailed(Message message) {
	if (!flushing) return false; // do nothing in this case

	--outstandingMessages;
	if (Debug.DEBUG_FLUSH) showPending("Message dropped");
	if (droppedMessages == null) droppedMessages = new ArrayList();
	droppedMessages.add(message);
	if (outstandingMessages <= 0) notify();

	return true;  // tell LinkSender to abandon further send attempts
    }


    /**
     * Wait for all queued messages for our client to be either
     * delivered or dropped. 
     * @return the list of dropped messages, which could be null.
     */
    public synchronized ArrayList flushMessages() {
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
	return droppedMessages;
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

