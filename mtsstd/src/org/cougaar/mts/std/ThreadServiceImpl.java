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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;

/**
 * This class creates and registers the ServiceProvider for the
 * ThreadService and ThreadControlService.  The provider class itself,
 * as well as the service proxy classes, are private and are not
 * directly accessible from anywhere else.
 */
class ThreadServiceImpl
{
    private static final String InitialPoolSizeProp =
	"org.cougaar.thread.poolsize.initial";
    private static final int InitialPoolSizeDefault = 32;
    private static final String MaxPoolSizeProp =
	"org.cougaar.thread.poolsize.max";
    private static final int MaxPoolSizeDefault = 64;
    private static final String MaxRunningCountProp =
	"org.cougaar.thread.running.max";
    private static final int MaxRunningCountDefault = Integer.MAX_VALUE;


    ThreadServiceImpl(ServiceBroker sb) {
	LoggingService loggingService = (LoggingService)
	    sb.getService(this, LoggingService.class, null);
	ThreadServiceProvider provider = 
	    new ThreadServiceProvider(loggingService);
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
	private LoggingService loggingService;

	ThreadServiceProvider(LoggingService loggingService) {
	    proxies = new HashMap();
	    this.loggingService = loggingService;
	}

	private synchronized Object findOrMakeProxyForClient (Object client) {
	    Object p = proxies.get(client);
	    if (p == null) {
		p =  new ThreadServiceProxy((ThreadServiceClient)client,
					    loggingService);
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
		    loggingService.error(requestor + 
					 " is not a ThreadServiceClient");
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

	public void setQueueComparator(ThreadService svc, 
				       Comparator comparator)
	{
	    if (svc != null && svc instanceof ThreadServiceProxy) {
		ThreadServiceProxy proxy = (ThreadServiceProxy) svc;
		ControllablePool pool = proxy.threadPool;
		pool.setQueueComparator(comparator);
	    }
	}


	public void setMaxRunningThreadCount(ThreadService svc, int count) {
	    if (svc != null && svc instanceof ThreadServiceProxy) {
		ThreadServiceProxy proxy = (ThreadServiceProxy) svc;
		proxy.threadPool.setMaxRunningThreadCount(count);
	    }
	}

	public int maxRunningThreadCount(ThreadService svc) {
	    if (svc != null && svc instanceof ThreadServiceProxy) {
		ThreadServiceProxy proxy = (ThreadServiceProxy) svc;
		return proxy.threadPool.maxRunningThreadCount();
	    } else {
		return -1;
	    }
	}

	public int runningThreadCount(ThreadService svc) {
	    if (svc != null && svc instanceof ThreadServiceProxy) {
		ThreadServiceProxy proxy = (ThreadServiceProxy) svc;
		return proxy.threadPool.runningThreadCount();
	    } else {
		return -1;
	    }
	}


	public int activeThreadCount(ThreadService svc) {
	    if (svc != null && svc instanceof ThreadServiceProxy) {
		ThreadServiceProxy proxy = (ThreadServiceProxy) svc;
		return proxy.threadPool.activeThreadCount();
	    } else {
		return -1;
	    }
	}


	public int pendingThreadCount(ThreadService svc) {
	    if (svc != null && svc instanceof ThreadServiceProxy) {
		ThreadServiceProxy proxy = (ThreadServiceProxy) svc;
		return proxy.threadPool.pendingThreadCount();
	    } else {
		return -1;
	    }
	}


    }


    /**
     * The proxy implementation of ThreadListenerService.
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


    private static Comparator timeComparator =
	new Comparator() {
	    public boolean equals(Object x) {
		return x == this;
	    }

	    public int compare (Object x, Object y) {
		ControllableThread t1 =
		    (ControllableThread) x;
		ControllableThread t2 =
		    (ControllableThread) y;
		if (t1.timestamp < t2.timestamp)
		    return -1;
		else if (t1.timestamp > t2.timestamp)
		    return 1;
		else
		    return 0;
	    }
	};

    /**
     * A special kind of ReusableThreadPool which makes
     * ControllableThreads.
     */
    private static class ControllablePool extends ReusableThreadPool {
	private ThreadServiceProxy proxy;
	private DynamicSortedQueue pendingThreads;
	private int maxRunningThreads;
	private int runningThreadCount = 0;

	public ControllablePool(ThreadServiceProxy proxy,
				ThreadGroup group, 
				int init, int max) 
	{
	    super(group, init, max);
	    this.proxy = proxy;
	    pendingThreads = new DynamicSortedQueue(timeComparator);
	    maxRunningThreads = 
		PropertyParser.getInt(MaxRunningCountProp, 
				      MaxRunningCountDefault);
	}

	private synchronized void setQueueComparator(Comparator comparator)
	{
	    pendingThreads.setComparator(comparator);
	}

	protected ReusableThread constructReusableThread() {
	    return  new ControllableThread(this);
	}


	private int maxRunningThreadCount() {
	    return maxRunningThreads;
	}

	private synchronized void setMaxRunningThreadCount(int count) {
	    maxRunningThreads = count;
	    
	    // Maybe we can run some pending threads
	    while (canStartThread() && !pendingThreads.isEmpty()) {
		runNextThread();
	    }
	}

	private synchronized int pendingThreadCount() {
	    return pendingThreads.size();
	}

	private synchronized int runningThreadCount() {
	    return runningThreadCount;
	}


	private synchronized int activeThreadCount() {
	    return runningThreadCount + pendingThreads.size();
	}

	private boolean canStartThread() {
	    return runningThreadCount < maxRunningThreads;
	}


	private void runNextThread() {
	    ((ControllableThread)pendingThreads.next()).start();
	}


	private void removeRunningThread(ControllableThread thread) {
	    synchronized (this) {
		--runningThreadCount; 
		if (!pendingThreads.isEmpty()) runNextThread();
	    }
	    proxy.notifyEnd(thread);
	}

	private synchronized void yieldRunningThread(ControllableThread thread)
	{
	    --runningThreadCount; 
	    if (!pendingThreads.isEmpty()) runNextThread();
	}

	private synchronized boolean resumeYieldedThread(ControllableThread thread)
	{
	    if (canStartThread()) {
		++runningThreadCount;
		return true;
	    } else {
		// couldn'resume - place the thread back on the queue
		addPendingThread(thread);
		return false;
	    }
	}

	private void startRunningThread(ControllableThread thread) {
	    synchronized (this) {
		++runningThreadCount; 
	    }
	    proxy.notifyStart(thread);
	}
 
	private void addPendingThread(ControllableThread thread) 
	{
	    thread.timestamp = System.currentTimeMillis();
	    proxy.notifyPending(thread);
	    pendingThreads.add(thread);
	    if (Debug.isDebugEnabled(proxy.loggingService, Debug.THREAD))
		proxy.loggingService.debug("Added to queue " + pendingThreads);
	}

	private synchronized void startOrQueue(ControllableThread thread) {
	    if (canStartThread()) {
		thread.thread_start();
	    } else {
		addPendingThread(thread);
	    }
	}

    }



    /**
     * A special kind of ReusableThread which will notify listeners at
     * the beginning and end of the internal run method of the thread.
     */
    private static class ControllableThread
	extends ReusableThread
    {
	private ControllablePool pool;
	private long timestamp;
	private Object consumer;
	private boolean suspended;
	private Object suspendLock;

	ControllableThread(ControllablePool pool) 
	{
	    super(pool);
	    this.pool = pool;
	    this.suspendLock = new Object();
	}



	protected void claim() {
	    // thread has started or restarted
	    super.claim();
	    pool.startRunningThread(this);
	}


	private void suspend(long millis) {
	    pool.yieldRunningThread(this);
	    try { sleep(millis); }
	    catch (InterruptedException ex) {}
	    attemptResume();
	}


	// The argument is only here to avoid overriding yield(),
	// 
	// The semantics is not the same as Thread.yield, since a
	// lower priority thread may get control.  Fix this later.
	private void yield(Object ignore) {
	    pool.yieldRunningThread(this);
	    attemptResume();
	}

	// Must be called from a block that's synchronized on lock.
	private void wait(Object lock, long millis) {
	    pool.yieldRunningThread(this);
	    try { lock.wait(millis); }
	    catch (InterruptedException ex) {}
	    attemptResume();
	}

	// Must be called from a block that's synchronized on lock.
	private void wait(Object lock) {
	    pool.yieldRunningThread(this);
	    try { lock.wait(); }
	    catch (InterruptedException ex) {}
	    attemptResume();
	}


	private void attemptResume() {
	    suspended = true;
	    synchronized (suspendLock) {
		suspended = !pool.resumeYieldedThread(this);
		if (suspended) {
		    // Couldn't be resumed - requeued instead
		    while (true) {
			try {
			    // When the thread is pulled off the
			    // queue, a notify will wake up this wait.
			    suspendLock.wait();
			    break;
			} catch (InterruptedException ex) {
			}
		    }
		    suspended = false;
		} 
	    }
	}


	protected void reclaim() {
	    // thread is done
	    pool.removeRunningThread(this);
	    super.reclaim();
	}

	private void thread_start() {
	    super.start();
	}

	public void start() {
	    if (suspended) {
		synchronized (suspendLock) {
		    suspendLock.notify();
		    return;
		}
	    } else {
		pool.startOrQueue(this);
	    }
	}

    }


    /**
     * The proxy implementation of Thread Service.
     */
    private static class ThreadServiceProxy implements ThreadService {
	private ControllablePool threadPool;
	private ArrayList listeners;
	private ThreadGroup group;
	private TimerRunnable timer;
	private LoggingService loggingService;

	private ThreadServiceProxy(ThreadServiceClient client,
				    LoggingService loggingService) 
	{
	    this.loggingService = loggingService;
	    listeners = new ArrayList();
	    group = client.getGroup();
	    timer = new TimerRunnable(this, loggingService);

	    int initialSize = PropertyParser.getInt(InitialPoolSizeProp, 
						    InitialPoolSizeDefault);
	    int maxSize = PropertyParser.getInt(MaxPoolSizeProp, 
						MaxPoolSizeDefault);

	    if (group != null) 
		group = new ThreadGroup(client.toString() + "_ThreadGroup");
	    threadPool = new ControllablePool(this,
					      group, 
					      initialSize,
					      maxSize);

	    // Use a special Thread for the timer
	    Thread thread = new Thread(group, timer, "Timer");
	    thread.setDaemon(true);
	    thread.start();
	}


	private Thread consumeThread(Thread thread,  Object consumer) {
	    ((ControllableThread) thread).consumer = consumer;
	    return thread;
	}


	synchronized void notifyPending(ControllableThread thread) {
	    Object consumer = thread.consumer;
 	    Iterator itr = listeners.iterator();
	    while (itr.hasNext()) {
		ThreadListener listener = (ThreadListener) itr.next();
		listener.threadPending(thread, consumer);
	    }
	}

	synchronized void notifyStart(ControllableThread thread) {
	    Object consumer = thread.consumer;
 	    Iterator itr = listeners.iterator();
	    while (itr.hasNext()) {
		ThreadListener listener = (ThreadListener) itr.next();
		listener.threadStarted(thread, consumer);
	    }
	}

	synchronized void notifyEnd(ControllableThread thread) {
	    Object consumer = thread.consumer;
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
	    return consumeThread(threadPool.getThread(runnable),  consumer);
	}

	public Thread getThread(Object consumer, 
				Runnable runnable, 
				String name) 
	{
	    return consumeThread(threadPool.getThread(runnable, name), 
				 consumer);
	}


	public TimerTask getTimerTask(Object consumer, Runnable runnable) {
	    return timer.getTimerTask(consumer, runnable);
	}

	public void schedule(TimerTask task, long delay) {
	    timer.schedule(task, delay);
	}


	public void schedule(TimerTask task, long delay, long interval) {
	    timer.schedule(task, delay, interval);
	}

	public void scheduleAtFixedRate(TimerTask task, 
					long delay, 
					long interval)
	{
	    timer.scheduleAtFixedRate(task, delay, interval);
	}


	public void suspendCurrentThread(long millis) {
	    Thread thread = Thread.currentThread();
	    if (thread instanceof ControllableThread) {
		((ControllableThread) thread).suspend(millis);
	    }
	}


	public void yieldCurrentThread() {
	    Thread thread = Thread.currentThread();
	    if (thread instanceof ControllableThread) {
		((ControllableThread) thread).yield(null);
	    }
	}
	public void blockCurrentThread(Object lock, long millis) {
	    Thread thread = Thread.currentThread();
	    if (thread instanceof ControllableThread) {
		((ControllableThread) thread).wait(lock, millis);
	    }
	}
	public void blockCurrentThread(Object lock) {
	    Thread thread = Thread.currentThread();
	    if (thread instanceof ControllableThread) {
		((ControllableThread) thread).wait(lock);
	    }
	}


    }

}
