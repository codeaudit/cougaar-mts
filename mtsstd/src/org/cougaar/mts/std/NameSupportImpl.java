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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URI;
import java.util.Iterator;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Application;
import org.cougaar.core.service.wp.Cert;
import org.cougaar.core.service.wp.WhitePagesService;

/**
 * This is utility class which hides the grimy details of dealing with
 * NameServers from the rest of the message transport subsystem . */
public final class NameSupportImpl implements ServiceProvider
{
    private static final long TTL = Long.MAX_VALUE;
    private static final Cert CERT = Cert.NULL;

    private NameSupport service;

    NameSupportImpl(String id,  ServiceBroker sb) 
    {
	service = new ServiceImpl(id, sb);
	AspectSupport aspectSupport = (AspectSupport)
	    sb.getService(this, AspectSupport.class, null);
	service = (NameSupport) 
	    aspectSupport.attachAspects(service, NameSupport.class);
    }

    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	if (serviceClass == NameSupport.class) {
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



    private final static class ServiceImpl 
	implements NameSupport
    {
	private LoggingService loggingService;
	private WhitePagesService wpService;
	private MessageAddress myNodeAddress;
	private String id;
	private String hostname;

	private ServiceImpl(String id, ServiceBroker sb) {
	    this.id = id;
	    wpService = (WhitePagesService) 
		sb.getService(this, WhitePagesService.class, null);
	    loggingService = (LoggingService)
		sb.getService(this, LoggingService.class, null);
 	    myNodeAddress = MessageAddress.getMessageAddress(id+"(MTS)");

	    try {
		hostname =java.net.InetAddress.getLocalHost().getHostAddress();
	    } catch (java.net.UnknownHostException ex) {
		loggingService.error(null, ex);
	    }
	}

	public MessageAddress  getNodeMessageAddress() {
	    return myNodeAddress;
	}


	private final void _register(String agent, 
				     URI ref,
				     String application) 
	{
	    Application app = Application.getApplication(application);
	    AddressEntry entry = new AddressEntry(agent, app, ref, CERT, TTL);
	    try {
		wpService.rebind(entry);
	    } catch (Exception ex) {
		loggingService.error(null, ex);
	    }
	}

	private final void _unregister(String agent, 
				       URI ref,
				       String application) 
	{
	    Application app = Application.getApplication(application);
	    AddressEntry entry = new AddressEntry(agent, app, ref, CERT, TTL);
	    try {
		wpService.unbind(entry);
	    } catch (Exception ex) {
		loggingService.error(null, ex);
	    }
	}

    

	public void registerAgentInNameServer(URI reference, 
					      MessageAddress addr, 
					      String protocol)
	{	
	    _register(addr.getAddress(), reference, protocol);
	}

	public void unregisterAgentInNameServer(URI reference, 
						MessageAddress addr, 
						String protocol)
	{	
	    _unregister(addr.getAddress(), reference, protocol);
	}

	public URI lookupAddressInNameServer(MessageAddress address, 
					     String protocol)
	{
	    AddressEntry[] result = null;
	    try {
		result = wpService.get(address.getAddress());
	    } catch (Exception ex) {
		loggingService.error(null, ex);
	    }

	    if (result == null) return null;

	    for (int i=0; i<result.length; i++) {
		AddressEntry entry = result[i];
		if (entry.getApplication().toString().equals(protocol)) {
		    return entry.getAddress();
		}
	    }

	    // no match
	    return null;
	}



	public class AddressIterator implements Iterator {
	    private AddressEntry[] entries;
	    private int index;

	    AddressIterator(AddressEntry[] entries) 
	    {
		this.entries = entries;
		this.index = 0;
	    }

	    public boolean hasNext() {
		return index < entries.length;
	    }

	    public Object next() {
		if (index >= entries.length) return null;
		AddressEntry entry = entries[index++];
		String node = entry.getApplication().toString();
		return MessageAddress.getMessageAddress(node);
	    }

	    public void remove() {
		throw new RuntimeException("No way Jose");
	    }

	}


	public void registerMTS(MessageAddress mts_address) {
	    String name = mts_address.getAddress();
	    URI reference = URI.create("mts://" +name);
	    _register("MTS", reference, name);
	}


	public Iterator lookupMulticast(MulticastMessageAddress address) {
	    try {
		AddressEntry[] result = wpService.get("MTS");
		if (result == null) return null;
		return new AddressIterator(result);
	    } catch (Exception ex) {
		loggingService.error(null, ex);
		return null;
	    }
	}
  

    }

}
