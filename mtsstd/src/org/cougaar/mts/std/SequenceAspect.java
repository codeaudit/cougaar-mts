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
{ private static int  count=1;
    public SequenceAspect() {
	
    }


    private static class SequenceEnvelope extends MessageEnvelope {
	int sequence_number;

	SequenceEnvelope(Message message, int sequence_number) {
	    super(message, message.getOriginator(), message.getTarget());
	    this.sequence_number = sequence_number;
	}

	public String toString() {
	    return "SequenceEnvelope:# " + sequence_number + " from " 
		+ this.getOriginator() + " to " + this.getTarget() ;
	}
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

    private class SequencedSendLink 
	extends SendLinkDelegateImplBase 
    {
	HashMap sequenceNumbers;
	private SequencedSendLink(SendLink link) {
	    super(link);
	    sequenceNumbers = new HashMap();
	}

	private int nextSeq(Message msg) {
	    // Verify that msg.getOriginator()  == getAddress() ?
	    MessageAddress dest = msg.getTarget();
	    Integer next = (Integer) sequenceNumbers.get(dest);
	    if (next == null) {
		sequenceNumbers.put(dest, new Integer(2));
		return 1;
	    } else {
		int n = next.intValue();
		sequenceNumbers.put(dest, new Integer(1+n));
		return n;
	    }
	}

	public void sendMessage(Message message) 
	{
	    int sequence_number = nextSeq(message);
	    link.sendMessage(new SequenceEnvelope(message, sequence_number));
	}
    }

    private static class MessageComparator implements Comparator{
	public int compare(Object msg1, Object msg2){
	    SequenceEnvelope message1 =(SequenceEnvelope) msg1;
	    SequenceEnvelope message2 =(SequenceEnvelope) msg2;
	    if (message1.sequence_number == message2.sequence_number)
		return 0;
	    else if ( message1.sequence_number< message2.sequence_number)
		return -1;
	    else
		return 1;
	}

	public boolean equals (Object obj) {
	    return (obj == this);
	}

    }

    private class ConversationState {
	int nextSeqNum;
	TreeSet heldMessages;
	ReceiveLink link;

	public  ConversationState (ReceiveLink link){
	    nextSeqNum = 1;
	    heldMessages = new TreeSet(new MessageComparator());
	    this.link = link;
	}

	private void stripAndDeliver(SequenceEnvelope  message){
	       link.deliverMessage(message.getContents());
	       nextSeqNum++;
	}
     
	private void handleNewMessage(SequenceEnvelope message) {
	    if (nextSeqNum == message.sequence_number) {
		stripAndDeliver(message);
		Iterator itr  = heldMessages.iterator();
		while (itr.hasNext()) {
		    SequenceEnvelope next = (SequenceEnvelope)itr.next();
		    if (next.sequence_number == nextSeqNum){
			if (debugService.isDebugEnabled())
			    debugService.debug("delivered held message" + 
					       next); 
			stripAndDeliver(next);
			itr.remove();
		    }
		}//end while
	    }
	    else {
		if (debugService.isDebugEnabled())
		    debugService.debug("holding a out of sequence message" + 
				       message); 
		heldMessages.add(message);
	    }
	}
    }

 private class SequencedReceiveLink extends ReceiveLinkDelegateImplBase {
     HashMap conversationState;

     private SequencedReceiveLink(ReceiveLink link) {
	 super(link);
	 conversationState = new HashMap();
     }
     
     public void deliverMessage(Message message) {
	 if ( message instanceof SequenceEnvelope ) {
	     MessageAddress src = message.getOriginator();
	     ConversationState conversation =   (ConversationState)conversationState.get(src);
	     if(conversation == null) {
		 conversation = new ConversationState(link);
		 conversationState.put (src, conversation);
	     }
	     
	     conversation.handleNewMessage((SequenceEnvelope) message);
	 }
	 else {
	     if (debugService.isErrorEnabled())
		 debugService.error("Not a SequenceEnvelope: " + message);
	     link.deliverMessage(message);
	 }

     }
 }

}
