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

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.LoggingService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The MessageTransportRegistry singleton is a utility instance that
 * helps certain pieces of the message transport subsystem to find one
 * another. */
final class MessageTransportRegistry 
    implements DebugFlags, ServiceProvider
{

    private ServiceImpl service;

    MessageTransportRegistry(String name, ServiceBroker sb) 
    {
	service = new ServiceImpl(name, sb);
    }


    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	if (serviceClass == MessageTransportRegistryService.class) {
	    return service;
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




    static final class ServiceImpl
	implements MessageTransportRegistryService 
    {
    
	private String name;
	private HashMap receiveLinks = new HashMap(89);
	private ArrayList linkProtocols = new ArrayList();
	private ReceiveLinkProviderService receiveLinkProvider;
	private NameSupport nameSupport;
	private ServiceBroker sb;
	private LoggingService loggingService;

	private ServiceImpl(String name, ServiceBroker sb) {
	    this.name = name;
	    this.sb = sb;
	    loggingService = 
		(LoggingService) sb.getService(this, LoggingService.class, null);
	}

	private NameSupport nameSupport() {
	    if (nameSupport == null) 
		nameSupport = (NameSupport) sb.getService(this, 
							  NameSupport.class,
							  null);
	    return nameSupport;
	}


	private ReceiveLink makeReceiveLink(MessageTransportClient client) {
	    if (receiveLinkProvider == null) {
		receiveLinkProvider =
		    (ReceiveLinkProviderService) 
		    sb.getService(this, 
				  ReceiveLinkProviderService.class,
				  null);
	    }
	    ReceiveLink link = receiveLinkProvider.getReceiveLink(client);
	    receiveLinks.put(client.getMessageAddress(), link);
	    return link;
	}


	private void registerClientWithSociety(MessageTransportClient client) {
	    // register with each component transport
	    Iterator protocols = linkProtocols.iterator();
	    while (protocols.hasNext()) {
		LinkProtocol protocol = (LinkProtocol) protocols.next();
		protocol.registerClient(client);
	    }
	}


	private void unregisterClientWithSociety(MessageTransportClient client)
	{
	    // register with each component transport
	    Iterator protocols = linkProtocols.iterator();
	    while (protocols.hasNext()) {
		LinkProtocol protocol = (LinkProtocol) protocols.next();
		protocol.unregisterClient(client);
	    }
	}



	private void addLocalClient(MessageTransportClient client) {
	    MessageAddress key = client.getMessageAddress();
	    try {
		synchronized (this) {
		    ReceiveLink link = findLocalReceiveLink(key);
		    if (link == null) {
			link = makeReceiveLink(client);
		    }
		}
	    } catch (Exception e) {
		if (loggingService.isErrorEnabled())
		    loggingService.error(e.toString());
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



	public void addLinkProtocol(LinkProtocol lp) {
	    linkProtocols.add(lp);
	}


	public String getIdentifier() {
	    return name;
	}


	public boolean isLocalClient(MessageAddress id) {
	    synchronized (this) {
		return receiveLinks.get(id) != null ||
		    id.equals(MessageAddress.LOCAL);
	    }
	}


	public  ReceiveLink findLocalReceiveLink(MessageAddress id) {
	    return (ReceiveLink) receiveLinks.get(id);
	}


	// this is a slow implementation, as it conses a new set each time.
	// Better alternatives surely exist.
	public Iterator findLocalMulticastReceivers(MulticastMessageAddress addr)
	{
	    if (addr.hasReceiverClass()) {
		ArrayList result = new ArrayList();
		Class mclass = addr.getReceiverClass();
		if (mclass != null) {
		    Iterator itr = receiveLinks.entrySet().iterator();
		    while (itr.hasNext()) {
			Map.Entry entry = (Map.Entry) itr.next();
			ReceiveLink link = (ReceiveLink) entry.getValue();
			MessageTransportClient client = link.getClient();
			if (mclass.isAssignableFrom(client.getClass())) {
			    result.add(entry.getKey());
			    if (Debug.isDebugEnabled(loggingService,MULTICAST))
				loggingService.debug("Client " +
							  client + 
							  " matches " +
							  mclass + ", added " +
							  entry.getKey());
			} else {
			    if (Debug.isDebugEnabled(loggingService,MULTICAST)) 
				loggingService.debug("Client " +
							  client +
							  " doesn't match " +
							  mclass);
			}
		    }
		}
		if (Debug.isDebugEnabled(loggingService,MULTICAST)) 
		    loggingService.debug("result=" + result);
		return result.iterator();

	    } else {
		return new ArrayList(receiveLinks.keySet()).iterator();
	    }
	}






	public Iterator findRemoteMulticastTransports(MulticastMessageAddress addr)
	{
	    return nameSupport().lookupMulticast(addr);
	}


	public void registerClient(MessageTransportClient client) {
	    addLocalClient(client);
	    registerClientWithSociety(client);
	}


	public void unregisterClient(MessageTransportClient client) {
	    unregisterClientWithSociety(client);
	    removeLocalClient(client);
	}


	public void registerMTS(MessageTransportClient client) {
	    MessageAddress mts_address = client.getMessageAddress();

	    // Give each LinkProtocol a chance to register some object
	    // using the MTS address (but *not* in the MTS DirContext).
	    Iterator protocols = linkProtocols.iterator();
	    while (protocols.hasNext()) {
		LinkProtocol protocol = (LinkProtocol) protocols.next();
		protocol.registerMTS(mts_address);
	    }
	
	    // Now register the single MTS entry for this Node in the MTS
	    // DirContext
	    nameSupport().registerMTS(mts_address);

	}


	public boolean addressKnown(MessageAddress address) {
	    Iterator protocols = linkProtocols.iterator();
	    while (protocols.hasNext()) {
		LinkProtocol protocol = (LinkProtocol) protocols.next();
		if (protocol.addressKnown(address)) return true;
	    }
	    return false;
	}

	public ArrayList getDestinationLinks(MessageAddress destination) 
	{
	    ArrayList destinationLinks = new ArrayList();
	    Iterator itr = linkProtocols.iterator();
	    DestinationLink link;
	    while (itr.hasNext()) {
		LinkProtocol lp = (LinkProtocol) itr.next();
		// Class lp_class = lp.getClass();
		link = lp.getDestinationLink(destination);
		destinationLinks.add(link);
	    }
	    return destinationLinks;
	}

    }


}
