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

import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;


import java.io.PrintWriter;
import java.io.FileWriter;

/**
 * This is a very simple aspect which is mostly for demonstration
 * purposes.  It attaches aspect delegates to each interface, and each
 * such delegate prints a message to System.err.  This provides a
 * trivial trace of a message as it passes through the various stages
 * of the message transport subsystem.  */
public class TraceAspect 
    extends StandardAspect
{

    // logging support
    private PrintWriter logStream = null;
    private MessageTransportRegistry registry;

    public TraceAspect() {
	registry =  MessageTransportRegistry.getRegistry();
    }


    private PrintWriter getLog() {
	if (logStream == null) {
	    try {
		String id = registry.getIdentifier();
		logStream = new PrintWriter(new FileWriter(id+".cml"), true);
	    } catch (Exception e) {
		e.printStackTrace();
		System.err.println("Logging required but not possible - exiting");
		System.exit(1);
	    }
	}
	return logStream;
    }


    protected void log(String key, String info) {
	String id = registry.getIdentifier();
	getLog().println(id+"\t"+System.currentTimeMillis()+"\t"+key+"\t"+info);
    }

    public Object getDelegate(Object delegate, Class type) {
	if (type == MessageTransportService.class) {
	    return new ServiceProxyDelegate((MessageTransportService) delegate);
	} else if (type == SendQueue.class) {
	    return new SendQueueDelegate((SendQueue) delegate);
	} else if (type == Router.class) {
	    return new RouterDelegate((Router) delegate);
	} else if (type == DestinationQueue.class) {
	    return new DestinationQueueDelegate((DestinationQueue) delegate);
	} else if (type == DestinationLink.class) {
	    return new DestinationLinkDelegate((DestinationLink) delegate);
	} else if (type == MessageDeliverer.class) {
	    return new MessageDelivererDelegate((MessageDeliverer) delegate);
	} else if (type == ReceiveLink.class) {
	    return new ReceiveLinkDelegate((ReceiveLink) delegate);
	} else {
	    return null;
	}
    }


    public class ServiceProxyDelegate implements MessageTransportService
    {
	private MessageTransportService server;
	
	public ServiceProxyDelegate (MessageTransportService server) {
	    this.server = server;
	}

	public void sendMessage(Message message) {
	    server.sendMessage(message);
	}

	public void registerClient(MessageTransportClient client) {
	    server.registerClient(client);
	}

	public void unregisterClient(MessageTransportClient client) {
	    server.unregisterClient(client);
	}

	public java.util.ArrayList flushMessages() {
	    return server.flushMessages();
	}

	public String getIdentifier() {
	    return server.getIdentifier();
	}

	public boolean addressKnown(MessageAddress address) {
	    return server.addressKnown(address);
	}
	
    }


    public class SendQueueDelegate implements SendQueue
    {
	private SendQueue server;
	
	public SendQueueDelegate (SendQueue server)
	{
	    this.server = server;
	}
	
	public void sendMessage(Message message) {
	    log("SendQueue", message.toString()+" ("+size()+")");
	    server.sendMessage(message);
	}
	public int size() {
	    return server.size();
	}
	
	public boolean matches(String name){
	    return server.matches(name);
	}
    }



    public class RouterDelegate implements Router
    {
	private Router server;
	
	public RouterDelegate (Router server)
	{
	    this.server = server;
	}
	
	public void routeMessage(Message message) {
	    log("Router", message.getTarget().toString());
	    server.routeMessage(message);
	}

    }



    public class DestinationQueueDelegate implements DestinationQueue
    {
	private DestinationQueue server;
	
	public DestinationQueueDelegate (DestinationQueue server)
	{
	    this.server = server;
	}
	
	public boolean isEmpty() {
	    return server.isEmpty();
	}

	public int size() {
	    return server.size();
	}

	public Object next() {
	    return server.next();
	}

	public void holdMessage(Message message) {
	    log("DestinationQueue", message.toString());
	    server.holdMessage(message);
	}
	
	public boolean matches(MessageAddress addr){
	    return server.matches(addr);
	}
    }


    public class DestinationLinkDelegate implements DestinationLink
    {
	private DestinationLink server;
	
	public DestinationLinkDelegate (DestinationLink server)
	{
	    this.server = server;
	}
	
	public void forwardMessage(Message message) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException

	{
	    log("DestinationLink", message.toString());
	    server.forwardMessage(message);
	}
	
	public int cost(Message message){
	    return server.cost(message);
	}
    }


    public class MessageDelivererDelegate implements MessageDeliverer
    {
	private MessageDeliverer server;
	
	public MessageDelivererDelegate (MessageDeliverer server)
	{
	    this.server = server;
	}
	
	public void deliverMessage(Message message) {
	    log("MessageDeliverer", message.toString());
	    server.deliverMessage(message);
	}
	
	public boolean matches(String name) {
	    return server.matches(name);
	}
    }

    public class ReceiveLinkDelegate implements ReceiveLink
    {
	private ReceiveLink server;
	
	public ReceiveLinkDelegate (ReceiveLink server)
	{
	    this.server = server;
	}
	
	public void deliverMessage(Message message) {
	    log("ReceiveLink", message.toString());
	    server.deliverMessage(message);
	}

    }
}



    
