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
import org.cougaar.core.society.MessageEnvelope;

import java.util.HashMap;

/**
 * First attempt at a security aspect.  The message is secured by a
 * RemoteProxy aspect delegate and unsecued by a RemoteImpl aspect
 * delegate.
 * */
public class SequenceAspect extends StandardAspect
{


    public SequenceAspect() {
    }


    private static class SequenceEnvelope extends MessageEnvelope {
	int sequence_number;

	SequenceEnvelope(Message message, int sequence_number) {
	    super(message, message.getOriginator(), message.getTarget());
	    this.sequence_number = sequence_number;
	}

	protected Message getContents() {
	    return super.getContents();
	}
    
    }

    public Object getDelegate(Object delegate,
			      LinkProtocol protocol,
			      Class type) 
    {
	if (type ==  DestinationLink.class) {
	    return new SequencedDestinationLink((DestinationLink) delegate);
	} else {
	    return null;
	}
    }


    public Object getReverseDelegate(Object delegate,
				     LinkProtocol protocol,
				     Class type) 
    {
	if (type == MessageDeliverer.class) {
	    return new SequencedDeliverer((MessageDeliverer) delegate);
	} else {
	    return null;
	}
    }
    


    private class SequencedDestinationLink 
	extends DestinationLinkDelegateImplBase 
    {
	HashMap sequenceNumbers;
	private SequencedDestinationLink(DestinationLink link) {
	    super(link);
	    sequenceNumbers = new HashMap();
	}

	private int nextSeq(MessageAddress source) {
	    Integer next = (Integer) sequenceNumbers.get(source);
	    if (next == null) {
		sequenceNumbers.put(source, new Integer(1));
		return 0;
	    } else {
		int n = next.intValue();
		sequenceNumbers.put(source, new Integer(1+n));
		return n;
	    }
	}

	public void forwardMessage(Message message) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException
	{
	    int sequence_number = nextSeq(message.getOriginator());
	    link.forwardMessage(new SequenceEnvelope(message, sequence_number));
	}


    }



    private class SequencedDeliverer extends MessageDelivererDelegateImplBase {
	private SequencedDeliverer(MessageDeliverer deliverer) {
	    super(deliverer);
	}

	public void deliverMessage(Message message, MessageAddress dest) 
	    throws MisdeliveredMessageException
	{
	    if (message instanceof SequenceEnvelope) {
		SequenceEnvelope seqmsg = (SequenceEnvelope) message;
		Message contents = seqmsg.getContents();
		System.out.print("#" + seqmsg.sequence_number);
		deliverer.deliverMessage(contents, dest);
	    } else {
		System.err.println("### Not a SequenceEnvelope: " + message);
		deliverer.deliverMessage(message, dest);
	    }
	}

    }
}
