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

    private static final String MCAST = 
	"org.cougaar.message.transport.is-multicast";

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




    public class SendLinkDelegate extends SendLinkDelegateImplBase {
	
	public SendLinkDelegate (SendLink link) {
	    super(link);
	}
	

	public void sendMessage(AttributedMessage msg) {
	    MessageAddress destination = msg.getTarget();
	    if (destination instanceof MulticastMessageAddress) {
		msg.setAttribute(MCAST, destination);
		AttributedMessage copy;
		MessageAddress nodeAddr;
		if (destination.equals(MessageAddress.LOCAL)) {
		    if (Debug.isDebugEnabled(loggingService,MULTICAST))
			loggingService.debug("MCAST: Local multicast");
		    nodeAddr = getRegistry().getLocalAddress();
		    copy = new AttributedMessage(msg);
		    copy.setTarget(nodeAddr);
		    super.sendMessage(copy);
		} else {
		    if (Debug.isDebugEnabled(loggingService,MULTICAST))
			loggingService.debug("MCAST: Remote multicast to "
						  + destination);
		    MulticastMessageAddress dst = 
			(MulticastMessageAddress) destination;
		    Iterator itr = 
			getRegistry().findRemoteMulticastTransports(dst);
		    while (itr.hasNext()) {
			nodeAddr = (MessageAddress) itr.next();
			if (Debug.isDebugEnabled(loggingService,MULTICAST))
			    loggingService.debug("MCAST: next address = " 
						      + nodeAddr);
			copy = new AttributedMessage(msg);
			copy.setTarget(nodeAddr);
			super.sendMessage(copy);
		    }
		}
	    } else {
		super.sendMessage(msg);
	    }
	}

    }



    public class MessageDelivererDelegate
	extends MessageDelivererDelegateImplBase 
    {

	public MessageDelivererDelegate (MessageDeliverer deliverer) {
	    super(deliverer);
	}
	
	public MessageAttributes deliverMessage(AttributedMessage msg, 
						MessageAddress destination) 
	    throws MisdeliveredMessageException
	{
	    MulticastMessageAddress mcastAddr = 
		(MulticastMessageAddress) msg.getAttribute(MCAST);
	    
	    if (mcastAddr != null) {
		if (Debug.isDebugEnabled(loggingService,MULTICAST)) 
		    loggingService.debug("MCAST: Received multicast to "
					      + mcastAddr);
		Iterator i = 
		    getRegistry().findLocalMulticastReceivers(mcastAddr);
		MessageAddress localDestination = null;
		AttributedMessage copy = 
		    new AttributedMessage(msg.getRawMessage(), msg);
		while (i.hasNext()) {
		    localDestination = (MessageAddress) i.next();
		    if (Debug.isDebugEnabled(loggingService,MULTICAST))
			loggingService.debug("MCAST: Delivering to "
						  + localDestination);
		    super.deliverMessage(copy, localDestination);
		}
		// Hmm...
		MessageAttributes meta = new SimpleMessageAttributes();
		meta.setAttribute(MessageAttributes.DELIVERY_ATTRIBUTE,
				  MessageAttributes.DELIVERY_STATUS_DELIVERED);
		return meta;
	    } else {	
		return super.deliverMessage(msg, destination);
	    }
	}
	
    }

}



    
