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

package org.cougaar.mts.std.experimental;

import java.io.FileInputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.ThreadControlService;
import org.cougaar.core.service.ThreadListenerService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.thread.ThreadListener;
import org.cougaar.mts.base.DestinationQueue;
import org.cougaar.mts.base.StandardAspect;

/**
 * Not an aspect, but the aspect mechanism provides a simple way to load classes
 * on demand. This is actually a ThreadListener which alters the ThreadService's
 * queuing behavior.
 */
public class PrioritizedThreadsAspect
        extends StandardAspect
        implements ThreadListener {

    private Map<Object,Integer> agent_priorities;
    private Map<Schedulable,Integer> thread_priorities;

    private final Comparator<Schedulable> priorityComparator = new Comparator<Schedulable>() {
        public boolean equals(Object x) {
            return x == this;
        }

        public int compare(Schedulable o1, Schedulable o2) {
            Integer x = thread_priorities.get(o1);
            Integer y = thread_priorities.get(o2);

            // Entries placed on the queue before our listener
            // starts won't be found by the map. Deal with
            // that here.
            if (x == null && y == null) {
                return 0;
            }
            if (x == null) {
                return -1;
            }
            if (y == null) {
                return 1;
            }

            // Higher priority should precede lower, so reverse
            // the arguments.
            return y.compareTo(x);
        }
    };

    public void load() {
        super.load();

        thread_priorities = new HashMap<Schedulable,Integer>();
        agent_priorities = new HashMap<Object,Integer>();
        Properties p = new Properties();
        String priorities_file = SystemProperties.getProperty("org.cougaar.lib.quo.priorities");
        if (priorities_file != null) {
            try {
                FileInputStream fis = new FileInputStream(priorities_file);
                p.load(fis);
                fis.close();
            } catch (java.io.IOException ex) {
                System.err.println(ex);
            }
        }

        for (Map.Entry<Object,Object> entry : p.entrySet()) {
            Object key = entry.getKey();
            String priority = (String) entry.getValue();
            if (priority != null) {
                agent_priorities.put(key, Integer.parseInt(priority));
            }
        }

        ServiceBroker sb = getServiceBroker();
        ThreadListenerService listenerService =
                sb.getService(this, ThreadListenerService.class, null);
        ThreadControlService controlService = sb.getService(this, ThreadControlService.class, null);

        // Whack the queue
        listenerService.addListener(this);
        controlService.setQueueComparator(priorityComparator);

        // we should release the services now.

    }

    // MessageTransportAspect
    public Object getDelegate(Object delegatee, Class<?> type) {
        // not a real aspect, so no delegates
        return null;
    }

    // ThreadListener
    public void threadQueued(Schedulable thread, Object consumer) {
        if (consumer instanceof DestinationQueue) {
            // Note the thread's priority just before it goes on the
            // queue. The comparator will use this later.
            DestinationQueue q = (DestinationQueue) consumer;
            MessageAddress address = q.getDestination();
            Integer priority = agent_priorities.get(address.toString());
            thread_priorities.put(thread, priority);
        }
    }

    public void threadDequeued(Schedulable thread, Object consumer) {
    }

    public void threadStarted(Schedulable thread, Object consumer) {
    }

    public void threadStopped(Schedulable thread, Object consumer) {
    }

    public void rightGiven(String consumer) {
    }

    public void rightReturned(String consumer) {
    }

}
