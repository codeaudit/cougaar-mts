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


import org.cougaar.core.node.DummyMessageSecurityManager;
import org.cougaar.core.node.SecureMessage;

import java.beans.Beans;

/**
 * First attempt at a security aspect.  The message is secured by a
 * RemoteProxy aspect delegate and unsecued by a RemoteImpl aspect
 * delegate.
 * @property org.cougaar.message.security Controls the MessageTransport SecurityAspect.
 */
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
	    }
	} else {
	    msm = new DummyMessageSecurityManager();
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
    AttributedMessage secure(AttributedMessage message) {
	if (msm != null) {
	    if (Debug.isDebugEnabled(loggingService,SECURITY)) 
		loggingService.debug("Securing message " + message);
	    Message rawMessage =  message.getRawMessage();
	    Message secureMsg = msm.secureMessage(rawMessage);
	    return new AttributedMessage(secureMsg, message);
	} else {
	    return message;
	}
    }

    // Temporarily package access, rather than private, until we get
    // rid of MessageTransportClassic
    AttributedMessage unsecure(AttributedMessage message) {
	if (msm == null) {
	    if (loggingService.isErrorEnabled())
		loggingService.error("MessageTransport "+this+
					  " received SecureMessage "+message+
					  " but has no MessageSecurityManager.");
	    return null;
	} else {
	    if (Debug.isDebugEnabled(loggingService,SECURITY))
		loggingService.debug("Unsecuring message " + message);
	    SecureMessage rawMessage = (SecureMessage) message.getRawMessage();
	    Message originalMessage = msm.unsecureMessage(rawMessage);
	    AttributedMessage msg = 
		new AttributedMessage(originalMessage, message);
	    if (msg == null && loggingService.isErrorEnabled()) {
		loggingService.error("MessageTransport "+this+
					  " received an unverifiable SecureMessage "
					  +message);
	    }
	    return msg;
	}
    }



    public Object getDelegate(Object delegate, Class type) 
    {
	if (type ==  DestinationLink.class) {
	    DestinationLink link = (DestinationLink) delegate;
	    return new SecureDestinationLink(link);
	} else {
	    return null;
	}
    }


    public Object getReverseDelegate(Object delegate, Class type) 
    {
	if (type == MessageDeliverer.class) {
	    return new SecureDeliverer((MessageDeliverer) delegate);
	} else {
	    return null;
	}
    }
    


    private class SecureDestinationLink 
	extends DestinationLinkDelegateImplBase 
    {
	private SecureDestinationLink(DestinationLink link) {
	    super(link);
	}

	public MessageAttributes forwardMessage(AttributedMessage message) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException
	{
	    return super.forwardMessage(secure(message));
	}


    }



    private class SecureDeliverer extends MessageDelivererDelegateImplBase {
	private SecureDeliverer(MessageDeliverer deliverer) {
	    super(deliverer);
	}

	public MessageAttributes deliverMessage(AttributedMessage m, 
						MessageAddress dest) 
	    throws MisdeliveredMessageException
	{
	    return super.deliverMessage(unsecure(m), dest);
	}

    }
}
