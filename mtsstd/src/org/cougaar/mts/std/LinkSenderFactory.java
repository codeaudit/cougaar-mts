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
    private MessageTransportFactory transportFactory;
    private LinkSelectionPolicy selectionPolicy;
	
    LinkSenderFactory(MessageTransportRegistry registry,
		      MessageTransportFactory transportFactory,
		      LinkSelectionPolicy selectionPolicy)
    {
	this.registry = registry;
	this.transportFactory = transportFactory;
	this.selectionPolicy = selectionPolicy;
    }


    /**
     * Instantiate a LinkSender (no find-or-make here). */
    public LinkSender getLinkSender(MessageAddress destination, 
				    Object queue_lock,
				    DestinationQueue queue)
					
    {
	return new LinkSender(destination.toString(), destination,
			      registry, transportFactory, 
			      queue, queue_lock,
			      selectionPolicy);
    }

}
