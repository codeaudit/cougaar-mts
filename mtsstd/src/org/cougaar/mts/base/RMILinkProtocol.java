/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.mts.base;
import java.net.InetAddress;
import java.net.URI;
import java.rmi.Remote;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.service.IncarnationService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.thread.SchedulableStatus;
import org.cougaar.mts.std.AttributedMessage;
import org.cougaar.mts.std.RMISocketControlService;

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
    private Object ipAddrLock = new Object();
    private ArrayList clients = new ArrayList();
    private IncarnationService incarnationService;
    private WhitePagesService wpService;

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

	incarnationService = (IncarnationService)
	    sb.getService(this, IncarnationService.class, null);
	if (incarnationService == null && loggingService.isWarnEnabled())
	    loggingService.warn("Couldn't load IncarnationService");

	wpService = (WhitePagesService)
	    sb.getService(this, WhitePagesService.class, null);
	if (wpService == null && loggingService.isWarnEnabled())
	    loggingService.warn("Couldn't load WhitePagesService");
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
		// Throwing a CommFailureException doesn't seem right
		// anymore (as of 1.4.2).  So don't do it anymore,
		// but log it.
		if (loggingService.isDebugEnabled())
		    loggingService.debug("Got a SocketException as the cause of a MarshallException: "  + cause.getMessage(), ex);
// 		cause = new TransientIOException(cause.getMessage());
// 		throw new CommFailureException((Exception) cause);
	    }
	} else if (cause instanceof java.rmi.UnmarshalException) {
	    Throwable remote_cause = cause.getCause();
	    if (remote_cause instanceof CougaarIOException) {
		throw new CommFailureException((Exception) remote_cause);
	    }
	} 
    }

    // caller synchronizes on ipAddrLock
    private void findOrMakeMT() {
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



    // Unregister all current clients (inform the WP); close the RMI
    // listener; remake the RMI impl; re-register clients (WP).
    public void ipAddressChanged()
    {
	synchronized (ipAddrLock) {
	    // update hostname property
	    try {
		InetAddress local = InetAddress.getLocalHost();
		String hostaddr = local.getHostAddress();
		System.setProperty("java.rmi.server.hostname", hostaddr);
	    } catch (java.net.UnknownHostException ex) {
		// log something
		if (loggingService.isWarnEnabled())
		    loggingService.warn("Couldn't get localhost: " 
					+ ex.getMessage());
	    }
	    NameSupport ns = getNameSupport();
	    String type = getProtocolType();
	    MessageTransportClient client;
	    for (int i=0; i<clients.size(); i++) {
		client = (MessageTransportClient) clients.get(i);
		ns.unregisterAgentInNameServer(ref, client.getMessageAddress(),
					       type);
	    }
	    try {
		UnicastRemoteObject.unexportObject(myProxy, true);
	    } catch (java.rmi.NoSuchObjectException ex) {
		// don't care
	    }
	    myProxy = null;
	    findOrMakeMT();
	    for (int i=0; i<clients.size(); i++) {
		client = (MessageTransportClient) clients.get(i);
		ns.registerAgentInNameServer(ref, client.getMessageAddress(),
					     type);
	    }
	}
    }

    public final void registerClient(MessageTransportClient client) {
	synchronized (ipAddrLock) {
	    findOrMakeMT();
	    try {
		// Assume node-redirect
		MessageAddress addr = client.getMessageAddress();
		getNameSupport().registerAgentInNameServer(ref,addr,
							   getProtocolType());
		clients.add(client);
	    } catch (Exception e) {
		if (loggingService.isErrorEnabled())
		    loggingService.error("Error registering client", e);
	    }
	}
    }


    public final void unregisterClient(MessageTransportClient client) {
	synchronized (ipAddrLock) {
	    try {
		// Assume node-redirect
		MessageAddress addr = client.getMessageAddress();
		getNameSupport().unregisterAgentInNameServer(ref,addr,
							     getProtocolType());
		clients.remove(client);
	    } catch (Exception e) {
		if (loggingService.isErrorEnabled())
		    loggingService.error("Error unregistering client", e);
	    }
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

    private class WPCallback 
	implements Callback
    {
	Link link;

	WPCallback(Link link)
	{
	    this.link = link;
	}

	private long extractIncarnation(Map entries) 
	{
	    // parse "(.. type=version uri=version:///1234/blah)"
	    if (entries == null) return 0;

	    AddressEntry ae = (AddressEntry) entries.get("version");
	    if (ae == null) return 0;

	    try {
		String path = ae.getURI().getPath();
		int end = path.indexOf('/', 1);
		String incn_str = path.substring(1, end);
		return Long.parseLong(incn_str);
	    } catch (Exception e) {
		if (loggingService.isDetailEnabled()) {
		    loggingService.detail("ignoring invalid version entry: "
					  +ae);
		}
		return 0;
	    }
	}
					    
	public void execute(Response response) {
	    Response.GetAll rg = (Response.GetAll) response;
	    Map entries = rg.getAddressEntries();
	    AddressEntry entry = null;
	    long incn = 0;
	    if (entries != null) {
		entry = (AddressEntry) entries.get(getProtocolType());
		incn = extractIncarnation(entries);
	    }
	    if (loggingService.isDebugEnabled())
		loggingService.debug("Brand spanking new WP callback: " 
				     +entry+ 
				     " incarnation = " +incn);
	    link.handleCallback(entry, incn);
	}
    }

    class Link implements DestinationLink,  IncarnationService.Callback
    {
	
	private MessageAddress target;
	private MT remote;
	private boolean lookup_pending = false;
	private URI lookup_result = null;
	private Object lookup_lock = new Object();
	private long incarnation;
	private Callback lookup_cb = new WPCallback(this);


	protected Link(MessageAddress destination)
	{
	    this.target = destination;
	    // subscribe to IncarnationService
	    if (incarnationService != null) {
		incarnation = incarnationService.getIncarnation(target);
		incarnationService.subscribe(destination, this);
	    }
	}

	private void handleCallback(AddressEntry entry, long incn)
	{
	    synchronized (lookup_lock) {
		lookup_pending = false;
		lookup_result =
		    (entry != null && incn >= incarnation)
		    ? entry.getURI() : null;
	    }
	}


	private void decache() {
	    remote = null;
	    synchronized (lookup_lock) {
		if (!lookup_pending) lookup_result = null;
	    }
	}

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
		    wpService.getAll(target.getAddress(), lookup_cb);
		    // The results may have arrived as part of
		    // registering callback
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





	public void incarnationChanged(MessageAddress addr, long incn) {
	    this.incarnation = incn;
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
	

	public boolean isValid() {
	    try {
		cacheRemote();
		return true;
	    }
	    catch (NameLookupException name_ex) {
		return false;
	    }
	    catch (UnregisteredNameException unknown_ex) {
		// still waiting?
		return false;
	    }
	    catch (Throwable th) {
		loggingService.error("Can't compute RMI cost", th);
		return false;
	    }
	}

	public int cost(AttributedMessage message) {
	    if (remote == null)
		return Integer.MAX_VALUE;
	    else
		return computeCost(message);
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

