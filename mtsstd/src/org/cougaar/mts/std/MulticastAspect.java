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
import org.cougaar.core.society.MessageEnvelope;
import org.cougaar.core.society.MulticastMessageAddress;

import java.util.ArrayList;
import java.util.Iterator;

public class MulticastAspect extends StandardAspect
{

    private MessageTransportRegistry registry;

    public MulticastAspect() {
	super();
	registry = MessageTransportRegistry.getRegistry();
    }


    public Object getDelegate(Object delegate, Class type) 
    {
	if (type == SendLink.class) {
	    return new SendLinkDelegate((SendLink) delegate);
	} else if (type == MessageDeliverer.class) {
	    return new DelivererDelegate((MessageDeliverer) delegate);
	} else {
	    return null;
	}
    }



    private static class MulticastMessageEnvelope extends MessageEnvelope {
    

	MulticastMessageEnvelope(Message message, MessageAddress destination) {
	    super(message, message.getOriginator(), destination);
	}

    
    }


    public class SendLinkDelegate extends SendLinkDelegateImplBase {
	
	public SendLinkDelegate (SendLink link) {
	    super(link);
	}
	

	public void sendMessage(Message msg) {
	    MessageAddress destination = msg.getTarget();
	    if (destination instanceof MulticastMessageAddress) {
		if (destination.equals(MessageAddress.LOCAL)) {
		    if (Debug.debug(MULTICAST))
			System.out.println("### MCAST: Local multicast");
		    msg = new MulticastMessageEnvelope(msg,  destination);
		    link.sendMessage(msg);
		} else {
		    if (Debug.debug(MULTICAST))
			System.out.println("### MCAST: Remote multicast to "
					   + destination);
		    MulticastMessageAddress dst = 
			(MulticastMessageAddress) destination;
		    Iterator itr = registry.findRemoteMulticastTransports(dst);
		    MulticastMessageEnvelope envelope;
		    MessageAddress addr;
		    while (itr.hasNext()) {
			addr = (MessageAddress) itr.next();
			if (Debug.debug(MULTICAST))
			    System.out.println("### MCAST: next address = " 
					       + addr);
			envelope = new MulticastMessageEnvelope(msg, addr);
			link.sendMessage(envelope);
		    }
		}
	    } else {
		link.sendMessage(msg);
	    }
	}

    }



    public class DelivererDelegate extends MessageDelivererDelegateImplBase {

	public DelivererDelegate (MessageDeliverer deliverer) {
	    super(deliverer);
	}
	
	public void deliverMessage(Message msg, MessageAddress dest) 
	    throws MisdeliveredMessageException
	{
	    if (msg instanceof MulticastMessageEnvelope) {
		msg = ((MulticastMessageEnvelope) msg).getContents();
		dest = msg.getTarget();
		MulticastMessageAddress addr = (MulticastMessageAddress) dest;
		if (Debug.debug(MULTICAST))
		    System.out.println("### MCAST: Received multicast to "
					   + addr);
		Iterator i = registry.findLocalMulticastReceivers(addr);
		while (i.hasNext()) {
		    dest = (MessageAddress) i.next();
		    deliverer.deliverMessage(msg, dest);
		    if (Debug.debug(MULTICAST))
			System.out.println("### MCAST: Delivering to "
					   + dest);
		}
	    } else {	
		deliverer.deliverMessage(msg, dest);
	    }
	}
	
    }

}



    
