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

import org.cougaar.core.service.LoggingService;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;

public class DeliveryVerificationAspect 
    extends StandardAspect
    implements AttributeConstants
{
    private HashMap pendingMessages = new HashMap();

    public DeliveryVerificationAspect() {
    }

    public void load() {
	super.load();

	TimerTask task = new TimerTask() {
		public void run() {
		    verify();
		}};
	getThreadService().schedule(task, 0, 5000);
    }

    private void verify() {
	Iterator itr = pendingMessages.entrySet().iterator();
	long now = System.currentTimeMillis();
	LoggingService lsvc = getLoggingService();
	synchronized(pendingMessages) {
	    while (itr.hasNext()) {
		Map.Entry entry = (Map.Entry) itr.next();
		AttributedMessage msg = (AttributedMessage)
		    entry.getKey();
		long time = ((Long) entry.getValue()).longValue();
		long deltaT = now-time;
		if (deltaT > 10000) {
		    if (lsvc.isErrorEnabled())
			lsvc.error(msg +
				   " has been pending for "+
				   deltaT + "ms");
		}
	    }
	}
    }

    public Object getDelegate(Object delegatee, Class type) 
    {
	if (type ==  SendLink.class) {
	    SendLink link = (SendLink) delegatee;
	    return new VerificationSendLink(link);
	} else if (type ==  DestinationLink.class) {
	    DestinationLink link = (DestinationLink) delegatee;
	    return new VerificationDestinationLink(link);
	} else {
	    return null;
	}
    }


    private class  VerificationSendLink
	extends SendLinkDelegateImplBase 
    {
	
	private VerificationSendLink(SendLink link) {
	    super(link);
	}
	
	public void sendMessage(AttributedMessage message) {
	    synchronized (pendingMessages) {
		Long now = new Long(System.currentTimeMillis());
		pendingMessages.put(message, now);
	    }
	    super.sendMessage(message);
	}

    }

    private class VerificationDestinationLink
	extends DestinationLinkDelegateImplBase 
    {
	private VerificationDestinationLink(DestinationLink link) {
	    super(link);
	}

	public MessageAttributes forwardMessage(AttributedMessage message) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException
	{
	    MessageAttributes result = super.forwardMessage(message);
	    Object status = result.getAttribute(DELIVERY_ATTRIBUTE);
	    if (status.equals(DELIVERY_STATUS_DELIVERED)) {
		synchronized (pendingMessages) {
		    pendingMessages.remove(message);
		}
	    }
	    return result;
	}
    }


}

