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

import org.cougaar.core.component.ServiceBroker;

/**
 * Default base Aspect class, which will accept any transport at any
 * cutpoint.
 */
abstract public class StandardAspect 
    implements MessageTransportAspect
{
    protected ServiceBroker sb;
    
    public void setServiceBroker(ServiceBroker sb) {
	this.sb = sb;
    }

    public boolean rejectTransport(MessageTransport transport, Class type) {
	// Accept any transport at any cut point by default
	return false;
    }
}
