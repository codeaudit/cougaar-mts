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
public class RMIMessageTransport 
    extends MessageTransport
{
    private static final String TRANSPORT_TYPE = ":simpleRMI";
    

    private MessageAddress myAddress;
    private HashMap links;


    public RMIMessageTransport(String id, java.util.ArrayList aspects) {
	super(aspects); 
	links = new HashMap();
	RMISocketFactory socfac = RMISocketFactory.getSocketFactory();
	if (! (socfac instanceof SocketFactory)) {
	    socfac = new SocketFactory(this);
	    try {
		RMISocketFactory.setSocketFactory(socfac);
	    }
	    catch (java.io.IOException ex) {
		ex.printStackTrace();
	    }
	}
    }


    private MT lookupRMIObject(MessageAddress address) throws Exception {
	Object object = 
	    nameSupport.lookupAddressInNameServer(address, TRANSPORT_TYPE);

	if (object == null) {
	    return null;
	} 
	object = getClientSideProxy(object);
	if (object instanceof MT) {
	    return (MT) object;
	} else {
	    throw new RuntimeException("Object "
				       +object+
				       " is not a MessageTransport!");
	}

    }


    private final void registerNodeWithSociety() 
	throws RemoteException
    {
	synchronized (this) {
	    if (myAddress == null) {
		myAddress = nameSupport.getNodeMessageAddress();
		MTImpl impl = new MTImpl(this, myAddress, recvQ);
		MT proxy = getServerSideProxy(impl);
		nameSupport.registerNodeInNameServer(proxy,TRANSPORT_TYPE);
	    }
	}
    }

    public final void registerClient(MessageTransportClient client) {
	try {
	    // Register a MT for the Node
	    // Since there is no explicit time for registering the Node
	    // Attempt every time you register a Client
	    registerNodeWithSociety();

	    // Assume node-redirect
	    Object proxy = myAddress;
	    nameSupport.registerAgentInNameServer(proxy,client,TRANSPORT_TYPE);
	} catch (Exception e) {
	    System.err.println("Error registering MessageTransport:");
	    e.printStackTrace();
	}
    }

    public boolean addressKnown(MessageAddress address) {
	try {
	    return lookupRMIObject(address) != null;
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
	    link = (DestinationLink) attachAspects(link, DestinationLink.class,
						   this);
	    links.put(address, link);
	}
	return link;
    }



    private MT getClientSideProxy(Object object) {
	return (MT) attachAspects(object, MT.class);
    }


    // For now this can return an object of any arbitrary type!  The
    // corresponding client proxy code has the responsibility for
    // extracting a usable MT out of the object.
    private MT getServerSideProxy(Object object) 
	throws RemoteException
    {
	return (MT) attachAspects(object, MTImpl.class);
    }





    /**
     * The DestinationLink class for this transport.  Forwarding a
     * message with this link means looking up the MT proxy for a
     * remote MTImpl, and calling rerouteMessage on it.  The cost is
     * currently hardwired at an arbitrary value of 1000. */
    class Link implements DestinationLink {
	
	private MessageAddress target;
	private MT remote;

	Link(MessageAddress destination) {
	    this.target = destination;
	}

	private void cacheRemote() 
	    throws NameLookupException, UnregisteredNameException
	{
	    if (remote == null) {
		try {
		    remote = lookupRMIObject(target);
		}
		catch (Exception lookup_failure) {
		    throw new  NameLookupException(lookup_failure);
		}

		if (remote == null) 
		    throw new UnregisteredNameException(target);

	    }
	}

	public int cost (Message message) {
	    try {
		cacheRemote();
		return 1000;
	    }
	    catch (Exception ex) {
		// not found
		return Integer.MAX_VALUE;
	    }
	}


	public void forwardMessage(Message message) 
	    throws NameLookupException, 
		   UnregisteredNameException, 
		   CommFailureException
	{
	    cacheRemote();
	    try {
		remote.rerouteMessage(message);
	    } 
	    catch (RemoteException ex) {
		// force recache of remote
		remote = null;
		throw new CommFailureException(ex);
	    }
		    
	}
    }


}
   
