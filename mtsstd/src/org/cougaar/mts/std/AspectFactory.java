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

import java.lang.reflect.Constructor;

/**
 * The root class of all aspect-ready factories.  Such factories have
 * the ability to attach a cascading series of aspect delegates to the
 * instances the factory is making.  The aspects themselves are
 * instantiated once, via reflection, and passed in to the constructor
 * of the factory.  The aspect delegates are made on the fly by each
 * aspect, if it wishes to attach one for a given factory
 * interface. */
abstract public class AspectFactory implements DebugFlags
{
    private AspectSupport aspectSupport;


    protected AspectFactory(AspectSupport aspectSupport) {
	this.aspectSupport = aspectSupport;
    }

    public Object attachAspects(Object delegate, Class type) {
	return aspectSupport.attachAspects(delegate, type);
    }




}
