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


import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.StringTokenizer;

import org.cougaar.core.component.Container;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;

/**
 * This is utility class which supports loading aspects
 * @property org.cougaar.message.transport.aspects A comma-seperated
 * list of the MTS aspect classes to be instantiated.
 */
final class AspectSupportImpl implements ServiceProvider
{
    private final static String ASPECTS_PROPERTY = 
	"org.cougaar.message.transport.aspects";

    private static AspectSupport service;

    AspectSupportImpl(Container container, DebugService debugService) {
	service = new ServiceImpl(container, debugService);
    }


    // The SocketFactory needs to attach aspect delegates for Socket.
    // But the client side of the connection can't get at a
    // ServiceBroker since the factory was serlialized and sent over
    // by the server side of the connection.  To get around this
    // problem we need to open a hole into the aspectSupport.  Give it
    // package-access in a feeble attempt at security.
    static Socket attachRMISocketAspects(Socket rmi_socket) {
	return (Socket) service.attachAspects(rmi_socket, Socket.class);
    }



    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	if (serviceClass == AspectSupport.class) {
	    return service;
	} else {
	    return null;
	}
    }

    public void releaseService(ServiceBroker sb, 
			       Object requestor, 
			       Class serviceClass, 
			       Object service)
    {
    }





    private class ServiceImpl implements AspectSupport, DebugFlags
    {

	private ArrayList aspects;
	private HashMap aspects_table;
	private Container container;
	private DebugService debugService;


	private ServiceImpl(Container container, DebugService debugService) {
	    aspects = new ArrayList();
	    aspects_table = new HashMap();
	    this.container = container;
	    this.debugService = debugService;
	}
    
 
	public void readAspects() {
	    String classes = System.getProperty(ASPECTS_PROPERTY);

	    if (classes == null) return;

	    StringTokenizer tokenizer = new StringTokenizer(classes, ",");
	    while (tokenizer.hasMoreElements()) {
		String classname = tokenizer.nextToken();
		MessageTransportAspect aspect = findAspect(classname);
		if (aspect != null && debugService.isErrorEnabled()) {
		    debugService.error("Ignoring duplicate aspect "+
					      classname);
		    continue;
		}
		try {
		    Class aspectClass = Class.forName(classname);
		    aspect =(MessageTransportAspect) aspectClass.newInstance();
		    addAspect(aspect);
		}
		catch (Exception ex) {
		    debugService.error(null, ex);
		}
	    }
	}

	// Note that we allow multiple instances of a given aspect class
	// but that only the most recent instance of any given class can
	// be found by name.
	public synchronized MessageTransportAspect findAspect(String classname)
	{
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
	    if (debugService.isDebugEnabled(ASPECTS))
		debugService.debug("Added aspect " + aspect);
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
		    if (debugService.isDebugEnabled(ASPECTS))
			debugService.debug("attached " + delegate);
		}
	    }

	    ListIterator litr = aspects.listIterator(aspects.size());
	    while (litr.hasPrevious()) {
		MessageTransportAspect aspect = 
		    (MessageTransportAspect) litr.previous();

		Object candidate = aspect.getReverseDelegate(delegate, type);
		if (candidate != null) {
		    delegate = candidate;
		    if (debugService.isDebugEnabled(ASPECTS))
			debugService.debug("reverse attached " 
						  + delegate);
		}
	    }
	
	    return delegate;
	}

    }
}
 


 
