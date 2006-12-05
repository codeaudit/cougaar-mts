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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.service.IncarnationService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.mts.std.AttributedMessage;

/**
 * This is the parent class of all {@link LinkProtocol}s that use a
 * remote-procedure-call (rpc) semantics.  Instantiable extensions
 * must provide implementations for a small set of abstract methods,
 * covering protocol specifics.
 */
abstract public class RPCLinkProtocol
    extends LinkProtocol
{

    private URI ref;
    private IncarnationService incarnationService;
    private WhitePagesService wpService;
    private HashMap links;
    private Object ipAddrLock = new Object();
    private ArrayList clients = new ArrayList();


    // Just for testing -- an example of supplying a service from a
    // LinkProtocol.

    public interface Service extends LinkProtocolService {
	// protocol-specific methods would go here.
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




    // If LinkProtocols classes want to define this method, eg in
    // order to provide a service, they should not in general invoke
    // super.load(), since if they do they'll end up clobbering any
    // services defined by super classes service.  Instead they should
    // use super_load(), defined in LinkProtocol, which runs the
    // standard load() method without running any intervening ones.
    public void load() {
	super_load();
	links = new HashMap();

	ServiceBroker sb = getServiceBroker();
	sb.addService(Service.class, this);

	incarnationService = (IncarnationService)
	    sb.getService(this, IncarnationService.class, null);
	if (incarnationService == null && loggingService.isWarnEnabled())
	    loggingService.warn("Couldn't load IncarnationService");

	wpService = (WhitePagesService)
	    sb.getService(this, WhitePagesService.class, null);
	if (wpService == null && loggingService.isWarnEnabled())
	    loggingService.warn("Couldn't load WhitePagesService");
    }



    // subclass responsibility

    /**
     * @return an identifier of the protocol that can be used by the
     * WP to distinguish them from one another.
     */
    abstract protected String getProtocolType();
    
    /**
     * @return a boolean indicating where or not the protocol uses ssl
     */
    abstract protected Boolean usesEncryptedSocket();

    /**
     * @return the cost of transmitting the message over this
     * protocol.
     */
    abstract protected int computeCost(AttributedMessage message);

    /**
     * Return a protocol-specific {@link DestinationLink} for the
     * target address.  The name should not be taken to mean that a
     * new link will be created for every call.
    */
    abstract protected DestinationLink createDestinationLink(MessageAddress address);

    /**
     * Ensure that some abstract form of 'servant' object exists for
     * this protocol that will allow other Nodes to send messages to his
     * one.
    */
    abstract protected void findOrMakeNodeServant();

    /**
     * Releases all resources associated with this link protocol.
     * <p>
     * 
     * This method is invoked when the MTS is unloaded so the garbage
     * collector can reclaim all resources that had been allocated.
     * 
     * Fix for bug 3965:
     * http://bugs.cougaar.org/show_bug.cgi?id=3965
     */
    abstract protected void releaseNodeServant();
    
    /**
     * Force the proticol to remake its 'servant', typically because
     * the address of the Host on which the Node is running has changed.
     * Some protoocol (eg HTTP) can ignore this.
    */
    abstract protected void remakeNodeServant();



    protected void setNodeURI(URI ref) {
	this.ref = ref;
    }



    public boolean addressKnown(MessageAddress address) {
	if (loggingService.isErrorEnabled())
	    loggingService.error("The addressKnown method of RMILinkProtocol is no longer supported");
	Link link =  (Link) links.get(address);
	return link != null && link.remote_ref != null;
    }

    protected boolean isServantAlive() {
	return true;
    }

    public final void registerClient(MessageTransportClient client) {
	synchronized (ipAddrLock) {
	    findOrMakeNodeServant();
	    if (isServantAlive()) {
		try {
		    // Assume node-redirect
		    MessageAddress addr = client.getMessageAddress();
		    getNameSupport().registerAgentInNameServer(ref, addr, getProtocolType());
		} catch (Exception e) {
		    if (loggingService.isErrorEnabled())
			loggingService.error("Error registering client", e);
		}
	    }
	    clients.add(client);
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

        // Fix for bug 3965: need to release resources.
        // http://bugs.cougaar.org/show_bug.cgi?id=3965
	// 
	// XXX: We do NOT want to release the node servant when a single
	// client unregisters.  Releasing node-level resources has to 
	// happen elsewhere (don't know where yet).
        releaseNodeServant();
        
        } catch (Exception e) {
		if (loggingService.isErrorEnabled())
		    loggingService.error("Error unregistering client", e);
	    }
	}
    }
    
    public final void reregisterClients() {
	synchronized (ipAddrLock) {
	    if (ref != null) {
		String protocolType = getProtocolType();
		for (int i=0; i<clients.size(); i++) {
		    MessageTransportClient client = (MessageTransportClient) clients.get(i);
		    MessageAddress addr = client.getMessageAddress();
		    getNameSupport().registerAgentInNameServer(ref, addr, protocolType);
		}
	    }
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
		SystemProperties.setProperty("java.rmi.server.hostname", hostaddr);
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

	    remakeNodeServant();
	    for (int i=0; i<clients.size(); i++) {
		client = (MessageTransportClient) clients.get(i);
		ns.registerAgentInNameServer(ref, client.getMessageAddress(),
					     type);
	    }
	}
    }




    // Factory methods:

    public DestinationLink getDestinationLink(MessageAddress address) {
	DestinationLink link = null;
	synchronized (links) {
	    link = (DestinationLink) links.get(address);
	    if (link == null) {
		link = createDestinationLink(address);
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
	    link.handleWPCallback(entry, incn);
	}
    }

    abstract protected class Link
	implements DestinationLink,  IncarnationService.Callback
    {
	
	private MessageAddress target;
	private boolean lookup_pending = false;
	private AddressEntry lookup_result = null;
	private Object lookup_lock = new Object();
	private long incarnation;
	private Callback lookup_cb = new WPCallback(this);
	private Object remote_lock = new Object();
	private Object remote_ref; // URI, URL etc

	protected Link(MessageAddress destination)
	{
	    this.target = destination;
	    // subscribe to IncarnationService
	    if (incarnationService != null) {
		incarnation = incarnationService.getIncarnation(target);
		incarnationService.subscribe(destination, this);
	    }
	}

	abstract protected Object decodeRemoteRef(URI ref)
	    throws Exception;


	abstract protected MessageAttributes forwardByProtocol(Object remote,
							       AttributedMessage message)
	    throws NameLookupException, 
		   UnregisteredNameException, 
		   CommFailureException,
		   MisdeliveredMessageException;



	// WP callback
	private void handleWPCallback(AddressEntry entry, long incn)
	{
	    synchronized (lookup_lock) {
		lookup_pending = false;
		if (incn > incarnation) {
		    // tell the incarnation service
		    incarnationService.updateIncarnation(target, incn);
		    incarnation = incn;
		}
		lookup_result =
		    (entry != null && incn == incarnation)
		    ? entry : null;
	    }
	}


	protected void decache() {
	    synchronized (remote_lock) {
		remote_ref = null;
	    }
	    synchronized (lookup_lock) {
		if (!lookup_pending) lookup_result = null;
	    }
	}

	protected URI getRemoteURI()
	{
	    synchronized (lookup_lock) {
		if (lookup_result != null) {
		    return lookup_result.getURI();
		} else if (lookup_pending) {
		    return  null;
		} else {
		    lookup_pending = true;
		    wpService.getAll(target.getAddress(), lookup_cb);
		    // The results may have arrived as part of
		    // registering callback
		    if (lookup_result != null) {
			return lookup_result.getURI();
		    } else {
			return null;
		    }
		}
	    }
	}


	// IncarnationService callback
	public void incarnationChanged(MessageAddress addr, long incn) {
	    synchronized (lookup_lock) {
		if (incn > incarnation) {
		    // newer value -- decache the stub
		    incarnation = incn;
		    decache();
		} else if (incn < incarnation) {
		    // out-of-date info
		    if (loggingService.isWarnEnabled())
			loggingService.warn("Incarnation service callback has out of date incarnation number " 
					    +incn+
					    " for " 
					    +addr);
		}
	    }
	}



	// NB: Intentionally not synchronized on remote_lock because a
	// network invocation can happen in lookupRMIObject().  Worst
	// case scenario: normal return (no exception) but 'remote' has
	// been reset to null in the meantime.  The two callers
	// (isValid and forwardMessage) deal with this case.
	protected void cacheRemote() 
	    throws NameLookupException, UnregisteredNameException
	{
	    if (remote_ref == null) {
		try {
		    URI ref = getRemoteURI();
		    remote_ref = decodeRemoteRef(ref);
		}
		catch (Exception lookup_failure) {
		    throw new  NameLookupException(lookup_failure);
		}

		if (remote_ref == null) 
		    throw new UnregisteredNameException(target);

	    }
	}



	public MessageAttributes forwardMessage(AttributedMessage message) 
	    throws NameLookupException, 
		   UnregisteredNameException, 
		   CommFailureException,
		   MisdeliveredMessageException
	{
	    cacheRemote();
	    // Ordinarily cacheRemote either throws an Exception or
	    // caches a non-null reference.  But with the addition of
	    // the IncarnationService callbacks, the reference can now
	    // be clobbered subsequently by another thread.  Deal with
	    // that here.
	    synchronized (remote_lock) {
		if (remote_ref == null) {
		    Exception cause = 
			new Exception("Inconsistent remote reference cache");
		    throw new NameLookupException(cause);
		} else {
		    return forwardByProtocol(remote_ref, message);
		}
	    }
	}


	public boolean retryFailedMessage(AttributedMessage message,
					  int retryCount) 
	{
	    return true;
	}

    

	public boolean isValid() {
	    try {
		cacheRemote();
		// Ordinarily cacheRemote either throws an Exception
		// or caches a non-null reference.  But with the
		// addition of the IncarnationService callback, the
		// reference can now be clobbered subsequently by
		// another thread.  Deal with that here.
		synchronized (remote_lock) {
		    return remote_ref != null;
		}
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
	    synchronized (remote_lock) {
		if (remote_ref == null)
		    return Integer.MAX_VALUE;
		else
		    return computeCost(message);
	    }
	}

	public MessageAddress getDestination() {
	    return target;
	}



	public Object getRemoteReference() {
	    return remote_ref;
	}

	public void addMessageAttributes(MessageAttributes attrs) {
	    attrs.addValue(MessageAttributes.IS_STREAMING_ATTRIBUTE,
			   Boolean.TRUE);
	    

	    attrs.addValue(MessageAttributes.ENCRYPTED_SOCKET_ATTRIBUTE,
			   usesEncryptedSocket());

	}

    }

}

