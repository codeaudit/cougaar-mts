package org.cougaar.core.mts;

import org.cougaar.core.service.LoggingService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TimerTask;

public class TimerRunnable implements Runnable
{

    private class TaskWrapper extends TimerTask {
	Runnable task;
	long period;
	long nextrun;
	boolean fixedRate;
	boolean runnable;
	boolean cancelled;
	Object consumer;
	String name;

	TaskWrapper(Object consumer, Runnable task) {
	    this(consumer, task, "TimerTask");
	}

	TaskWrapper(Object consumer, Runnable task, String name) {
	    this.task = task;
	    this.consumer = consumer;
	    this.name = name;
	}


	private Thread getThread() {
	    return threadService.getThread(consumer, this, name);
	}


	private void setIntervals(boolean fixedRate, long delay, long period) {
	    this.period = period;
	    this.fixedRate = fixedRate;
	    nextrun = System.currentTimeMillis()+delay;
	}

	public void run() {
	    if (fixedRate && (period > 0)) {
		nextrun = System.currentTimeMillis()+period;
	    }
	    if (Debug.isDebugEnabled(loggingService, Debug.THREAD))
		loggingService.debug("Running TimerWrapper at " +
				     System.currentTimeMillis());
	    task.run();
	    if (period == -1) {
		cancelled = true;
	    } else  if (!fixedRate) {
		nextrun = System.currentTimeMillis()+period;
	    }

	    // Is this really necessary?
	    taskCompleted(this);
	}

	public boolean cancel() {
	    cancelled = true;
	    return true;
	}
    }


    private ArrayList tasks;
    private Object lock;
    private ThreadService threadService;
    private LoggingService loggingService;

    public TimerRunnable(ThreadService threadService,
			 LoggingService loggingService)
    {
	tasks = new ArrayList();
	lock = new Object();
	this.loggingService = loggingService;
	this.threadService = threadService;
    }


    public TimerTask getTimerTask(Object consumer, Runnable runnable) {
	return new TaskWrapper(consumer, runnable);
    }


    public TimerTask getTimerTask(Object consumer, 
				  Runnable runnable,
				  String name) 
    {
	return new TaskWrapper(consumer, runnable, name);
    }


    private void taskCompleted(TaskWrapper task) {
	synchronized (lock) {
	    // If the completed task can ever run again, its interval
	    // must be > 0. In that case it's ready to run now.
	    task.runnable = task.period > 0;
	    lock.notify();
	}
    }

    
    private void scheduleTask(TimerTask task,
			      long delay,
			      long interval,
			      boolean fixedRate)
    {	
	// task must be a TaskWrapper
	TaskWrapper wrapper = (TaskWrapper) task;
	wrapper.setIntervals(fixedRate, delay, interval);
	wrapper.runnable = true;
	synchronized (lock) {
	    tasks.add(wrapper);
	    lock.notify();
	}
    }


    public void schedule(TimerTask task, long delay) { 
	scheduleTask(task, delay, -1, false);
    }

    public void schedule(TimerTask task, long delay, long period) {
	scheduleTask(task, delay, period, false);
    }

    public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
	scheduleTask(task, delay, period, true);
    }

    private long findNextRuntime() {
	long next = -1;
	TaskWrapper first = null;
	Iterator itr = tasks.iterator();
	while (itr.hasNext()) {
	    TaskWrapper wrapper = (TaskWrapper) itr.next();
	    if (!wrapper.runnable || wrapper.cancelled) {
		// Don't consider the next runtime if the flag
		// isn't set, since the value will be out of date.
		continue;
	    } else if (first == null) {
		first = wrapper;
		next = wrapper.nextrun;
	    } else  if (wrapper.nextrun < next) {
		first = wrapper;
		next = wrapper.nextrun;
	    }
	}
	return next;
    }

    private void runReadyTasks(long time) {
	Iterator itr = tasks.iterator();
	while (itr.hasNext()) {
	    TaskWrapper wrapper = (TaskWrapper) itr.next();
	    if (wrapper.cancelled) continue;
	    if (wrapper.runnable && wrapper.nextrun <= time) {
		wrapper.runnable = false;
		Thread thread = wrapper.getThread();
		thread.start();
		// wrapper.run();
	    }
	}
    }


    public void run() {
	while (true) {
	    synchronized (lock) {
		long now = System.currentTimeMillis();
		runReadyTasks(now);
		long next = findNextRuntime();
		long sleep = next == -1 ? -1 : Math.max(next-now, 0);
		if (sleep == 0) continue; // no need to wait()
		while (true) {
		    try { 
			if (sleep != -1) 
			    lock.wait(sleep); 
			else
			    lock.wait();
			break;
		    } 
		    catch (InterruptedException ex) { }
		}
	    }
		
	}
    }

}

