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
import org.cougaar.core.qos.metrics.GossipKeyDistributionService;
import org.cougaar.core.qos.metrics.GossipUpdateService;
import org.cougaar.core.qos.metrics.MetricsService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GossipAspect 
    extends StandardAspect
{
    private static final String VALUE_GOSSIP_ATTR = 
	"org.cougaar.core.mts.value-gossip";
    private static final String KEY_GOSSIP_ATTR = 
	"org.cougaar.core.mts.key-gossip";

    private MetricsService metricsService;

    // Maps address to KeyGossip, one per neighbor.
    // Each entry is the gossip that we've asked for so far from that
    // neighbor. 
    private HashMap propagatedRequests;

    // Local data we'd like to get via gossip.
    private KeyGossip localRequests;

    // Maps address to KeyGossip, one per requesting neighbor.
    // Each entry is the gossip that neighbor wants from us
    private HashMap neighborsRequests;

    // Maps address to GossipSubscription, one per requesting
    // neighbor.  Each entry is the latest data we should forward to
    // that neighbor.
    private HashMap neighborsSubscriptions;

    private GossipKeyDistributionService keyService;

    private GossipUpdateService updateService;

    public Object getDelegate(Object delegatee, Class type) 
    {
	if (type == DestinationLink.class) {
	    // RMI only!
	    DestinationLink link = (DestinationLink) delegatee;
	    Class cl = link.getProtocolClass();
	    if (RMILinkProtocol.class.isAssignableFrom(cl)) {
		return new DestinationLinkDelegate(link);
	    }
	} else if (type == MessageDeliverer.class) {
	    return new DelivererDelegate((MessageDeliverer) delegatee);
	}

	return null;
    }

    public void load() {
	super.load();

	ServiceBroker sb = getServiceBroker();

	metricsService = (MetricsService)
	    sb.getService(this, MetricsService.class, null);

	localRequests = new KeyGossip();
	propagatedRequests = new HashMap();
	neighborsRequests = new HashMap();
	neighborsSubscriptions = new HashMap();

	keyService = new GossipKeyDistributionServiceImpl();
	
	NodeControlService ncs = (NodeControlService)
	    sb.getService(this, NodeControlService.class, null);
	ServiceBroker rootsb = ncs.getRootServiceBroker();

	ServiceProvider sp = new GossipServices();
	rootsb.addService(GossipKeyDistributionService.class, sp);
	loggingService.info("Registered GossipKeyDistributionService");
    }

    // A neighbor wants us to notify him if we see this key
    private void handleKeyGossip(MessageAddress neighbor, KeyGossip gossip) {
	loggingService.info("Received gossip requests from " 
			    +neighbor+ "="
			    +gossip.prettyPrint());
	synchronized(this) {
	   KeyGossip old = (KeyGossip) neighborsRequests.get(neighbor);
	    if (old == null) {
		old = new KeyGossip();
		neighborsRequests.put(neighbor, old);
	    }
	    old.add(gossip);

	    GossipSubscription sub = (GossipSubscription) 
		neighborsSubscriptions.get(neighbor);
	    if (sub == null) {
		sub = new GossipSubscription(neighbor, metricsService);
		neighborsSubscriptions.put(neighbor, sub);
	    }
	    sub.add(gossip);
	}
    }

    // A neighbor has provided us with a value we asked for
    private void handleValueGossip(MessageAddress neighbor, ValueGossip gossip)
    {
	loggingService.info("Received gossip data from " 
			     +neighbor+ "="
			    +gossip.prettyPrint());
	ServiceBroker sb = getServiceBroker();
	synchronized (this) {
	    if (updateService == null) 
		updateService = (GossipUpdateService)
		    sb.getService(this, GossipUpdateService.class, null);
	    if (updateService != null) {
		gossip.update(updateService);
	    }
	}
    }


    // Ask target for any gossip we haven't already asked him for
    private synchronized void addRequests(AttributedMessage message,
					  KeyGossip potentialGossip) 
    {
	KeyGossip messageGossip = (KeyGossip) 
	    message.getAttribute(KEY_GOSSIP_ATTR);
	
	MessageAddress destination = message.getTarget();
	KeyGossip propagatedGossip = (KeyGossip) 
	    propagatedRequests.get(destination);
	if (propagatedGossip == null) {
	    propagatedGossip = new KeyGossip();
	    propagatedRequests.put(destination, propagatedGossip);
	}
	
	KeyGossip addendum = potentialGossip.computeAddendum(propagatedGossip);

	if (messageGossip != null)
	    loggingService.info("Existing requests for " +destination+ '=' 
				+messageGossip.prettyPrint());
	if (addendum != null) {
	    loggingService.info("Additional requests for " 
				+destination+ '='
				+addendum.prettyPrint());
	    if (messageGossip == null) messageGossip = new KeyGossip(); 
	    messageGossip.add(addendum);
	}

	if (messageGossip != null) {
	    message.setAttribute(KEY_GOSSIP_ATTR, messageGossip);
	}
	
    }


    private synchronized void commitRequests(AttributedMessage message) 
    {
	KeyGossip messageGossip = (KeyGossip) 
	    message.getAttribute(KEY_GOSSIP_ATTR);
	if (messageGossip == null) return;

	MessageAddress destination = message.getTarget();
	KeyGossip propagatedGossip = (KeyGossip) 
	    propagatedRequests.get(destination);
	if (propagatedGossip != null) propagatedGossip.add(messageGossip);
    }


    private synchronized void addGossipValues(MessageAddress destination,
					      AttributedMessage message)
    {
	GossipSubscription sub = (GossipSubscription)
	    neighborsSubscriptions.get(destination);
	if (sub != null) {
	    ValueGossip changes = sub.getChanges();
	    if (changes != null) {
		loggingService.info("Adding gossip data for "
				    +destination+
				    "="
				    +changes.prettyPrint());
		message.setAttribute(VALUE_GOSSIP_ATTR, changes);
	    }
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
	    // Add gossip attributes
	    // Local requests
	    addRequests(message, localRequests);

	    // Neighbor requests (excluding the recipient)
	    Iterator itr = neighborsRequests.entrySet().iterator();
	    MessageAddress destination = message.getTarget();
	    while (itr.hasNext()) {
		Map.Entry entry = (Map.Entry) itr.next();
		MessageAddress addr = (MessageAddress) entry.getKey();
		KeyGossip gossip = (KeyGossip) entry.getValue();
		if (!addr.equals(destination)) {
		    addRequests(message, gossip);
		}
	    }

	    // Now add any updates for the destination
	    addGossipValues(destination, message);
	    

	    MessageAttributes result = super.forwardMessage(message);
	    
	    // If the forward succeeds, commit changes.
	    // If there was an exception, we won't get here
	    commitRequests(message);

	    // TO BE DONE: commit values
	    
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
	    Object keyGossip = 	message.getAttribute(KEY_GOSSIP_ATTR);
	    if (keyGossip != null) {
		if (keyGossip instanceof KeyGossip) {
		    handleKeyGossip(message.getOriginator(),
				    (KeyGossip) keyGossip);
		} else {
		    loggingService.error("Weird gossip request in " 
					 +KEY_GOSSIP_ATTR+
					 "="
					 +keyGossip);
		}
		message.removeAttribute(KEY_GOSSIP_ATTR);
	    }
		
	    Object valueGossip = message.getAttribute(VALUE_GOSSIP_ATTR);
	    if (valueGossip != null) {
		if (valueGossip instanceof ValueGossip) {
		    handleValueGossip(message.getOriginator(),
				      (ValueGossip) valueGossip);
		} else {
		    loggingService.error("Weird gossip data in " 
					 +VALUE_GOSSIP_ATTR+
					 "="
					 +valueGossip);
		}
		message.removeAttribute(VALUE_GOSSIP_ATTR);
	    }

	    return super.deliverMessage(message, dest);
	}

    }

    private class GossipServices implements ServiceProvider {
	public Object getService(ServiceBroker sb, 
				 Object requestor, 
				 Class serviceClass) 
	{
	    if (serviceClass == GossipKeyDistributionService.class) {
		return keyService;
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

    }


    private class GossipKeyDistributionServiceImpl
	implements GossipKeyDistributionService
    {

	public void addKey(String key) {
	    loggingService.info("GossipKeyDistributionService.addKey " +key);
	    localRequests.add(key);
	}

	public void removeKey(String key) {
	    localRequests.removeEntry(key);
	}

    }

}
