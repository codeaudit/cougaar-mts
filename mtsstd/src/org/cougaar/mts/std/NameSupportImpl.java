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
import org.cougaar.core.naming.NamingService;

import javax.naming.NamingException;
import javax.naming.Name;
import javax.naming.directory.DirContext;
import javax.naming.directory.BasicAttributes;

/**
 * This is utility class which hides the grimy details of dealing with
 * NameServers from the rest of the message transport subsystem.  */
public class NameSupportImpl implements NameSupport 
{
    private NamingService namingService;
    private MessageAddress myNodeAddress;

    public NameSupportImpl(String id, NamingService namingService) {
	myNodeAddress = new MessageAddress(id+"(Node)");
        this.namingService = namingService;
    }

    public MessageAddress  getNodeMessageAddress() {
	return myNodeAddress;
    }

    private final void _registerWithSociety(String key, Object proxy) 
	throws NamingException
    {
        DirContext ctx = namingService.getRootContext();
        Name name = ctx.getNameParser("").parse(key);
        boolean exists = true;
        while (name.size() > 1) {
            Name prefix = name.getPrefix(1);
            name = name.getSuffix(1);
            try {
                ctx = (DirContext) ctx.lookup(prefix);
            } catch (NamingException ne) {
                ctx = (DirContext) ctx.createSubcontext(prefix, new BasicAttributes());
                exists = false;
            } catch (Exception e) {
                throw new NamingException(e.toString());
            }
        }
	if (proxy != null) 
	    ctx.rebind(name, proxy, new BasicAttributes());
	else
	    ctx.unbind(name);
    }

    public void registerAgentInNameServer(Object proxy, 
					  MessageTransportClient client, 
					  String transportType)
    {	
	MessageAddress addr = client.getMessageAddress();
	try {
	    String key = CLUSTERDIR + addr + transportType;
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
	    String key = CLUSTERDIR + addr + transportType;
	    _registerWithSociety(key, null);
	} catch (Exception e) {
	    System.err.println("Failed to remove Client "+ addr + 
			       " from NameServer for transport" + 
			       transportType);
	    e.printStackTrace();
	}
    }

    public void registerNodeInNameServer(Object proxy, String transportType) {
	try {
	    _registerWithSociety(MTDIR+myNodeAddress.getAddress()+transportType, proxy);
	    _registerWithSociety(CLUSTERDIR+myNodeAddress.getAddress()+transportType, proxy);
	} catch (Exception e) {
	    System.err.println("Failed to add Node " + myNodeAddress.getAddress() +
			       "to NameServer for transport" + transportType);
	    e.printStackTrace();
	}
    }

    public Object lookupAddressInNameServer(MessageAddress address, 
					    String transportType)
    {
	MessageAddress addr = address;
	String key = CLUSTERDIR + addr.getAddress() + transportType ;
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
	    } else {
		return object;
	    }
	} catch (NamingException ne) {
	    // unknown?
	    return null; 
	}


	// If we get here the lookup returned an address.  Use it as a
	// forwarding pointer.
	key = CLUSTERDIR + addr.getAddress() + transportType ;
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

}
