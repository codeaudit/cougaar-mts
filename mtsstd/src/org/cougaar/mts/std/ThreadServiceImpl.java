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

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.util.PropertyParser;
import org.cougaar.util.ReusableThreadPool;
import org.cougaar.util.ReusableThread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This class creates and registers the ServiceProvider for the
 * ThreadService and ThreadControlService.  The provider class itself,
 * as well as the service proxy classes, are private and are not
 * directly accessible from anywhere else.
 */
class ThreadServiceImpl
{
    ThreadServiceImpl(ServiceBroker sb) {
	ThreadServiceProvider provider = new ThreadServiceProvider();
	sb.addService(ThreadService.class, provider);
	sb.addService(ThreadControlService.class, provider);
	sb.addService(ThreadListenerService.class, provider);
    }



    /**
     * The ServiceProvider for ThreadService and ThreadControlService.
     * The former is only available to ThreadServiceClients, which
     * should only be SharedThreadServiceBrokers.  This ensure proper
     * thread grouping and control, but there's probably no way to
     * enforce this. We're still in the process of determining who
     * should have access to the ThreadControlService.
     */
    private static class ThreadServiceProvider implements ServiceProvider {

	private HashMap proxies;
	private LoggingService log;

	ThreadServiceProvider() {
	    proxies = new HashMap();
	}

	private synchronized Object getProxyForClient(Object client) {
	    return proxies.get(client);
	}

	private synchronized Object findOrMakeProxyForClient (Object client) {
	    Object p = proxies.get(client);
	    if (p == null) {
		p =  new ThreadServiceProxy((ThreadServiceClient)client);
		proxies.put(client, p);
	    }
	    return p;
	}

	private synchronized void removeProxyForClient(Object client, 
						       Object svc) 
	{
	    if (proxies.get(client) == svc) proxies.remove(client);
	}



	public Object getService(ServiceBroker sb, 
				 Object requestor, 
				 Class serviceClass) 
	{
	    if (serviceClass == ThreadService.class) {
		if (requestor instanceof ThreadServiceClient) {
		    return findOrMakeProxyForClient(requestor);
		} else {
		    if (log == null) {
			log = (LoggingService)
			    sb.getService(this, LoggingService.class, null);
		    }
		    log.error(requestor + " is not a ThreadServiceClient");
		    return null;
		}
	    } else if (serviceClass == ThreadControlService.class) {
		// Later this will be tightly restricted
		return new ThreadController();
	    } else if (serviceClass == ThreadListenerService.class) {
		// Later this will be tightly restricted
		return new ThreadListenerImpl();
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


    /**
     * The proxy implementation of ThreadControlService.
     */
    private static class ThreadController implements ThreadControlService {

	ThreadController() {
	}

	public void setClientPriority(ThreadService svc, int priority) {
	    if (svc != null && svc instanceof ThreadServiceProxy) {
		ThreadServiceProxy proxy = (ThreadServiceProxy) svc;
		// Now set the priority of all the threads in proxy's
		// ThreadGroup.
	    }
	}

	public int activeThreadCount(ThreadService svc) {
	    if (svc != null && svc instanceof ThreadServiceProxy) {
		ThreadServiceProxy proxy = (ThreadServiceProxy) svc;
		return proxy.activeThreadCount();
	    } else {
		return -1;
	    }
	}


    }


    /**
     * The proxy implementation of ThreadControlService.
     */
    private static class ThreadListenerImpl implements ThreadListenerService {


	ThreadListenerImpl() {
	}

	public void addListener(ThreadService svc, ThreadListener listener) {
	    if (svc != null && svc instanceof ThreadServiceProxy) {
		ThreadServiceProxy proxy = (ThreadServiceProxy) svc;
		proxy.addListener(listener);
	    }
	}

	public void removeListener(ThreadService svc,ThreadListener listener) {
	    if (svc != null && svc instanceof ThreadServiceProxy) {
		ThreadServiceProxy proxy = (ThreadServiceProxy) svc;
		proxy.removeListener(listener);
	    } 
	}


    }




    /**
     * A special kind of ReusableThreadPool which makes
     * ControllableThreads.
     */
    private static class ControllablePool extends ReusableThreadPool {
	private ThreadServiceProxy proxy;
	public ControllablePool(ThreadServiceProxy proxy, int init, int max) {
	    super(init,max);
	    this.proxy = proxy;
	}

	public ControllablePool(ThreadServiceProxy proxy,
				ThreadGroup group, 
				int init, int max) 
	{
	    super(group, init, max);
	    this.proxy = proxy;
	}

	protected ReusableThread constructReusableThread() {
	    return new ControllableThread(this);
	}
    }


    /**
     * A special kind of ReusableThread which will notify listeners at
     * the beginning and end of the internal run method of the thread.
     */
    private static class ControllableThread extends ReusableThread {
	private ControllablePool pool;

	ControllableThread(ControllablePool pool) 
	{
	    super(pool);
	    this.pool = pool;
	}


	protected void claim() {
	    // thread has started or restarted
	    pool.proxy.notifyStart(this);
	    super.claim();
	}

	protected void reclaim() {
	    // thread is done
	    super.reclaim();
	    pool.proxy.notifyEnd(this);
	}

    }


    /**
     * The proxy implementation of Thread Service.
     */
    private static class ThreadServiceProxy implements ThreadService {
	private static final String InitialPoolSizeProp =
	    "org.cougaar.ReusableThread.initialPoolSize";
	private static final int InitialPoolSizeDefault = 32;
	private static final String MaxPoolSizeProp =
	    "org.cougaar.ReusableThread.maximumPoolSize";
	private static final int MaxPoolSizeDefault = 64;


	private ControllablePool threadPool;
	private HashMap consumers;
	private ArrayList listeners;
	private ThreadGroup group;
	private int activeThreadCount;

	private ThreadServiceProxy(ThreadServiceClient client) {
	    listeners = new ArrayList();
	    consumers = new HashMap();
	    activeThreadCount = 0;
	    group = client.getGroup();

	    int initialSize = PropertyParser.getInt(InitialPoolSizeProp, 
						    InitialPoolSizeDefault);
	    int maxSize = PropertyParser.getInt(MaxPoolSizeProp, 
						MaxPoolSizeDefault);

	    if (group != null)
		threadPool = new ControllablePool(this,
						  group, 
						  initialSize,
						  maxSize);
	    else
		threadPool = new ControllablePool(this,
						  initialSize, 
						  maxSize);
	}

	private int activeThreadCount() {
	    return activeThreadCount;
	}

	private Thread consumeThread(Thread thread, Object consumer) {
	    consumers.put(thread, consumer);
	    return thread;
	}

	private Object threadConsumer(Thread thread) {
	    return consumers.get(thread);
	}

	synchronized void notifyStart(Thread thread) {
	    ++activeThreadCount;
	    Object consumer = threadConsumer(thread);
 	    Iterator itr = listeners.iterator();
	    while (itr.hasNext()) {
		ThreadListener listener = (ThreadListener) itr.next();
		listener.threadStarted(thread, consumer);
	    }
	}

	synchronized void notifyEnd(Thread thread) {
	    --activeThreadCount;
	    Object consumer = threadConsumer(thread);
  	    Iterator itr = listeners.iterator();
	    while (itr.hasNext()) {
		ThreadListener listener = (ThreadListener) itr.next();
		listener.threadStopped(thread, consumer);
	    }
	}




	synchronized void addListener(ThreadListener listener) {
	    listeners.add(listener);
	}


	synchronized void removeListener(ThreadListener listener) {
	    listeners.remove(listener);
	}


	public Thread getThread(Object consumer, Runnable runnable) {
	    return consumeThread(threadPool.getThread(runnable), consumer);
	}

	public Thread getThread(Object consumer, 
				Runnable runnable, 
				String name) 
	{
	    return consumeThread(threadPool.getThread(runnable, name), 
				 consumer);
	}
    }

}
