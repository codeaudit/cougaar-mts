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
import org.cougaar.core.component.ServiceRevokedListener;

/**
 * The default, and for now only, implementation of Router.  The
 * <strong>routeMesageMethod</strong> finds the DestinationQueue for
 * each message's target, and enqueues the outgoing message there.  */
final class RouterImpl implements Router
{
    private DestinationQueueProviderService destQService;

    RouterImpl(ServiceBroker sb)
    {
	destQService = (DestinationQueueProviderService)
	    sb.getService(this, DestinationQueueProviderService.class, null);
    }

    /** Find or make a DestinationQueue for this message, then add the
     message to that queue.  The factory has a fairly efficient cache,
     so we do not have to cache here.  */
    public void routeMessage(AttributedMessage message) {
	MessageAddress destination = message.getTarget();
	DestinationQueue queue = destQService.getDestinationQueue(destination);
	queue.holdMessage(message);
    }

}
