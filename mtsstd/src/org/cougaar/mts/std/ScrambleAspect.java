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

import org.cougaar.core.service.*;

import org.cougaar.core.node.*;

import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageEnvelope;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Scrambles the messages 
 * This is a test for SequenceAspect.java
 * 
 * */
public class ScrambleAspect extends StandardAspect
{
    protected Timer timer;
    public ScrambleAspect() {
	timer = new Timer();
    }

    public Object getDelegate(Object delegate, Class type) 
    {
	if (type ==  SendLink.class) {
	    return new ScrambledSendLink((SendLink) delegate);
	} else {
	    return null;
	}
    }

    private class ScrambledSendLink 
	extends SendLinkDelegateImplBase 
    {
	
	SendMessageTimerTask timerTask;
	Message heldMessage;
	
	int heldMessageCount;
	int flippedMessageCount;
	int forcedMessageCount;
	int messageCount; 
	
	private ScrambledSendLink(SendLink link) {
	    super(link);
	    //long  timeStarted = System.currentTimeMillis();
	}
	
	
	public synchronized void sendMessage(Message message) {
	    messageCount++;
	    if (heldMessage == null)//Case 1: On a new message //a -- No held messages yet
		holdMessage(message);
	    else if (heldMessage != null) //b --there is already a held messgae
		flipMessage(message, heldMessage);
	}

	
	private class SendMessageTimerTask extends TimerTask {
	    private ScrambledSendLink link;

	    public SendMessageTimerTask ( ScrambledSendLink link ){
		super();
		this.link = link;
	    }
	
	    public void run() {
		link.forcedHeldMessage();

	    }
	}
    
	//================util methods
	private void holdMessage(Message message){
	    timerTask = new SendMessageTimerTask (this);
	    timer.schedule(timerTask, 3000); //schedule a timer task
	    heldMessage = message;
	    heldMessageCount++;
	    if (loggingService.isDebugEnabled())
		loggingService.debug("Holding message #" +printString()  
				   + "  " +  heldMessageCount );
	}
	private void flipMessage(Message message, Message heldMessage){
	    timerTask.cancel();
	    link.sendMessage(message);
	    link.sendMessage(heldMessage);
	    flippedMessageCount++;
	    int previousCount = messageCount - 1;
	    if (loggingService.isDebugEnabled())
		loggingService.debug("Flipping messages #" + previousCount +
				   " and #" + printString() +  
				   " and " + message.getTarget() + "  "
				   +  flippedMessageCount );
	    heldMessage = null;
	}

	private synchronized void forcedHeldMessage() {
	    if (heldMessage != null){
		link.sendMessage(heldMessage);
		forcedMessageCount++;
		if (loggingService.isDebugEnabled())
		    loggingService.debug("Forcing message #" + printString() +
				       "  " + forcedMessageCount);
		heldMessage = null;
	    }
	}
	
	private String printString() {
	    return messageCount +  " from "+  
		heldMessage.getOriginator()+ " to " + heldMessage.getTarget() ;
	}
	//===========================
    }


}

