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

import java.util.ArrayList;
import java.util.Iterator;
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
    implements MessageTransportCutpoints, Debug
{
    private ArrayList aspects;

    protected AspectFactory(ArrayList aspects) {
	this.aspects = aspects;
    }

    /**
     * Loops through the aspects, allowing each one to attach an
     * aspect delegat in a cascaded series.  If any spects attach a
     * delegate, the final aspect delegate is returned.  If no aspects
     * attach a delegate, the original object, as created by the
     * factory, is returned.  The 'iface' argument describes the
     * abstract type of the objects which the factory creates.  */
    public Object attachAspects(Object delegate, int cutpoint) {
	return attachTransportAspects(delegate, cutpoint, null);
    }


    public Object attachTransportAspects(Object delegate, 
					 int cutpoint, 
					 MessageTransport transport)
    {
	if (aspects != null) {
	    Iterator itr = aspects.iterator();
	    while (itr.hasNext()) {
		MessageTransportAspect aspect = 
		    (MessageTransportAspect) itr.next();
		if (transport != null && aspect.rejectTransport(transport, cutpoint))
		    continue; //skip it

		Object candidate = aspect.getDelegate(delegate, cutpoint);
		if (candidate != null) delegate = candidate;
		if (DEBUG_TRANSPORT) System.out.println("======> " + delegate);
	    }
	}
	return delegate;
    }


}
