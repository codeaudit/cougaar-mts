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

import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.society.MulticastMessageAddress;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


/**
 * The MessageTransportRegistry singleton is a utility instance that
 * helps certain pieces of the message transport subsystem to find one
 * another. */
class MessageTransportRegistry
{
    private static MessageTransportRegistry instance;

    public static synchronized MessageTransportRegistry 
	makeRegistry(String name, MessageTransportServiceProvider server)
    {
	if (instance == null) {
	    instance = new MessageTransportRegistry(name, server);
	    return instance;
	} else {
	    System.err.println("##### Attempt to make a second MessageTransportRegistry!");
	    return null;
	}
    }

    public static synchronized MessageTransportRegistry
	getRegistry()
    {
	if (instance != null) {
	    return instance;
	} else {
	    Thread.dumpStack();
	    System.err.println("##### Attempt to find registry before creating it");
	    return null;
	}
    }

    private String name;
    private HashMap receiveLinks = new HashMap(89);
    private HashMap serviceProxies = new HashMap(89);
    private MessageTransportServiceProvider serviceProvider;
    private LinkProtocolFactory protocolFactory;
    private ReceiveLinkFactory receiveLinkFactory;
    private NameSupport nameSupport;

    private MessageTransportRegistry(String name, 
				     MessageTransportServiceProvider provider)
    {
	this.name = name;
	this.serviceProvider = provider;
    }

    void setNameSupport(NameSupport nameSupport) {
	this.nameSupport = nameSupport;
    }

    void setProtocolFactory(LinkProtocolFactory factory) {
	this.protocolFactory = factory;
    }

    void setReceiveLinkFactory(ReceiveLinkFactory receiveLinkFactory) {
	this.receiveLinkFactory = receiveLinkFactory;
    }


    String getIdentifier() {
	return name;
    }

    
    void registerServiceProxy(MessageTransportServiceProxy proxy,
			      MessageAddress address)
    {
	MessageTransportServiceProxy old = findServiceProxy(address);
	if (old != null && old != proxy) {
	    System.err.println("##### Reregistering " + address + 
			       " as " + proxy);
	}
	serviceProxies.put(address, proxy);
    }

    MessageTransportServiceProxy findServiceProxy(MessageAddress address) {
	return (MessageTransportServiceProxy) serviceProxies.get(address);
    }
	

    private void addLocalClient(MessageTransportClient client) {
	MessageAddress key = client.getMessageAddress();
	try {
	    synchronized (this) {
		ReceiveLink link = receiveLinkFactory.getReceiveLink(client);
		receiveLinks.put(key, link);
	    }
	} catch (Exception e) {
	    System.err.println(e);
	}
    }

    private void removeLocalClient(MessageTransportClient client) {
	MessageAddress key = client.getMessageAddress();
	synchronized (this) {
	    try {
		receiveLinks.remove(key);
	    } catch (Exception e) {}
	}
    }

    boolean isLocalClient(MessageAddress id) {
	synchronized (this) {
	    return receiveLinks.get(id) != null ||
		id.equals(MessageAddress.LOCAL);
	}
    }




    // Caller MUST acquire the lock before calling these two
    // functions!
    ReceiveLink findLocalReceiveLink(MessageAddress id) {
	return (ReceiveLink) receiveLinks.get(id);
    }


    // this is a slow implementation, as it conses a new set each time.
    // Better alternatives surely exist.
    Iterator findLocalMulticastReceivers(MulticastMessageAddress addr)
    {
	return new ArrayList(receiveLinks.keySet()).iterator();
    }






    Iterator findRemoteMulticastTransports(MulticastMessageAddress addr)
    {
	return nameSupport.lookupMulticast(addr);
    }




    private void registerClientWithSociety(MessageTransportClient client) {
	// register with each component transport
	Iterator protocols = protocolFactory.getProtocols().iterator();
	while (protocols.hasNext()) {
	    LinkProtocol protocol = (LinkProtocol) protocols.next();
	    protocol.registerClient(client);
	}
    }


    private void unregisterClientWithSociety(MessageTransportClient client) {
	// register with each component transport
	Iterator protocols = protocolFactory.getProtocols().iterator();
	while (protocols.hasNext()) {
	    LinkProtocol protocol = (LinkProtocol) protocols.next();
	    protocol.unregisterClient(client);
	}
    }




    void registerClient(MessageTransportClient client) {
	addLocalClient(client);
	registerClientWithSociety(client);
    }


    void unregisterClient(MessageTransportClient client) {
	unregisterClientWithSociety(client);
	removeLocalClient(client);
    }





    boolean addressKnown(MessageAddress address) {
	Iterator protocols = protocolFactory.getProtocols().iterator();
	while (protocols.hasNext()) {
	    LinkProtocol protocol = (LinkProtocol) protocols.next();
	    if (protocol.addressKnown(address)) return true;
	}
	return false;
    }



}
