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

import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.society.MulticastMessageAddress;


import java.io.PrintWriter;
import java.io.FileWriter;
import java.util.Iterator;
import javax.naming.directory.Attributes;

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

    public Object getDelegate(Object delegate,  Class type) 
    {
	if (type == SendQueue.class) {
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
	} else if (type == NameSupport.class) {
	    return new NameSupportDelegate((NameSupport) delegate);
	} else {
	    return null;
	}
    }

    public class NameSupportDelegate extends NameSupportDelegateImplBase {
	
	public NameSupportDelegate (NameSupport nameSupport) {
	    super(nameSupport);
	}

	public MessageAddress  getNodeMessageAddress() {
	    return nameSupport.getNodeMessageAddress();
	}

	public void registerAgentInNameServer(Object proxy, 
					      MessageAddress addr, 
					      String type)
	{
	    log("NameSupport", "Register Agent" + proxy);
	    nameSupport.registerAgentInNameServer(proxy, addr, type);
	}

	public void unregisterAgentInNameServer(Object proxy, 
						MessageAddress addr, 
						String type) 
	{
	    log("NameSupport", "Unregister Agent " + proxy);
	    nameSupport.unregisterAgentInNameServer(proxy, addr, type);
	}

	public void registerMTS(MessageAddress addr)
	{
	    log("NameSupport", "Register MTS " + addr);
	    nameSupport.registerMTS(addr);
	}

	public Object lookupAddressInNameServer(MessageAddress address, 
						String type)
	{
	    Object res = nameSupport.lookupAddressInNameServer(address, type);
	    log("NameSupport", "Lookup of " + address + " returned " + res);
	    return res;
	}

    }



    public class SendQueueDelegate 
	extends SendQueueDelegateImplBase
    {
	public SendQueueDelegate (SendQueue queue) {
	    super(queue);
	}
	
	public void sendMessage(Message message) {
	    log("SendQueue", message.toString()+" ("+this.size()+")");
	    queue.sendMessage(message);
	}
    }



    public class RouterDelegate extends RouterDelegateImplBase
    {
	public RouterDelegate (Router router) {
	    super(router);
	}
	
	public void routeMessage(Message message) {
	    log("Router", message.getTarget().toString());
	    router.routeMessage(message);
	}

    }



    public class DestinationQueueDelegate 
	extends DestinationQueueDelegateImplBase
    {
	public DestinationQueueDelegate (DestinationQueue queue) {
	    super(queue);
	}
	

	public void holdMessage(Message message) {
	    log("DestinationQueue", message.toString());
	    queue.holdMessage(message);
	}

	public void dispatchNextMessage(Message message) {
	    log("DestinationQueue dispatch", message.toString());
	    queue.dispatchNextMessage(message);
	}
	
    }


    public class DestinationLinkDelegate 
	extends DestinationLinkDelegateImplBase
    {
	public DestinationLinkDelegate (DestinationLink link)
	{
	    super(link);
	}
	
	public void forwardMessage(Message message) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException

	{
	    log("DestinationLink", message.toString());
	    link.forwardMessage(message);
	}
	
    }


    public class MessageDelivererDelegate 
	extends MessageDelivererDelegateImplBase
    {
	public MessageDelivererDelegate (MessageDeliverer deliverer) {
	    super(deliverer);
	}
	
	public void deliverMessage(Message message, MessageAddress dest) 
	    throws MisdeliveredMessageException
	{
	    log("MessageDeliverer", message.toString());
	    deliverer.deliverMessage(message, dest);
	}
	
    }

    public class ReceiveLinkDelegate 
	extends ReceiveLinkDelegateImplBase
    {
	public ReceiveLinkDelegate (ReceiveLink link) {
	    super(link);
	}
	
	public void deliverMessage(Message message) {
	    log("ReceiveLink", message.toString());
	    link.deliverMessage(message);
	}

    }
}



    
