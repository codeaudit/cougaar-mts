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

import java.util.TimerTask;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;

/**
 * Scrambles the messages 
 * This is a test for SequenceAspect.java
 * 
 * */
public class ScrambleAspect extends StandardAspect
{
    public ScrambleAspect() {
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
	
	TimerTask timerTask;
	AttributedMessage heldMessage;
	
	int heldMessageCount;
	int flippedMessageCount;
	int forcedMessageCount;
	int messageCount; 
	
	private ScrambledSendLink(SendLink link) {
	    super(link);
	    //long  timeStarted = System.currentTimeMillis();
	}
	

	
	private class SendMessageTask extends TimerTask {
	    public void run() {
		forcedHeldMessage();
	    }
	}
    




	public synchronized void sendMessage(AttributedMessage message) {
	    messageCount++;
	    if (heldMessage == null)
		holdMessage(message);
	    else
		flipMessage(message);
	}

	
	//================util methods
	private void holdMessage(AttributedMessage message){
	    heldMessage = message;
	    timerTask = new SendMessageTask ();
	    threadService.schedule(timerTask, 3000); //schedule a timer task
	    heldMessageCount++;
	    if (loggingService.isDebugEnabled())
		loggingService.debug("Holding message #" +printString()  
				   + "  " +  heldMessageCount );
	}

	private void flipMessage(AttributedMessage message)
	{
	    timerTask.cancel();
	    // Cancelling the task doesn't guarantee that it won't
	    // run.  But the only purpose of this aspect is to test
	    // weird cases (messages out of order) so it might as well
	    // test duplicates sometimes too...

	    super.sendMessage(message);
	    super.sendMessage(heldMessage);
	    heldMessage = null;
	    flippedMessageCount++;
	    int previousCount = messageCount - 1;
	    if (loggingService.isDebugEnabled())
		loggingService.debug("Flipping messages #" + previousCount +
				   " and #" + printString() +  
				   " and " + message.getTarget() + "  "
				   +  flippedMessageCount );
	}

	private synchronized void forcedHeldMessage() {
	    if (heldMessage != null){
		super.sendMessage(heldMessage);
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

