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
import org.cougaar.core.service.LoggingService;

/**
 * An simple example of controlling the message stepper, in this case
 * by pausing each DestinationQueue after the second message.  Be sure
 * to list such aspects after the StepperAspect.  */
public class StepperControlExampleAspect
    extends StandardAspect
{
    public Object getDelegate(Object delegatee, Class type) 
    {
	if (type == DestinationQueue.class) {
	    return new DestinationQueueDelegate((DestinationQueue) delegatee);
	} else {
	    return null;
	}
    }


    private class DestinationQueueDelegate
	extends DestinationQueueDelegateImplBase
    {
	int count = 0;

	public DestinationQueueDelegate (DestinationQueue delegatee) {
	    super(delegatee);
	}

	public void dispatchNextMessage(AttributedMessage msg) {
	    super.dispatchNextMessage(msg);
	    if (++count == 2) {
		// Seccond message to this destination has just gone
		// through.  Tell the stepper to pause this queue.
		ServiceBroker sb = getServiceBroker();
		StepService svc =
		    (StepService) sb.getService(this, StepService.class, null);
		if (svc != null)
		    svc.pause(getDestination());
		else if (loggingService.isErrorEnabled())
		    loggingService.error("StepperAspect not loaded?");
	    }
	}
    }
}
