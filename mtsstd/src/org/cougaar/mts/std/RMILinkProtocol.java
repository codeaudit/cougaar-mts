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

import org.cougaar.core.naming.NS;
import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;


import java.rmi.RemoteException;
import java.rmi.server.RMISocketFactory;
import java.util.HashMap;

/**
 * This is a minimal rmi message transport which does nothing other
 * than rmi-specific functionality.  The hope is that we can pull out
 * a number of other aspects from the big RMIMessageTransport, leaving
 * this simple and maintainable body of code to deal with the actual
 * rmi functionality per se.  
 *
 * The cost function of the DestinationLink inner subclass is
 * currently hardwired to an arbitrary value of 1000.  This should be
 * made smarter eventually. 
 *
 * The RMI transport supports two additional factory methods in
 * addition to the standard getDestinationLink (which is supported by
 * all transports).  For RMI, we also have factory methods for
 * constructing an RMI server object to be registered in the
 * nameserver, and for constructing RMI client stubs which refer to
 * remote MTs.  The registered servers are ordinarily MTImpls, one per
 * node.  By using the factory style, we allow aspects to "wrap" the
 * MTImpl before the final wrapped object is registered, without
 * requiring new subclasses of the transort itself. The RMI client
 * stubs are MTs, as returned by the nameserver.  Again, by using the
 * factory style, aspects can wrap the stub returned by the
 * nameserver.  QuO uses these two new aspects to add qos support to
 * the RMI pieces of Alp.
 * */
public class RMILinkProtocol 
    extends LinkProtocol
{

    // private MessageAddress myAddress;
    private MT myProxy;
    private HashMap links;
    private SocketFactory socfac;

    public RMILinkProtocol(String id, AspectSupport aspectSupport) {
	super(aspectSupport); 
	links = new HashMap();
	socfac = getSocketFactory();
    }


    protected String getProtocolType() {
	return "-RMI";
    }

    protected SocketFactory getSocketFactory() {
	return new SocketFactory(false);
    }


    // If this is called, we've already found the remote reference.
    protected int computeCost(Message message) {
	return 1000;
    }


    protected MTImpl makeMTImpl(MessageAddress myAddress,
				MessageDeliverer deliverer,
				SocketFactory socfac)
	throws java.rmi.RemoteException
    {
	return new MTImpl(myAddress, deliverer, socfac);
    }

    protected void doForwarding(MT remote, Message message) 
	throws MisdeliveredMessageException, java.rmi.RemoteException
    {
	remote.rerouteMessage(message);
    }




    private MT lookupRMIObject(MessageAddress address, boolean getProxy) 
	throws Exception 
    {
	Object object = 
	    nameSupport.lookupAddressInNameServer(address, getProtocolType());

	if (object == null || !getProxy) return (MT) object;


	object = getClientSideProxy(object);
	if (object instanceof MT) {
	    return (MT) object;
	} else {
	    throw new RuntimeException("Object "
				       +object+
				       " is not a MessageTransport!");
	}

    }


    private void findOrMakeMT() {
	if (myProxy != null) return;
	try {
	    MessageAddress myAddress = nameSupport.getNodeMessageAddress();
	    MTImpl impl = makeMTImpl(myAddress, deliverer, socfac);
	    myProxy = getServerSideProxy(impl);
	} catch (java.rmi.RemoteException ex) {
	    ex.printStackTrace();
	}
    }

    public final void registerMTS(MessageAddress addr) {
	findOrMakeMT();
	try {
	    Object proxy = myProxy;
	    nameSupport.registerAgentInNameServer(proxy,addr,
						  getProtocolType());
	} catch (Exception e) {
	    System.err.println("Error registering MessageTransport:");
	    e.printStackTrace();
	}
    }

    public final void registerClient(MessageTransportClient client) {
	try {
	    // Assume node-redirect
	    Object proxy = myProxy;
	    MessageAddress addr = client.getMessageAddress();
	    nameSupport.registerAgentInNameServer(proxy,addr,
						  getProtocolType());
	} catch (Exception e) {
	    System.err.println("Error registering MessageTransport:");
	    e.printStackTrace();
	}
    }


    public final void unregisterClient(MessageTransportClient client) {
	try {
	    // Assume node-redirect
	    Object proxy = myProxy;
	    MessageAddress addr = client.getMessageAddress();
	    nameSupport.unregisterAgentInNameServer(proxy,addr,
						    getProtocolType());
	} catch (Exception e) {
	    System.err.println("Error unregistering MessageTransport:");
	    e.printStackTrace();
	}
    }



    public boolean addressKnown(MessageAddress address) {
	try {
	    return lookupRMIObject(address, false) != null;
	} catch (Exception e) {
	    //System.err.println("Failed in addressKnown:"+e);
	    //e.printStackTrace();
	}
	return false;
    }


    // Factory methods:

    public DestinationLink getDestinationLink(MessageAddress address) {
	DestinationLink link = (DestinationLink) links.get(address);
	if (link == null) {
	    link = new Link(address); // attach aspects
	    link =(DestinationLink) attachAspects(link, DestinationLink.class);
	    links.put(address, link);
	}
	return link;
    }



    // Hook to attach aspects to the client-side stub for the remote
    // MT.
    private MT getClientSideProxy(Object object) {
	return (MT) attachAspects(object, MT.class);
    }


    // Hook to attach aspects to the MT server object.  The class tag
    // here is MTImpl to distinguish it from the client side proxy,
    // above.  The final object itself only needs to match the MT
    // interface, it doesn't have to be an MTImpl.
    private MT getServerSideProxy(Object object) 
	throws RemoteException
    {
	return (MT) attachAspects(object, MTImpl.class);
    }


    class Link implements DestinationLink
    {
	
	private MessageAddress target;
	private MT remote;

	protected Link(MessageAddress destination)
	{
	    this.target = destination;
	}

	private void cacheRemote() 
	    throws NameLookupException, UnregisteredNameException
	{
	    if (remote == null) {
		try {
		    remote = lookupRMIObject(target, true);
		}
		catch (Exception lookup_failure) {
		    throw new  NameLookupException(lookup_failure);
		}

		if (remote == null) 
		    throw new UnregisteredNameException(target);

	    }
	}

	public boolean retryFailedMessage(Message message, int retryCount) {
	    return true;
	}

    
	public Class getProtocolClass() {
	    return RMILinkProtocol.this.getClass();
	}
	

	public int cost (Message message) {
	    try {
		cacheRemote();
		return computeCost(message);
	    }
	    catch (Exception ex) {
		// not found
		return Integer.MAX_VALUE;
	    }
	}


	public MessageAddress getDestination() {
	    return target;
	}


	public void forwardMessage(Message message) 
	    throws NameLookupException, 
		   UnregisteredNameException, 
		   CommFailureException,
		   MisdeliveredMessageException
	{
	    cacheRemote();
	    try {
		doForwarding(remote, message);
	    } 
	    catch (MisdeliveredMessageException mis) {
		// force recache of remote
		remote = null;
		throw mis;
	    }
	    catch (Exception ex) {
		// force recache of remote
		if (Debug.debug(COMM)) ex.printStackTrace();
		remote = null;
		// Assume anything else is a comm failure
		throw new CommFailureException(ex);
	    }
	}

    }

}

