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


public class FilterStreamAspect extends StandardAspect
{
    public FilterStreamAspect() {
    }



    public Object getDelegate(Object delegate, Class type) 
    {
	if (type ==  DestinationQueue.class) {
	    DestinationQueue queue = (DestinationQueue) delegate;
	    return new FilteredDestinationQueue(queue);
	} else {
	    return null;
	}
    }


    public Object getReverseDelegate(Object delegate, Class type) 
    {
	if (type == MessageDeliverer.class) {
	    return new FilteredDeliverer((MessageDeliverer) delegate);
	} else {
	    return null;
	}
    }
    


    private class FilteredDestinationQueue
	extends DestinationQueueDelegateImplBase 
    {
	private FilteredDestinationQueue(DestinationQueue queue) {
	    super(queue);
	}



	public void dispatchNextMessage(Message message) 
	{
	    super.dispatchNextMessage(new ExternalizableEnvelope(message));
	}


    }



    private class FilteredDeliverer extends MessageDelivererDelegateImplBase {
	private FilteredDeliverer(MessageDeliverer deliverer) {
	    super(deliverer);
	}

	public void deliverMessage(Message m, MessageAddress dest) 
	    throws MisdeliveredMessageException
	{
	    if (m instanceof ExternalizableEnvelope) {
		Message contents = ((ExternalizableEnvelope) m).getContents();
		super.deliverMessage(contents, dest);
	    } else {
		System.err.println("Received a message that isn't an" +
				   " ExternalizableEnvelope");
		super.deliverMessage(m, dest);
	    }
	}

    }
}
