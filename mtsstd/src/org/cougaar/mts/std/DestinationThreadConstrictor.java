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

import java.net.URI;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.ThreadControlService;
import org.cougaar.core.service.ThreadListenerService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.thread.ThreadListener;
import org.cougaar.mts.base.BoundComponent;
import org.cougaar.mts.base.DestinationQueue;

public class DestinationThreadConstrictor
    extends BoundComponent
    implements ThreadListener
{

    
    private class Constrictor implements Comparator
    {

	int timestamp_compare (Object o1, Object o2) {
	    long t1 = getTimestamp(o1);
	    long t2 = getTimestamp(o2);
	    if (t1 < t2)
		return -1;
	    else if (t1 > t2)
		return 1;
	    else
		return 0;
	}

	public int compare(Object o1, Object o2)
	{
	    int count1 = getSchedulableNodeCount((Schedulable) o1);
	    int count2 = getSchedulableNodeCount((Schedulable) o2);

		
	    if (count1 == count2) {
		// Since they're equal, only the timestamp is relevant.
		return timestamp_compare(o1, o2);
	    }

	    if (count1 < maxPerNode && count2 < maxPerNode) {
		// Both are under the limit, so use the timestamp.
		return timestamp_compare(o1, o2);
	    }


	    // If we get here, one or both exceeds the max, but not
	    // equally.  Prefer the smaller one.
	    int compare = 0;
	    if (count1 < count2) 
		compare = -1; // prefer the first
	    else
		compare = 1;  // prefer the second

	    if (loggingService.isInfoEnabled()) {
		// If the result is different from the default, based
		// on timestamps, log that here.
		int default_compare = timestamp_compare(o1, o2);
		if (compare  == default_compare) {
		    // silent
		} else if (compare == -1) {
		    loggingService.info("Prefer " +o1+
					"[" +count1+
					"] even though " +o2+ 
					"[" +count2+
					"] is older");
		} else {
		    loggingService.info("Prefer " +o2+
					"[" +count2+
					"] even though " +o1+ 
					"[" +count1+
					"] is older");
		}
	    }

	    return compare;
	}

	public boolean equals(Object x) 
	{
	    return x == this;
	}

    }


    private static final int MAX_PER_NODE_DEFAULT = 1;
    private static final int MAX_THREADS_DEFAULT = 15;
    private static final String TOPOLOGY = "topology";

    private HashMap counts = new HashMap();
    private HashMap timestamps = new HashMap();
    private HashMap agent_nodes = new HashMap();
    private HashSet agents = new HashSet();
    private WhitePagesService wpService;
    private int maxPerNode;
    private int maxThreads;

    private long getTimestamp(Object x)
    {
	synchronized (timestamps) {
	    return ((Long) timestamps.get(x)).longValue();
	}
    }

    private int getSchedulableNodeCount(Schedulable sched)
    {
	Object o = sched.getConsumer();
	if (o instanceof DestinationQueue) {
	    MessageAddress addr = ((DestinationQueue) o).getDestination();
	    String node = getNodeName(addr);
	    if (node == null) return 0;
	    Integer count = null;
	    synchronized (counts) {
		count = (Integer) counts.get(node);
	    }
	    if (count == null) return 0;
	    return count.intValue();
	} else {
	    return 0;
	}

    }

    private String getNodeName(MessageAddress agent)
    {
	synchronized (agent_nodes) {
	    String node = (String) agent_nodes.get(agent.getPrimary());
	    if (node != null) return node;
	}

	synchronized (agents) {
	    agents.add(agent.getPrimary());
	}

	if (loggingService.isDebugEnabled())
	    loggingService.debug("Couldn't find node of Agent " +agent);
	

	return null;
    }

    // Runs in a scheduled task, since we don't want to call WP in
    // the Comparator.
    private void lookupNodes()
    {
	// TBD: Agent -> Node
	// WP call? Ugh.
	HashSet copy = null;
	synchronized (agents) {
	    copy = new HashSet(agents);
	}

	Iterator itr = copy.iterator();
	while (itr.hasNext()) {
	    MessageAddress agent = (MessageAddress) itr.next();
	    try {
		AddressEntry entry = 
		    wpService.get(agent.getAddress(), TOPOLOGY, -1);
		if (entry != null) {
		    URI uri = entry.getURI();
		    synchronized (agent_nodes) {
			agent_nodes.put(agent, uri.getPath().substring(1));
		    }
		} else {
		    if (loggingService.isDebugEnabled())
			loggingService.debug("Couldn't find node of Agent " +agent+
					     ": WP returned null");
		}
	    } catch (Exception ex) {
		if (loggingService.isDebugEnabled())
		    loggingService.debug("Couldn't find node of Agent " +agent+
					 ": " +ex.getMessage());
	    }
	}
    }


    private void incrementCount(Object o)
    {
	if (o instanceof DestinationQueue) {
	    MessageAddress addr = ((DestinationQueue) o).getDestination();
	    String node = getNodeName(addr);
	    if (node == null) return;
	    synchronized (counts) {
		Integer count = (Integer) counts.get(node);
		if (count == null)
		    count = new Integer(1);
		else
		    count = new Integer(count.intValue() + 1);
		counts.put(node, count);
		if (count.intValue() >= maxThreads) {
		    if (loggingService.isWarnEnabled())
			loggingService.warn("Node " +node+
					    " is using all the threads [" 
					    +count+ "] in pool");
		}
		if (loggingService.isDebugEnabled())
		    loggingService.debug("Increment: count for " +addr+
					" on node " +node+
					" = " +count);
	    }
	}
    }

    private void decrementCount(Object o)
    {
	if (o instanceof DestinationQueue) {
	    MessageAddress addr = ((DestinationQueue) o).getDestination();
	    String node = getNodeName(addr);
	    if (node == null) return;
	    synchronized (counts) {
		Integer count = (Integer) counts.get(node);
		if (count != null)
		    count = new Integer(count.intValue() - 1);
		else
		    count = new Integer(0);
		if (loggingService.isDebugEnabled())
		    loggingService.debug("Decrement: count for " +addr+
					" on node " +node+
					" = " +count);
		counts.put(node, count);
	    }
	}
    }


    public void start()
    {
	super.start();

	maxPerNode = (int) getParameter("MaxPerNode", MAX_PER_NODE_DEFAULT);
	maxThreads = (int) getParameter("MaxThreads", MAX_THREADS_DEFAULT);
	int lane = ThreadService.WILL_BLOCK_LANE;

	ServiceBroker sb = getServiceBroker();

	wpService = (WhitePagesService)
	    sb.getService(this, WhitePagesService.class, null);
	if (wpService == null) {
	    throw new RuntimeException("Can't get WhitePagesService");
	}

	ThreadService tsvc = (ThreadService)
	    sb.getService(this, ThreadService.class, null);
	Runnable runnable = new Runnable() {
		public void run() {
		    lookupNodes();
		}
	    };
	Schedulable sched = tsvc.getThread(this, runnable, "Constrictor Node Lookup");
	sched.schedule(0, 1000);
	sb.releaseService(this, ThreadService.class, tsvc);

	ThreadListenerService tls = (ThreadListenerService)
	    sb.getService(this, ThreadListenerService.class, null);
	if (tls != null) {
	    tls.addListener(this, lane);
	} else {
	    throw new RuntimeException("Can't get ThreadListenerService");
	}

	ThreadControlService tcs = (ThreadControlService)
	    sb.getService(this, ThreadControlService.class, null);
	if (tcs != null) {
	    Comparator cmp = new Constrictor();
	    tcs.setQueueComparator(cmp, lane);
	    tcs.setMaxRunningThreadCount(maxThreads, lane);
	} else {
	    tls.removeListener(this, lane);
	    sb.releaseService(this, ThreadListenerService.class, tls);
	    throw new RuntimeException("Can't get ThreadControlService");
	}
	
	sb.releaseService(this, ThreadListenerService.class, tls);
	sb.releaseService(this, ThreadControlService.class, tcs);
	  
    }
	
    // ThreadListener

    public void threadStarted(Schedulable schedulable, Object consumer)
    {
	incrementCount(consumer);
    }

    public void threadStopped(Schedulable schedulable, Object consumer)
    {
	decrementCount(consumer);
    }


    public void threadQueued(Schedulable schedulable, Object consumer)
    {
	synchronized (timestamps) {
	    timestamps.put(schedulable, new Long(System.currentTimeMillis()));
	}
    }

    public void threadDequeued(Schedulable schedulable, Object consumer)
    {
	synchronized (timestamps) {
	    timestamps.remove(schedulable);
	}
    }

    public void rightGiven(String consumer)
    {
    }

    public void rightReturned(String consumer)
    {
    }

} 
