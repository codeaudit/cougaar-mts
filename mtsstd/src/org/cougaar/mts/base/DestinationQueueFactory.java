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

package org.cougaar.mts.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.util.UnaryPredicate;

/**
 * This Component implements the {@link DestinationQueueProviderService}, which
 * makes DestinationQueues on demand, and the
 * {@link DestinationQueueMonitorService}, which allows clients to be notified
 * of queue events. It also acts the {@link ServiceProvider} for those services.
 * 
 * For instantiation of DestinationQueues, it uses the standard find-or-make
 * approach, where a target address is used for finding. Since this Component is
 * a subclass of @{link AspectFactory}, aspect delegates will be attached to
 * {@link DestinationQueue}s when they're instantiated.
 * 
 */
public class DestinationQueueFactory
        extends QueueFactory
        implements DestinationQueueProviderService, DestinationQueueMonitorService, ServiceProvider {
    private final Map<MessageAddress,DestinationQueue> queues;
    private final List<DestinationQueueImpl> impls;

    DestinationQueueFactory(MessageTransportServiceProvider container) {
        super(container);
        queues = new HashMap<MessageAddress,DestinationQueue>();
        impls = new ArrayList<DestinationQueueImpl>();
    }

    /**
     * Find a DestinationQueue for the given address, or make a new one of type
     * DestinationQueueImpl if there isn't one yet. In the latter case, attach
     * all relevant aspects as part of the process of creating the queue. The
     * final object returned is the outermost aspect delegate, or the
     * DestinationQueueImpl itself if there are no aspects.
     */
    public DestinationQueue getDestinationQueue(MessageAddress destination) {
        MessageAddress dest = destination.getPrimary();
        DestinationQueue q = null;
        DestinationQueueImpl qimpl = null;
        synchronized (queues) {
            q = queues.get(dest);
            if (q == null) {
                qimpl = new DestinationQueueImpl(dest);
                addComponent(qimpl);
                q = attachAspects(qimpl, DestinationQueue.class);
                qimpl.setDelegate(q);
                queues.put(dest, q);
                synchronized (impls) {
                    impls.add(qimpl);
                }
            }
        }
        return q;
    }

    // NB: This does _not_ prevent another thread from adding new
    // messages while the remove operation is in progress.
    public void removeMessages(UnaryPredicate pred, List<Message> removed) {
        List<MessageQueue> copy;
        synchronized (impls) {
            copy = new ArrayList<MessageQueue>(impls);
        }
        for (MessageQueue queue : copy) {
            queue.removeMessages(pred, removed);
        }
        notifyListeners(removed);
    }

    public MessageAddress[] getDestinations() {
        synchronized (queues) {
            MessageAddress[] ret = new MessageAddress[queues.size()];
            queues.keySet().toArray(ret);
            return ret;
        }
    }

    public AttributedMessage[] snapshotQueue(MessageAddress destination) {
        DestinationQueue q = null;
        MessageAddress dest = destination.getPrimary();
        synchronized (queues) {
            q = queues.get(dest);
        }
        return q == null ? null : q.snapshot();
    }

    public Object getService(ServiceBroker sb, Object requestor, Class<?> serviceClass) {
        // Restrict this service
        if (serviceClass == DestinationQueueProviderService.class) {
            return this;
        } else if (serviceClass == DestinationQueueMonitorService.class) {
            return this;
        }
        return null;
    }

    public void releaseService(ServiceBroker sb,
                               Object requestor,
                               Class<?> serviceClass,
                               Object service) {
    }

}
