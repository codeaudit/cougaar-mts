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

public class MulticastAspect extends StandardAspect
{


    public Object getDelegate(Object delegatee, Class type) 
    {
	if (type == SendLink.class) {
	    return new SendLinkDelegate((SendLink) delegatee);
	} else {
	    return null;
	}
    }


    public Object getReverseDelegate(Object delegatee, Class type) 
    {
	if (type == MessageDeliverer.class) {
	    return new MessageDelivererDelegate((MessageDeliverer) delegatee);
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
		    if (debugService.isDebugEnabled(MULTICAST))
			debugService.debug("MCAST: Local multicast");
		    destination = getRegistry().getLocalAddress();
		    msg = new MulticastMessageEnvelope(msg,  destination);
		    link.sendMessage(msg);
		} else {
		    if (debugService.isDebugEnabled(MULTICAST))
			debugService.debug("MCAST: Remote multicast to "
						  + destination);
		    MulticastMessageAddress dst = 
			(MulticastMessageAddress) destination;
		    Iterator itr = 
			getRegistry().findRemoteMulticastTransports(dst);
		    MulticastMessageEnvelope envelope;
		    MessageAddress addr;
		    while (itr.hasNext()) {
			addr = (MessageAddress) itr.next();
			if (debugService.isDebugEnabled(MULTICAST))
			    debugService.debug("MCAST: next address = " 
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



    public class MessageDelivererDelegate
	extends MessageDelivererDelegateImplBase 
    {

	public MessageDelivererDelegate (MessageDeliverer delegatee) {
	    super(delegatee);
	}
	
	public void deliverMessage(Message msg, MessageAddress destination) 
	    throws MisdeliveredMessageException
	{
	    if (msg instanceof MulticastMessageEnvelope) {
		msg = ((MulticastMessageEnvelope) msg).getContents();
		MulticastMessageAddress addr = 
		    (MulticastMessageAddress) msg.getTarget();
		if (debugService.isDebugEnabled(MULTICAST)) 
		    debugService.debug("MCAST: Received multicast to "
					      + addr);
		Iterator i = getRegistry().findLocalMulticastReceivers(addr);
		MessageAddress localDestination = null;
		while (i.hasNext()) {
		    localDestination = (MessageAddress) i.next();
		    if (debugService.isDebugEnabled(MULTICAST))
			debugService.debug("MCAST: Delivering to "
						  + localDestination);
		    deliverer.deliverMessage(msg, localDestination);
		}
	    } else {	
		super.deliverMessage(msg, destination);
	    }
	}
	
    }

}



    
