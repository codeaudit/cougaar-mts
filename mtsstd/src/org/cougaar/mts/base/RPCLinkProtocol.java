package org.cougaar.mts.base;

import java.net.InetAddress;
import java.net.URI;
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
import org.cougaar.mts.std.AttributedMessage;

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
	// RMI-specific methods would go here.
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
    abstract protected String getProtocolType();
    
    abstract protected Boolean usesEncryptedSocket();

    abstract protected int computeCost(AttributedMessage message);

    abstract protected DestinationLink createDestinationLink(MessageAddress address);

    abstract protected void findOrMakeNodeServant();

    abstract protected void remakeNodeServant();



    protected void setNodeURI(URI ref)
    {
	this.ref = ref;
    }



    public boolean addressKnown(MessageAddress address) {
	if (loggingService.isErrorEnabled())
	    loggingService.error("The addressKnown method of RMILinkProtocol is no longer supported");
	Link link =  (Link) links.get(address);
	return link != null && link.remote_ref != null;
    }



    public final void registerClient(MessageTransportClient client) {
	synchronized (ipAddrLock) {
	    findOrMakeNodeServant();
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

