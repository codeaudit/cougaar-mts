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
    public static final String PROTOCOL_TYPE = "-RMI";

    // private MessageAddress myAddress;
    private MT myProxy;
    private HashMap links;


    public RMILinkProtocol(String id, AspectSupport aspectSupport) {
	super(aspectSupport); 
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
	    nameSupport.lookupAddressInNameServer(address, PROTOCOL_TYPE);

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


    private void makeMT() {
	if (myProxy != null) return;
	try {
	    MessageAddress myAddress = nameSupport.getNodeMessageAddress();
	    MTImpl impl = new MTImpl(myAddress, deliverer);
	    myProxy = getServerSideProxy(impl);
	} catch (java.rmi.RemoteException ex) {
	    ex.printStackTrace();
	}
    }

    public final void registerMTS(MessageAddress addr) {
	makeMT();
	try {
	    Object proxy = myProxy;
	    nameSupport.registerAgentInNameServer(proxy,addr,PROTOCOL_TYPE);
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
	    nameSupport.registerAgentInNameServer(proxy,addr,PROTOCOL_TYPE);
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
	    nameSupport.unregisterAgentInNameServer(proxy,addr,PROTOCOL_TYPE);
	} catch (Exception e) {
	    System.err.println("Error unregistering MessageTransport:");
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
		   CommFailureException,
		   MisdeliveredMessageException
	{
	    cacheRemote();
	    try {
		remote.rerouteMessage(message);
	    } 
	    catch (MisdeliveredMessageException mis) {
		// force recache of remote
		remote = null;
		throw mis;
	    }
	    catch (Exception ex) {
		// force recache of remote
		remote = null;
		// Assume anything else is a comm failure
		throw new CommFailureException(ex);
	    }
	}
    }


}
   
