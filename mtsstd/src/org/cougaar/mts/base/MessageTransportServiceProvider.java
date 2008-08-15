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

import org.cougaar.core.agent.Agent;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.AgentStatusService;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.node.ComponentInitializerService;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.MessageStatisticsService;
import org.cougaar.core.service.MessageTransportService;
import org.cougaar.core.service.MessageWatcherService;
import org.cougaar.core.thread.ThreadServiceProvider;
import org.cougaar.mts.std.AgentStatusAspect;
import org.cougaar.mts.std.MessageWatcherServiceImpl;
import org.cougaar.mts.std.StatisticsAspect;
import org.cougaar.mts.std.WatcherAspect;

/**
 * This Component and Container is the ServiceProvider for the
 * {@link MessageTransportService}. This the overall organizing class for the
 * MTS as a whole.
 */
public final class MessageTransportServiceProvider
        extends ContainerSupport
        implements ServiceProvider {

    // Services we use (more than once)
    private AspectSupport aspectSupport;
    private LoggingService loggingService;

    private String id;
    private final Map<MessageAddress,MessageTransportServiceProxy> proxies = 
        new HashMap<MessageAddress,MessageTransportServiceProxy>();

    private AspectSupportImpl aspectSupportImpl;

    private MessageStreamsFactory msgFactory;

    protected String specifyContainmentPoint() {
        return Agent.INSERTION_POINT + ".MessageTransport";
    }

    public void load() {
        transitState("load()", UNLOADED, LOADED);

        beforeConfig();

        addAll(readConfig());

        afterConfig();
    }

    // disable super load sequence
    protected void loadHighPriorityComponents() {
    }

    protected void loadInternalPriorityComponents() {
    }

    protected void loadBinderPriorityComponents() {
    }

    protected void loadComponentPriorityComponents() {
    }

    protected void loadLowPriorityComponents() {
    }

    protected ComponentDescriptions findInitialComponentDescriptions() {
        return null;
    }

    protected ComponentDescriptions findExternalComponentDescriptions() {
        return null;
    }

    // components that must be loaded before the config's components
    private void beforeConfig() {
        ServiceBroker csb = getChildServiceBroker();

        NodeIdentificationService nis = csb.getService(this, NodeIdentificationService.class, null);
        id = nis.getMessageAddress().toString();
        loggingService = csb.getService(this, LoggingService.class, null);

        aspectSupportImpl = new AspectSupportImpl(this, loggingService);
        csb.addService(AspectSupport.class, aspectSupportImpl);

        // Could use a ComponentDescription
        ThreadServiceProvider tsp = new ThreadServiceProvider();
        tsp.setParameter("name=MTS");
        add(tsp);

        MessageTransportRegistry reg = new MessageTransportRegistry(id, csb);
        csb.addService(MessageTransportRegistryService.class, reg);

        LinkSelectionProvision lsp = new LinkSelectionProvision();
        csb.addService(LinkSelectionProvisionService.class, lsp);

        SocketControlProvision scp = new SocketControlProvision();
        csb.addService(SocketControlProvisionService.class, scp);
        // SocketFactory has no access to services, so set it manually
        // in a static.
        SocketFactory.configureProvider(csb);
    }

    // components specified in the config files
    private List<Object> readConfig() {
        ComponentDescriptions descs;
        if (loadState instanceof ComponentDescriptions) {
            // rehydration
            descs = (ComponentDescriptions) loadState;
            loadState = null;
        } else {
            // read config
            ServiceBroker csb = getChildServiceBroker();
            ComponentInitializerService cis =
                    csb.getService(this, ComponentInitializerService.class, null);
            try {
                String cp = specifyContainmentPoint();
                descs = new ComponentDescriptions(cis.getComponentDescriptions(id, cp));
            } catch (ComponentInitializerService.InitializerException cise) {
                if (loggingService.isInfoEnabled()) {
                    loggingService.info("\nUnable to add " + id + "'s plugins ", cise);
                }
                descs = null;
            } finally {
                csb.releaseService(this, ComponentInitializerService.class, cis);
            }
        }

        // flatten
        List<Object> l = new ArrayList<Object>();
        if (descs != null) {
            // ubug 13451:
            @SuppressWarnings("unchecked") // util is not genericized
            List<Object> internal = 
                descs.selectComponentDescriptions(ComponentDescription.PRIORITY_INTERNAL);
            l.addAll(internal);
            
            // typical Aspects/LinkProtocols/etc
            @SuppressWarnings("unchecked") // util is not genericized
            List<Object> component = 
                descs.selectComponentDescriptions(ComponentDescription.PRIORITY_COMPONENT);
            l.addAll(component);
        }
        return l;
    }

    // components that must be loaded after the config's components
    private void afterConfig() {
        ServiceBroker csb = getChildServiceBroker();

        aspectSupport = csb.getService(this, AspectSupport.class, null);

        // The rest of the services depend on aspects.
        createNameSupport(id);
        createFactories();

        NodeControlService ncs = csb.getService(this, NodeControlService.class, null);

        ServiceBroker rootsb = ncs.getRootServiceBroker();
        rootsb.addService(MessageTransportService.class, this);
        rootsb.addService(MessageStatisticsService.class, this);
        rootsb.addService(MessageWatcherService.class, this);
        rootsb.addService(AgentStatusService.class, this);
    }

    private void createNameSupport(String id) {
        ServiceBroker csb = getChildServiceBroker();
        NameSupportImpl impl = new NameSupportImpl(id, csb);
        csb.addService(NameSupport.class, impl);
    }

    private void createFactories() {
        ServiceBroker csb = getChildServiceBroker();

        msgFactory = MessageStreamsFactory.makeFactory();
        add(msgFactory);

        ReceiveLinkFactory receiveLinkFactory = new ReceiveLinkFactory();
        add(receiveLinkFactory);
        csb.addService(ReceiveLinkProviderService.class, receiveLinkFactory);

        LinkSelectionPolicyServiceProvider lspsp =
                new LinkSelectionPolicyServiceProvider(csb, this);
        csb.addService(LinkSelectionPolicy.class, lspsp);

        DestinationQueueFactory destQFactory = new DestinationQueueFactory(this);
        add(destQFactory);
        csb.addService(DestinationQueueProviderService.class, destQFactory);
        csb.addService(DestinationQueueMonitorService.class, destQFactory);

        // Singletons, though produced by factories.
        MessageDelivererFactory delivererFactory = new MessageDelivererFactory(id);
        add(delivererFactory);
        csb.addService(MessageDeliverer.class, delivererFactory);

        RouterFactory routerFactory = new RouterFactory();
        add(routerFactory);
        csb.addService(Router.class, routerFactory);

        SendQueueFactory sendQFactory = new SendQueueFactory(id, this);
        add(sendQFactory);
        csb.addService(SendQueueProviderService.class, sendQFactory);

        // load LinkProtocols
        new LinkProtocolFactory(this, csb);
    }

    private Object findOrMakeProxy(Object requestor) {
        MessageTransportClient client = (MessageTransportClient) requestor;
        MessageAddress addr = client.getMessageAddress();
        long incarnation = client.getIncarnationNumber();
        MessageTransportServiceProxy proxy = proxies.get(addr);
        if (proxy != null && proxy.getIncarnationNumber() == incarnation) {
            return proxy;
        }

        // Make SendLink and attach aspect delegates
        SendLink link = new SendLinkImpl(addr, incarnation, getChildServiceBroker());
        Class<SendLink> c = SendLink.class;
        link = aspectSupport.attachAspects(link, c);

        // Make proxy
        proxy = new MessageTransportServiceProxy(client, link);
        proxies.put(addr, proxy);
        if (loggingService.isDebugEnabled()) {
            loggingService.debug("Created MessageTransportServiceProxy for " + requestor
                    + " with address " + client.getMessageAddress());
        }
        return proxy;

    }

    // ServiceProvider

    public Object getService(ServiceBroker sb, Object requestor, Class<?> serviceClass) {
        if (serviceClass == MessageTransportService.class) {
            if (requestor instanceof MessageTransportClient) {
                return findOrMakeProxy(requestor);
            } else {
                throw new IllegalArgumentException("Requestor is not a MessageTransportClient");
            }
        } 
        if (serviceClass == MessageStatisticsService.class) {
            return aspectSupport.findAspect(StatisticsAspect.class);
        } else if (serviceClass == MessageWatcherService.class) {
            WatcherAspect watcherAspect = aspectSupport.findAspect(WatcherAspect.class);
            return new MessageWatcherServiceImpl(watcherAspect);
        } else if (serviceClass == AgentStatusService.class) {
            return aspectSupport.findAspect(AgentStatusAspect.class);
        } else {
            return null;
        }
    }

    public void releaseService(ServiceBroker sb,
                               Object requestor,
                               Class<?> serviceClass,
                               Object service) {
        if (serviceClass == MessageTransportService.class) {
            if (requestor instanceof MessageTransportClient) {
                MessageTransportClient client = (MessageTransportClient) requestor;
                MessageAddress addr = client.getMessageAddress();
                MessageTransportService svc = proxies.get(addr);
                MessageTransportServiceProxy proxy = proxies.get(addr);
                if (svc != service) {
                    return; // ???
                }
                proxies.remove(addr);
                proxy.release();
            }
        } else if (serviceClass == MessageStatisticsService.class) {
            // The only resource used here is the StatisticsAspect,
            // which stays around.
        } else if (serviceClass == AgentStatusService.class) {
            // The only resource used here is the aspect, which stays
            // around.
        } else if (serviceClass == MessageWatcherService.class) {
            if (service instanceof MessageWatcherServiceImpl) {
                ((MessageWatcherServiceImpl) service).release();
            }
        }
    }

    /**
     * @see org.cougaar.core.component.ContainerSupport#unload()
     */
    public void unload() {
        super.unload();
        aspectSupportImpl.unload();

        msgFactory.releaseFactory();
    }
}
