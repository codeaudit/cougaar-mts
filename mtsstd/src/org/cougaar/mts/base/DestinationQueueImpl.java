/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.mts.base;
import java.util.ArrayList;
import java.util.Iterator;

import org.cougaar.mts.std.AttributedMessage;

import org.cougaar.core.component.Container;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.ThreadService;
import org.cougaar.util.PropertyParser;

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
    implements DestinationQueue
{
    private static final int INITIAL_RETRY_TIMEOUT = 
	PropertyParser.getInt("org.cougaar.core.mts.destq.retry.initialTimeout",
			      500) ; // 1/2 second
    private static final int MAX_RETRY_TIMEOUT = 
	PropertyParser.getInt("org.cougaar.core.mts.destq.retry.maxTimeout",
			      60 * 1000); // 1 minute
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

    int getLane() {
	return ThreadService.WILL_BLOCK_LANE;
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

    private int retryTimeout = INITIAL_RETRY_TIMEOUT;
    private int retryCount = 0;
    private Exception lastException = null;
    private AttributedMessage previous = null;

    private void resetState() {
	retryTimeout = INITIAL_RETRY_TIMEOUT;
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
	    if (loggingService.isDebugEnabled())
		loggingService.debug("Retrying " +message);
	}

	Iterator links = destinationLinks.iterator();
	DestinationLink link = 
	    selectionPolicy.selectLink(links, message, previous,
				       retryCount, lastException);
	if (link != null) {
	    if (loggingService.isDebugEnabled())
		loggingService.debug("To Agent="+destination+
				     " Selected Protocol " +
				     link.getProtocolClass());
	    try {
		link.addMessageAttributes(message);
		link.forwardMessage(message);
		resetState();
		return;
	    } catch (UnregisteredNameException no_name) {
		lastException = no_name;
		// nothing to say here
	    } catch (NameLookupException lookup_error) {
		lastException = lookup_error;
		if (loggingService.isErrorEnabled()) 
		    loggingService.error(null, lookup_error);
	    } catch (CommFailureException comm_failure) {
		Exception cause = (Exception) comm_failure.getCause();	
                if (loggingService.isWarnEnabled()) {
		  String msg = "Failure in communication, message " +message+
		      " caused by \n" +cause;
                  loggingService.warn(msg);
                  if (loggingService.isInfoEnabled()) {
                      loggingService.info("", cause);
                  }
                }
		if (cause instanceof DontRetryException) {
		    // Act as if the message has gone through.
		    resetState();
		    return;
		} else {
		    // This is some other kind of CommFailure, not
		    // related to security.  Retry.
		    lastException = comm_failure;
		}
	    } catch (MisdeliveredMessageException misd) {
		lastException = misd;
		if (loggingService.isDebugEnabled()) 
		    loggingService.debug(misd.toString());
	    }

	    if (!link.retryFailedMessage(message, retryCount)) {
		resetState();
		return;
	    }
	} else if (loggingService.isDebugEnabled()) {
	    loggingService.debug("No Protocol selected for Agent" +
				 message.getTarget());
	}


	retryCount++;
	previous = new AttributedMessage(message);
	message.restoreSnapshot();
	scheduleRestart(retryTimeout);
	retryTimeout  = Math.min(retryTimeout + retryTimeout,
				 MAX_RETRY_TIMEOUT);
    }



}
