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

import org.cougaar.core.society.MulticastMessageAddress;
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.naming.NamingService;
import org.cougaar.core.naming.NS;

import java.util.ArrayList;
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
public final class NameSupportImpl implements NameSupport 
{
    private static final String MTS_ATTR = "MTS";
    private static final String ADDRESS_ATTR = "Address";

    // Singleton
    private static NameSupport instance;

    public static NameSupport instance() {
	return instance;
    }

    public static NameSupport makeInstance(String id, 
					   NamingService namingService)
    {
	if (instance == null) {
	    instance = new NameSupportImpl(id, namingService);
	} else {
	    System.err.println("### NameSupport singleton already exists");
	}
	return instance;
    }

    private NamingService namingService;
    private MessageAddress myNodeAddress;

    private NameSupportImpl(String id, NamingService namingService) {
	myNodeAddress = new MessageAddress(id+"(Node)");
        this.namingService = namingService;
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
            try {
                ctx = (DirContext) ctx.lookup(prefix);
            } catch (NamingException ne) {
		BasicAttributes empty_attr = new BasicAttributes();
                ctx = (DirContext) ctx.createSubcontext(prefix, empty_attr);
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
					  MessageTransportClient client, 
					  String transportType)
    {	
	MessageAddress addr = client.getMessageAddress();
	try {
	    String key = makeName(AGENT_DIR, addr, transportType);
	    _registerWithSociety(key, proxy);
	} catch (Exception e) {
	    System.err.println("Failed to add Client "+ addr + 
			       " to NameServer for transport" + transportType);
	    e.printStackTrace();
	}
    }

    public void unregisterAgentInNameServer(Object proxy, 
					    MessageTransportClient client, 
					    String transportType)
    {	
	MessageAddress addr = client.getMessageAddress();
	try {
	    String key = makeName(AGENT_DIR, addr, transportType);
	    _registerWithSociety(key, null);
	} catch (Exception e) {
	    System.err.println("Failed to remove Client "+ addr + 
			       " from NameServer for transport" + 
			       transportType);
	    e.printStackTrace();
	}
    }

    public void registerNodeInNameServer(Object proxy, 
					 String transportType) 
    {
	// Register Node address as MTS multicast handler
	String name = makeName(MTS_DIR, myNodeAddress, transportType);
	try {
	    BasicAttributes mts_attr = new BasicAttributes();
	    mts_attr.put(MTS_ATTR, Boolean.TRUE);
	    mts_attr.put(ADDRESS_ATTR, myNodeAddress);
	    _registerWithSociety(name, proxy, mts_attr);
	} catch (Exception e) {
	    System.err.println("Failed to register " +  name);
	    e.printStackTrace();
	}

	// Register Node address as Agent.  Is this used?
// 	name = makeName(AGENT_DIR, myNodeAddress, transportType);
// 	try {
// 	    _registerWithSociety(name, proxy);
// 	} catch (Exception e) {
// 	    System.err.println("Failed to register " + name);
// 	    e.printStackTrace();
// 	}
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
		System.out.println("$$$ Trying redirect of " + address + 
				   " to " + addr);
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
		throw new RuntimeException("Address " + address + " loops");
	    } else {
		return object;
	    }
	} catch (NamingException ne) {
	    // unknown?
	    return null; 
	}
    }


    /**
     * Hides the messy details of a NamingEnumeration.  This version
     * returns a specific attribute value for each next() call.
     */
    public static class NamingIterator implements Iterator {
	private NamingEnumeration e;
	private String attribute;

	NamingIterator(NamingEnumeration e, String attribute) {
	    this.e = e;
	    this.attribute = attribute;
	}

	public boolean hasNext() {
	    try {
		return e.hasMore();
	    } catch (NamingException ex) {
		ex.printStackTrace();
		return false;
	    }
	}

	public Object next() {
	    try {
		SearchResult result = (SearchResult) e.next();
		Attributes attrs = result.getAttributes();
		Attribute attr = attrs.get(attribute);
                return attr != null ? attr.get() : null;
	    } catch (NamingException ex) {
		ex.printStackTrace();
		return null;
	    }
	}

	public void remove() {
	    throw new RuntimeException("No way Jose");
	}

    }

    // Fixed for all time...
    private static final Attributes MC_MATCH = 
	new BasicAttributes(MTS_ATTR, Boolean.TRUE);
    private static final String[] MC_RETATTR = { ADDRESS_ATTR };


    public Iterator lookupMulticast(MulticastMessageAddress address) {
	try {
	    DirContext ctx = namingService.getRootContext();
	    NamingEnumeration e = ctx.search(MTS_DIR, MC_MATCH, MC_RETATTR);
	    // Return an Iterator instead of the messy NamingEnumeration
            return new NamingIterator(e, ADDRESS_ATTR);
	} catch (NamingException ne) {
	    ne.printStackTrace();
	    return null;
	}
    }
  

}
