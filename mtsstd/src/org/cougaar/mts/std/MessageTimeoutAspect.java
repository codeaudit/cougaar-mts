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
 * Aspect to throw out a timed out message. Necessary for MsgLog et. al. 
 * Checks every thread in MTS for timed out attributes on a message:<pre>
 *   -SendLink
 *   -Router
 *   -DestinationLink
 *   -ReceiveLink
 * </pre>
 *
 * @property org.cougaar.syncClock
 *   Is NTP clock synchronization guaranteed?  default is false.
 */
public class MessageTimeoutAspect 
    extends StandardAspect
    implements AttributeConstants
{ 
    public static final boolean SYNC_CLOCK_AVAILABLE =
	PropertyParser.getBoolean("org.cougaar.syncClock", false);

    public MessageTimeoutAspect() {
    }
  
  
    // Helper methods
    private boolean delivered(MessageAttributes attributes) {
	return 
	    attributes != null &
	    attributes.getAttribute(DELIVERY_ATTRIBUTE).equals(DELIVERY_STATUS_DELIVERED);
    }
  
    // retrieves absolute timeout that convertTimeout stored for us
    private long getTimeout(AttributedMessage message) {
	long the_timeout = -1;
	Object attr = message.getAttribute(MESSAGE_SEND_DEADLINE_ATTRIBUTE);
	if(attr!=null) { // check for, convert to long
	    if(attr instanceof Long) {
		the_timeout = ((Long) attr).longValue();
		return the_timeout;
	    }
	}
	return -1;  // something extraordinarily large so msgs will never time out
    }
  
    private boolean timedOut(AttributedMessage message, String station) {
	long the_timeout = getTimeout(message);
	// absolute timeout value of must be greater than 0;
	if(the_timeout > 0){ 
	    long now = System.currentTimeMillis();
	    if(the_timeout < now ) {
		// log that the message timed out
		if (loggingService.isWarnEnabled())
		    loggingService.warn( station +
					" threw away a message="+ message +
					" Beyond deadline="+ 
					 (now-the_timeout) +
					 " ms");
		return true;
	    }
	}
	return false;
    }

    /* 
     * Aspect Code to hook into all the links in the MTS chain
     */
    public Object getDelegate(Object object, Class type) {
	if (type == SendLink.class) {
	    return new SendLinkDelegate((SendLink) object);
	} else if (type == Router.class) {
	    return new RouterDelegate((Router) object);
	} else if (type == DestinationLink.class) {
	    return new DestinationLinkDelegate((DestinationLink) object);
	} else if (type == ReceiveLink.class) {
	    if (SYNC_CLOCK_AVAILABLE) {
		return new ReceiveLinkDelegate((ReceiveLink) object);
	    } else {
		return null;
	    }
	} else {
	    return null;
	}
    }
  
  
    /*
     * First thread in the msg chain to check timeout values
     * Also computes timeout
     */
    private class SendLinkDelegate
	extends SendLinkDelegateImplBase
    {    
	SendLinkDelegate(SendLink link)
	{
	    super(link);
	}
    
	// turns relative into absolute timeout
	// stores back into absolute for other delegates to access
	// null means no timeout was used
	long convertTimeout(AttributedMessage message) {
      
	    long the_timeout = -1;
      
	    // Get either the relative or absolute timeout values here
	    // One (should) be null
	    Object attr = message.getAttribute(MESSAGE_SEND_TIMEOUT_ATTRIBUTE);
	    if(attr!=null) { // check for relative
		if(attr instanceof Integer) {
		    the_timeout = ((Integer)attr).intValue();
		    the_timeout+=System.currentTimeMillis();  // turn into absolute time
		    // store back into absolute attribute value
		    message.setAttribute(MESSAGE_SEND_DEADLINE_ATTRIBUTE, new Long(the_timeout));
		}
	    } else if(attr == null) {
		attr = message.getAttribute(MESSAGE_SEND_DEADLINE_ATTRIBUTE);
		if(attr!=null) { // check for absolute
		    if(attr instanceof Long) {
			the_timeout = ((Long)attr).longValue();
		    }
		}
	    }
	    return the_timeout;
	}
    
	public void sendMessage(AttributedMessage message)
	{ 
            // convert relative timeouts to absolute
	    long the_timeout = convertTimeout(message);
      
	    if(timedOut(message, "SendLink")) {
		// drop message silently
		return;
	    }
	    super.sendMessage(message);
	}
    } 
  
  
    /*
     * Second station in the msg chain to check timeout values
     */
    private class RouterDelegate 
	extends RouterDelegateImplBase 
    {
    
	RouterDelegate(Router delegatee) {
	    super(delegatee);
	}
    
	public void routeMessage(AttributedMessage message)
	{

	    if(timedOut(message, "Router")) {
		// drop message silently
		return;
	    }
	    super.routeMessage(message);
	}
    }
  
    private class DestinationLinkDelegate 
	extends DestinationLinkDelegateImplBase 
    {    
	DestinationLinkDelegate(DestinationLink delegatee) {
	    super(delegatee);
	}
    
	public MessageAttributes forwardMessage(AttributedMessage message)
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException
	{
	    if(timedOut(message, "DestinationLink")) {
		//drop message, set delivery status to dropped
		MessageAttributes metadata = new MessageReply(message);
		metadata.setAttribute(MessageAttributes.DELIVERY_ATTRIBUTE,
				      MessageAttributes.DELIVERY_STATUS_DROPPED);
		return metadata;
	    }
	    MessageAttributes metadata = super.forwardMessage(message);
	    return metadata;
	}
    }
  
  
    private class ReceiveLinkDelegate 
	extends ReceiveLinkDelegateImplBase 
    {    
	ReceiveLinkDelegate(ReceiveLink delegatee) {
	    super(delegatee);
	}
    
	public MessageAttributes deliverMessage(AttributedMessage message)
	{
	    if(timedOut(message, "Deliverer")) {
		//drop message, set delivery status to dropped
		MessageAttributes metadata = new MessageReply(message);
		metadata.setAttribute(MessageAttributes.DELIVERY_ATTRIBUTE,
				      MessageAttributes.DELIVERY_STATUS_DROPPED);
		return metadata;
	    }
	    MessageAttributes metadata = super.deliverMessage(message);
	    return metadata;
	}
    }
} 
