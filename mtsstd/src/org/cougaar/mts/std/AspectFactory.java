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

import java.lang.reflect.Constructor;

/**
 * The root class of all aspect-ready factories.  Such factories have
 * the ability to attach a cascading series of aspect delegates to the
 * instances the factory is making.  The aspects themselves are
 * instantiated once, via reflection, and passed in to the constructor
 * of the factory.  The aspect delegates are made on the fly by each
 * aspect, if it wishes to attach one for a given factory
 * interface. */
abstract public class AspectFactory 
{
    private AspectSupport aspectSupport;


    protected AspectFactory(AspectSupport aspectSupport) {
	this.aspectSupport = aspectSupport;
    }

    public Object attachAspects(Object delegate, Class type) {
	return aspectSupport.attachAspects(delegate, type, null);
    }


    public Object attachAspects(Object delegate, 
				Class type, 
				LinkProtocol protocol)
    {
	return aspectSupport.attachAspects(delegate, type, protocol);
    }


}
