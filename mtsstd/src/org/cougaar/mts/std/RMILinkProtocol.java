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

import java.net.URI;
import java.rmi.MarshalException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.RMILinkProtocol.Service;
import org.cougaar.core.service.LoggingService;

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
    private URI ref;
    private HashMap links, remoteRefs;
    private SocketFactory socfac;
    private RMISocketControlService controlService;

    public RMILinkProtocol() {
	super(); 
	links = new HashMap();
	remoteRefs = new HashMap();
	socfac = getSocketFactory();
    }



    // Just for testing -- an example of supplying a service from a
    // LinkProtocol.

    public interface Service extends LinkProtocolService {
	// RMI-specific methods would go here.
    }

    private class ServiceProxy 
	extends LinkProtocol.ServiceProxy 
	implements Service
    {

    }

    // If LinkProtocols classes want to define this method, eg in
    // order to provide a service, they should not in general invoke
    // super.load(), since if they do they'll end up clobbering any
    // services defined by super classes service.  Instead they should
    // use super_load(), defined in LinkProtocol, which runs the
    // standard load() method without running any intervening ones.
    public void load() {
	super_load();
	ServiceBroker sb = getServiceBroker();
	sb.addService(Service.class, this);

	// RMISocketControlService could be null
	controlService = (RMISocketControlService)
	    sb.getService(this, RMISocketControlService.class, null);
    }


    public Object getService(ServiceBroker sb,
			     Object requestor, 
			     Class serviceClass)
    {
	if (serviceClass == Service.class) {
	    return new ServiceProxy();
	} else {
	    return null;
	}
    }

    public void releaseService(ServiceBroker sb,
			       Object requestor,
			       Class serviceClass,
			       Object service)
    {

	if (serviceClass == Service.class) {
	    // no-op for this example
	}
    }




    protected String getProtocolType() {
	return "-RMI";
    }

    protected SocketFactory getSocketFactory() {
	return new SocketFactory(false, true);
    }


    // If this is called, we've already found the remote reference.
    protected int computeCost(AttributedMessage message) {
	return 1000;
    }


    protected MTImpl makeMTImpl(MessageAddress myAddress,
				SocketFactory socfac)
	throws java.rmi.RemoteException
    {
	return new MTImpl(myAddress, getServiceBroker(), socfac);
    }

    protected MessageAttributes doForwarding(MT remote, 
					     AttributedMessage message) 
	throws MisdeliveredMessageException, 
	       CommFailureException,
	       java.rmi.RemoteException
    {
	return remote.rerouteMessage(message);
    }


    protected Boolean usesEncryptedSocket() {
	return Boolean.FALSE;
    }


    // Standard RMI handling of security exceptions. Subclasses may
    // need to do something different (see CORBALinkProtocol).
    //
    // If the argument itself is a MarshalException whose cause is a
    // MessageSecurityException, a local security error has occured.
    //
    // If the argument is some other RemoteException whose cause is an
    // UnmarshalException whose cause in turn is a MessageSecurityEx,
    // a remote security error has occured.
    //
    // Otherwise this is some other kind of remote error.
    protected void handleSecurityException(Exception ex) 
	throws CommFailureException
    {
	Throwable cause = ex.getCause();
	if (ex instanceof java.rmi.MarshalException) {
	    if (cause instanceof DontRetryException) {
		throw new CommFailureException((Exception) cause);
	    }
	} else if (cause instanceof java.rmi.UnmarshalException) {
	    Throwable remote_cause = cause.getCause();
	    if (remote_cause instanceof DontRetryException) {
		throw new CommFailureException((Exception) remote_cause);
	    }
	} 
    }

    private MT lookupRMIObject(MessageAddress address, boolean getProxy) 
	throws Exception 
    {
	URI ref =  
	    getNameSupport().lookupAddressInNameServer(address, 
						       getProtocolType());

	if (ref == null) return null;

	Object object = null;
	try {
	    object = RMIRemoteObjectDecoder.decode(ref);
	} catch (Throwable ex) {
	    loggingService.error("Can't decode URI " +ref, ex);
	}

	remoteRefs.put(address, object);

	if (object == null || !getProxy) return (MT) object;

	if (controlService != null) 
	    controlService.setReferenceAddress((Remote) object, address);
	

	if (object instanceof MT) {
	    return (MT) object;
	} else {
	    throw new RuntimeException("Object "
				       +object+
				       " is not a MessageTransport!");
	}

    }


    private synchronized void findOrMakeMT() {
	if (myProxy != null) return;
	try {
	    MessageAddress myAddress = 
		getNameSupport().getNodeMessageAddress();
	    myProxy = makeMTImpl(myAddress, socfac);
	    Remote remote
		= UnicastRemoteObject.exportObject(myProxy, 0, socfac, socfac);
	    ref =  RMIRemoteObjectEncoder.encode(remote);
	} catch (java.rmi.RemoteException ex) {
	    loggingService.error(null, ex);
	} catch (Exception other) {
	    loggingService.error(null, other);
	}
    }



    public final void registerMTS(MessageAddress addr) {
	// No-op now.  Probably the whole notion of per-protocol
	// registerMTS is no longer useful.
    }

    public final void registerClient(MessageTransportClient client) {
	findOrMakeMT();
	try {
	    // Assume node-redirect
	    MessageAddress addr = client.getMessageAddress();
	    getNameSupport().registerAgentInNameServer(ref,addr,
						       getProtocolType());
	} catch (Exception e) {
	    if (loggingService.isErrorEnabled())
		loggingService.error("Error registering client", e);
	}
    }


    public final void unregisterClient(MessageTransportClient client) {
	try {
	    // Assume node-redirect
	    MessageAddress addr = client.getMessageAddress();
	    getNameSupport().unregisterAgentInNameServer(ref,addr,
							 getProtocolType());
	} catch (Exception e) {
	    if (loggingService.isErrorEnabled())
		loggingService.error("Error unregistering client", e);
	}
    }



    public boolean addressKnown(MessageAddress address) {
	try {
	    return lookupRMIObject(address, false) != null;
	} catch (Exception e) {
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






    class Link implements DestinationLink
    {
	
	private MessageAddress target;
	private MT remote;

	protected Link(MessageAddress destination)
	{
	    this.target = destination;
	}


	// *** HACK ****.  This is called from MTImpl.  Should be part
	// *** of the DestinationLink interface.
	public void incarnationChanged() {
	    remote = null;
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

	public boolean retryFailedMessage(AttributedMessage message,
					  int retryCount) 
	{
	    return true;
	}

    
	public Class getProtocolClass() {
	    return RMILinkProtocol.this.getClass();
	}
	

	public int cost (AttributedMessage message) {
	    try {
		cacheRemote();
		return computeCost(message);
	    }
	    catch (Exception ex) {
		// not found, fail silently
		return Integer.MAX_VALUE;
	    }
	    catch (Throwable th) {
		loggingService.error("Can't compute RMI cost", th);
		return Integer.MAX_VALUE;
	    }
	}


	public MessageAddress getDestination() {
	    return target;
	}


	public MessageAttributes forwardMessage(AttributedMessage message) 
	    throws NameLookupException, 
		   UnregisteredNameException, 
		   CommFailureException,
		   MisdeliveredMessageException
	{
	    cacheRemote();
	    try {
		return doForwarding(remote, message);
	    } 
	    catch (MisdeliveredMessageException mis) {
		// force recache of remote
		remote = null;
		throw mis;
	    }
	    catch (CommFailureException cfe) {
		// force recache of remote
		remote = null;
		throw cfe;
	    }
	    catch (java.rmi.RemoteException ex) {
		if (Debug.isErrorEnabled(loggingService,COMM)) 
		    loggingService.error(null, ex);
		handleSecurityException(ex);
		// If we get here it wasn't a security exception
		remote = null;
		throw new CommFailureException(ex);
	    }
	    catch (Exception ex) {
		// Ordinary comm failure.  Force recache of remote
		if (Debug.isErrorEnabled(loggingService,COMM)) 
		    loggingService.error(null, ex);
		remote = null;
		//  Ordinary comm failure
		throw new CommFailureException(ex);
	    }
	}



	public Object getRemoteReference() {
	    return remoteRefs.get(target);
	}

	public void addMessageAttributes(MessageAttributes attrs) {
	    attrs.addValue(MessageAttributes.IS_STREAMING_ATTRIBUTE,
			   Boolean.TRUE);
	    

	    attrs.addValue(MessageAttributes.ENCRYPTED_SOCKET_ATTRIBUTE,
			   usesEncryptedSocket());

	}

    }

}

