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


import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.Message;


/**
 * The default, and for now only, implementation of SendQueue.  The
 * implementation of <strong>sendMessage</strong> simply adds the
 * message to the queue.  This kind of queue includes its own thread,
 * which invokes <strong>dispatch</strong> as each message is popped
 * off the queue (see MessageQueue).  This, in turn, requests the
 * Router to route the message to the appropriate DestinationQueue.
 * */
class SendQueueImpl extends MessageQueue implements SendQueue
{
    private Router router;
    private MessageTransportRegistryService registry;

    SendQueueImpl(String name, 
		  ServiceBroker sb,
		  Router router, 
		  MessageTransportRegistryService registry) 
    {
	super(name, sb);
	this.router = router;
	this.registry = registry;
    }


    /**
     * This is the callback from the internal thread.  */
    void dispatch(Message message) {
	router.routeMessage(message);
    }


    /**
     * The implementation of this SendQueue method simply adds the
     * message to the internal queue (a CircularQueue).  */
    public void sendMessage(Message message) {
	add(message);
    }

    /**
     * In a system with more than one SendQueue, each would have a
     * unique name. If the SendQueueFactory is ever asked to make a
     * queue with a name that's alreayd in use, it will instead find
     * the existing queue by means of this method.  */
    public boolean matches(String name) {
	return name.equals(getName());
    }

}
