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

import org.cougaar.core.society.MessageAddress;


/**
 * A factory for making LinkSenders.  There are no aspects associated
 * with LinkSenders, so this is not an AspectFactory.  */
public class LinkSenderFactory 
{
    private MessageTransportRegistry registry;
    private LinkProtocolFactory protocolFactory;
    private LinkSelectionPolicy selectionPolicy;
	
    LinkSenderFactory(MessageTransportRegistry registry,
		      LinkProtocolFactory protocolFactory,
		      LinkSelectionPolicy selectionPolicy)
    {
	this.registry = registry;
	this.protocolFactory = protocolFactory;
	this.selectionPolicy = selectionPolicy;
    }


    /**
     * Instantiate a LinkSender (no find-or-make here). */
    public LinkSender getLinkSender(MessageAddress destination, 
				    Object queue_lock,
				    DestinationQueue queue)
					
    {
	return new LinkSender(destination.toString(), destination,
			      registry, protocolFactory, 
			      queue, queue_lock,
			      selectionPolicy);
    }

}
