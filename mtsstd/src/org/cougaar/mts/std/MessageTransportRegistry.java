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
    private HashMap myClients = new HashMap(89);
    private HashMap receiveLinks = new HashMap();
    private MessageTransportServiceProvider serviceProvider;
    private MessageTransportFactory transportFactory;
    private ReceiveLinkFactory receiveLinkFactory;
    private WatcherAspect watcherAspect;

    private MessageTransportRegistry(String name, MessageTransportServiceProvider serviceProvider) {
	this.name = name;
	this.serviceProvider = serviceProvider;
    }

    void setTransportFactory(MessageTransportFactory transportFactory) {
	this.transportFactory = transportFactory;
    }

    void setReceiveLinkFactory(ReceiveLinkFactory receiveLinkFactory) {
	this.receiveLinkFactory = receiveLinkFactory;
    }


    String getIdentifier() {
	return name;
    }



    void setWatcherManager(WatcherAspect watcherAspect) {
	this.watcherAspect =watcherAspect;
    }
 

    public WatcherAspect getWatcherManager() {
	return watcherAspect;
    }



    private void addLocalClient(MessageTransportClient client) {
	synchronized (myClients) {
	    try {
		myClients.put(client.getMessageAddress(), client);
	    } catch(Exception e) {}
	}
    }
    private void removeLocalClient(MessageTransportClient client) {
	synchronized (myClients) {
	    try {
		myClients.remove(client.getMessageAddress());
	    } catch (Exception e) {}
	}
    }

    MessageTransportClient findLocalClient(MessageAddress id) {
	synchronized (myClients) {
	    return (MessageTransportClient) myClients.get(id);
	}
    }

    // this is a slow implementation, as it conses a new set each time.
    // Better alternatives surely exist.
    Iterator findLocalMulticastClients(MulticastMessageAddress addr)
    {
	synchronized (myClients) {
	    return new ArrayList(myClients.values()).iterator();
	}
    }





    private void addLocalReceiveLink(ReceiveLink link, MessageAddress key) {
	synchronized (receiveLinks) {
	    try {
		receiveLinks.put(key, link);
	    } catch(Exception e) {}
	}
    }


    ReceiveLink findLocalReceiveLink(MessageAddress id) {
	synchronized (receiveLinks) {
	    return (ReceiveLink) receiveLinks.get(id);
	}
    }

    // this is a slow implementation, as it conses a new set each time.
    // Better alternatives surely exist.
    Iterator findLocalMulticastReceiveLinks(MulticastMessageAddress addr)
    {
	synchronized (receiveLinks) {
	    return new ArrayList(receiveLinks.values()).iterator();
	}
    }







    private void registerClientWithSociety(MessageTransportClient client) {
	// register with each component transport
	Iterator transports = transportFactory.getTransports().iterator();
	while (transports.hasNext()) {
	    MessageTransport mt = (MessageTransport) transports.next();
	    mt.registerClient(client);
	}
    }

    void registerClient(MessageTransportClient client) {
	addLocalClient(client);
	registerClientWithSociety(client);
	ReceiveLink link = receiveLinkFactory.getReceiveLink(client);
	addLocalReceiveLink(link, client.getMessageAddress());
    }





    boolean addressKnown(MessageAddress address) {
	Iterator transports = transportFactory.getTransports().iterator();
	while (transports.hasNext()) {
	    MessageTransport mt = (MessageTransport) transports.next();
	    if (mt.addressKnown(address)) return true;
	}
	return false;
    }



}
