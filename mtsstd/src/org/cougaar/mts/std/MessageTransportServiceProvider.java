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
import org.cougaar.core.service.NamingService;
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
 * @property org.cougaar.message.transport.policy Sets the message
 * transport policy to instance o the specified class.
 */

public class MessageTransportServiceProvider 
    extends ContainerSupport
    implements ContainerAPI, ServiceProvider
{

    private final static String POLICY_PROPERTY =
	"org.cougaar.message.transport.policy";

    // Some special aspect classes
    private final static String STATISTICS_ASPECT = 
	"org.cougaar.core.mts.StatisticsAspect";

    // Factories
    private LinkProtocolFactory protocolFactory;
    private SendQueueFactory sendQFactory;
    private MessageDelivererFactory delivererFactory;
    private DestinationQueueFactory destQFactory;
    private RouterFactory routerFactory;
    private ReceiveLinkFactory receiveLinkFactory;


    // Singletons
    private NameSupport nameSupport;
    private MessageTransportRegistry registry;
    private AspectSupport aspectSupport;

    // Assuming one policy per provider, but could also be one per
    // sender
    private Router router;
    private SendQueue sendQ;
    private MessageDeliverer deliverer;
    private WatcherAspect watcherAspect;
    private AgentStatusAspect agentStatusAspect;

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
 

    private void createNameSupport(String id) {
        ServiceBroker sb = getServiceBroker();
        if (sb == null) throw new RuntimeException("No service broker");
	Object svc = sb.getService(this, NamingService.class, null);
        Object ns = NameSupportImpl.makeInstance(id, (NamingService) svc);
	ns = aspectSupport.attachAspects(ns, NameSupport.class);
	nameSupport = (NameSupport) ns;
	registry.setNameSupport(nameSupport);
    }


    private void createAspectSupport() {
	aspectSupport = AspectSupportImpl.makeInstance(this);

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
	protocolFactory = new LinkProtocolFactory(id, this,
						  registry, nameSupport, 
						  aspectSupport);
	receiveLinkFactory = new ReceiveLinkFactory(aspectSupport);

	registry.setReceiveLinkFactory(receiveLinkFactory);

	delivererFactory = new MessageDelivererFactory(registry,aspectSupport);
	deliverer = delivererFactory.getMessageDeliverer(id+"/Deliverer");

	LinkSelectionPolicy selectionPolicy = createSelectionPolicy();

	
	destQFactory = 
	    new DestinationQueueFactory(registry, 
					protocolFactory,
					selectionPolicy,
					aspectSupport);
	routerFactory =
	    new RouterFactory(registry, destQFactory, aspectSupport);

	router = routerFactory.getRouter();

	sendQFactory = new SendQueueFactory(registry, aspectSupport);
	sendQ = sendQFactory.getSendQueue(id+"/OutQ", router);

	protocolFactory.setDeliverer(deliverer);
	// force transports to be created here
	protocolFactory.loadProtocols();
    }

    private LinkSelectionPolicy createSelectionPolicy() {
	String policy_classname = System.getProperty(POLICY_PROPERTY);
	if (policy_classname == null) {
	    return new MinCostLinkSelectionPolicy();
	} else {
	    try {
		Class policy_class = Class.forName(policy_classname);
		LinkSelectionPolicy selectionPolicy = 
		    (LinkSelectionPolicy) policy_class.newInstance();
		if (Debug.debug(DebugFlags.POLICY))
		    System.out.println("% Created " +  policy_classname);

		return selectionPolicy;
	    } catch (Exception ex) {
		ex.printStackTrace();
		return new MinCostLinkSelectionPolicy();
	    }
	}	       
    }



    private Object findOrMakeProxy(Object requestor) {
	MessageTransportClient client = (MessageTransportClient) requestor;
	MessageAddress addr = client.getMessageAddress();
	Object proxy = proxies.get(addr);
	if (proxy != null) return proxy;
	
	// Make SendLink and attach aspect delegates
	SendLink link = new SendLinkImpl(sendQ, addr);
	Class c = SendLink.class;
	Object raw = aspectSupport.attachAspects(link, c);
	link = (SendLink) raw;

	// Make proxy
	proxy = new MessageTransportServiceProxy(client, link);
	proxies.put(addr, proxy);
	if (Debug.debug(DebugFlags.SERVICE))
	    System.out.println("=== Created MessageTransportServiceProxy for " 
			       +  requestor);
	return proxy;

    }



    public void initialize() {
	registry = MessageTransportRegistry.makeRegistry(id, this);
	createAspectSupport();
        createNameSupport(id);
	createFactories();
	registry.registerMTS();
        super.initialize();
    }


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

    // may need to override ComponentFactory specifyComponentFactory()

    protected String specifyContainmentPoint() {
	return "messagetransportservice.messagetransport";
    }

    public void requestStop() {}

    public final void setBindingSite(BindingSite bs) {
        super.setBindingSite(bs);
        setChildServiceBroker(new PropagatingServiceBroker(bs));
    }


    public ContainerAPI getContainerProxy() {
	return this;
    }



}

    
