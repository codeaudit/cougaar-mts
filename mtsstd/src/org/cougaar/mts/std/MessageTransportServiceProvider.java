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
import java.util.Iterator;
import java.util.StringTokenizer;

import org.cougaar.core.component.*;
import org.cougaar.core.naming.NamingService;
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.society.Node;


/**
 * The underlying implementation class for the
 * MessageTransportService.  It consists almost exclusively of
 * factories, each of which is described elsewhere.  The only
 * interesting local functions are those required for ServiceBrokers,
 * and a method to create the aspects from the
 * org.cougaar.message.transport.aspects property. */

public class MessageTransportServiceProvider 
    extends ContainerSupport
    implements ContainerAPI, ServiceProvider, Debug
{

    private final static String ASPECTS_PROPERTY = 
	"org.cougaar.message.transport.aspects";
    private final static String POLICY_PROPERTY =
	"org.cougaar.message.transport.policy";
    private final static String STATISTICS_ASPECT = 
	"org.cougaar.core.mts.StatisticsAspect";

    // Factories
    private MessageTransportFactory transportFactory;
    private SendQueueFactory sendQFactory;
    private MessageDelivererFactory delivererFactory;
    private DestinationQueueFactory destQFactory;
    private LinkSenderFactory linkSenderFactory;
    private RouterFactory routerFactory;
    private ReceiveLinkFactory receiveLinkFactory;


    // Singletons
    private NameSupport nameSupport;
    private MessageTransportRegistry registry;
    // Assuming one policy per provider, but could also be one per
    // sender
    private LinkSelectionPolicy selectionPolicy;
    private Router router;
    private SendQueue sendQ;
    private MessageDeliverer deliverer;
    private WatcherAspect watcherAspect;

    private String id;
    private HashMap proxies;


    private static ArrayList aspects;
    private static HashMap aspects_table;
    
    public static MessageTransportAspect findAspect(String classname) {
	return (MessageTransportAspect) aspects_table.get(classname);
    }


    public MessageTransportServiceProvider(String id) {
        this.id = id;
	proxies = new HashMap();
	aspects = new ArrayList();
	aspects_table = new HashMap();

    }


    private void readAspects() {
	String classes = System.getProperty(ASPECTS_PROPERTY);

	if (classes == null) return;

        ServiceBroker sb = getServiceBroker();
	StringTokenizer tokenizer = new StringTokenizer(classes, ",");
	while (tokenizer.hasMoreElements()) {
	    String classname = tokenizer.nextToken();
	    try {
		Class aspectClass = Class.forName(classname);
		MessageTransportAspect aspect = 
		    (MessageTransportAspect) aspectClass.newInstance();
		aspects.add(aspect);
		aspects_table.put(classname, aspect);
		
		aspect.setServiceBroker(sb);
	    }
	    catch (Exception ex) {
		ex.printStackTrace();
		// System.err.println(ex);
	    }
	}
    }

    private NameSupport createNameSupport(String id) {
        ServiceBroker sb = getServiceBroker();
        if (sb == null) throw new RuntimeException("No service broker");
	Object svc = sb.getService(this, NamingService.class, null);
        return new NameSupportImpl(id, (NamingService) svc);
    }

    public void initialize() {

        nameSupport = createNameSupport(id);
	registry = MessageTransportRegistry.makeRegistry(id, nameSupport,this);

	readAspects();

	//Watcher Aspect is special because the MTServicer interace
	//needs it.  So we have to make the Watcher Aspect all the
	//time.
	watcherAspect =  new WatcherAspect();
	if (aspects == null) aspects = new ArrayList();
	aspects.add(watcherAspect);

	transportFactory = 
	    new MessageTransportFactory(id, registry, nameSupport, aspects);
	receiveLinkFactory = new ReceiveLinkFactory(registry,
						    aspects);

	registry.setReceiveLinkFactory(receiveLinkFactory);
	registry.setTransportFactory(transportFactory);

	wireComponents(id);

	transportFactory.setDeliverer(deliverer);
	// force transports to be created here
	transportFactory.getTransports();
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
	delivererFactory = new MessageDelivererFactory(registry, aspects);
	deliverer = delivererFactory.getMessageDeliverer(id+"/Deliverer");


	linkSenderFactory =
	    new LinkSenderFactory(registry, transportFactory, selectionPolicy);
	
	destQFactory = 
	    new DestinationQueueFactory(registry, 
					transportFactory, 
					linkSenderFactory,
					aspects);
	routerFactory =
	    new RouterFactory(registry, destQFactory, aspects);

	router = routerFactory.getRouter();

	sendQFactory = new SendQueueFactory(registry, aspects);
	sendQ = sendQFactory.getSendQueue(id+"/OutQ", router);

    }



    private Object findOrMakeProxy(Object requestor) {
	MessageTransportClient client = (MessageTransportClient) requestor;
	MessageAddress addr = client.getMessageAddress();
	Object proxy = proxies.get(addr);
	if (proxy != null) return proxy;
	proxy = new MessageTransportServiceProxy(client,registry,sendQ);
	proxy = AspectFactory.attachAspects(aspects, proxy, 
					    MessageTransportService.class,
					    null);
	proxies.put(addr, proxy);
	if (Debug.DEBUG_TRANSPORT)
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
		(StatisticsAspect) findAspect(STATISTICS_ASPECT);
	    return aspect;
	} else if (serviceClass == MessageWatcherService.class) {
	    return watcherAspect;
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
		Object proxy = proxies.get(addr);
		if (proxy != service) return; // ???
		proxies.remove(addr);
		MessageTransportService svc = (MessageTransportService) proxy;
		svc.unregisterClient(client);
	    }
	} else if (serviceClass == MessageStatisticsService.class) {
	    // TO BE DONE
	} else if (serviceClass == MessageWatcherService.class) {
	    // TO BE DONE
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
    
