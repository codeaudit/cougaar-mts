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

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.qos.metrics.Metric;

final public class GossipStatisticsServiceAspect
    extends StandardAspect
    implements ServiceProvider
{
    private GossipTrafficRecord stats;
    private GossipStatisticsService impl;

    public GossipStatisticsServiceAspect()
    {
    }

    public void load() {
	super.load();

	stats = new GossipTrafficRecord();
	this.impl = new Impl();

	ServiceBroker sb = getServiceBroker();

	NodeControlService ncs = (NodeControlService)
	    sb.getService(this, NodeControlService.class, null);

	ServiceBroker rootsb = ncs.getRootServiceBroker();
	sb.releaseService(this, NodeControlService.class, null);

	rootsb.addService(GossipStatisticsService.class, this);

	
    }

    public Object getReverseDelegate(Object delegatee, Class type) 
    {
	if (type == MessageDeliverer.class) {
	    return new DelivererDelegate((MessageDeliverer) delegatee);
	} else {
	    return null;
	}
    }

    public Object getDelegate(Object delegatee, Class type) 
    {
	if (type == DestinationLink.class) {
	    // RMI only!
	    DestinationLink link = (DestinationLink) delegatee;
	    Class cl = link.getProtocolClass();
	    if (RMILinkProtocol.class.isAssignableFrom(cl)) {
		return new DestinationLinkDelegate(link);
	    } else {
		return null;
	    }
	} else {
	    return null;
	}
    }


    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	if (serviceClass == GossipStatisticsService.class) {
	    return impl;
	} else {
	    return null;
	}
    }

    public void releaseService(ServiceBroker sb, 
			       Object requestor, 
			       Class serviceClass, 
			       Object service)
    {
    }


    void requestsSent(KeyGossip gossip) {
	stats.requests_sent += gossip.size();
    }

    void requestsReceived(KeyGossip gossip) {
	stats.requests_rcvd += gossip.size();
    }

    void valuesSent(ValueGossip gossip) {
	stats.values_sent += gossip.size();
    }

    void valuesReceived(ValueGossip gossip) {
	stats.values_rcvd += gossip.size();
    }

    private boolean hasGossip(Gossip gossip)
    {
	return gossip != null && !gossip.isEmpty();
    }


    private class Impl implements GossipStatisticsService {

	Impl() {
	}

	public GossipTrafficRecord getStatistics() {
	    return new GossipTrafficRecord(stats);
	}

    }

    private class DestinationLinkDelegate 
	extends DestinationLinkDelegateImplBase
    {
	DestinationLinkDelegate(DestinationLink delegatee) {
	    super(delegatee);
	}

	public MessageAttributes forwardMessage(AttributedMessage message) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException
	{
	    MessageAttributes result = super.forwardMessage(message);
	    // statistics
	    KeyGossip keyGossip = (KeyGossip) 
		message.getAttribute(GossipAspect.REQUEST_GOSSIP_ATTR);
	    ValueGossip valueGossip = (ValueGossip) 
		message.getAttribute(GossipAspect.VALUE_GOSSIP_ATTR);
	    if (hasGossip(keyGossip)) requestsSent(keyGossip);
	    if (hasGossip(valueGossip)) valuesSent(valueGossip);
	    ++stats.msg_sent;
	    if (hasGossip(keyGossip) || hasGossip(valueGossip))
		++stats.msg_with_gossip_sent;
	    return result;
	}

    }

    private class DelivererDelegate 
	extends MessageDelivererDelegateImplBase
    {
	DelivererDelegate(MessageDeliverer delegatee) {
	    super(delegatee);
	}


	public MessageAttributes deliverMessage(AttributedMessage message,
						MessageAddress dest)
	    throws MisdeliveredMessageException
	{
	    // statistics
	    KeyGossip keyGossip = (KeyGossip) 
		message.getAttribute(GossipAspect.REQUEST_GOSSIP_ATTR);
	    ValueGossip valueGossip = (ValueGossip) 
		message.getAttribute(GossipAspect.VALUE_GOSSIP_ATTR);
	    if (hasGossip(keyGossip)) requestsReceived(keyGossip);
	    if (hasGossip(valueGossip)) valuesReceived(valueGossip);
	    ++stats.msg_rcvd;
	    if (hasGossip(keyGossip) || hasGossip(valueGossip))
		++stats.msg_with_gossip_rcvd;
	    return super.deliverMessage(message, dest);
	}
	}

}

