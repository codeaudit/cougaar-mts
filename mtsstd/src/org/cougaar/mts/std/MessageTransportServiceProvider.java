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
    implements ContainerAPI, ServiceProvider, Debug, MessageTransportCutpoints
{

    // Factories
    protected MessageTransportFactory transportFactory;
    protected SendQueueFactory sendQFactory;
    protected ReceiveQueueFactory recvQFactory;
    protected DestinationQueueFactory destQFactory;
    protected LinkSenderFactory linkSenderFactory;
    protected RouterFactory routerFactory;
    protected ReceiveLinkFactory receiveLinkFactory;


    // Singletons
    protected NameSupport nameSupport;
    protected MessageTransportRegistry registry;
    protected Router router;
    protected SendQueue sendQ;
    protected ReceiveQueue recvQ;

    private String id;
    private HashMap proxies;

    private static ArrayList aspects;
    private static HashMap aspects_table;
    
    public static MessageTransportAspect findAspect(String classname) {
	return (MessageTransportAspect) aspects_table.get(classname);
    }

    private void readAspects() {
	String property = "org.cougaar.message.transport.aspects";
	String classes = System.getProperty(property);
	if (classes == null) return;

	aspects = new ArrayList();
	aspects_table = new HashMap();
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

    public MessageTransportServiceProvider(String id) {
        this.id = id;
	proxies = new HashMap();
    }

    public void initialize() {

        nameSupport = createNameSupport(id);
	registry = MessageTransportRegistry.makeRegistry(id, this);

	readAspects();

	//Watcher Aspect is special because the MTServicer interace
	//needs it.  So we have to make the Watcher Aspect all the
	//time.
	WatcherAspect watcherAspect =  new WatcherAspect();
	registry.setWatcherManager(watcherAspect);
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

    protected void wireComponents(String id) {
	recvQFactory = new ReceiveQueueFactory(registry, aspects);
	recvQ = recvQFactory.getReceiveQueue(id+"/InQ");


	linkSenderFactory =
	    new LinkSenderFactory(registry, transportFactory);
	
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








    // Hmm, this is copied from AspectFactory, since we can't extend
    // it. Ugh.
    private Object attachAspects(Object delegate, int cutpoint)
    {
	if (aspects != null) {
	    Iterator itr = aspects.iterator();
	    while (itr.hasNext()) {
		MessageTransportAspect aspect = 
		    (MessageTransportAspect) itr.next();
		Object candidate = aspect.getDelegate(delegate, cutpoint);
		if (candidate != null) delegate = candidate;
		if (DEBUG_TRANSPORT) System.out.println("======> " + delegate);
	    }
	}
	return delegate;
    }


    private boolean validateRequestor(Object requestor, 
				      Class serviceClass) 
    {
	return requestor instanceof Node &&
	    serviceClass == MessageTransportService.class;
    }

    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	if (validateRequestor(requestor, serviceClass)) {
	    Object proxy = proxies.get(requestor);
	    if (proxy == null) {
		proxy = new MessageTransportServiceProxy(registry, sendQ);
		proxy = attachAspects(proxy, ServiceProxy);
		proxies.put(requestor, proxy);
	    }
	    return proxy;
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
    
