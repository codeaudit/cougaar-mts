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
import java.util.HashSet;
import java.util.Iterator;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.mts.SimpleMessageAttributes;
import org.cougaar.core.service.IncarnationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MessageDelivererDelegateImplBase;
import org.cougaar.mts.base.MessageTransportRegistryService;
import org.cougaar.mts.base.RMILinkProtocol;
import org.cougaar.mts.base.SendLink;
import org.cougaar.mts.base.SendLinkDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.UnregisteredNameException;


public class OldIncarnationAspect extends StandardAspect
{

    private static transient MessageAttributes DummyReturn;
    static {
	DummyReturn = new SimpleMessageAttributes();
	DummyReturn.setAttribute(MessageAttributes.DELIVERY_ATTRIBUTE,
				 MessageAttributes.DELIVERY_STATUS_OLD_INCARNATION);
    }

    private HashSet obsoleteAgents;
    private HashMap incarnationNumbers;
    private MessageTransportRegistryService registry;

    public void load()
    {
	super.load();
	obsoleteAgents = new HashSet();
	incarnationNumbers = new HashMap();
	ServiceBroker sb = getServiceBroker();
	this.registry = (MessageTransportRegistryService)
	    sb.getService(this, MessageTransportRegistryService.class, null);
    }

    private boolean agentIsObsolete(MessageAddress agent)
    {
	synchronized (obsoleteAgents) {
	    return obsoleteAgents.contains(agent.getPrimary());
	}
    }


    private void decacheDestinationLink(MessageAddress sender) 
    {
	// Find the RMI DestinationLink for this address and force a
	// decache.
	ArrayList links = registry.getDestinationLinks(sender);
	if (links != null) {
	    Iterator itr = links.iterator();
	    while (itr.hasNext()) {
		Object next = itr.next();
		if (next instanceof IncarnationService.Callback) {
		    IncarnationService.Callback cb = (IncarnationService.Callback)
			next;
		    cb.incarnationChanged(sender, -1);
		}
	    }
	}
    }


    public Object getDelegate(Object delegate, Class type) 
    {
	if (type == DestinationLink.class) {
	    return new DestinationLinkDelegate((DestinationLink) delegate);
	} else if (type == MessageDeliverer.class) {
	    return new MessageDelivererDelegate((MessageDeliverer) delegate);
	} else if (type == SendLink.class) {
	    return new SendLinkDelegate((SendLink) delegate);
	} else {
	    return null;
	}
    }


    private class SendLinkDelegate
	extends SendLinkDelegateImplBase
    {
	public SendLinkDelegate (SendLink link) 
	{
	    super(link);
	}

	public void registerClient(MessageTransportClient client)
	{
	    // remove from obsoleteAgents
	    synchronized (obsoleteAgents) {
		obsoleteAgents.remove(client.getMessageAddress().getPrimary());
	    }
	    super.registerClient(client);
	}

    }

    private class MessageDelivererDelegate
	extends MessageDelivererDelegateImplBase
    {
	public MessageDelivererDelegate (MessageDeliverer deliverer) 
	{
	    super(deliverer);
	}

	public MessageAttributes deliverMessage(AttributedMessage message, 
						MessageAddress addr) 
	    throws MisdeliveredMessageException
	{
	    LoggingService loggingService = getLoggingService();
	    MessageAddress sender = message.getOriginator();
	    String sender_string = sender.getAddress();
	    Long incarnation = (Long)
		message.getAttribute(AttributeConstants.INCARNATION_ATTRIBUTE);
	    Long old = (Long) incarnationNumbers.get(sender_string);
	    if (incarnation == null) {
		if (loggingService.isInfoEnabled())
		    loggingService.info("No incarnation number in message " +
					message);
	    } else if (old == null || 
		       old.longValue() < incarnation.longValue()) {
		// First message from this sender or new incarnation 
		if (old != null && loggingService.isInfoEnabled())
		    loggingService.info("Detected new incarnation number " 
					+incarnation+ " in message " +message);
		incarnationNumbers.put(sender_string, incarnation);
		decacheDestinationLink(sender);
	    } else if (old.longValue() > incarnation.longValue()) {
		// Bogus message from old incarnation.  Pretend normal
		// delivery but don't process it.
		if (loggingService.isInfoEnabled())
		    loggingService.info("Detected obsolete incarnation number " 
					+incarnation+ " in message " +message+
					"\nShould be " +old);
		return DummyReturn;
	    }

	    if (agentIsObsolete(addr)) {
		if (loggingService.isErrorEnabled()) {
		    loggingService.error("Blocking message to obsolete agent: "
					 +message);
		}
		throw new MisdeliveredMessageException(message);
	    } else {
		return super.deliverMessage(message, addr);
	    }
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
	    if (agentIsObsolete(message.getOriginator())) {
		LoggingService loggingService = getLoggingService();
		if (loggingService.isWarnEnabled()) {
		    loggingService.warn("Blocking message from obsolete agent: " 
					+message);
		}
		return DummyReturn;
	    }
	    try {
		MessageAttributes reply = super.forwardMessage(message);
		Object status = reply.getAttribute(MessageAttributes.DELIVERY_ATTRIBUTE);
		if (status.equals(MessageAttributes.DELIVERY_STATUS_OLD_INCARNATION)) {
		    synchronized (obsoleteAgents) {
			obsoleteAgents.add(message.getOriginator().getPrimary());
		    }
		    LoggingService loggingService = getLoggingService();
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



    
