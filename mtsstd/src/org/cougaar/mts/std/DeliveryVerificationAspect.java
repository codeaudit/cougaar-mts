/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.mts.std;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.UnregisteredNameException;
import org.cougaar.mts.base.DontRetryException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.SendLink;
import org.cougaar.mts.base.SendLinkDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;

public class DeliveryVerificationAspect 
    extends StandardAspect
    implements AttributeConstants, Runnable
{
    private static final int TOO_LONG = 10000; // 10 seconds
    private static final int SAMPLE_PERIOD = TOO_LONG / 2;
    private static final String WARN_TIME_VALUE_STR="100";
    private static final String WARN_TIME_PARAM= 
	"warn-time";
    private static final String INFO_TIME_VALUE_STR="10";
    private static final String INFO_TIME_PARAM= 
	"info-time";

    // Less ugly than writing a proper math function
    private static final long[] LIMITS = { TOO_LONG,
					   TOO_LONG*3,
					   TOO_LONG*10,
					   TOO_LONG*30,
					   TOO_LONG*100,
					   TOO_LONG*300,
					   TOO_LONG*1000 };


    private HashMap pendingMessages = new HashMap();
    private Schedulable schedulable;
    private int timeout = TOO_LONG;
    private int infoTime;
    private int warnTime;

    public DeliveryVerificationAspect() {
    }

    public void load() {
	super.load();
	
// 	initializeParameter(WARN_TIME_PARAM,WARN_TIME_VALUE_STR);
// 	initializeParameter(INFO_TIME_PARAM,INFO_TIME_VALUE_STR);
	dynamicParameterChanged(WARN_TIME_PARAM,
				getParameter(WARN_TIME_PARAM,
					     WARN_TIME_VALUE_STR));
	dynamicParameterChanged(INFO_TIME_PARAM,
				getParameter(INFO_TIME_PARAM,
					     INFO_TIME_VALUE_STR));
	

	ThreadService tsvc = getThreadService();
	schedulable = tsvc.getThread(this, this, 
					     "DeliveryVerification");
	schedulable.schedule(0, SAMPLE_PERIOD);
	
    }

    protected void dynamicParameterChanged(String name, String value)
    {
	if (name.equals(WARN_TIME_PARAM)) {
	    warnTime = Integer.parseInt(value) *1000; //millisecond
	}
	if (name.equals(INFO_TIME_PARAM)) {
	    infoTime = Integer.parseInt(value) *1000; //millisecond
	}
    }

    private boolean timeToLog(long deltaT) {
	for (int i=0; i<LIMITS.length; i++) {
	    long lowerBound = LIMITS[i];
	    if (deltaT < lowerBound) return false;
	    long upperBound = lowerBound + SAMPLE_PERIOD;
	    if (deltaT < upperBound) return true;
	}
	return false;
    }

    // Runnable
    public void run() {
	LoggingService lsvc = getLoggingService();
	if (!lsvc.isWarnEnabled()) return;

	long now = System.currentTimeMillis();
	synchronized(pendingMessages) {
	    Iterator itr = pendingMessages.entrySet().iterator();
	    while (itr.hasNext()) {
		Map.Entry entry = (Map.Entry) itr.next();
		AttributedMessage msg = (AttributedMessage)
		    entry.getKey();
		long time = ((Long) entry.getValue()).longValue();
		long deltaT = now-time;
		if (timeToLog(deltaT)) {
		    if (deltaT >= warnTime)
			lsvc.warn(msg +
				  " has been pending for "+
				  deltaT + "ms (Pending Messages=" +
				  pendingMessages.size()+")" );
		    else if (deltaT >= infoTime)
			lsvc.info(msg +
				  " has been pending for "+
				  deltaT + "ms (Pending Messages=" +
				  pendingMessages.size()+")" );
		}
	    }
	}
    }


    // assume caller synchronizes on pendingMessages
    private void removeMessage(Message message) {
	LoggingService lsvc = getLoggingService();
	if (lsvc.isInfoEnabled()) {
	    Long time = (Long) pendingMessages.get(message);
	    if (time != null) {
		long now = System.currentTimeMillis();
		long deltaT = now-time.longValue();
		if (deltaT > timeout) {
		    lsvc.info("Pending " + message + 
			       " has been sent after "+
			       deltaT + "ms");
		}
	    }
	}
	
	pendingMessages.remove(message);
    }

    // Aspect
    public Object getDelegate(Object delegatee, Class type) 
    {
	if (type ==  SendLink.class) {
	    SendLink link = (SendLink) delegatee;
	    return new VerificationSendLink(link);
	} else {
	    return null;
	}
    }


    public Object getReverseDelegate(Object delegatee, Class type) {
	if (type == DestinationLink.class) {
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
	    MessageAddress destination = message.getTarget();
	    if (destination instanceof MulticastMessageAddress) {
		LoggingService lsvc = getLoggingService();
		if (lsvc.isWarnEnabled()) {
		    lsvc.warn("Ignoring Multicast Message " + message);
		}
	    } else {
		Long now = new Long(System.currentTimeMillis());
		synchronized (pendingMessages) {
		    pendingMessages.put(message, now);
		}
	    }
	    super.sendMessage(message);
	}

	public void flushMessages(ArrayList messages) {
	    super.flushMessages(messages);
	    // 'messages' now holds a list of all retracted
	    // Messages. Remove them from the pendingMessages map. 
	    synchronized (pendingMessages) {
		Iterator i = messages.iterator();
		while (i.hasNext()) {
		    Message message = (Message) i.next();
		    removeMessage(message);
		}
	    }
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
	    try {
		MessageAttributes result = super.forwardMessage(message);
		// If it returns without an exception, declare the message delivered
		// ignore delivery status.
		synchronized (pendingMessages) {
		    removeMessage(message);
		}
		return result;
	    } catch ( CommFailureException ex) {
		if (ex.getCause() instanceof DontRetryException) {
		// Security dropped message, so declared it delivered
		    synchronized (pendingMessages) {
			removeMessage(message);
		    }
		}
		throw ex;
	    }
	    
	}
    }
}

