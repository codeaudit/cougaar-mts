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
import org.cougaar.core.society.MulticastMessageAddress;

import java.util.ArrayList;
import java.util.Iterator;

public class MulticastAspect extends StandardAspect
{

    private MessageTransportRegistry registry;

    public MulticastAspect() {
	super();
	registry = MessageTransportRegistry.getRegistry();
    }



    public Object getDelegate(Object delegate, Class type) {
	if (type == MessageTransportService.class) {
	    return new ServiceDelegate((MessageTransportService) delegate);
	} else if (type == MessageDeliverer.class) {
	    return new DelivererDelegate((MessageDeliverer) delegate);
	} else {
	    return null;
	}
    }



    private static class MulticastMessageEnvelope extends Message {
    
	private Message contents;

	MulticastMessageEnvelope(Message message, MessageAddress destination) {
	    super(message.getOriginator(), destination);
	    this.contents = message;
	}

	Message getContents() {
	    return contents;
	}
    
    }


    public class ServiceDelegate implements MessageTransportService {
	private MessageTransportService server;
	
	public ServiceDelegate (MessageTransportService server) {
	    this.server = server;
	}
	

	public void sendMessage(Message msg) {
	    MessageAddress destination = msg.getTarget();
	    if (destination instanceof MulticastMessageAddress) {
		if (destination.equals(MessageAddress.LOCAL)) {
		    if (Debug.debugMulticast())
			System.out.println("### MCAST: Local multicast");
		    msg = new MulticastMessageEnvelope(msg,  destination);
		    server.sendMessage(msg);
		} else {
		    if (Debug.debugMulticast())
			System.out.println("### MCAST: Remote multicast");
		    MulticastMessageAddress dst = 
			(MulticastMessageAddress) destination;
		    Iterator itr = registry.findRemoteMulticastTransports(dst);
		    MulticastMessageEnvelope envelope;
		    MessageAddress addr;
		    while (itr.hasNext()) {
			addr = (MessageAddress) itr.next();
			if (Debug.debugMulticast())
			    System.out.println("### MCAST: next address = " 
					       + addr);
			envelope = new MulticastMessageEnvelope(msg, addr);
			server.sendMessage(envelope);
		    }
		}
	    } else {
		server.sendMessage(msg);
	    }
	}

	public void registerClient(MessageTransportClient client) {
	    server.registerClient(client);
	}

	public void unregisterClient(MessageTransportClient client) {
	    server.unregisterClient(client);
	}
	
	public ArrayList flushMessages() {
	    return server.flushMessages();
	}

	public String getIdentifier() {
	    return server.getIdentifier();
	}

	public boolean addressKnown(MessageAddress addr) {
	    return server.addressKnown(addr);
	}

    }



    public class DelivererDelegate implements MessageDeliverer {
	private MessageDeliverer server;
	
	public DelivererDelegate (MessageDeliverer server)
	{
	    this.server = server;
	}
	
	public void deliverMessage(Message msg) 
	    throws MisdeliveredMessageException
	{
	    if (msg instanceof MulticastMessageEnvelope) {
		msg = ((MulticastMessageEnvelope) msg).getContents();
		MulticastMessageAddress addr = 
		    (MulticastMessageAddress) msg.getTarget();
		Object lock = registry.getLock();
		synchronized (lock) {
		    Iterator i = registry.findLocalMulticastReceiveLinks(addr);
		    while (i.hasNext()) {
			ReceiveLink link = (ReceiveLink) i.next();
			link.deliverMessage(msg);
			if (Debug.debugMulticast())
			    System.out.println("### MCAST: Delivering to "
					       + link);
		    }
		}
	    } else {	
		server.deliverMessage(msg);
	    }
	}
	
	public boolean matches(String name) {
	    return server.matches(name);
	}
    }

}



    
