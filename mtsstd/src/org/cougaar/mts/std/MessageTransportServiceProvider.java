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


import org.cougaar.core.component.BinderFactory;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.ContainerAPI;
import org.cougaar.core.component.PropagatingServiceBroker;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.StateObject;
import org.cougaar.core.service.MessageStatisticsService;
import org.cougaar.core.service.MessageTransportService;
import org.cougaar.core.service.MessageWatcherService;
import org.cougaar.core.node.Node;

import java.util.ArrayList;
import java.util.HashMap;


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

    // MTS address
    private MessageAddress address;

    // Services we use (more than once)
    private AspectSupport aspectSupport;

    // Hang on to these because they implement services we provide.
    private WatcherAspect watcherAspect;
    private AgentStatusAspect agentStatusAspect;

    // Implicit singleton -- one sendQ for all senders.
    private SendQueue sendQ;

    private String id;
    private HashMap proxies;




    public MessageTransportServiceProvider(String id) {
        this.id = id;
	proxies = new HashMap();
	BinderFactory bf = new MTSBinderFactory();
	if (!attachBinderFactory(bf)) {
	    throw new RuntimeException("Failed to load the BinderFactory in MessageTransportServiceProvider");
	}
    }
 

    
    // The MTS itself as a client
    public void receiveMessage(Message message) {
	System.err.println("# MTS received unwanted message: " + message);
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


    private void createAspectSupport() {
        ServiceBroker sb = getServiceBroker();
        if (sb == null) throw new RuntimeException("No service broker");

	AspectSupportImpl impl = new AspectSupportImpl(this);
	sb.addService(AspectSupport.class, impl);
	aspectSupport = 
	    (AspectSupport) sb.getService(this, AspectSupport.class, null);

	// Do the standard set first, since they're assumed to be more
	// generic than the user-specified set.

	// For the MessageWatcher service
	watcherAspect =  new WatcherAspect();
	aspectSupport.addAspect(watcherAspect);

	// Keep track of Agent state
	agentStatusAspect =  new AgentStatusAspect();
	aspectSupport.addAspect(agentStatusAspect);

	// Handling multicast messages
	aspectSupport.addAspect(new MulticastAspect());

	// Handling flushMessage();
	aspectSupport.addAspect(new FlushAspect());

        // Traffic Masking Generator
        // aspectSupport.addAspect(new TrafficMaskingGeneratorAspect());
        

	// Now read user-supplied aspects
	aspectSupport.readAspects();
    }

    private void createFactories() {
	ServiceBroker sb = getServiceBroker();

	ReceiveLinkFactory receiveLinkFactory = new ReceiveLinkFactory(sb);
	sb.addService(ReceiveLinkProviderService.class, receiveLinkFactory);

	LinkProtocolFactory protocolFactory = 
	    new LinkProtocolFactory(this, sb);

	MessageDelivererFactory delivererFactory = 
	    new MessageDelivererFactory(sb, id);
	sb.addService(MessageDeliverer.class, delivererFactory);

	LinkSelectionPolicyServiceProvider lspsp =
	    new LinkSelectionPolicyServiceProvider();
	sb.addService(LinkSelectionPolicy.class, lspsp);

	
	DestinationQueueFactory	destQFactory = 
	    new DestinationQueueFactory(sb);
	sb.addService(DestinationQueueProviderService.class, destQFactory);

	// Router and SendQueue are singletons, though produced by
	// factories.
	RouterFactory routerFactory =  new RouterFactory(sb);
	sb.addService(Router.class, routerFactory);

	SendQueueFactory sendQFactory = new SendQueueFactory(sb);
	sendQ = sendQFactory.getSendQueue(id+"/OutQ");


	// force transports to be created here
	protocolFactory.loadProtocols();
    }



    private Object findOrMakeProxy(Object requestor) {
	MessageTransportClient client = (MessageTransportClient) requestor;
	MessageAddress addr = client.getMessageAddress();
	Object proxy = proxies.get(addr);
	if (proxy != null) return proxy;
	
	// Make SendLink and attach aspect delegates
	SendLink link = new SendLinkImpl(sendQ, addr, getServiceBroker());
	Class c = SendLink.class;
	Object raw = aspectSupport.attachAspects(link, c);
	link = (SendLink) raw;

	// Make proxy
	proxy = new MessageTransportServiceProxy(client, link);
	proxies.put(addr, proxy);
	if (Debug.debug(DebugFlags.SERVICE))
	    System.out.println("=== Created MessageTransportServiceProxy for " 
			       +requestor+
			       " with address "
			       +client.getMessageAddress());
	return proxy;

    }


    public void initialize() {
        super.initialize();
        ServiceBroker sb = getServiceBroker(); // is this mine or Node's ?
	
	// Later this will be replaced by a Node-level service
	ThreadServiceProvider tsp = new ThreadServiceProvider();
	sb.addService(ThreadService.class, tsp);

	MessageTransportRegistry reg = new MessageTransportRegistry(id, sb);
	sb.addService(MessageTransportRegistryService.class, reg);

	MessageTransportRegistryService registry = 
	    (MessageTransportRegistryService)
	    sb.getService(this, MessageTransportRegistryService.class,  null);

	createAspectSupport();
        createNameSupport(id);
	createFactories();

	// The MTS itself as a client
	NameSupport nameSupport = 
	    (NameSupport) 
	    sb.getService(this, NameSupport.class, null);
	address = nameSupport.getNodeMessageAddress();

	// MessageTransportService isn't available at this point, so
	// do the calls manually (ugh).
	MessageTransportService svc = 
	    (MessageTransportService) findOrMakeProxy(this);
	svc.registerClient(this);
	registry.registerMTS(this);
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
		return null;
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
		svc.unregisterClient(client);
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
	return "Node.MessageTransport";
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



}

    
