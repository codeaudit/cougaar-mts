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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.AgentStatusService;
import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.qos.metrics.Constants;
import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.qos.metrics.MetricImpl;
import org.cougaar.core.qos.metrics.MetricsUpdateService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.DestinationQueueProviderService;
import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MessageDelivererDelegateImplBase;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.QueueListener;
import org.cougaar.mts.base.SendLink;
import org.cougaar.mts.base.SendLinkDelegateImplBase;
import org.cougaar.mts.base.SendQueue;
import org.cougaar.mts.base.SendQueueDelegateImplBase;
import org.cougaar.mts.base.SendQueueProviderService;
import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.base.UnregisteredNameException;

/**
 * This Aspect implements the {@link AgentStatusService}.
 * 
 * In the <a
 * href="../../../../../OnlineManual/MetricsService/sensors.html">Sensor Data
 * Flow</a> pattern this class plays the role of <b>Sensor</b> for message
 * counts and size among Agents and Nodes.
 * 
 * @see org.cougaar.core.qos.metrics.AgentStatusRatePlugin
 * @see org.cougaar.core.qos.metrics.AgentLoadServlet
 * 
 */
public class AgentStatusAspect
        extends StandardAspect
        implements AgentStatusService, QueueListener, Constants, AttributeConstants {

    private static final double SEND_CREDIBILITY = Constants.SECOND_MEAS_CREDIBILITY;

    private final Map<MessageAddress,AgentState> remoteStates;
    private final Map<MessageAddress,AgentState> localStates;
    private final AgentState nodeState;
    private MetricsUpdateService metricsUpdateService;

    public AgentStatusAspect() {
        remoteStates = new HashMap<MessageAddress,AgentState>();
        localStates = new HashMap<MessageAddress,AgentState>();
        nodeState = newAgentState();
    }

    public void load() {
        super.load();

        ServiceBroker sb = getServiceBroker();
        metricsUpdateService = sb.getService(this, MetricsUpdateService.class, null);
        
        NodeControlService ncs = sb.getService(this, NodeControlService.class, null);

        ServiceBroker rootsb = ncs.getRootServiceBroker();
        rootsb.addService(AgentStatusService.class, new ServiceProvider(){

            public Object getService(ServiceBroker sb, Object requestor, Class<?> serviceClass) {
                if (serviceClass == AgentStatusService.class) {
                    return AgentStatusAspect.this;
                } else {
                    return null;
                }
            }

            public void releaseService(ServiceBroker sb,
                                       Object requestor,
                                       Class<?> serviceClass,
                                       Object service) {
                // no-op
            }
            
        });
    }

    public void start() {
        super.start();

        ServiceBroker sb = getServiceBroker();

        SendQueueProviderService sendq_fact =
                sb.getService(this, SendQueueProviderService.class, null);

        DestinationQueueProviderService destq_fact =
                sb.getService(this, DestinationQueueProviderService.class, null);

        LoggingService lsvc = getLoggingService();

        if (sendq_fact != null) {
            sendq_fact.addListener(this);
            sb.releaseService(this, SendQueueProviderService.class, sendq_fact);
        } else if (lsvc.isInfoEnabled()) {
            lsvc.info("Couldn't get SendQueueProviderService");
        }

        if (destq_fact != null) {
            destq_fact.addListener(this);
            sb.releaseService(this, DestinationQueueProviderService.class, destq_fact);
        } else if (lsvc.isInfoEnabled()) {
            lsvc.info("Couldn't get DestinationQueueProviderService");
        }
    }

    public void messagesRemoved(List<Message> messages) {
        LoggingService lsvc = getLoggingService();
        synchronized (messages) {
            for (Message message : messages) {
                // handle removed message
                MessageAddress remoteAddr = message.getTarget().getPrimary();
                AgentState remoteState = ensureRemoteState(remoteAddr);
                if (lsvc.isInfoEnabled()) {
                    lsvc.info("Messages removed from queue " + remoteAddr);
                }
                synchronized (remoteState) {
                    remoteState.queueLength--;
                }
            }
        }
    }

    private AgentState ensureRemoteState(MessageAddress address) {
        AgentState state = null;
        synchronized (remoteStates) {
            state = remoteStates.get(address);
            if (state == null) {
                state = newAgentState();
                remoteStates.put(address, state);
            }
        }
        return state;
    }

    private AgentState getRemoteState(MessageAddress address) {
        AgentState state = null;
        synchronized (remoteStates) {
            state = remoteStates.get(address);
        }
        return state;
    }

    private AgentState ensureLocalState(MessageAddress address) {
        AgentState state = null;
        synchronized (localStates) {
            state = localStates.get(address);
            if (state == null) {
                state = newAgentState();
                localStates.put(address, state);
            }
        }
        return state;
    }

    private AgentState getLocalState(MessageAddress address) {
        AgentState state = null;
        synchronized (localStates) {
            state = localStates.get(address);
        }
        return state;
    }

    private AgentState newAgentState() {
        return new AgentState();
    }

    private AgentState snapshotState(AgentState state) {
        try {
            return state.clone();
        } catch (CloneNotSupportedException e) {
            System.err.println("This can't happen");
            return null;
        }
    }

    private Metric longMetric(long value) {
        return new MetricImpl(new Long(value), SEND_CREDIBILITY, "", "AgentStatusAspect");
    }

    // Agent Status Service Public Interface

    public AgentState getNodeState() {
        return snapshotState(nodeState);
    }

    public AgentState getLocalAgentState(MessageAddress address) {
        AgentState state = getLocalState(address);
        // must snapshot state or caller will get a dynamic value.
        if (state != null) {
            return snapshotState(state);
        } else {
            return null;
        }
    }

    public AgentState getRemoteAgentState(MessageAddress address) {
        AgentState state = getRemoteState(address);
        // must snapshot state or caller will get a dynamic value.
        if (state != null) {
            return snapshotState(state);
        } else {
            return null;
        }
    }

    public Set<MessageAddress> getLocalAgents() {
        Set<MessageAddress> result = new HashSet<MessageAddress>();
        synchronized (localStates) {
            result.addAll(localStates.keySet());
        }
        return result;
    }

    public Set<MessageAddress> getRemoteAgents() {
        Set<MessageAddress> result = new HashSet<MessageAddress>();
        synchronized (remoteStates) {
            result.addAll(remoteStates.keySet());
        }
        return result;
    }

    // 
    // Aspect Code to implement Sensors

    // To gather sensible send-side statistics, this aspect's
    // delegates need to run very late on the SendQueue (so as to
    // count any internal messages added to the queue by other aspect
    // delegates) but very early on the DestinationLink (because the
    // delegate on that side is processing the return). The aspect
    // mechanism doesn't provide for station-specific ordering. But
    // it does provide an implicit early-vs-late switch, since
    // reverse delegates always run early. Use that here.
    public Object getDelegate(Object object, Class<?> type) {
        if (type == SendQueue.class) {
            return new SendQueueDelegate((SendQueue) object);
        } else if (type == SendLink.class) {
            return new SendLinkDelegate((SendLink) object);
        } else if (type == MessageDeliverer.class) {
            return new MessageDelivererDelegate((MessageDeliverer) object);
        } else {
            return null;
        }
    }

    public Object getReverseDelegate(Object object, Class<?> type) {
        if (type == DestinationLink.class) {
            return new DestinationLinkDelegate((DestinationLink) object);
        } else {
            return null;
        }
    }

    private class SendLinkDelegate
            extends SendLinkDelegateImplBase {
        SendLinkDelegate(SendLink link) {
            super(link);
        }

        public void release() {
            MessageAddress addr = getAddress().getPrimary();
            synchronized (localStates) {
                localStates.remove(addr);
            }
        }

        public void registerClient(MessageTransportClient client) {
            super.registerClient(client);
            ensureLocalState(getAddress().getPrimary());
        }

    }

    private class DestinationLinkDelegate
            extends DestinationLinkDelegateImplBase {
        private final String spoke_key, heard_key, error_key;

        public DestinationLinkDelegate(DestinationLink link) {
            super(link);
            String remoteAgent = link.getDestination().getAddress();
            spoke_key = "Agent" + KEY_SEPR + remoteAgent + KEY_SEPR + "SpokeTime";
            heard_key = "Agent" + KEY_SEPR + remoteAgent + KEY_SEPR + "HeardTime";
            error_key = "Agent" + KEY_SEPR + remoteAgent + KEY_SEPR + "SpokeErrorTime";
        }

        boolean delivered(MessageAttributes attributes) {
            return attributes != null
                    && attributes.getAttribute(DELIVERY_ATTRIBUTE).equals(DELIVERY_STATUS_DELIVERED);
        }

        public MessageAttributes forwardMessage(AttributedMessage message)
                throws UnregisteredNameException, NameLookupException, CommFailureException,
                MisdeliveredMessageException {
            MessageAddress remoteAddr = message.getTarget().getPrimary();
            AgentState remoteState = ensureRemoteState(remoteAddr);
            MessageAddress localAddr = message.getOriginator().getPrimary();
            AgentState localState = getLocalState(localAddr);

            if (localState == null) {
                // Leftover message from an unregistered agent
                LoggingService lsvc = getLoggingService();
                if (lsvc.isErrorEnabled()) {
                    lsvc.error("Forwarding leftover message from unregistered agent " + localAddr);
                }
                return super.forwardMessage(message);
            }

            try {
                long startTime = System.currentTimeMillis();
                synchronized (remoteState) {
                    remoteState.lastLinkProtocolTried = getProtocolClass().getName();
                }
                // Attempt to Deliver message
                MessageAttributes meta = super.forwardMessage(message);

                // successful Delivery
                long endTime = System.currentTimeMillis();
                boolean success = delivered(meta);
                if (success) {
                    metricsUpdateService.updateValue(heard_key, longMetric(endTime));
                    metricsUpdateService.updateValue(spoke_key, longMetric(endTime));
                }

                int msgBytes = 0;
                Object attr = message.getAttribute(MESSAGE_BYTES_ATTRIBUTE);
                if (attr != null && attr instanceof Number) {
                    msgBytes = ((Number) attr).intValue();
                }

                long latency = endTime - startTime;
                double alpha = 0.20;
                synchronized (remoteState) {
                    if (success) {
                        remoteState.lastHeardFrom = endTime;
                        remoteState.lastSentTo = endTime;
                    }
                    remoteState.status = AgentStatusService.Status.ACTIVE;
                    remoteState.timestamp = System.currentTimeMillis();
                    remoteState.deliveredCount++;
                    remoteState.deliveredBytes += msgBytes;
                    remoteState.lastDeliveredBytes = msgBytes;
                    remoteState.queueLength--;
                    remoteState.lastDeliveredLatency = (int) latency;
                    remoteState.deliveredLatencySum += latency;
                    remoteState.averageDeliveredLatency =
                            alpha * latency + (1 - alpha) * remoteState.averageDeliveredLatency;
                    remoteState.lastLinkProtocolSuccess = getProtocolClass().getName();
                }
                synchronized (localState) {
                    localState.status = AgentStatusService.Status.ACTIVE;
                    localState.timestamp = System.currentTimeMillis();
                    localState.deliveredCount++;
                    localState.deliveredBytes += msgBytes;
                    localState.lastDeliveredBytes = msgBytes;
                }
                synchronized (nodeState) {
                    nodeState.timestamp = System.currentTimeMillis();
                    nodeState.deliveredCount++;
                    nodeState.deliveredBytes += msgBytes;
                }

                return meta;

            } catch (UnregisteredNameException unreg) {
                long now = System.currentTimeMillis();
                synchronized (remoteState) {
                    remoteState.status = Status.UNREGISTERED;
                    remoteState.timestamp = now;
                    remoteState.unregisteredNameCount++;
                    remoteState.lastFailedSend = now;
                }
                metricsUpdateService.updateValue(error_key, longMetric(now));
                throw unreg;
            } catch (NameLookupException namex) {
                long now = System.currentTimeMillis();
                synchronized (remoteState) {
                    remoteState.status = Status.UNKNOWN;
                    remoteState.timestamp = now;
                    remoteState.nameLookupFailureCount++;
                    remoteState.lastFailedSend = now;
                }
                metricsUpdateService.updateValue(error_key, longMetric(now));
                throw namex;
            } catch (CommFailureException commex) {
                long now = System.currentTimeMillis();
                synchronized (remoteState) {
                    remoteState.status = Status.UNREACHABLE;
                    remoteState.timestamp = now;
                    remoteState.commFailureCount++;
                    remoteState.lastFailedSend = now;
                }
                metricsUpdateService.updateValue(error_key, longMetric(now));
                throw commex;
            } catch (MisdeliveredMessageException misd) {
                long now = System.currentTimeMillis();
                synchronized (remoteState) {
                    remoteState.status = Status.UNREGISTERED;
                    remoteState.timestamp = now;
                    remoteState.misdeliveredMessageCount++;
                    remoteState.lastFailedSend = now;
                }
                metricsUpdateService.updateValue(error_key, longMetric(now));
                throw misd;
            }
        }

    }

    private class MessageDelivererDelegate
            extends MessageDelivererDelegateImplBase {

        MessageDelivererDelegate(MessageDeliverer delegatee) {
            super(delegatee);
        }

        public MessageAttributes deliverMessage(AttributedMessage message, MessageAddress dest)
                throws MisdeliveredMessageException {
            String remoteAgent = message.getOriginator().getAddress();
            String heard_key = "Agent" + KEY_SEPR + remoteAgent + KEY_SEPR + "HeardTime";
            long receiveTime = System.currentTimeMillis();
            metricsUpdateService.updateValue(heard_key, longMetric(receiveTime));

            int msgBytes = 0;
            Object attr = message.getAttribute(MESSAGE_BYTES_ATTRIBUTE);
            if (attr != null && attr instanceof Number) {
                msgBytes = ((Number) attr).intValue();
            }

            AgentState remoteState = ensureRemoteState(message.getOriginator().getPrimary());
            synchronized (remoteState) {
                remoteState.receivedCount++;
                remoteState.receivedBytes += msgBytes;
                remoteState.lastHeardFrom = receiveTime;
            }

            AgentState localState = getLocalState(message.getTarget().getPrimary());
            if (localState != null) {
                synchronized (localState) {
                    localState.receivedCount++;
                    localState.receivedBytes += msgBytes;
                }
            } else {
                LoggingService lsvc = getLoggingService();
                if (lsvc.isInfoEnabled()) {
                    lsvc.info("Received message for non-local agent " + message.getTarget());
                }
            }

            synchronized (nodeState) {
                nodeState.receivedCount++;
                nodeState.receivedBytes += msgBytes;
            }

            return super.deliverMessage(message, dest);
        }

    }

    private class SendQueueDelegate
            extends SendQueueDelegateImplBase {
        
        public SendQueueDelegate(SendQueue queue) {
            super(queue);
        }

        public void sendMessage(AttributedMessage message) {
            MessageAddress remoteAddr = message.getTarget().getPrimary();
            AgentState remoteState = ensureRemoteState(remoteAddr);
            MessageAddress localAddr = message.getOriginator().getPrimary();
            AgentState localState = getLocalState(localAddr);

            synchronized (remoteState) {
                remoteState.sendCount++;
                remoteState.queueLength++;
            }

            long receiveTime = System.currentTimeMillis();

            if (localState != null) {
                synchronized (localState) {
                    localState.sendCount++;
                    localState.lastHeardFrom = receiveTime;
                }

                // Local agent sending message means that the MTS has
                // "heard from" the local agent
                String localAgent = localAddr.getAddress();
                String heard_key = "Agent" + KEY_SEPR + localAgent + KEY_SEPR + "HeardTime";
                metricsUpdateService.updateValue(heard_key, longMetric(receiveTime));
            } else {
                LoggingService lsvc = getLoggingService();
                if (lsvc.isErrorEnabled()) {
                    lsvc.error("SendQueue sending leftover message from " + localAddr);
                }
            }

            synchronized (nodeState) {
                nodeState.sendCount++;
                nodeState.lastHeardFrom = receiveTime; // ???
            }

            super.sendMessage(message);
        }
    }
}
