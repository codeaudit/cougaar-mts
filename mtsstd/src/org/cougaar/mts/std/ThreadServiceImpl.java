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

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.util.PropertyParser;
import org.cougaar.util.ReusableThreadPool;
import org.cougaar.util.ReusableThread;

import java.util.HashMap;

class ThreadServiceImpl
{
    private HashMap proxies;

    ThreadServiceImpl(ServiceBroker sb) {
	proxies = new HashMap();
	ServiceProvider sp = new ThreadServiceProvider();
	sb.addService(ThreadService.class, sp);
	sb.addService(ThreadControlService.class, sp);
    }

    private synchronized Object getProxyForClient(Object client) {
	return proxies.get(client);
    }

    private synchronized Object findOrMakeProxyForClient (ThreadServiceClient client) 
    {
	Object p = proxies.get(client);
	if (p == null) {
	    p =  new ThreadPoolServer(client);
	    proxies.put(client, p);
	}
	return p;
    }

    private synchronized void removeProxyForClient(Object client, Object svc) {
	if (proxies.get(client) == svc) proxies.remove(client);
    }



    private class ThreadServiceProvider implements ServiceProvider {

    
	public Object getService(ServiceBroker sb, 
				 Object requestor, 
				 Class serviceClass) 
	{
	    if (serviceClass == ThreadService.class) {
		if (requestor instanceof ThreadServiceClient) {
		    return findOrMakeProxyForClient((ThreadServiceClient) requestor);
		} else {
		    System.err.println(requestor + " is not a ThreadServiceClient");
		    return null;
		}
	    } else if (serviceClass == ThreadControlService.class) {
		// Later this will be tightly restricted
		return new ThreadController();
	    } else {
		return null;
	    }
	}

	public void releaseService(ServiceBroker sb, 
				   Object requestor, 
				   Class serviceClass, 
				   Object service)
	{
	    if (serviceClass == ThreadService.class) {
		removeProxyForClient(requestor, service);
	    }
	}

 
    }

    private class ThreadController implements ThreadControlService {

	public void setClientPriority(Object client, int priority) {
	    Object raw = getProxyForClient(client);
	    if (raw != null && raw instanceof ThreadPoolServer) {
		ThreadPoolServer svc = (ThreadPoolServer) raw;
		// etc
	    }
	}

    }


    private class ThreadPoolServer implements ThreadService {
	private final ReusableThreadPool threadPool;

	private ThreadPoolServer(ThreadServiceClient client) {
	    int initialSize = 
		PropertyParser.getInt("org.cougaar.ReusableThread.initialPoolSize", 
				      32);
	    int maxSize = 
		PropertyParser.getInt("org.cougaar.ReusableThread.maximumPoolSize",
				      64);

	    ThreadGroup group = client.getGroup();
	    if (group != null)
		threadPool = new ReusableThreadPool(group, initialSize, maxSize);
	    else
		threadPool = new ReusableThreadPool(initialSize, maxSize);
	}

	public Thread getThread() {
	    return threadPool.getThread();
	}

	public Thread getThread(String name) {
	    return threadPool.getThread(name);
	}

	public Thread getThread(Runnable runnable) {
	    return threadPool.getThread(runnable);
	}

	public Thread getThread(Runnable runnable, String name) {
	    return threadPool.getThread(runnable, name);
	}
    }

}
