/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.mts;

import java.util.ArrayList;
import java.util.HashMap;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.ContainerAPI;
import org.cougaar.core.component.PropagatingServiceBroker;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.naming.NamingService;
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.society.Node;


/**
 * The underlying implementation class for the
 * MessageTransportService.  It consists almost exclusively of
 * factories, each of which is described elsewhere.  The only
 * interesting local functions are those required for
 * ServiceBrokers. */

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
    private LinkSenderFactory linkSenderFactory;
    private RouterFactory routerFactory;
    private ReceiveLinkFactory receiveLinkFactory;


    // Singletons
    private NameSupport nameSupport;
    private MessageTransportRegistry registry;
    private AspectSupport aspectSupport;

    // Assuming one policy per provider, but could also be one per
    // sender
    private LinkSelectionPolicy selectionPolicy;
    private Router router;
    private SendQueue sendQ;
    private MessageDeliverer deliverer;
    private WatcherAspect watcherAspect;

    private String id;
    private HashMap rawProxies;
    private HashMap proxies;



    public MessageTransportServiceProvider(String id) {
        this.id = id;
	proxies = new HashMap();
	rawProxies = new HashMap();

    }
 

    private NameSupport createNameSupport(String id) {
        ServiceBroker sb = getServiceBroker();
        if (sb == null) throw new RuntimeException("No service broker");
	Object svc = sb.getService(this, NamingService.class, null);
        Object ns = NameSupportImpl.makeInstance(id, (NamingService) svc);
	ns = aspectSupport.attachAspects(ns, NameSupport.class, null);
	return (NameSupport) ns;
    }

    public void initialize() {
	ServiceBroker sb = getServiceBroker();
 	
	registry = MessageTransportRegistry.makeRegistry(id, this);

	aspectSupport = new AspectSupportImpl(sb);

        nameSupport = createNameSupport(id);
	registry.setNameSupport(nameSupport);

	//Watcher Aspect is special because the MTServicer interface
	//needs it.  So we have to make the Watcher Aspect all the
	//time.
	watcherAspect =  new WatcherAspect();
	aspectSupport.addAspect(watcherAspect);

	// Multicast Aspect is always required.
	aspectSupport.addAspect(new MulticastAspect());

	protocolFactory = 
	    new LinkProtocolFactory(id, registry, nameSupport, aspectSupport);
	receiveLinkFactory = new ReceiveLinkFactory(registry, aspectSupport);

	registry.setReceiveLinkFactory(receiveLinkFactory);
	registry.setProtocolFactory(protocolFactory);

	wireComponents(id);

	protocolFactory.setDeliverer(deliverer);
	// force transports to be created here
	protocolFactory.getProtocols();
        super.initialize();
    }

    private void getSelectionPolicy() {
	String policy_classname = System.getProperty(POLICY_PROPERTY);
	if (policy_classname == null) {
	    selectionPolicy = new MinCostLinkSelectionPolicy();
	} else {
	    try {
		Class policy_class = Class.forName(policy_classname);
		selectionPolicy = 
		    (LinkSelectionPolicy) policy_class.newInstance();
		System.out.println("Created " +  policy_classname);
	    } catch (Exception ex) {
		ex.printStackTrace();
		selectionPolicy = new MinCostLinkSelectionPolicy();
	    }
	}	       
    }



    private void wireComponents(String id) {
	getSelectionPolicy();
	delivererFactory = new MessageDelivererFactory(registry,aspectSupport);
	deliverer = delivererFactory.getMessageDeliverer(id+"/Deliverer");


	linkSenderFactory =
	    new LinkSenderFactory(registry, protocolFactory, selectionPolicy);
	
	destQFactory = 
	    new DestinationQueueFactory(registry, linkSenderFactory, aspectSupport);
	routerFactory =
	    new RouterFactory(registry, destQFactory, aspectSupport);

	router = routerFactory.getRouter();

	sendQFactory = new SendQueueFactory(registry, aspectSupport);
	sendQ = sendQFactory.getSendQueue(id+"/OutQ", router);

    }



    private Object findOrMakeProxy(Object requestor) {
	MessageTransportClient client = (MessageTransportClient) requestor;
	MessageAddress addr = client.getMessageAddress();
	Object proxy = proxies.get(addr);
	if (proxy != null) return proxy;
	proxy = new MessageTransportServiceProxy(client,registry,sendQ);
	rawProxies.put(addr, proxy);
	proxy = aspectSupport.attachAspects(proxy, 
					    MessageTransportService.class,
					    null);
	proxies.put(addr, proxy);
	if (Debug.debugService())
	    System.out.println("=== Created MessageTransportServiceProxy for " 
			       +  requestor);
	return proxy;

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
		    (MessageTransportServiceProxy) rawProxies.get(addr);
		if (svc != service) return; // ???
		proxies.remove(addr);
		rawProxies.remove(addr);
		svc.unregisterClient(client);
		proxy.release();
	    }
	} else if (serviceClass == MessageStatisticsService.class) {
	    // The only resource used here is the StatisticsAspect,
	    // which stays around.  
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

    protected Class specifyChildBindingSite() {
        return null;
    }

    public ContainerAPI getContainerProxy() {
	return this;
    }

}
    
