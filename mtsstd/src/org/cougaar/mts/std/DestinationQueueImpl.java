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

import org.cougaar.core.component.Container;
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
    private LinkSelectionPolicy selectionPolicy;
    private DestinationQueue delegate;

    private ArrayList destinationLinks;

    DestinationQueueImpl(MessageAddress destination, Container container)
    {
	super(destination.toString()+"/DestQ", container);
	this.destination = destination;
	container.add(this);
    }

    public void load() {
	super.load();
	ServiceBroker sb = getServiceBroker();
	selectionPolicy =
	(LinkSelectionPolicy)
	    sb.getService(this, LinkSelectionPolicy.class, null);

	this.delegate = this;

	// cache DestinationLinks, per transport
	destinationLinks = getRegistry().getDestinationLinks(destination);
    }


    public MessageAddress getDestination() {
	return destination;
    }


    /**
     * Enqueues the given message. */
    public void holdMessage(AttributedMessage message) {
	add(message);
    }

    public boolean matches(MessageAddress address) {
	return destination.equals(address);
    }
    
    void setDelegate(DestinationQueue delegate) {
	this.delegate = delegate;
    }



    // Save retry-state as instance variables
    private int delay = 500;
    private int retryCount = 0;
    private Exception lastException = null;
    private AttributedMessage previous = null;

    private void resetState() {
	delay = 500; // comes from a property
	retryCount = 0;
	lastException = null;
	previous = null;
    }


     /**
      * Processes the next dequeued message. */
    boolean dispatch(AttributedMessage message) {
	if (message == null) return true;
	if (retryCount == 0)
	    delegate.dispatchNextMessage(message);
	else
	    dispatchNextMessage(message);
	return retryCount == 0;
    }


    public void dispatchNextMessage(AttributedMessage message) {
	if (retryCount == 0) {
	    message.snapshotAttributes();
	    previous = message;
	} else {
	    if (Debug.isDebugEnabled(loggingService,SERVICE))
		loggingService.debug("Retrying " +message);
	}

	Iterator links = destinationLinks.iterator();
	DestinationLink link = 
	    selectionPolicy.selectLink(links, message, previous,
				       retryCount, lastException);
	if (link != null) {
	    if (Debug.isDebugEnabled(loggingService,POLICY))
		loggingService.debug("Selected Protocol " +
				     link.getProtocolClass());
	    try {
		link.addMessageAttributes(message);
		MessageAttributes meta = link.forwardMessage(message);
		resetState();
		return;
	    } catch (UnregisteredNameException no_name) {
		lastException = no_name;
		// nothing to say here
	    } catch (NameLookupException lookup_error) {
		lastException = lookup_error;
		if (Debug.isErrorEnabled(loggingService,COMM)) 
		    loggingService.error(null, lookup_error);
	    } catch (CommFailureException comm_failure) {
		Exception exception = comm_failure.getException();
		Throwable cause = exception.getCause();
		boolean remote = false;
		// A local security exception will be wrapped in a
		// MarshalException.  In this case 'exception' is the
		// MarshalException. and 'cause' is the
		// MessageSecurityException.
		//
		// A remote security exception will be wrapped in an
		// RemoteException which will in turn be wrapped by an
		// UnmarshalException.  In this case 'exception' is
		// the RemoteException, 'cause' is the
		// UnmarshalException and 'cause.getCause' is the
		// MessageSecurityException.
		//
		// Finally, the original GeneralSecurityException is
		// the cause of the MessageSecurityException.
		//
		// What could be simpler?
		//
		if (cause instanceof java.rmi.UnmarshalException) {
		    cause = cause.getCause();
		    remote = true;
		}
		if (cause instanceof MessageSecurityException) {
		    // Always log these.
		    String tag = remote ? "Remote " : "Local ";
		    tag += "Security Exception " 
			+message.getOriginator()+ "->" 
			+message.getTarget();
		    loggingService.error(tag, cause.getCause());

		    // Act as if the message has gone through.
		    resetState();
		    return;
		} else {
		    // This is some other kind of CommFailure, not
		    // related to security.  Retry.
		    lastException = comm_failure;
		    if (Debug.isErrorEnabled(loggingService,COMM)) 
			loggingService.error(null, comm_failure);
		}
	    } catch (MisdeliveredMessageException misd) {
		lastException = misd;
		if (Debug.isDebugEnabled(loggingService,COMM)) 
		    loggingService.debug(misd.toString());
	    }

	    if (!link.retryFailedMessage(message, retryCount)) {
		resetState();
		return;
	    }
	} else if (Debug.isDebugEnabled(loggingService,POLICY)) {
	    loggingService.debug("No Protocol selected ");
	}


	retryCount++;
	if (delay < MAX_DELAY) delay += delay;
	previous = new AttributedMessage(message);
	message.restoreSnapshot();
	scheduleRestart(delay);
    }



}
