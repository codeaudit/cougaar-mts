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

import org.cougaar.util.PropertyParser;

/**
 * Aspect that tags all messages with a "send time" attribute when
 * they enter the MTS SendLink.
 */
public class MessageSendTimeAspect 
    extends StandardAspect
    implements AttributeConstants
{ 
    public MessageSendTimeAspect() {
    }
  
    public Object getDelegate(Object object, Class type) {
	if (type == SendLink.class) {
	    return new SendLinkDelegate((SendLink) object);
	} else {
	    return null;
	}
    }
  
    private class SendLinkDelegate
	extends SendLinkDelegateImplBase
    {    
	SendLinkDelegate(SendLink link) {
	    super(link);
	}
    
	public void sendMessage(AttributedMessage message)
	{ 
	    long now = System.currentTimeMillis();
            message.setAttribute(MESSAGE_SEND_TIME_ATTRIBUTE, new Long(now));
	    super.sendMessage(message);
	}
    }
} 
