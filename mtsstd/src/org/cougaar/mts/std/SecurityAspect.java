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
import org.cougaar.core.society.MessageSecurityManager;
import org.cougaar.core.society.SecureMessage;

import java.beans.Beans;

/**
 * First attempt at a security aspect.  The message is secured by a
 * RemoteProxy aspect delegate and unsecued by a RemoteImpl aspect
 * delegate.
 * */
public class SecurityAspect implements MessageTransportAspect
{
    private static MessageSecurityManager msm = null; 

    private static synchronized MessageSecurityManager ensure_msm() {
	if (msm != null) return msm;

	String name = System.getProperty("org.cougaar.message.security");
	if (name != null && (!name.equals("")) &&(!name.equals("none"))) {
	    try {
		// Object raw = Beans.instantiate(null, name);
		Object raw = Class.forName(name).newInstance();
		msm = (MessageSecurityManager) raw;
	    } catch (Exception ex) {
		ex.printStackTrace();
	    }
	}
	return msm;
    }



    private boolean enabled = false;

    public SecurityAspect() {
	enabled = ensure_msm() != null;
    }

    public boolean rejectTransport(MessageTransport transport, int cutpoint) {
	return (transport instanceof LoopbackMessageTransport);
    }

    public boolean isEnabled() {
	return enabled;
    }

    // Temporarily package access, rather than private, until we get
    // rid of MessageTransportClassic
    Message secure(Message message) {
	if (msm != null) {
	    if (Debug.DEBUG_TRANSPORT) System.out.println("Securing message " + message);
	    return msm.secureMessage(message);
	} else {
	    return message;
	}
    }

    // Temporarily package access, rather than private, until we get
    // rid of MessageTransportClassic
    Message unsecure(Message message) {
	if (!(message instanceof SecureMessage)) return message;


	if (msm == null) {
	    System.err.println("MessageTransport "+this+
			       " received SecureMessage "+message+
			       " but has no MessageSecurityManager.");
	    return null;
	} else {
	    if (Debug.DEBUG_TRANSPORT)
		System.out.println("Unsecuring message " + message);
	    Message msg = msm.unsecureMessage((SecureMessage) message);
	    if (msg == null) {
		System.err.println("MessageTransport "+this+
				   " received an unverifiable SecureMessage "
				   +message);
	    }
	    return msg;
	}
    }


    public Object getDelegate(Object delegate, int cutpoint) {
	switch (cutpoint) {
	case DestinationLink:
	    return new SecureDestinationLink((DestinationLink) delegate);

	case ReceiveQueue:
	    return new SecureReceiveQueue((ReceiveQueue) delegate);

	default:
	    return null;
	}
    }
    


    private class SecureDestinationLink implements DestinationLink {
	private DestinationLink link;

	private SecureDestinationLink(DestinationLink link) {
	    this.link = link;
	}

	public void forwardMessage(Message message) 
	    throws DestinationLink.UnregisteredNameException, 
		   DestinationLink.NameLookupException, 
		   DestinationLink.CommFailureException
	{
	    link.forwardMessage(secure(message));
	}

	public int cost(Message message) {
	    // does signing add cost?
	    return link.cost(message);
	}

    }



    private class SecureReceiveQueue implements ReceiveQueue {
	private ReceiveQueue queue;

	private SecureReceiveQueue(ReceiveQueue queue) {
	    this.queue = queue;
	}

	public void deliverMessage(Message m) {
	    queue.deliverMessage(unsecure(m));
	}

	public boolean matches(String name) {
	    return queue.matches(name);
	}

	public int size() {
	    return queue.size();
	}
    }
}
