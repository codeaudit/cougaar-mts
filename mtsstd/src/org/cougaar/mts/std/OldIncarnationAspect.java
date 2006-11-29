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

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.SimpleMessageAttributes;
import org.cougaar.core.service.IncarnationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MessageDelivererDelegateImplBase;
import org.cougaar.mts.base.MessageTransportRegistryService;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.base.UnregisteredNameException;


/**
 * This Aspect assists in the detection of out-of-date incarnations of
 * Agents. 
 */
public class OldIncarnationAspect extends StandardAspect
{

    private static transient MessageAttributes DummyReturn;
    static {
	DummyReturn = new SimpleMessageAttributes();
	DummyReturn.setAttribute(MessageAttributes.DELIVERY_ATTRIBUTE,
				 MessageAttributes.DELIVERY_STATUS_OLD_INCARNATION);
    }



    private MessageTransportRegistryService registry;
    private IncarnationService incarnationSvc;

    public void load()
    {
	super.load();
	LoggingService loggingService = getLoggingService();
	ServiceBroker sb = getServiceBroker();

	registry = (MessageTransportRegistryService)
	    sb.getService(this, MessageTransportRegistryService.class, null);
	if (registry == null && loggingService.isWarnEnabled()) 
	    loggingService.warn("Couldn't get MessageTransportRegistryService");

	incarnationSvc = (IncarnationService)
	    sb.getService(this, IncarnationService.class, null);
	if (incarnationSvc == null  && loggingService.isWarnEnabled())
	    loggingService.warn("Couldn't get IncarnationService");
    }


    public Object getDelegate(Object delegate, Class type) 
    {
	if (type == DestinationLink.class) {
	    return new DestinationLinkDelegate((DestinationLink) delegate);
	} else if (type == MessageDeliverer.class) {
	    return new MessageDelivererDelegate((MessageDeliverer) delegate);
	} else {
	    return null;
	}
    }



    private class MessageDelivererDelegate
	extends MessageDelivererDelegateImplBase
    {
	public MessageDelivererDelegate (MessageDeliverer deliverer) 
	{
	    super(deliverer);
	}


	// The sender's incarnation has to be checked for two cases.
	// If it's new we need to inform the incarnation service of
	// that fact. If it's an obsolete incarnation we need to avoid
	// delivering the message, and also to notify the sender (the
	// sender-side handling of this case is in the DestinationLink
	// delegate, below).
	public MessageAttributes deliverMessage(AttributedMessage message, 
						MessageAddress addr) 
	    throws MisdeliveredMessageException
	{
	    LoggingService loggingService = getLoggingService();

	    // Check sender incarnation
	    MessageAddress sender = message.getOriginator();
	    Long incarnationAttr = (Long)
		message.getAttribute(AttributeConstants.INCARNATION_ATTRIBUTE);

	    if (incarnationAttr == null) {
		throw new RuntimeException("No incarnation number in message " +
					message);
	    } 

	    long incarnation = incarnationAttr.longValue();
	    int status = incarnationSvc.updateIncarnation(sender, incarnation);
	    if (status > 0) {
		if (loggingService.isInfoEnabled())
		    loggingService.info("Detected new incarnation number " 
					+incarnation+ " in message " +message);
	    } else if (status < 0) {
		// Bogus message from old incarnation.  Pretend normal
		// delivery but don't process it.
		if (loggingService.isInfoEnabled())
		    loggingService.info("Detected obsolete incarnation number " 
					+incarnation+ " in message " +message);
		long new_incarnation = incarnationSvc.getIncarnation(sender);


		MessageAttributes reply = new SimpleMessageAttributes();
		reply.setAttribute(MessageAttributes.DELIVERY_ATTRIBUTE,
				    MessageAttributes.DELIVERY_STATUS_OLD_INCARNATION);
		reply.setAttribute(AttributeConstants.INCARNATION_ATTRIBUTE,
				   new Long(new_incarnation));
		return reply;
	    }



	    return super.deliverMessage(message, addr);
	}
	
    }

    private class DestinationLinkDelegate 
	extends DestinationLinkDelegateImplBase 
    {
	
	public DestinationLinkDelegate (DestinationLink link) 
	{
	    super(link);
	}
	

	public MessageAttributes forwardMessage(AttributedMessage message) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException
		   
	{
	    LoggingService loggingService = getLoggingService();

	    // Check sender - don't allow messages from obsolete local
	    // agents. 
	    if (!registry.isLocalClient(message.getOriginator())) {
		if (loggingService.isWarnEnabled()) {
		    loggingService.warn("Blocking message from obsolete agent: " 
					+message);
		}
		return DummyReturn;
	    }

	    // Handle the special return if the receive side thinks
	    // the sender is obsolete.
	    try {
		MessageAttributes reply = super.forwardMessage(message);
		Object status = reply.getAttribute(MessageAttributes.DELIVERY_ATTRIBUTE);
		if (status.equals(MessageAttributes.DELIVERY_STATUS_OLD_INCARNATION)) {
		    Long new_incarnation_attr = (Long)
			reply.getAttribute(AttributeConstants.INCARNATION_ATTRIBUTE);
		    long new_incarnation = new_incarnation_attr.longValue();
		    MessageAddress sender = message.getOriginator();
		    incarnationSvc.updateIncarnation(sender, new_incarnation);
		    if (loggingService.isInfoEnabled()) {
			loggingService.info(message.getTarget() 
					    +" says that "+
					    message.getOriginator()
					    + " is obsolete");
		    }
		}
		return reply;
	    } catch (UnregisteredNameException ex1) {
		throw ex1;
	    } catch (NameLookupException ex2) {
		throw ex2;
	    } catch (CommFailureException ex3) {
		throw ex3;
	    } catch (MisdeliveredMessageException ex4) {
		throw ex4;
	    }
	}

    }




}



    
