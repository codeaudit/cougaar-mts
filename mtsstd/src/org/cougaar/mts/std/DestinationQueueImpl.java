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
final class DestinationQueueImpl 
    extends MessageQueue 
    implements DestinationQueue, DebugFlags
{
    private static final int MAX_DELAY = 60 * 1000; // 1 minute
    private MessageAddress destination;
    private MessageTransportRegistryService registry;
    private LinkSelectionPolicy selectionPolicy;
    private DestinationQueue delegate;

    private ArrayList destinationLinks;

    DestinationQueueImpl(MessageAddress destination, ServiceBroker sb)
    {
	super(destination.toString(), sb);
	this.destination = destination;

	registry = (MessageTransportRegistryService)
	    sb.getService(this, MessageTransportRegistryService.class, null);
	selectionPolicy =
	(LinkSelectionPolicy)
	    sb.getService(this, LinkSelectionPolicy.class, null);

	this.delegate = this;

	// cache DestinationLinks, per transport
	destinationLinks = registry.getDestinationLinks(destination);
    }



    /**
     * Enqueues the given message. */
    public void holdMessage(Message message) {
	add(message);
    }

    public boolean matches(MessageAddress address) {
	return destination.equals(address);
    }
    
    void setDelegate(DestinationQueue delegate) {
	this.delegate = delegate;
    }


     /**
      * Processes the next dequeued message. */
    void dispatch(Message message) {
	if (message != null) delegate.dispatchNextMessage(message);
    }

    public void dispatchNextMessage(Message message) {
	int delay = 500; // comes from a property
	Iterator links;
	DestinationLink link;
	int retryCount = 0;
	Exception lastException = null;
	while (true) {
	    if (retryCount > 0 && Debug.debug(SERVICE))
		System.err.println("% Retrying " +message);

	    links = destinationLinks.iterator();
	    link = selectionPolicy.selectLink(links, message, retryCount, lastException);
	    if (link != null) {
		if (Debug.debug(POLICY))
		System.out.println("#### Selected Protocol " +
				   link.getProtocolClass());
		try {
		    link.forwardMessage(message);
		    break;
		} catch (UnregisteredNameException no_name) {
		    lastException = no_name;
		    // nothing to say here
		} catch (NameLookupException lookup_error) {
		    lastException = lookup_error;
		    if (Debug.debug(COMM)) lookup_error.printStackTrace();
		} catch (CommFailureException comm_failure) {
		    lastException = comm_failure;
		    if (Debug.debug(COMM)) comm_failure.printStackTrace();
		} catch (MisdeliveredMessageException misd) {
		    lastException = misd;
		    if (Debug.debug(COMM)) System.err.println(misd);
		}

		if (!link.retryFailedMessage(message, retryCount)) break;
	    } else if (Debug.debug(POLICY)) {
		System.out.println("#### No Protocol selected ");
	    }


	    retryCount++;
	    try { Thread.sleep(delay);}
	    catch (InterruptedException ex){}
	    if (delay < MAX_DELAY) delay += delay;
	}
    }



}
