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
import org.cougaar.core.society.NameSupport;
import org.cougaar.core.society.NewNameSupport;
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

    // Factories
    private MessageTransportFactory transportFactory;
    private SendQueueFactory sendQFactory;
    private ReceiveQueueFactory recvQFactory;
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
    private ReceiveQueue recvQ;
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
	String property = "org.cougaar.message.transport.aspects";
	String classes = System.getProperty(property);

	if (classes == null) return;

	StringTokenizer tokenizer = new StringTokenizer(classes, ",");
	while (tokenizer.hasMoreElements()) {
	    String classname = tokenizer.nextToken();
	    try {
		Class aspectClass = Class.forName(classname);
		MessageTransportAspect aspect = 
		    (MessageTransportAspect) aspectClass.newInstance();
		aspects.add(aspect);
		aspects_table.put(classname, aspect);
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
        return new NewNameSupport(id, (NamingService)
                                  sb.getService(this, NamingService.class, null));
    }

    public void initialize() {

        nameSupport = createNameSupport(id);
	registry = MessageTransportRegistry.makeRegistry(id, this);

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

	transportFactory.setRecvQ(recvQ);
	// force transports to be created here
	transportFactory.getTransports();
        super.initialize();
    }

    private void getSelectionPolicy() {
	String policy_classname = 
	    System.getProperty("org.cougaar.message.transport.policy");
	if (policy_classname == null) {
	    selectionPolicy = new MinCostLinkSelectionPolicy();
	} else {
	    try {
		Class policy_class = Class.forName(policy_classname);
		selectionPolicy = (LinkSelectionPolicy) policy_class.newInstance();
		System.out.println("Created " +  policy_classname);
	    } catch (Exception ex) {
		ex.printStackTrace();
		selectionPolicy = new MinCostLinkSelectionPolicy();
	    }
	}	       
    }



    private void wireComponents(String id) {
	getSelectionPolicy();
	recvQFactory = new ReceiveQueueFactory(registry, aspects);
	recvQ = recvQFactory.getReceiveQueue(id+"/InQ");


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



    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	if (serviceClass == MessageTransportService.class) {
	    if (requestor instanceof MessageTransportClient) {
		Object proxy = proxies.get(requestor);
		if (proxy == null) {
		    proxy = new MessageTransportServiceProxy(registry, sendQ);
		    proxy = AspectFactory.attachAspects(aspects, proxy, 
							MessageTransportService.class,
							null);
		    proxies.put(requestor, proxy);
		    if (Debug.DEBUG_TRANSPORT)
			System.out.println("======= Created MessageTransportServiceProxy for "
					   +  requestor);
		}
		return proxy;
	    } else {
		return null;
	    }
	} else if (serviceClass == MessageStatisticsService.class) {
	    StatisticsAspect aspect = 
		(StatisticsAspect) findAspect("org.cougaar.core.mts.StatisticsAspect");
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
    
