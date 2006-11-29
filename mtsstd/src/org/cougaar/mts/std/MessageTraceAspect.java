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
import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.service.LoggingService;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.SendLink;
import org.cougaar.mts.base.SendLinkDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.base.UnregisteredNameException;

/**
 * This test Aspect logs message delivery elapsed time.
 */
public class MessageTraceAspect 
    extends StandardAspect
    implements AttributeConstants
{
    private static final String SUBMISSION_TIME_ATTRIBUTE = "SUBMIT!";

    public void load() 
    {
	super.load();
    }

    public Object getDelegate(Object delegatee, Class type) 
    {
	if (type == DestinationLink.class) {
	    // forward time
	    return new DestinationLinkDelegate((DestinationLink) delegatee);
	} else if (type == SendLink.class) {
	    // note submit time
	    return new SendLinkDelegate((SendLink) delegatee);
	} else {
	    return null;
	}
    }




    private class SendLinkDelegate
	extends SendLinkDelegateImplBase
    {
	SendLinkDelegate(SendLink link)
	{
	    super(link);
	}

	public void sendMessage(AttributedMessage msg)
	{
	    long now =  System.currentTimeMillis();
	    msg.setLocalAttribute(SUBMISSION_TIME_ATTRIBUTE, new Long(now));
	    super.sendMessage(msg);
	}
    }
    


    private class DestinationLinkDelegate 
	extends DestinationLinkDelegateImplBase 
    {
	
	public DestinationLinkDelegate (DestinationLink link) 
	{
	    super(link);
	}
	

	private void logSuccess(AttributedMessage msg, long forward_time)
	{
	    logEntry(msg, forward_time, "Success");
	}

	private void logFailure(AttributedMessage msg, 
				long forward_time,
				Exception reason)
	{
	    logEntry(msg, forward_time, "Exception " + 
		     reason.getClass().getName());
	}


	private void logEntry(AttributedMessage msg, 
			       long forward_time,
			       String tag)
	{
	    long submit_time = ((Long) msg.getAttribute(SUBMISSION_TIME_ATTRIBUTE)).longValue();
	    long now =  System.currentTimeMillis();
	    long delta1 = now - submit_time;  // cleint send time
	    long delta2 = now - forward_time; // link send time
	    MessageAddress from = msg.getOriginator();
	    MessageAddress to = msg.getTarget();
	    Integer msg_size = (Integer) msg.getAttribute(MESSAGE_BYTES_ATTRIBUTE);
	    int message_size = msg_size == null ? 0 : msg_size.intValue();
	    Integer hdr_size = (Integer) msg.getAttribute(HEADER_BYTES_ATTRIBUTE);
	    int header_size = hdr_size == null ? 0 : hdr_size.intValue();

	    LoggingService lsvc = getLoggingService();
	    lsvc.info(tag  +","+
		      from  +","+
		      to  +","+
		      submit_time  +","+
		      delta1 +","+
		      delta2 +","+
		      message_size +","+
		      header_size);
	}
		      


	public MessageAttributes forwardMessage(AttributedMessage msg) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException
		   
	{
	    long forward_time = System.currentTimeMillis();
	    try {
		MessageAttributes result = super.forwardMessage(msg);

		logSuccess(msg, forward_time);
		return result;
	    } catch (UnregisteredNameException ex1) {
		logFailure(msg, forward_time, ex1);
		throw ex1;
	    } catch (NameLookupException ex2) {
		logFailure(msg, forward_time, ex2);
		throw ex2;
	    } catch (CommFailureException ex3) {
		logFailure(msg, forward_time, ex3);
		throw ex3;
	    } catch (MisdeliveredMessageException ex4) {
		logFailure(msg, forward_time, ex4);
		throw ex4;
	    }
	}

    }




}



    
