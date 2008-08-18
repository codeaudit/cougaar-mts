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

import java.util.ArrayList;
import java.util.List;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.MessageTransportWatcher;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.MessageWatcherService;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MessageDelivererDelegateImplBase;
import org.cougaar.mts.base.MessageTransportServiceProvider;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.SendQueue;
import org.cougaar.mts.base.SendQueueDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;

/**
 * This Aspect is used in conjunction with {@link MessageWatcherServiceImpl},
 * the implementaion of the {@link MessageWatcherService}. Both are instantiated
 * by the {@link MessageTransportServiceProvider}, which is also the provider of
 * the {@link MessageWatcherService}. The actual "watching" happens in this
 * Aspect. The service is a core front-end.
 */
public class WatcherAspect
        extends StandardAspect {
    private final List<MessageTransportWatcher> watchers;

    public WatcherAspect() {
        this.watchers = new ArrayList<MessageTransportWatcher>();
    }
    
    public void load() {
        super.load();
        
        NodeControlService ncs = getServiceBroker().getService(this, NodeControlService.class, null);
        ServiceBroker rootsb = ncs.getRootServiceBroker();
        rootsb.addService(MessageWatcherService.class, new ServiceProvider(){

            public Object getService(ServiceBroker sb, Object requestor, Class<?> serviceClass) {
                if (serviceClass == MessageWatcherService.class) {
                    return new MessageWatcherServiceImpl(WatcherAspect.this);
                } else {
                    return null;
                }
            }

            public void releaseService(ServiceBroker sb,
                                       Object requestor,
                                       Class<?> serviceClass,
                                       Object service) {
                if (serviceClass == MessageWatcherService.class) {
                    if (service instanceof MessageWatcherServiceImpl) {
                        ((MessageWatcherServiceImpl) service).release();
                    }
                }
            }
        });
    }

    public Object getDelegate(Object delegate, Class<?> type) {
        if (type == SendQueue.class) {
            return new SendQueueDelegate((SendQueue) delegate);
        } else if (type == MessageDeliverer.class) {
            return new MessageDelivererDelegate((MessageDeliverer) delegate);
        } else {
            return null;
        }
    }

    synchronized void addWatcher(MessageTransportWatcher watcher) {
        watchers.add(watcher);
    }

    synchronized void removeWatcher(MessageTransportWatcher watcher) {
        watchers.remove(watcher);
    }

    // Should the watchers see the AttributedMessage or its contents?
    private void notifyWatchersOfSend(AttributedMessage message) {
        Message rawMessage = message.getRawMessage();
        synchronized (this) {
            for (MessageTransportWatcher w : watchers) {
                if (loggingService.isDebugEnabled()) {
                    loggingService.debug("Notifying " + w + " of send");
                }
                w.messageSent(rawMessage);
            }
        }
    }

    private void notifyWatchersOfReceive(AttributedMessage message) {
        Message rawMessage = message.getRawMessage();
        synchronized (this) {
            for (MessageTransportWatcher w : watchers) {
                if (loggingService.isDebugEnabled()) {
                    loggingService.debug("Notifying " + w + " of receive");
                }
                w.messageReceived(rawMessage);
            }
        }
    }

    public class SendQueueDelegate
            extends SendQueueDelegateImplBase {
        public SendQueueDelegate(SendQueue queue) {
            super(queue);
        }

        public void sendMessage(AttributedMessage message) {
            super.sendMessage(message);
            notifyWatchersOfSend(message);
        }

    }

    public class MessageDelivererDelegate
            extends MessageDelivererDelegateImplBase {
        public MessageDelivererDelegate(MessageDeliverer deliverer) {
            super(deliverer);
        }

        public MessageAttributes deliverMessage(AttributedMessage message, MessageAddress dest)
                throws MisdeliveredMessageException {
            MessageAttributes result = super.deliverMessage(message, dest);
            notifyWatchersOfReceive(message);
            return result;
        }

    }
}
