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

import org.cougaar.core.service.*;

import org.cougaar.core.node.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.StringTokenizer;

import org.cougaar.core.component.Container;

/**
 * This is utility class which supports loading aspects
 * @property org.cougaar.message.transport.aspects A comma-seperated
 * list of the MTS aspect classes to be instantiated.
 */
public final class AspectSupportImpl 
    implements AspectSupport, DebugFlags
{
    private final static String ASPECTS_PROPERTY = 
	"org.cougaar.message.transport.aspects";

    private static AspectSupportImpl instance;

    public static AspectSupport instance() {
	return instance;
    }

    public static AspectSupport makeInstance(Container container) {
	instance = new AspectSupportImpl(container);
	return instance;
    }

    private ArrayList aspects;
    private HashMap aspects_table;
    private Container container;

    // Should this be a singleton?
    private AspectSupportImpl(Container container) {
	aspects = new ArrayList();
	aspects_table = new HashMap();
	this.container = container;
    }
    
 
    public void readAspects() {
	String classes = System.getProperty(ASPECTS_PROPERTY);

	if (classes == null) return;

	StringTokenizer tokenizer = new StringTokenizer(classes, ",");
	while (tokenizer.hasMoreElements()) {
	    String classname = tokenizer.nextToken();
	    try {
		Class aspectClass = Class.forName(classname);
		MessageTransportAspect aspect = 
		    (MessageTransportAspect) aspectClass.newInstance();
		addAspect(aspect);
	    }
	    catch (Exception ex) {
		ex.printStackTrace();
		// System.err.println(ex);
	    }
	}
    }

    // Note that we allow multiple instances of a given aspect class
    // but that only the most recent instance of any given class can
    // be found by name.
    public synchronized MessageTransportAspect findAspect(String classname) {
	return (MessageTransportAspect) aspects_table.get(classname);
    }


    public void addAspect(MessageTransportAspect aspect)
    {
	String classname = aspect.getClass().getName();
	synchronized (this) {
	    aspects.add(aspect);
	    aspects_table.put(classname, aspect);
	}
	container.add(aspect);
	if (Debug.debug(ASPECTS))
	    System.out.println("******* added aspect " + aspect);
    }


    /**
     * Loops through the aspects, allowing each one to attach an
     * aspect delegate in a cascaded series.  If any aspects attach a
     * delegate, the final aspect delegate is returned.  If no aspects
     * attach a delegate, the original object, as created by the
     * factory, is returned.  */
    public Object attachAspects (Object delegate, 
					      Class type)
    {
	Iterator itr = aspects.iterator();
	while (itr.hasNext()) {
	    MessageTransportAspect aspect = 
		(MessageTransportAspect) itr.next();

	    Object candidate = aspect.getDelegate(delegate, type);
	    if (candidate != null) {
		delegate = candidate;
		if (Debug.debug(ASPECTS))
		    System.out.println("======> " + delegate);
	    }
	}

	ListIterator litr = aspects.listIterator(aspects.size());
	while (litr.hasPrevious()) {
	    MessageTransportAspect aspect = 
		(MessageTransportAspect) litr.previous();

	    Object candidate = aspect.getReverseDelegate(delegate, type);
	    if (candidate != null) {
		delegate = candidate;
		if (Debug.debug(ASPECTS))
		    System.out.println("(r)======> " + delegate);
	    }
	}
	
	return delegate;
    }


}
 


 
