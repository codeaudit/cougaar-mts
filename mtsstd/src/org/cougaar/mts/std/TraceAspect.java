/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;

import org.cougaar.core.service.LoggingService;

/**
 * This is a very simple aspect which is mostly for demonstration
 * purposes.  It provides a trivial trace of a message as it passes
 * through the various stages of the message transport subsystem.  */
public class TraceAspect 
    extends StandardAspect
{

    // logging support
    private PrintWriter logStream = null;

    public TraceAspect() {
    }


    private PrintWriter getLog() {
	if (logStream == null) {
	    try {
		String id = getRegistry().getIdentifier();
		logStream = new PrintWriter(new FileWriter(id+".cml"), true);
	    } catch (Exception e) {
		if (loggingService.isErrorEnabled())
		    loggingService.error("Logging required but not possible - exiting", e);
		System.exit(1);
	    }
	}
	return logStream;
    }


    protected void log(String key, String info) {
	String id = getRegistry().getIdentifier();
	String cleanInfo = info.replace('\n', '_');
	getLog().println(id+"\t"+System.currentTimeMillis()+"\t"+key+"\t"+cleanInfo);
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
	    return super.getNodeMessageAddress();
	}

	public void registerAgentInNameServer(URI reference, 
					      MessageAddress addr, 
					      String protocol)
	{
	    log("NameSupport", "Register Agent " + addr + " " + reference);
	    super.registerAgentInNameServer(reference, addr, protocol);
	}

	public void unregisterAgentInNameServer(URI reference, 
						MessageAddress addr, 
						String protocol) 
	{
	    log("NameSupport", "Unregister Agent " + addr + " " + reference);
	    super.unregisterAgentInNameServer(reference, addr, protocol);
	}

	public URI lookupAddressInNameServer(MessageAddress address, 
					     String protocol)
	{
	    URI res = super.lookupAddressInNameServer(address, protocol);
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
	
	public void sendMessage(AttributedMessage message) {
	    log("SendQueue", message.toString()+" ("+this.size()+")");
	    super.sendMessage(message);
	}
    }



    public class RouterDelegate extends RouterDelegateImplBase
    {
	public RouterDelegate (Router router) {
	    super(router);
	}
	
	public void routeMessage(AttributedMessage message) {
	    log("Router", message.getTarget().toString());
	    super.routeMessage(message);
	}

    }



    public class DestinationQueueDelegate 
	extends DestinationQueueDelegateImplBase
    {
	public DestinationQueueDelegate (DestinationQueue queue) {
	    super(queue);
	}
	

	public void holdMessage(AttributedMessage message) {
	    log("DestinationQueue", message.toString());
	    super.holdMessage(message);
	}

	public void dispatchNextMessage(AttributedMessage message) {
	    log("DestinationQueue dispatch", message.toString());
	    super.dispatchNextMessage(message);
	}
	
    }


    public class DestinationLinkDelegate 
	extends DestinationLinkDelegateImplBase
    {
	public DestinationLinkDelegate (DestinationLink link)
	{
	    super(link);
	}
	
	public MessageAttributes forwardMessage(AttributedMessage message) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException

	{
	    log("DestinationLink", message.toString());
	    return super.forwardMessage(message);
	}
	
    }


    public class MessageDelivererDelegate 
	extends MessageDelivererDelegateImplBase
    {
	public MessageDelivererDelegate (MessageDeliverer deliverer) {
	    super(deliverer);
	}
	
	public MessageAttributes deliverMessage(AttributedMessage message, 
						MessageAddress dest) 
	    throws MisdeliveredMessageException
	{
	    log("MessageDeliverer", message.toString());
	    return super.deliverMessage(message, dest);
	}
	
    }

    public class ReceiveLinkDelegate 
	extends ReceiveLinkDelegateImplBase
    {
	public ReceiveLinkDelegate (ReceiveLink link) {
	    super(link);
	}
	
	public MessageAttributes deliverMessage(AttributedMessage message) {
	    log("ReceiveLink", message.toString());
	    return super.deliverMessage(message);
	}

    }
}



    
