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
import org.cougaar.core.naming.NS;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.NamingService;
import org.cougaar.core.service.TopologyEntry;
import org.cougaar.core.service.TopologyWriterService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

/**
 * This is utility class which hides the grimy details of dealing with
 * NameServers from the rest of the message transport subsystem.  */
public final class NameSupportImpl implements ServiceProvider
{
    private static final String MTS_ATTR = "MTS";
    private static final String ADDRESS_ATTR = "Address";

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
	private TopologyWriterService topologyService;
	private LoggingService loggingService;
	private NamingService namingService;
	private MessageAddress myNodeAddress;
	private String id;
	private String hostname;

	private ServiceImpl(String id, ServiceBroker sb) {
	    this.id = id;
	    namingService = (NamingService) 
		sb.getService(this, NamingService.class, null);
	    loggingService = (LoggingService)
		sb.getService(this, LoggingService.class, null);
	    topologyService = (TopologyWriterService)
		sb.getService(this, TopologyWriterService.class, null);
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

	private String makeName(String directory, 
				MessageAddress address, 
				String suffix) 
	{
	    return directory + NS.DirSeparator + address.getAddress() + suffix;
	}

	private final void _registerWithSociety(String key, 
						Object proxy,
						BasicAttributes attributes) 
	    throws NamingException
	{
	    DirContext ctx = namingService.getRootContext();
	    Name name = ctx.getNameParser("").parse(key);
	    while (name.size() > 1) {
		Name prefix = name.getPrefix(1);
		name = name.getSuffix(1);
                // find or create the context given by prefix. First
                // try lookup. If that fails try creating the
                // subcontext. If _that_ fails, assume it was because
                // another MTS created it after we did the lookup, but
                // before our attempt to create. Do a second lookup to
                // get the subcontext created by the other MTS. If the
                // second lookup fails, punt!
		try {
		    ctx = (DirContext) ctx.lookup(prefix);
		} catch (NamingException ne1) {
		    try {
                        BasicAttributes empty_attr = new BasicAttributes();
                        ctx = (DirContext)
                            ctx.createSubcontext(prefix, empty_attr);
                    } catch (NamingException ne2) {
                        try {
                            ctx = (DirContext) ctx.lookup(prefix);
                        } catch (NamingException ne3) {
                            ne3.initCause(ne2);
                            ne2.initCause(ne1);
                            throw ne3;
                        }
                    }
		} catch (Exception e) {
		    throw new NamingException(e.toString());
		}
	    }
	    if (proxy != null) 
		ctx.rebind(name, proxy, attributes);
	    else
		ctx.unbind(name);
	}
	private final void _registerWithSociety(String key, Object proxy) 
	    throws NamingException
	{
	    _registerWithSociety(key, proxy, new BasicAttributes());
	}
    

	public void registerAgentInNameServer(Object proxy, 
					      MessageAddress addr, 
					      String transportType)
	{	
	    try {
		String key = makeName(AGENT_DIR, addr, transportType);
		_registerWithSociety(key, proxy);
	    } catch (Exception e) {
		if (loggingService.isErrorEnabled())
		    loggingService.error("Failed to add Client "+ addr + 
					      " to N`ameServer for transport" 
					      +  transportType,
					      e);
	    }
	}

	public void unregisterAgentInNameServer(Object proxy, 
						MessageAddress addr, 
						String transportType)
	{	
	    try {
		String key = makeName(AGENT_DIR, addr, transportType);
		_registerWithSociety(key, null);
	    } catch (Exception e) {
		if (loggingService.isErrorEnabled())
		    loggingService.error("Failed to remove Client "+addr+ 
					  " from NameServer for transport" + 
					  transportType,
					  e);
	    }
	}

	public void registerMTS(MessageAddress mts_address) {
	    // Register Node address as MTS multicast handler
		   String name = makeName(MTS_DIR, mts_address, "");
	    try {
		BasicAttributes mts_attr = new BasicAttributes();
		mts_attr.put(MTS_ATTR, Boolean.TRUE.toString());
		mts_attr.put(ADDRESS_ATTR, mts_address.toString());
		_registerWithSociety(name, mts_address, mts_attr);
	    } catch (Exception e) {
		if (loggingService.isErrorEnabled())
		    loggingService.error("Failed to register " +  name,
					 e);
	    }

	    topologyService.createAgent(mts_address.getAddress(),
					TopologyEntry.SYSTEM_TYPE,
					0, 0,
					TopologyEntry.ACTIVE);

	}

	public Object lookupAddressInNameServer(MessageAddress address, 
						String transportType)
	{
	    MessageAddress addr = address;
	    String key = makeName(AGENT_DIR, addr, transportType);
	    Object object;
	    DirContext ctx = null;
       
	    try {
		ctx = namingService.getRootContext();
	    } catch (NamingException ne) {
		return null;
	    }

	    try {
		object = ctx.lookup(key);
		if (object instanceof MessageAddress) {
		    addr = (MessageAddress) object;
		    if (loggingService.isInfoEnabled())
			loggingService.info("Trying redirect of " 
						 +address+ 
						 " to " +addr);
		} else {
		    return object;
		}
	    } catch (NamingException ne) {
		// unknown?
		return null; 
	    }


	    // If we get here the lookup returned an address.  Use it as a
	    // forwarding pointer.
	    key = makeName(AGENT_DIR, addr, transportType);
	    try {
		object = ctx.lookup(key);
		if (object instanceof MessageAddress) {
		    throw new RuntimeException("Address " + address +
					       " loops");
		} else {
		    return object;
		}
	    } catch (NamingException ne) {
		// unknown?
		return null; 
	    }
	}

	/**
	 * The naming db is all strings now.  Define an interface to
	 * be used in a NamingIterator which can restore the proper
	 * type (AddressCoercer is the only example so far)
	 */
	interface Coercer {
	    Object coerce(String raw);
	}

	static class AddressCoercer implements Coercer {
	    public Object coerce(String raw) {
		return MessageAddress.getMessageAddress(raw);
	    }
	}

	static AddressCoercer addressCoercer = new AddressCoercer();


	/**
	 * Hides the messy details of a NamingEnumeration.  This version
	 * returns a specific attribute value for each next() call.
	 */
	public class NamingIterator implements Iterator {
	    private NamingEnumeration e;
	    private String attribute;
	    private Coercer coercer;

	    NamingIterator(NamingEnumeration e, 
			   String attribute,
			   Coercer coercer) 
	    {
		this.e = e;
		this.attribute = attribute;
		this.coercer = coercer;
	    }

	    public boolean hasNext() {
		try {
		    return e.hasMore();
		} catch (NamingException ex) {
		    loggingService.error(null, ex);
		    return false;
		}
	    }

	    public Object next() {
		try {
		    SearchResult result = (SearchResult) e.next();
		    Attributes attrs = result.getAttributes();
		    if (attribute == null) {
			// No specific attribute requested, return the
			// whole set.
			return attrs;
		    } else {
			Attribute attr = attrs.get(attribute);
			if (attr == null) return null;
			Object raw = attr.get();
			if (coercer != null) 
			    return coercer.coerce((String) raw);
			else
			    return raw;
		    }
		} catch (NamingException ex) {
		    loggingService.error(null, ex);
		    return null;
		}
	    }

	    public void remove() {
		throw new RuntimeException("No way Jose");
	    }

	}

	// Fixed for all time...
	private static final Attributes MC_MATCH = 
	    new BasicAttributes(MTS_ATTR, Boolean.TRUE.toString());
	private static final String[] MC_RETATTR = { ADDRESS_ATTR };


	

	public Iterator lookupMulticast(MulticastMessageAddress address) {
	    try {
		DirContext ctx = namingService.getRootContext();
		NamingEnumeration e = ctx.search(MTS_DIR, 
						 MC_MATCH, 
						 MC_RETATTR);
		// Return an Iterator instead of the messy NamingEnumeration
		return new NamingIterator(e, ADDRESS_ATTR, addressCoercer);
	    } catch (NamingException ne) {
		loggingService.error(null, ne);
		return null;
	    }
	}
  

    }

}
