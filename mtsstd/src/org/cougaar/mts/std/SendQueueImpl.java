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
    private MessageTransportRegistry registry;

    SendQueueImpl(String name, 
		  Router router, 
		  MessageTransportRegistry registry) {
	super(name);
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
