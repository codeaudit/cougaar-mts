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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.AgentState;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.mts.MulticastMessageAddress;
import org.cougaar.core.mts.SimpleMessageAttributes;
import org.cougaar.core.mts.SocketMessageAddress;
import org.cougaar.core.service.IncarnationService;
import org.cougaar.core.service.LoggingService;

/**
 * The MessageTransportRegistry {@link ServiceProvider} singleton is a utility
 * instance that helps certain pieces of the message transport subsystem to find
 * one another. It provides the {@link MessageTransportRegistryService}. An
 * inner class implements that service.
 */
public final class MessageTransportRegistry
        implements ServiceProvider {

    public ServiceImpl service;

    public MessageTransportRegistry(String name, ServiceBroker sb) {
        service = new ServiceImpl(name, sb);
    }

    public Object getService(ServiceBroker sb, Object requestor, Class<?> serviceClass) {
        if (serviceClass == MessageTransportRegistryService.class) {
            return service;
        } else {
            return null;
        }
    }

    public void releaseService(ServiceBroker sb,
                               Object requestor,
                               Class<?> serviceClass,
                               Object service) {
    }

    static final class ServiceImpl
            implements MessageTransportRegistryService, IncarnationService.Callback {

        private final String name;
        private final Map<MessageAddress,ReceiveLink> receiveLinks = 
            new HashMap<MessageAddress,ReceiveLink>();
        private final Map<MessageAddress,AgentState> agentStates = 
            new HashMap<MessageAddress,AgentState>();
        private final Map<MessageAddress,MessageTransportClient> localClients = 
            new HashMap<MessageAddress,MessageTransportClient>();
        private final List<LinkProtocol> linkProtocols = new ArrayList<LinkProtocol>();
        private ReceiveLinkProviderService receiveLinkProvider;
        private NameSupport nameSupport;
        private final ServiceBroker sb;
        private final LoggingService loggingService;
        private final IncarnationService incarnationService;
        private final Map<InetSocketAddress,Collection<MessageAddress>> mcast =
            new HashMap<InetSocketAddress,Collection<MessageAddress>>();

        private ServiceImpl(String name, ServiceBroker sb) {
            this.name = name;
            this.sb = sb;
            loggingService = sb.getService(this, LoggingService.class, null);

            incarnationService = sb.getService(this, IncarnationService.class, null);
            if (incarnationService == null && loggingService.isWarnEnabled()) {
                loggingService.warn("Couldn't load IncarnationService");
            }
        }

        private NameSupport nameSupport() {
            if (nameSupport == null) {
                nameSupport = sb.getService(this, NameSupport.class, null);
            }
            return nameSupport;
        }

        private ReceiveLink makeReceiveLink(MessageTransportClient client) {
            if (receiveLinkProvider == null) {
                receiveLinkProvider = sb.getService(this, ReceiveLinkProviderService.class, null);
            }
            ReceiveLink link = receiveLinkProvider.getReceiveLink(client);
            receiveLinks.put(client.getMessageAddress(), link);
            return link;
        }

        private void registerClientWithSociety(MessageTransportClient client) {
            // register with each component transport
            synchronized (linkProtocols) {
                for (LinkProtocol protocol : linkProtocols) {
                    protocol.registerClient(client);
                }
            }
        }

        private void unregisterClientWithSociety(MessageTransportClient client) {
            // register with each component transport
            synchronized (linkProtocols) {
                for (LinkProtocol protocol : linkProtocols) {
                    protocol.unregisterClient(client);
                }
                for (Map.Entry<InetSocketAddress,Collection<MessageAddress>> entry : mcast.entrySet()) {
                    Collection<MessageAddress> clients = entry.getValue();
                    clients.remove(client.getMessageAddress());
                    if (clients.isEmpty()) {
                        InetSocketAddress multicastAddress = entry.getKey();
                        removeMulticastListener(multicastAddress);
                    }
                }
            }
        }

        public synchronized void incarnationChanged(MessageAddress address, long incarnation) {
            MessageAddress key = address.getPrimary();
            MessageTransportClient client = null;
            client = localClients.get(key);
            if (client != null && client.getIncarnationNumber() < incarnation) {
                agentStates.remove(key);
                receiveLinks.remove(key);
                localClients.remove(key);
            }
        }

        private synchronized void addLocalClient(MessageTransportClient client) {
            MessageAddress key = client.getMessageAddress();
            localClients.put(key.getPrimary(), client);

            if (incarnationService != null) {
                incarnationService.subscribe(key, this);
            }

            try {
                ReceiveLink link = findLocalReceiveLink(key);
                if (link == null) {
                    link = makeReceiveLink(client);
                }
            } catch (Exception e) {
                if (loggingService.isErrorEnabled()) {
                    loggingService.error(e.toString());
                }
            }
        }

        private synchronized void removeLocalClient(MessageTransportClient client) {
            MessageAddress key = client.getMessageAddress();
            try {
                receiveLinks.remove(key);
                localClients.remove(key.getPrimary());
            } catch (Exception e) {
            }
        }

        public boolean hasLinkProtocols() {
            synchronized (linkProtocols) {
                return linkProtocols.size() > 0;
            }
        }

        public void addLinkProtocol(LinkProtocol lp) {
            synchronized (linkProtocols) {
                linkProtocols.add(lp);
            }
        }

        public String getIdentifier() {
            return name;
        }

        public synchronized AgentState getAgentState(MessageAddress id) {
            MessageAddress canonical_id = id.getPrimary();
            Object raw = agentStates.get(canonical_id);
            if (raw == null) {
                AgentState state = new SimpleMessageAttributes();
                agentStates.put(canonical_id, state);
                return state;
            } else if (raw instanceof AgentState) {
                return (AgentState) raw;
            } else {
                throw new RuntimeException("Cached state for " + id + "=" + raw
                        + " which is not an AgentState instance");
            }
        }

        public synchronized void removeAgentState(MessageAddress id) {
            agentStates.remove(id.getPrimary());
        }

        public boolean isLocalClient(MessageAddress id) {
            synchronized (this) {
                return receiveLinks.get(id.getPrimary()) != null
                        || id.equals(MessageAddress.MULTICAST_LOCAL);
            }
        }

        public ReceiveLink findLocalReceiveLink(MessageAddress id) {
            return receiveLinks.get(id.getPrimary());
        }

        // this is a slow implementation, as it conses a new set each time.
        // Better alternatives surely exist.
        public Iterator<MessageAddress> findLocalMulticastReceivers(MulticastMessageAddress addr) {
            if (addr.hasReceiverClass()) {
                List<MessageAddress> result = new ArrayList<MessageAddress>();
                Class<?> mclass = addr.getReceiverClass();
                if (mclass != null) {
                    for (Map.Entry<MessageAddress,ReceiveLink> entry : receiveLinks.entrySet()) {
                        ReceiveLink link = entry.getValue();
                        MessageTransportClient client = link.getClient();
                        if (mclass.isAssignableFrom(client.getClass())) {
                            result.add(entry.getKey());
                            if (loggingService.isDebugEnabled()) {
                                loggingService.debug("Client " + client + " matches " + mclass
                                        + ", added " + entry.getKey());
                            }
                        } else {
                            if (loggingService.isDebugEnabled()) {
                                loggingService.debug("Client " + client + " doesn't match "
                                        + mclass);
                            }
                        }
                    }
                }
                if (loggingService.isDebugEnabled()) {
                    loggingService.debug("result=" + result);
                }
                return result.iterator();

            } else {
                return new ArrayList<MessageAddress>(receiveLinks.keySet()).iterator();
            }
        }

        public Iterator<MessageAddress> findRemoteMulticastTransports(MulticastMessageAddress addr) {
            return nameSupport().lookupMulticast(addr);
        }

        public void registerClient(MessageTransportClient client) {
            registerClientWithSociety(client);
            addLocalClient(client);
        }

        public void unregisterClient(MessageTransportClient client) {
            removeLocalClient(client);
            unregisterClientWithSociety(client);
        }

        public void ipAddressChanged() {
            // inform each protocol
            synchronized (linkProtocols) {
                for (LinkProtocol protocol : linkProtocols) {
                    protocol.ipAddressChanged();
                }
            }
        }

        public boolean addressKnown(MessageAddress address) {
            synchronized (linkProtocols) {
                for (LinkProtocol protocol : linkProtocols) {
                    if (protocol.addressKnown(address)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public List<DestinationLink> getDestinationLinks(MessageAddress destination) {
            List<DestinationLink> destinationLinks = new ArrayList<DestinationLink>();
            synchronized (linkProtocols) {
                for (LinkProtocol lp : linkProtocols) {
                    if (lp.supportsAddressType(destination.getClass())) {
                        DestinationLink link = lp.getDestinationLink(destination);
                        destinationLinks.add(link);
                    }
                }
            }
            return destinationLinks;
        }
        
        // Multicast
        public Iterable<MessageAddress> getGroupListeners(InetSocketAddress multicastAddress) {
            synchronized (linkProtocols) {
                Collection<MessageAddress> addresses = mcast.get(multicastAddress);
                if (addresses == null) {
                    return null;
                } else {
                    return new ArrayList<MessageAddress>(addresses);
                }
            }
        }
        
        public void joinGroup(MessageTransportClient client, InetSocketAddress multicastAddress) {
            synchronized (linkProtocols) {
                Collection<MessageAddress> clients = mcast.get(multicastAddress);
                if (clients == null) {
                    clients = new HashSet<MessageAddress>();
                    mcast.put(multicastAddress, clients);
                }
                boolean newAddress = clients.isEmpty();
                clients.add(client.getMessageAddress());
                if (newAddress) {
                    addMulticastListener(multicastAddress);
                }
            }
        }

        public void leaveGroup(MessageTransportClient client, InetSocketAddress multicastAddress) {
            synchronized (linkProtocols) {
                Collection<MessageAddress> clients = mcast.get(multicastAddress);
                if (clients != null) {
                    clients.remove(client.getMessageAddress());
                    if (clients.isEmpty()) {
                        removeMulticastListener(multicastAddress);
                    }
                }
            }
        }

        private void addMulticastListener(InetSocketAddress multicastAddress) {
            for (LinkProtocol lp : linkProtocols) {
                if (lp.supportsAddressType(SocketMessageAddress.class)) {
                    try {
                        lp.join(multicastAddress);
                    } catch (IOException e) {
                        loggingService.error("Failed to join multicast group "
                                + multicastAddress);
                    }
                }
            }
        }

        private void removeMulticastListener(InetSocketAddress multicastAddress) {
            for (LinkProtocol lp : linkProtocols) {
                if (lp.supportsAddressType(SocketMessageAddress.class)) {
                    try {
                        lp.leave(multicastAddress);
                    } catch (IOException e) {
                        loggingService.error("Failed to leave multicast group "
                                             + multicastAddress);
                    }
                }
            }
        }
    }

}
