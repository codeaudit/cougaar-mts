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
 * The default, and for now only, implementation of DestinationQueue.
 * The dispatcher on this queue selects a DestinationLink based on the
 * LinkSelectionPolicy and forwards to that link.  If an exception
 * occurs during the forwarding, it will retry the whole process,
 * including link selection, continuosly, gradually increasing the
 * delay between retries.  Once the message has been successfully
 * forwared, the ServiceProxy will be notified. */
class DestinationQueueImpl extends MessageQueue implements DestinationQueue
{
    private static final int MAX_DELAY = 60 * 1000; // 1 minute
    private MessageAddress destination;
    private LinkProtocolFactory protocolFactory;
    private MessageTransportRegistry registry;
    private LinkSelectionPolicy selectionPolicy;

    private ArrayList destinationLinks;

    DestinationQueueImpl(MessageAddress destination,
			 MessageTransportRegistry registry,
			 LinkProtocolFactory protocolFactory,
			 LinkSelectionPolicy selectionPolicy)
    {
	super(destination.toString());
	this.destination = destination;
	this.protocolFactory = protocolFactory;
	this.registry =registry;
	this.selectionPolicy = selectionPolicy;

	// cache DestinationLinks, per transport
	destinationLinks = new ArrayList();
	getDestinationLinks();
    }


    /**
     * Here we ask each transport for DestinationLink.  The links will
     * be used later to find the cheapest transport for any given
     * message. */
    private void getDestinationLinks() 
    {
	Iterator itr = protocolFactory.getProtocols().iterator();
	DestinationLink link;
	while (itr.hasNext()) {
	    LinkProtocol lp = (LinkProtocol) itr.next();
	    // Class lp_class = lp.getClass();
	    link = lp.getDestinationLink(destination);
	    destinationLinks.add(link);
	}
    }

    /**
     * Enqueues the given message. */
    public void holdMessage(Message message) {
	add(message);
    }

    public boolean matches(MessageAddress address) {
	return destination.equals(address);
    }
    

     /**
      * Processes the next dequeued message. */
    void dispatch(Message message) {
	if (message != null) dispatchNextMessage(message);
    }

    public void dispatchNextMessage(Message message) {
	int delay = 500; // comes from a property
	Iterator links;
	DestinationLink link;
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
		} catch (UnregisteredNameException no_name) {
		    // nothing to say here
		} catch (NameLookupException lookup_error) {
		    lookup_error.printStackTrace();
		} catch (CommFailureException comm_failure) {
		    comm_failure.printStackTrace();
		} catch (MisdeliveredMessageException misd) {
		    System.err.println(misd);
		}
	    }

	    if (serviceProxy != null)
		if (serviceProxy.messageFailed(message)) break;

	    try { Thread.sleep(delay);}
	    catch (InterruptedException ex){}
	    if (delay < MAX_DELAY) delay += delay;
	}
    }



}
