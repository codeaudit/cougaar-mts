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

package org.cougaar.mts.corba;

import org.cougaar.mts.corba.idlj.*;

import org.cougaar.mts.std.AspectSupport;
import org.cougaar.mts.std.AttributedMessage;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.LinkProtocol;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.mts.base.DontRetryException;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.core.mts.SerializationUtils;
import org.cougaar.mts.base.UnregisteredNameException;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Response;

import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import java.net.URI;
import java.util.HashMap;

public class CorbaLinkProtocol 
    extends LinkProtocol
{
    public static final String PROTOCOL_TYPE = "-CORBA";

    // private MessageAddress myAddress;
    private MT myProxy;
    private URI reference;
    private HashMap links;
    private ORB orb;
    private POA poa;


    public CorbaLinkProtocol() {
	super(); 
	links = new HashMap();
	String[] args = null;
	orb = ORB.init(args, null);
	try {
	    org.omg.CORBA.Object raw = 
		orb.resolve_initial_references("RootPOA");
	    poa = POAHelper.narrow(raw);
	    poa.the_POAManager().activate();
	} catch (Exception error) {
	    loggingService.error(null, error);
	}
	
    }


    private synchronized void makeMT() {
	if (myProxy != null) return;
	MessageAddress myAddress = getNameSupport().getNodeMessageAddress();
	MTImpl impl = new MTImpl(myAddress, getDeliverer());
	try {
	    poa.activate_object(impl);
	} catch (Exception ex) {
	    loggingService.error(null, ex);
	}
	myProxy = impl._this();
	reference =  URI.create(orb.object_to_string(myProxy));
    }

    public final void registerClient(MessageTransportClient client) {
	makeMT();
	try {
	    // Assume node-redirect
	    MessageAddress addr = client.getMessageAddress();
	    getNameSupport().registerAgentInNameServer(reference, addr,
						       PROTOCOL_TYPE);
	} catch (Exception e) {
	    loggingService.error("Error registering MessageTransport", e);
	}
    }


    public final void unregisterClient(MessageTransportClient client) {
	try {
	    // Assume node-redirect
	    Object proxy = orb.object_to_string(myProxy);
	    MessageAddress addr = client.getMessageAddress();
	    getNameSupport().unregisterAgentInNameServer(reference, addr,
							 PROTOCOL_TYPE);
	} catch (Exception e) {
	    loggingService.error("Error unregistering MessageTransport", e);
	}
    }



    public boolean addressKnown(MessageAddress address) {
	if (loggingService.isErrorEnabled())
	    loggingService.error("The addressKnown method of CorbaLinkProtocol is no longer supported");
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
		link =(DestinationLink)
		    attachAspects(link, DestinationLink.class);
		links.put(address, link);
	    }
	}
	return link;
    }



    /**
     * The DestinationLink class for this transport.  Forwarding a
     * message with this link means looking up the MT proxy for a
     * remote MTImpl, and calling rerouteMessage on it.  The cost is
     * currently hardwired at an arbitrary value of 1000. */
    class Link implements DestinationLink {
	
	private MessageAddress target;
	private MT remote;
	private boolean lookup_pending = false;
	private URI lookup_result = null;
	private Object lookup_lock = new Object();

	private Callback lookup_cb = new Callback() {
		public void execute(Response response) {
		    Response.Get rg = (Response.Get) response;
		    AddressEntry entry = rg.getAddressEntry();
		    synchronized (lookup_lock) {
			lookup_pending = false;
			lookup_result =(entry != null) ? entry.getURI() : null;
		    }
		}
	    };



	Link(MessageAddress destination) {
	    this.target = destination;
	}

	private void decache() {
	    remote = null;
	    synchronized (lookup_lock) {
		if (!lookup_pending) lookup_result = null;
	    }
	}

	private MT lookupObject() {
	    URI reference = null;
	    Object object = null;

	    synchronized (lookup_lock) {
		if (lookup_result != null) {
		    reference = lookup_result;
		} else if (lookup_pending) {
		    return  null;
		} else {
		    lookup_pending = true;
		    getNameSupport().lookupAddressInNameServer(target, 
							       PROTOCOL_TYPE,
							       lookup_cb);
		    return null;
		}
	    }

	    String ior = reference.toString();
	    org.omg.CORBA.Object raw = orb.string_to_object(ior);
	    MT mt = MTHelper.narrow(raw);
	    return mt;
	}

	private void cacheRemote() 
	    throws NameLookupException, UnregisteredNameException
	{
	    if (remote == null) {
		try {
		    remote = lookupObject();
		}
		catch (Exception lookup_failure) {
		    throw new  NameLookupException(lookup_failure);
		}

		if (remote == null) 
		    throw new UnregisteredNameException(target);

	    }
	}

	public Class getProtocolClass() {
	    return CorbaLinkProtocol.class;
	}

	public boolean retryFailedMessage(AttributedMessage message, 
					  int retryCount) 
	{
	    return true;
	}

	public boolean isValid() {
	    try {
		cacheRemote();
		return true;
	    }
	    catch (Exception ex) {
		// not found
		return false;
	    }
	}

	public int cost (AttributedMessage message) {
	    return 500;
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
	    byte[] bytes = null;
	    try {
		bytes = SerializationUtils.toByteArray(message);
	    } catch (DontRetryException mex) {
		throw new CommFailureException(mex);
	    } catch (java.io.IOException iox) {
		// What would this mean?
	    }

	    byte[] res = null;
	    try {
		res = remote.rerouteMessage(bytes);
	    } catch (CorbaMisdeliveredMessage mis) {
		// force recache of remote
		decache();
		throw new MisdeliveredMessageException(message);
	    } catch (CorbaDontRetryException mex) {
		byte[] ex_bytes = mex.cause;
		try {
		    DontRetryException mse = (DontRetryException)
			SerializationUtils.fromByteArray(ex_bytes);
		    throw new CommFailureException(mse);
		} catch (Exception ex) {
		    // ???
		}
	    } catch (Exception corba_ex) {
		// Some other CORBA failure.  Decache and retry.
		decache();
		throw new CommFailureException(corba_ex);
	    }
	    
	    MessageAttributes attrs = null;
	    try {
		attrs = (MessageAttributes) 
		    SerializationUtils.fromByteArray(res);
	    } catch (DontRetryException mex) {
		throw new CommFailureException(mex);
	    } catch (java.io.IOException iox) {
		// What would this mean?
	    } catch (ClassNotFoundException cnf) {
	    }

	    return attrs;
	}


	public Object getRemoteReference() {
	    return remote;
	}

	
	public void addMessageAttributes(MessageAttributes attrs) {
	    attrs.addValue(MessageAttributes.IS_STREAMING_ATTRIBUTE,
			   Boolean.TRUE);
	    

	    // Always FALSE for now
	    attrs.addValue(MessageAttributes.ENCRYPTED_SOCKET_ATTRIBUTE,
			   Boolean.FALSE);

	}


    }


}
   
