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
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.StringTokenizer;

import org.cougaar.core.component.Container;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.LoggingService;

import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.base.MessageTransportAspect;

/**
 * This {@link ServiceProvider} provides the {@link AspectSupport}
 * service, which is implemented by an inner class.
 */
//final class AspectSupportImpl implements ServiceProvider
public class AspectSupportImpl implements ServiceProvider
{
    private static AspectSupport service;

    public AspectSupportImpl(Container container, LoggingService loggingService) {
	service = new ServiceImpl(container, loggingService);
    }


    // The SocketFactory needs to attach aspect delegates for Socket.
    // But the client side of the connection can't get at a
    // ServiceBroker since the factory was serlialized and sent over
    // by the server side of the connection.  To get around this
    // problem we need to open a hole into the aspectSupport.  Give it
    // package-access in a feeble attempt at security.
    public static Socket attachRMISocketAspects(Socket rmi_socket) {
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





    private class ServiceImpl 
	implements AspectSupport
    {

	private ArrayList aspects;
	private HashMap aspects_table;
	private Container container;
	private LoggingService loggingService;


	private ServiceImpl(Container container,
			    LoggingService loggingService) 
	{
	    aspects = new ArrayList();
	    aspects_table = new HashMap();
	    this.container = container;
	    this.loggingService = loggingService;
	}
    
 
	private void loadAspectClass(String classname) {
	    MessageTransportAspect aspect = findAspect(classname);
	    if (aspect != null && loggingService.isErrorEnabled()) {
		loggingService.error("Ignoring duplicate aspect "+
				     classname);
		return;
	    }
	    try {
		Class aspectClass = Class.forName(classname);
		aspect =(MessageTransportAspect) aspectClass.newInstance();
		container.add(aspect);
	    }
	    catch (Exception ex) {
		loggingService.error(null, ex);
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
	    if (loggingService.isDebugEnabled())
		loggingService.debug("Added aspect " + aspect);
	}

	public void addAspect(StandardAspect aspect)
	{
	    String classname = aspect.getClass().getName();
	    synchronized (this) {
		aspects.add(aspect);
		aspects_table.put(classname, aspect);
	    }
	    if (loggingService.isDebugEnabled())
		loggingService.debug("Added aspect " + aspect);
	}

	/**
	 * Loops through the aspects, allowing each one to attach an
	 * aspect delegate in a cascaded series.  If any aspects attach a
	 * delegate, the final aspect delegate is returned.  If no aspects
	 * attach a delegate, the original object, as created by the
	 * factory, is returned.  */
	public Object attachAspects (Object delegate, 
				     Class type,
				     ArrayList candidateClassNames)
	{
	    if (candidateClassNames == null) return delegate;
	    ArrayList candidates = new ArrayList(candidateClassNames.size());
	    Iterator itr = candidateClassNames.iterator();
	    while (itr.hasNext()) {
		String candidateClassName = (String) itr.next();
		Object candidate = findAspect(candidateClassName);
		if (candidate != null) candidates.add(candidate);
	    }
	    return attach(delegate, type, candidates);
	}


	public Object attachAspects (Object delegate, 
				     Class type)
	{
	    return attach(delegate, type, aspects);
	}

	private Object attach (Object delegate, 
			       Class type,
			       ArrayList candidates)
	{
	    Iterator itr = candidates.iterator();
	    while (itr.hasNext()) {
		MessageTransportAspect aspect = 
		    (MessageTransportAspect) itr.next();

		Object candidate = aspect.getDelegate(delegate, type);
		if (candidate != null) {
		    delegate = candidate;
		    if (loggingService.isDebugEnabled())
			loggingService.debug("attached " + delegate);
		}
	    }

	    ListIterator litr = candidates.listIterator(candidates.size());
	    while (litr.hasPrevious()) {
		MessageTransportAspect aspect = 
		    (MessageTransportAspect) litr.previous();

		Object candidate = aspect.getReverseDelegate(delegate, type);
		if (candidate != null) {
		    delegate = candidate;
		    if (loggingService.isDebugEnabled())
			loggingService.debug("reverse attached " 
						  + delegate);
		}
	    }
	
	    return delegate;
	}

    }
}
 


 
