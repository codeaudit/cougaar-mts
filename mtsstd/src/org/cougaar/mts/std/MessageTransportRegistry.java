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


    void registerMTS() {
	MessageAddress mts_address = nameSupport.getNodeMessageAddress();

	// Give each LinkProtocol a chance to register some object
	// using the MTS address (but *not* in the MTS DirContext).
	Iterator protocols = protocolFactory.getProtocols().iterator();
	while (protocols.hasNext()) {
	    LinkProtocol protocol = (LinkProtocol) protocols.next();
	    protocol.registerMTS(mts_address);
	}
	
	// Now register the single MTS entry for this Node in the MTS
	// DirContext
	nameSupport.registerMTS(mts_address);

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
