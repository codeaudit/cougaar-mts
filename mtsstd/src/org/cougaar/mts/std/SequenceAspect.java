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


import java.util.HashMap;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.Comparator;

/**
 * First attempt at a security aspect.  The message is secured by a
 * RemoteProxy aspect delegate and unsecued by a RemoteImpl aspect
 * delegate.
 * */
public class SequenceAspect extends StandardAspect
{ 

    private static int  count=1;
    private static final String SEQ = 
	"org.cougaar.message.transport.sequencenumber";
    private static final Integer ONE = new Integer(1);
    private static final Integer TWO = new Integer(2);


    
    public SequenceAspect() {
	
    }


    public Object getDelegate(Object delegate, Class type) 
    {
	if (type ==  SendLink.class) {
	    return new SequencedSendLink((SendLink) delegate);
	} else {
	    return null;
	}
    }


    public Object getReverseDelegate(Object delegate, Class type) 
    {
	if (type == ReceiveLink.class) {
	    return new SequencedReceiveLink((ReceiveLink) delegate);
	} else {
	    return null;
	}
    }


    private static int getSequenceNumber(Object message) {
	AttributedMessage m = (AttributedMessage) message;
	return ((Integer) m.getAttribute(SEQ)).intValue();
    }

    private class SequencedSendLink 
	extends SendLinkDelegateImplBase 
    {
	HashMap sequenceNumbers;
	private SequencedSendLink(SendLink link) {
	    super(link);
	    sequenceNumbers = new HashMap();
	}

	private Integer nextSeq(AttributedMessage msg) {
	    // Verify that msg.getOriginator()  == getAddress() ?
	    MessageAddress dest = msg.getTarget();
	    Integer next = (Integer) sequenceNumbers.get(dest);
	    if (next == null) {
		sequenceNumbers.put(dest, TWO);
		return ONE;
	    } else {
		int n = next.intValue();
		sequenceNumbers.put(dest, new Integer(1+n));
		return next;
	    }
	}

	public void sendMessage(AttributedMessage message) 
	{
	    Integer sequence_number = nextSeq(message);
	    message.setAttribute(SEQ, sequence_number);
	    super.sendMessage(message);
	}
    }

    private static class MessageComparator implements Comparator{
	public int compare(Object msg1, Object msg2){
	    int seq1 = getSequenceNumber(msg1);
	    int seq2 = getSequenceNumber(msg2);
	    if (seq1 == seq2)
		return 0;
	    else if ( seq1 < seq2)
		return -1;
	    else
		return 1;
	}

	public boolean equals (Object obj) {
	    return (obj == this);
	}

    }

    private class SequencedReceiveLink extends ReceiveLinkDelegateImplBase {
	HashMap conversationState;

	private class ConversationState {
	    int nextSeqNum;
	    TreeSet heldMessages;

	    public  ConversationState (){
		nextSeqNum = 1;
		heldMessages = new TreeSet(new MessageComparator());
	    }

	    private void stripAndDeliver(AttributedMessage  message){
		// message.removeAttribute(SEQ);
		superDeliverMessage(message);
		nextSeqNum++;
	    }
     
	    private MessageAttributes handleNewMessage(AttributedMessage message) {
		MessageAttributes meta = new SimpleMessageAttributes(message);
		String delivery_status = null;
		int msgSeqNum = getSequenceNumber(message);
		if (nextSeqNum > msgSeqNum) {
		    Message contents = message.getRawMessage();
		    if (loggingService.isDebugEnabled())
			loggingService.debug("Dropping duplicate " +
					     " <" 
					     +contents.getClass().getName()+ 
					     " " +contents.hashCode()+ 
					     " " +message.getOriginator()+ 
					     "->" +message.getTarget()+
					     " #" +msgSeqNum);
		    delivery_status =
			MessageAttributes.DELIVERY_STATUS_DROPPED_DUPLICATE;
		} else  if (nextSeqNum == msgSeqNum) {
		    stripAndDeliver(message);
		    Iterator itr  = heldMessages.iterator();
		    while (itr.hasNext()) {
			AttributedMessage next = 
			    (AttributedMessage)itr.next();
			if (getSequenceNumber(next) == nextSeqNum){
			    if (loggingService.isDebugEnabled())
				loggingService.debug("delivered held message" 
						     + next); 
			    stripAndDeliver(next);
			    itr.remove();
			}
		    }//end while
		    delivery_status =
			MessageAttributes.DELIVERY_STATUS_DELIVERED;
		} else {
		    if (loggingService.isDebugEnabled())
			loggingService.debug("holding out of sequence message"
					     + message); 
		    heldMessages.add(message);
		    delivery_status =
			MessageAttributes.DELIVERY_STATUS_HELD;
		}

		
		meta.setAttribute(MessageAttributes.DELIVERY_ATTRIBUTE,
				  delivery_status);
		return meta;
	    }
	}


	private SequencedReceiveLink(ReceiveLink link) {
	    super(link);
	    conversationState = new HashMap();
	}

	private void superDeliverMessage(AttributedMessage message) {
	    super.deliverMessage(message);
	}
     
	public MessageAttributes deliverMessage(AttributedMessage message) {
	    Object seq = message.getAttribute(SEQ);
	    if ( seq != null ) {
		MessageAddress src = message.getOriginator();
		ConversationState conversation =   
		    (ConversationState)conversationState.get(src);
		if(conversation == null) {
		    conversation = new ConversationState();
		    conversationState.put (src, conversation);
		}
	     
		return conversation.handleNewMessage(message);
	    }
	    else {
		if (loggingService.isErrorEnabled())
		    loggingService.error("No Sequence tag: " + message);
		return super.deliverMessage(message);
	    }

	}
    }

}
