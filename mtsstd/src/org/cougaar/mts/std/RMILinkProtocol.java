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
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.thread.SchedulableStatus;

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
    private HashMap links;
    private SocketFactory socfac;
    private RMISocketControlService controlService;

    public RMILinkProtocol() {
	super(); 
	links = new HashMap();
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

    // Even though MisdeliveredMessageExceptions are
    // RemoteExceptions, nonethless they'll be wrapped.  Check for
    // this case here.  Also look for IllegalArgumentExceptions,
    // which can also occur as a side-effect of mobility.
    protected void checkForMisdelivery(Throwable ex,
				       AttributedMessage message)
	throws MisdeliveredMessageException
    {
	if (ex instanceof MisdeliveredMessageException) {
	    throw ((MisdeliveredMessageException) ex);
	} else if (ex instanceof IllegalArgumentException) {
	    // Probably a misdelivered message that failed during
	    // deserialization.  Try to check with a string match...
	    String msg = ex.getMessage();
	    int match = msg.indexOf("is not an Agent on this node");
	    if (match > 0) {
		// pretend this is a MisdeliveredMessageException
		throw new MisdeliveredMessageException(message);
	    }
	}
	// If we get here, the caller is responsible for rethrowing
	// the exception.
    }

    protected MessageAttributes doForwarding(MT remote, 
					     AttributedMessage message) 
	throws MisdeliveredMessageException, 
	       java.rmi.RemoteException,
	       CommFailureException 
    // Declare CommFailureException because the signature needs to
    // match SerializedRMILinkProtocol's doForwarding method.  That
    // exception will never be thrown here.
    {
	MessageAttributes result = null;
	try {
	    SchedulableStatus.beginNetIO("RMI call");
	    result = remote.rerouteMessage(message);
	} catch (java.rmi.RemoteException remote_ex) {
	    Throwable cause = remote_ex.getCause();
	    checkForMisdelivery(cause, message);
	    // Not a misdelivery  - rethrow the remote exception
	    throw remote_ex;
	} catch (IllegalArgumentException illegal_arg) {
	    checkForMisdelivery(illegal_arg, message);
	    // Not a misdelivery  - rethrow the exception
	    throw illegal_arg;
	} finally {
	    SchedulableStatus.endBlocking();
	}
	return result;
    }


    protected Boolean usesEncryptedSocket() {
	return Boolean.FALSE;
    }


    // Standard RMI handling of security and other cougaar-specific io
    // exceptions. Subclasses may need to do something different (see
    // CORBALinkProtocol).
    //
    // If the argument itself is a MarshalException whose cause is a
    // CougaarIOException, a local cougaar-specific error has occured.
    //
    // If the argument is some other RemoteException whose cause is an
    // UnmarshalException whose cause in turn is a CougaarIOException,
    // a remote cougaar-specific error has occured.
    //
    // Otherwise this is some other kind of remote error.
    protected void handleSecurityException(Exception ex) 
	throws CommFailureException
    {
	Throwable cause = ex.getCause();
	if (ex instanceof java.rmi.MarshalException) {
	    if (cause instanceof CougaarIOException) {
		throw new CommFailureException((Exception) cause);
	    } 
	    // When a TransientIOException is thrown sometimes it
	    // triggers different exception on the socket, which gets
	    // through instead of the TransientIOException. For now we
	    // will catch these and treat them as if they were
	    // transient (though other kinds of SocketExceptions
	    // really shouldn't be).
	    else if (cause instanceof java.net.SocketException) {
		cause = new TransientIOException(cause.getMessage());
		throw new CommFailureException((Exception) cause);
	    }
	} else if (cause instanceof java.rmi.UnmarshalException) {
	    Throwable remote_cause = cause.getCause();
	    if (remote_cause instanceof CougaarIOException) {
		throw new CommFailureException((Exception) remote_cause);
	    }
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
	if (loggingService.isErrorEnabled())
	    loggingService.error("The addressKnown method of RMILinkProtocol is no longer supported");
	Link link =  (Link) links.get(address);
	return link != null && link.remote != null;
    }


    // Factory methods:

    public DestinationLink getDestinationLink(MessageAddress address) {
	DestinationLink link = null;
	synchronized (links) {
	    link = (DestinationLink) links.get(address);
	    if (link == null) {
		link = new Link(address); // attach aspects
		link = (DestinationLink) 
		    attachAspects(link, DestinationLink.class);
		links.put(address, link);
	    }
	}

	return link;
    }






    class Link implements DestinationLink
    {
	
	private MessageAddress target;
	private MT remote;
	private boolean lookup_pending = false;
	private URI lookup_result = null;
	private Object lookup_lock = new Object();


	protected Link(MessageAddress destination)
	{
	    this.target = destination;
	}

	private void decache() {
	    remote = null;
	    synchronized (lookup_lock) {
		if (!lookup_pending) lookup_result = null;
	    }
	}

	private Callback lookup_cb = new Callback() {
		public void execute(Response response) {
		    Response.Get rg = (Response.Get) response;
		    AddressEntry entry = rg.getAddressEntry();
		    if (loggingService.isDebugEnabled())
			loggingService.debug("WP callback: " +entry);
		    synchronized (lookup_lock) {
			lookup_pending = false;
			lookup_result =(entry != null) ? entry.getURI() : null;
		    }
		}
	    };


	private MT lookupRMIObject() 
	    throws Exception 
	{
	    if (getRegistry().isLocalClient(target)) {
		// myself as an RMI stub
		return myProxy;
	    }

	    URI ref = null;
	    Object object = null;

	    synchronized (lookup_lock) {
		if (lookup_result != null) {
		    ref = lookup_result;
		} else if (lookup_pending) {
		    return  null;
		} else {
		    lookup_pending = true;
		    String ptype = getProtocolType();
		    getNameSupport().lookupAddressInNameServer(target, 
							       ptype, 
							       lookup_cb);
		    // The results may have arrived as part of registering callback
		    if (lookup_result != null) {
			ref = lookup_result;
		    } else {
			return null;
		    }
		}
	    }

	    try {
		// This call can block in net i/o
		SchedulableStatus.beginNetIO("RMI reference decode");
		object = RMIRemoteObjectDecoder.decode(ref);
	    } catch (Throwable ex) {
		loggingService.error("Can't decode URI " +ref, ex);
	    } finally {
		SchedulableStatus.endBlocking();
	    }


	    if (object == null) return null;

	    if (controlService != null) 
		controlService.setReferenceAddress((Remote) object, target);
	

	    if (object instanceof MT) {
		return (MT) object;
	    } else {
		throw new RuntimeException("Object "
					   +object+
					   " is not a MessageTransport!");
	    }
	}





	// *** HACK ****.  This is called from MTImpl.  Should be part
	// *** of the DestinationLink interface.
	public void incarnationChanged() {
	    decache();
	}

	private void cacheRemote() 
	    throws NameLookupException, UnregisteredNameException
	{
	    if (remote == null) {
		try {
		    remote = lookupRMIObject();
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
		decache();
		throw mis;
	    }
	    // RMILinkProtocol won't throw this but subclasses might.
	    catch (CommFailureException cfe) {
		// force recache of remote
		decache();
		throw cfe;
	    }
	    catch (java.rmi.RemoteException ex) {
		if (loggingService.isDebugEnabled()) {
		    loggingService.debug("RemoteException", ex);
		}
		handleSecurityException(ex);
		// If we get here it wasn't a security exception
		decache();
		throw new CommFailureException(ex);
	    }
	    catch (Exception ex) {
		// Ordinary comm failure.  Force recache of remote
		if (loggingService.isDebugEnabled()) {
		    loggingService.debug("Ordinary comm failure", ex);
		}
		decache();
		//  Ordinary comm failure
		throw new CommFailureException(ex);
	    }
	}



	public Object getRemoteReference() {
	    return remote;
	}

	public void addMessageAttributes(MessageAttributes attrs) {
	    attrs.addValue(MessageAttributes.IS_STREAMING_ATTRIBUTE,
			   Boolean.TRUE);
	    

	    attrs.addValue(MessageAttributes.ENCRYPTED_SOCKET_ATTRIBUTE,
			   usesEncryptedSocket());

	}

    }

}

