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

import org.cougaar.core.component.ServiceBroker;

/**
 *  Just for testing access to LinkProtocol services.
 */
public class ServiceTestAspect extends StandardAspect
{
    private LinkProtocolService svc;

    public ServiceTestAspect() {
    }

    private void test(String text, MessageAddress addr) {
	synchronized (this) {
	    if (svc == null) {
		ServiceBroker sb = getServiceBroker();
		Object raw = sb.getService(this,
					   RMILinkProtocol.Service.class, 
					   null);
		svc = (LinkProtocolService) raw;
	    }
	}

	if (svc != null && debugService.isInfoEnabled()) {
	    debugService.info("LinkProtocol Service " + text + ":" + 
				     addr + "->" + svc.addressKnown(addr));
	}
			       
    }

    private Message send(Message message) {
	test("send", message.getTarget());
	return message;
    }

    private Message receive(Message message) {
	test("receive", message.getOriginator());
	return message;
    }



    public Object getDelegate(Object delegate, Class type) 
    {
	if (type ==  DestinationLink.class) {
	    DestinationLink link = (DestinationLink) delegate;
	    return new TestDestinationLink(link);
	} else {
	    return null;
	}
    }


    public Object getReverseDelegate(Object delegate, Class type) 
    {
	if (type == MessageDeliverer.class) {
	    return new TestDeliverer((MessageDeliverer) delegate);
	} else {
	    return null;
	}
    }
    


    private class TestDestinationLink 
	extends DestinationLinkDelegateImplBase 
    {
	private TestDestinationLink(DestinationLink link) {
	    super(link);
	}

	public void forwardMessage(Message message) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException
	{
	    link.forwardMessage(send(message));
	}


    }



    private class TestDeliverer extends MessageDelivererDelegateImplBase {
	private TestDeliverer(MessageDeliverer deliverer) {
	    super(deliverer);
	}

	public void deliverMessage(Message m, MessageAddress dest) 
	    throws MisdeliveredMessageException
	{
	    deliverer.deliverMessage(receive(m), dest);
	}

    }
}
