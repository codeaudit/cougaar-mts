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

import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.society.MessageSecurityManager;
import org.cougaar.core.society.SecureMessage;

import java.beans.Beans;

/**
 * First attempt at a security aspect.  The message is secured by a
 * RemoteProxy aspect delegate and unsecued by a RemoteImpl aspect
 * delegate.
 * */
public class SecurityAspect extends StandardAspect
{
    private static final String SECURITY_CLASS_PROPERTY =
	"org.cougaar.message.security";
    private static MessageSecurityManager msm = null; 

    private static synchronized MessageSecurityManager ensure_msm() {
	if (msm != null) return msm;

	String name = System.getProperty(SECURITY_CLASS_PROPERTY);
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

    public boolean isEnabled() {
	return enabled;
    }

    // Temporarily package access, rather than private, until we get
    // rid of MessageTransportClassic
    Message secure(Message message) {
	if (msm != null) {
	    if (Debug.debugSecurity()) 
		System.out.println("Securing message " + message);
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
	    if (Debug.debugSecurity())
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



    public Object getDelegate(Object delegate,
			      LinkProtocol protocol,
			      Class type) 
    {
	if (protocol instanceof LoopbackLinkProtocol) {
	    return null;
	} else if (type ==  DestinationLink.class) {
	    return new SecureDestinationLink((DestinationLink) delegate);
	} else if (type == MessageDeliverer.class) {
	    return new SecureDeliverer((MessageDeliverer) delegate);
	} else {
	    return null;
	}
    }
    


    private class SecureDestinationLink implements DestinationLink {
	private DestinationLink link;

	private SecureDestinationLink(DestinationLink link) {
	    this.link = link;
	}

	public void forwardMessage(Message message) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException
	{
	    link.forwardMessage(secure(message));
	}

	public int cost(Message message) {
	    // does signing add cost?
	    return link.cost(message);
	}

    }



    private class SecureDeliverer implements MessageDeliverer {
	private MessageDeliverer deliverer;

	private SecureDeliverer(MessageDeliverer deliverer) {
	    this.deliverer = deliverer;
	}

	public void deliverMessage(Message m, MessageAddress dest) 
	    throws MisdeliveredMessageException
	{
	    deliverer.deliverMessage(unsecure(m), dest);
	}

	public boolean matches(String name) {
	    return deliverer.matches(name);
	}
    }
}
