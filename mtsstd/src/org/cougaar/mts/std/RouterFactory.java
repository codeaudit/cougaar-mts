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



/**
 * A factory which makes Routers.  Since this factory is a subclass of
 * AspectFactory, aspects can be attached to a SendQueue when it's
 * first instantiated.  */
public class RouterFactory extends AspectFactory
{
    private MessageTransportRegistry registry;
    private DestinationQueueFactory destQFactory;

    RouterFactory(MessageTransportRegistry registry,
		  DestinationQueueFactory destQFactory,
		  AspectSupport aspectSupport)
    {
	super(aspectSupport);
	this.registry = registry;
	this.destQFactory = destQFactory;
    }

    /**
     * Make a RouterImpl abd attach all relevant aspects.  The final
     * object returned is the outermost aspect delegate, or the
     * RouterImpl itself if there are no aspects.  */
    Router getRouter() {
	Router router = new RouterImpl(registry, destQFactory);
	router = (Router) attachAspects(router, Router.class);
	return router;
    }
}
