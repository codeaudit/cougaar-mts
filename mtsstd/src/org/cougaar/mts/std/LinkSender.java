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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A LinkSender is essentially just a thread whose job is to pop
 * messages off a DestinationQueue and forward them on to the
 * "cheapest" transport.  There's one LinkSender per DestinationQueue,
 * and they're created by a LinkSenderFactory.  */
public class LinkSender implements Runnable
{
    private static final int MAX_DELAY = 60 * 1000; // 1 minute
    private MessageAddress destination;
    private MessageTransportFactory transportFactory;
    private MessageTransportRegistry registry;
    private Thread thread;
    private DestinationQueue queue;
    private Object queueLock;
    private ArrayList destinationLinks;
    private LinkSelectionPolicy selectionPolicy;

    LinkSender(String name, 
	       MessageAddress destination, 
	       MessageTransportRegistry registry,
	       MessageTransportFactory transportFactory,
	       DestinationQueue queue,
	       Object queueLock,
	       LinkSelectionPolicy selectionPolicy) 
    {
	this.destination = destination;
	this.queue = queue;
	this.queueLock = queueLock;
	this.transportFactory = transportFactory;
	this.registry = registry;
	this.selectionPolicy = selectionPolicy;

	// cache DestinationLinks, per transport
	destinationLinks = new ArrayList();
	getDestinationLinks();
	

	thread = new Thread(this, name);
	thread.start();
    }




    /**
     * Here we ask each transport for DestinationLink.  The links will
     * be used later to find the cheapest transport for any given
     * message. */
    private void getDestinationLinks() 
    {
	Iterator itr = transportFactory.getTransports().iterator();
	DestinationLink link;
	while (itr.hasNext()) {
	    MessageTransport tpt = (MessageTransport) itr.next();
	    Class tpt_class = tpt.getClass();
	    link = tpt.getDestinationLink(destination);
	    destinationLinks.add(link);
	}
    }


    /**
     * The thread body pops messages off the corresponding
     * DestinationQueue, finds the cheapest DestinationLink for that
     * message, and forwards the message to that link.  */
    public void run() {
	Message message = null;
	int delay = 500; // comes from a property
	Iterator links;
	DestinationLink link;
	while (true) {
	    synchronized (queueLock) {
		while (queue.isEmpty()) {
		    try { queueLock.wait(); } catch (InterruptedException e) {}
		}

		message = (Message) queue.next();
	    }
	    if (message != null) {
		MessageTransportServiceProxy serviceProxy =
		    registry.findServiceProxy(message.getOriginator());
		while (true) {
		    links = destinationLinks.iterator();
		    link = selectionPolicy.selectLink(links, message);
		    if (link != null) {
			try {
			    link.forwardMessage(message);
			    if (serviceProxy != null) 
				serviceProxy.messageDelivered(message);
			    break;
			} catch (DestinationLink.UnregisteredNameException no_name) {
			    // nothing to say here
			} catch (DestinationLink.NameLookupException lookup_error) {
			    lookup_error.printStackTrace();
			} catch (DestinationLink.CommFailureException comm_failure) {
			    comm_failure.printStackTrace();
			}
		    }

		    if (serviceProxy != null && serviceProxy.isFlushing()) {
			serviceProxy.messageDropped(message);
			break;
		    }

		    try { Thread.sleep(delay);}
		    catch (InterruptedException ex){}
		    if (delay < MAX_DELAY) delay += delay;
		}
	    }
	}
    }


}
