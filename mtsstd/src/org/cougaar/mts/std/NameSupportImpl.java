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
import java.util.Set;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.wp.ListAllNodes;

/**
 * This is utility class which hides the grimy details of dealing with
 * NameServers from the rest of the message transport subsystem . */
public final class NameSupportImpl implements ServiceProvider
{
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
 	    myNodeAddress = MessageAddress.getMessageAddress(id);

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
				     String protocol) 
	{
	    AddressEntry entry = 
                AddressEntry.getAddressEntry(agent, protocol, ref);
	    try {
              final LoggingService ls = loggingService;
              Callback cb = new Callback() {
                  public void execute(Response r) {
                    if (r.isSuccess()) {
                      if (ls.isInfoEnabled()) {
                        ls.info("WP Response: "+r);
                      }
                    } else {
                      ls.error("WP Error: "+r);
                    }
                  }
                };

		wpService.rebind(entry, cb);
	    } catch (Exception ex) {
		loggingService.error(null, ex);
	    }
	}

	private final void _unregister(String agent, 
				       URI ref,
				       String protocol) 
	{
	    AddressEntry entry =
               AddressEntry.getAddressEntry(agent, protocol, ref);
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

	public void lookupAddressInNameServer(MessageAddress address, 
					      String protocol,
					      Callback callback)
	{
	    wpService.get(address.getAddress(), protocol, callback);
	}

	public URI lookupAddressInNameServer(MessageAddress address, 
					     String protocol)
	{
	    return lookupAddressInNameServer(address, protocol, 0);
	}

	public URI lookupAddressInNameServer(MessageAddress address, 
					     String protocol,
					     long timeout)
	{
            AddressEntry entry;
	    try {
		entry = wpService.get(address.getAddress(), protocol, timeout);
	    } catch (Exception ex) {
                entry = null;
		loggingService.error(null, ex);
	    }
	    return (entry == null ? null : entry.getURI());
	}



	private static class EmptyIterator implements Iterator {
	    public boolean hasNext() {
		return false;
	    }
	    public Object next() {
		return null;
	    }
	    public void remove() {
		throw new UnsupportedOperationException();
	    }
	}

	public Iterator lookupMulticast(MulticastMessageAddress address) {
	    try {
		Set result = ListAllNodes.listAllNodes(wpService, 30000);
		if (result == null) return new EmptyIterator();
                final Iterator iter = result.iterator();
                return new Iterator() {
                    public boolean hasNext() {
                        return iter.hasNext();
                    }
                    public Object next() {
                        String node = (String) iter.next();
                        return MessageAddress.getMessageAddress(node);
                    }
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
	    } catch (Exception ex) {
		if (loggingService.isWarnEnabled())
		    loggingService.warn("Multicast had WP timout");
		return new EmptyIterator();
	    }
	}

    }

}
