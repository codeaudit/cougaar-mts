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

import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.node.NodeIdentificationService;


public class HeartBeatAspect 
    extends StandardAspect
    implements  AttributeConstants, Runnable
{
  
    private final String I_AM_A_HEARTBEAT_ATTRIBUTE = "i am a heartbeat";
    private final MessageAddress HB_DEST = 
	MessageAddress.getMessageAddress("FWD-MGMT-NODE");
    private final long delay = 1000;
    private final long timeout = 1000;
    private final long sendInterval = 500;
    long msgCount = 0;

    private SendQueue sendq;
    private MessageAddress us;

    public HeartBeatAspect() {
	super();
    }
  
  
    synchronized void maybeStartSending(SendQueue queue) {
	if (sendq != null) return;

	sendq = queue;

	ServiceBroker sb = getServiceBroker();

	NodeIdentificationService nisvc = (NodeIdentificationService)
	    sb.getService(this, NodeIdentificationService.class, null);
	us = nisvc.getMessageAddress();
	ThreadService tsvc = (ThreadService)
	    sb.getService(this, ThreadService.class, null);
	Schedulable sched = tsvc.getThread(this, this, "HeartBeater");
	sched.schedule(10000, sendInterval);
    }

    static class HBMessage extends Message {
	HBMessage(MessageAddress src, MessageAddress dest) {
	    super(src, dest);
	}
    };

    public void run() {
	if (sendq != null) {
	    Message message = new HBMessage(us, HB_DEST);
	    AttributedMessage a_message = new AttributedMessage(message);
	    a_message.setAttribute(MESSAGE_SEND_DEADLINE_ATTRIBUTE, 
				   new Long(System.currentTimeMillis() + timeout));
	    a_message.setAttribute(I_AM_A_HEARTBEAT_ATTRIBUTE, 
				   new Long(msgCount++));
	    sendq.sendMessage(a_message);
	    System.out.println("Sending message "+ a_message);
	}
    }

    // 
    // Aspect Code to implement TrafficRecord Collection
  
    public Object getDelegate(Object object, Class type) {
	if (type == DestinationLink.class) {
	    return new HeartBeatDestinationLink((DestinationLink) object);
	} else 	if (type == MessageDeliverer.class) {
 	    return new MessageDelivererDelegate((MessageDeliverer) object);
	} else if (type == SendQueue.class) {
	    // steal the sendqueue
	    maybeStartSending((SendQueue) object);
	    
	    return null;
	} else {
	    return null;
	}
    }
  
  
    // Used to added Delay
    public class HeartBeatDestinationLink 
	extends DestinationLinkDelegateImplBase
    {
    
	public HeartBeatDestinationLink(DestinationLink link)
	{
	    super(link);
	}
    
    
	public MessageAttributes forwardMessage(AttributedMessage message) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException
	{ 
	    // Attempt to Deliver message
	     Object count = message.getAttribute(I_AM_A_HEARTBEAT_ATTRIBUTE);
	    if (count != null) {
		try { Thread.sleep(delay); }
		catch (InterruptedException ex) {}
	    }
	    MessageAttributes meta = super.forwardMessage(message);
	    return meta;
	}
    }
  
    public class  MessageDelivererDelegate 
	extends MessageDelivererDelegateImplBase 
    {
    
	MessageDelivererDelegate(MessageDeliverer delegatee) {
	    super(delegatee);
	}
    
	public MessageAttributes deliverMessage(AttributedMessage message,
						MessageAddress dest)
	    throws MisdeliveredMessageException
	{  
	    Object count = message.getAttribute(I_AM_A_HEARTBEAT_ATTRIBUTE);
	    if (count != null) {
		System.out.println("Recieved Heartbeat from="
				   +message.getOriginator()+
				   "count=" + count);
		MessageAttributes metadata = new MessageReply(message);
		    metadata.setAttribute(MessageAttributes.DELIVERY_ATTRIBUTE,
					  MessageAttributes.DELIVERY_STATUS_DELIVERED);
		return metadata;
	    } else {
		return super.deliverMessage(message, dest);
	    }
	}
    
    }
  
  
    
}
