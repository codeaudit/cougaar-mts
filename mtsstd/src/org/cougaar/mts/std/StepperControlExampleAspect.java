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

package org.cougaar.mts.std;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.mts.base.DestinationQueue;
import org.cougaar.mts.base.DestinationQueueDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;

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
