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

import java.util.HashMap;
import org.cougaar.core.agent.Agent;
import org.cougaar.core.component.Binder;
import org.cougaar.core.component.BinderFactory;
import org.cougaar.core.component.BinderFactorySupport;
import org.cougaar.core.component.BinderSupport;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.Container;
import org.cougaar.core.component.ContainerAPI;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.PropagatingServiceBroker;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.StateObject;
import org.cougaar.core.node.ComponentInitializerService;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.MessageStatisticsService;
import org.cougaar.core.service.MessageTransportService;
import org.cougaar.core.service.MessageWatcherService;
import org.cougaar.core.thread.ThreadServiceProvider;

/**
 * The underlying implementation class for the
 * MessageTransportService.  It consists almost exclusively of
 * factories, each of which is described elsewhere.  The only
 * interesting local functions are those required for
 * ServiceBrokers.
 */



public final class MessageTransportServiceProvider 
    extends ContainerSupport
    implements ContainerAPI, ServiceProvider, MessageTransportClient, StateObject
{


    // Some special aspect classes
    private final static String STATISTICS_ASPECT = 
	"org.cougaar.core.mts.StatisticsAspect";

    private final static String NOT_A_CLIENT =
	"Requestor is not a MessageTransportClient";

    private final static String USE_NEW_FLUSH =
      "org.cougaar.message.transport.new_flush";

    // MTS address
    private MessageAddress address;

    // Services we use (more than once)
    private AspectSupport aspectSupport;
    private LoggingService loggingService;
    // Hang on to these because they implement services we provide.
    private WatcherAspect watcherAspect;
    private AgentStatusAspect agentStatusAspect;


    private String id;
    private HashMap proxies;




    public MessageTransportServiceProvider() {
	proxies = new HashMap();
	BinderFactory bf = new MTSBinderFactory();
	if (!attachBinderFactory(bf)) {
	    throw new RuntimeException("Failed to load the BinderFactory in MessageTransportServiceProvider");
	}
    }
 

    
    // The MTS itself as a client
    public void receiveMessage(Message message) {
	if (loggingService.isErrorEnabled()) {
	    MessageAddress target = message.getTarget();
	    if (!(target instanceof MulticastMessageAddress))
		loggingService.error("# MTS received unwanted message: " + 
				     message);
	}
    }

    public MessageAddress getMessageAddress() {
	return address;
    }




    private void createNameSupport(String id) {
        ServiceBroker sb = getServiceBroker();
        if (sb == null) throw new RuntimeException("No service broker");

	NameSupportImpl impl = new NameSupportImpl(id, sb);

	sb.addService(NameSupport.class, impl);
    }


    private void loadAspects() {
        ServiceBroker sb = getServiceBroker();
        if (sb == null) throw new RuntimeException("No service broker");

	aspectSupport = 
	    (AspectSupport) sb.getService(this, AspectSupport.class, null);
	aspectSupport.readAspects();
    }

    private void createFactories() {
	ServiceBroker sb = getServiceBroker();

	MessageStreamsFactory msgFactory = MessageStreamsFactory.makeFactory();
	add(msgFactory);

	ReceiveLinkFactory receiveLinkFactory = new ReceiveLinkFactory();
	add(receiveLinkFactory);
	sb.addService(ReceiveLinkProviderService.class, receiveLinkFactory);
	

	LinkSelectionPolicyServiceProvider lspsp =
	    new LinkSelectionPolicyServiceProvider(sb, this);
	sb.addService(LinkSelectionPolicy.class, lspsp);
	
	DestinationQueueFactory	destQFactory = 
	    new DestinationQueueFactory(this);
	add(destQFactory);
	sb.addService(DestinationQueueProviderService.class, destQFactory);

	//  Singletons, though produced by factories.
	MessageDelivererFactory delivererFactory = 
	    new MessageDelivererFactory(id);
	add(delivererFactory);
	sb.addService(MessageDeliverer.class, delivererFactory);

	RouterFactory routerFactory =  new RouterFactory();
	add(routerFactory);
	sb.addService(Router.class, routerFactory);

	SendQueueFactory sendQFactory = new SendQueueFactory(this, id);
	add(sendQFactory);
	sb.addService(SendQueue.class, sendQFactory);
	sb.addService(SendQueueImpl.class, sendQFactory);


	// load LinkProtocols
	new LinkProtocolFactory(this, sb);
    }


    private Object findOrMakeProxy(Object requestor) {
	MessageTransportClient client = (MessageTransportClient) requestor;
	MessageAddress addr = client.getMessageAddress();
	Object proxy = proxies.get(addr);
	if (proxy != null) return proxy;
	
	// Make SendLink and attach aspect delegates
	SendLink link = new SendLinkImpl(addr, getServiceBroker());
	Class c = SendLink.class;
	Object raw = aspectSupport.attachAspects(link, c);
	link = (SendLink) raw;

	// Make proxy
	proxy = new MessageTransportServiceProxy(client, link);
	proxies.put(addr, proxy);
	if (Debug.isDebugEnabled(loggingService, DebugFlags.SERVICE))
	    loggingService.debug("Created MessageTransportServiceProxy for " 
				      +requestor+
				      " with address "
				      +client.getMessageAddress());
	return proxy;

    }


    protected ComponentDescriptions findExternalComponentDescriptions() {
	ServiceBroker sb = getServiceBroker();
	ComponentInitializerService cis = (ComponentInitializerService) 
	    sb.getService(this, ComponentInitializerService.class, null);
	try {
	    String cp = specifyContainmentPoint();
 	    // Want only items _below_. Could filter (not doing so now)
	    return new ComponentDescriptions(cis.getComponentDescriptions(id,cp));
	} catch (ComponentInitializerService.InitializerException cise) {
	    if (loggingService.isInfoEnabled()) {
		loggingService.info("\nUnable to add "+id+"'s plugins ",cise);
	    }
	    return null;
	} finally {
	    sb.releaseService(this, ComponentInitializerService.class, cis);
	}
    }


    public void initialize() {
	super.initialize();
	ServiceBroker sb = getServiceBroker();
	NodeIdentificationService nis = (NodeIdentificationService)
	    sb.getService(this, NodeIdentificationService.class, null);
 	id = nis.getMessageAddress().toString();
	loggingService = 
	    (LoggingService) sb.getService(this, LoggingService.class, null);

	AttributedMessage.setLoggingService(loggingService);
	Debug.load(loggingService);
    }

    public void loadHighPriorityComponents() {
	super.loadHighPriorityComponents();

	ServiceBroker sb = getServiceBroker();

	AspectSupportImpl impl = new AspectSupportImpl(this, loggingService);
	sb.addService(AspectSupport.class, impl);

	// Do the standard set first, since they're assumed to be more
	// generic than the user-specified set.

	// For the MessageWatcher service
	watcherAspect =  new WatcherAspect();
	add(watcherAspect);

	// Keep track of Agent state
	agentStatusAspect =  new AgentStatusAspect();
	add(agentStatusAspect);

	// Handling multicast messages
	add(new MulticastAspect());

	// Handling flushMessage();
        if ("false".equalsIgnoreCase(
              System.getProperty(USE_NEW_FLUSH, "true")))
	    add(new FlushAspect());

	// could do this as a ComponentDescription
// 	ThreadServiceProvider tsp = new ThreadServiceProvider(sb, "MTS");
// 	tsp.provideServices(sb);

	// Could use a ComponentDescription
	ThreadServiceProvider tsp = new ThreadServiceProvider();
	tsp.setParameter("name=MTS");
	add(tsp);


	MessageTransportRegistry reg = new MessageTransportRegistry(id, sb);
	sb.addService(MessageTransportRegistryService.class, reg);

	LinkSelectionProvision lsp = new LinkSelectionProvision();
	sb.addService(LinkSelectionProvisionService.class, lsp);

   }

    // CSMART Aspects (INTERNAL priority) will load between
    // HighPriority and ComponentPriority.  CSMART LinkProtocols and
    // LinkSelectionPolicys (COMPONENT priority) will load in the
    // super.loadComponentPriorityComponents

    public void loadComponentPriorityComponents() {
	// CSMART LinkProtocols will be loaded in the super,
        super.loadComponentPriorityComponents();

	// backward compatibility for -D option
	loadAspects(); 

	// The rest of the services depend on aspects.
        createNameSupport(id);
	createFactories();

        ServiceBroker sb = getServiceBroker();

	NameSupport nameSupport = 
	    (NameSupport) 
	    sb.getService(this, NameSupport.class, null);
	address = nameSupport.getNodeMessageAddress();

	// MessageTransportService isn't available at this point, so
	// do the calls manually (ugh).
	MessageTransportRegistryService registry = 
	    (MessageTransportRegistryService)
	    sb.getService(this, MessageTransportRegistryService.class,  null);
	MessageTransportService svc = 
	    (MessageTransportService) findOrMakeProxy(this);
	svc.registerClient(this);
	registry.registerMTS(this);


	NodeControlService ncs = (NodeControlService)
	    sb.getService(this, NodeControlService.class, null);
	ServiceBroker rootsb = ncs.getRootServiceBroker();
	rootsb.addService(MessageTransportService.class, this);
	rootsb.addService(MessageStatisticsService.class, this);
	rootsb.addService(MessageWatcherService.class, this);
	rootsb.addService(AgentStatusService.class, this);
    }


    // ServiceProvider

    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	if (serviceClass == MessageTransportService.class) {
	    if (requestor instanceof MessageTransportClient) {
		return findOrMakeProxy(requestor);
	    } else {
		throw new IllegalArgumentException(NOT_A_CLIENT);
	    }
	} else if (serviceClass == MessageStatisticsService.class) {
	    StatisticsAspect aspect = 
		(StatisticsAspect) aspectSupport.findAspect(STATISTICS_ASPECT);
	    return aspect;
	} else if (serviceClass == MessageWatcherService.class) {
	    return new MessageWatcherServiceImpl(watcherAspect);
	} else if (serviceClass == AgentStatusService.class) {
	    return agentStatusAspect;
	} else {
	    return null;
	}
    }


    public void releaseService(ServiceBroker sb, 
			       Object requestor, 
			       Class serviceClass, 
			       Object service)
    {
	if (serviceClass == MessageTransportService.class) {
	    if (requestor instanceof MessageTransportClient) {
		MessageTransportClient client = 
		    (MessageTransportClient) requestor;
		MessageAddress addr = client.getMessageAddress();
		MessageTransportService svc = 
		    (MessageTransportService) proxies.get(addr);
		MessageTransportServiceProxy proxy =
		    (MessageTransportServiceProxy) proxies.get(addr);
		if (svc != service) return; // ???
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






    // Container


    // We're not using this yet but leave it in anyway.
    protected String specifyContainmentPoint() {
 	return Agent.INSERTION_POINT + ".MessageTransport";
    }

    public void requestStop() {}

    public final void setBindingSite(BindingSite bs) {
        super.setBindingSite(bs);
        setChildServiceBroker(new PropagatingServiceBroker(bs));
    }


    public ContainerAPI getContainerProxy() {
	return this;
    }


    // StateModel

    // Return a (serializable) snapshot that can be used to
    // reconstitute the state later.
    public Object getState() {
	// TBD
	return null;
    }

    // Reconstitute from the previously returned snapshot.
    public void setState(Object state) {
    }

    private static class MTSBinderFactory 
      extends BinderFactorySupport {
        public Binder getBinder(Object child) {
          return new MTSBinder(this, child);
        }
        private static class MTSBinder 
          extends BinderSupport
          implements BindingSite {
            public MTSBinder(BinderFactory bf, Object child) {
              super(bf, child);
            }
            protected final BindingSite getBinderProxy() {
              return this;
            }
          }
      }
}
