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
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.WhitePagesService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GossipAspect 
    extends StandardAspect
{
    // pseudo dynamic parameters
    private static final String REQUEST_GOSSIP_MAX_VALUE_STR = "500";
    private static final String REQUEST_GOSSIP_MAX_PARAM = "MaxRequestsPerMsg";

    private static final String TOPOLOGY = "topology";
    static final String VALUE_GOSSIP_ATTR = 
	"org.cougaar.core.mts.value-gossip";
    static final String REQUEST_GOSSIP_ATTR = 
	"org.cougaar.core.mts.request-gossip";
    private static final MessageAddress LIMBO =
	MessageAddress.getMessageAddress("wp_cant_find_node");

    private int requestGossipMaxPerMsg;
    private MetricsService metricsService;
    private GossipKeyDistributionService keyService;
    private GossipQualifierService qualifierService;
    private GossipUpdateService updateService;
    private WhitePagesService wpService;

    // Local data we'd like to get via gossip.
    private KeyGossip localRequests;

    // All requests, from all sources, local or remote.
    private KeyGossip allRequests;



    // Maps address to GossipSubscription, one per neighbor Node.
    // Each entry is the collection of requests we're waiting to send
    // to that neighbor.
    private HashMap pendingRequests;


    // Maps address to KeyGossip, one per requesting neighbor Node.
    // Each entry is the gossip that neighbor wants from us.
    private HashMap neighborsRequests;

    // Maps address to GossipSubscription, one per requesting neighbor
    // Node.  Each entry is the latest data we should forward to that
    // neighbor.
    private HashMap neighborsSubscriptions;

    // Maps Node addresses to Booleans, as a kind of per-Node lock
    // for key gossip.
    private HashMap neighborLocks;

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

    private synchronized void ensureQualifierService() {
	if (qualifierService == null) {
	    ServiceBroker sb = getServiceBroker();
	    qualifierService = (GossipQualifierService)
		sb.getService(this, GossipQualifierService.class, null);
	}
    }


    public void load() {
	super.load();

	ServiceBroker sb = getServiceBroker();

	metricsService = (MetricsService)
	    sb.getService(this, MetricsService.class, null);
	wpService = (WhitePagesService)
	    sb.getService(this, WhitePagesService.class, null);
	localRequests = new KeyGossip();
	allRequests = new KeyGossip();
	pendingRequests = new HashMap();
	neighborsRequests = new HashMap();
	neighborsSubscriptions = new HashMap();
	neighborLocks = new HashMap();

	keyService = new GossipKeyDistributionServiceImpl();
	
	NodeControlService ncs = (NodeControlService)
	    sb.getService(this, NodeControlService.class, null);
	ServiceBroker rootsb = ncs.getRootServiceBroker();

	ServiceProvider sp = new GossipServices();
	rootsb.addService(GossipKeyDistributionService.class, sp);
	if (loggingService.isInfoEnabled())
	    loggingService.info("Registered GossipKeyDistributionService");

// 	initializeParameter(REQUEST_GOSSIP_MAX_PARAM, REQUEST_GOSSIP_MAX_VALUE_STR);
	dynamicParameterChanged(REQUEST_GOSSIP_MAX_PARAM,
				getParameter(REQUEST_GOSSIP_MAX_PARAM,
					     REQUEST_GOSSIP_MAX_VALUE_STR));
	
    }


    protected void dynamicParameterChanged(String name, String value)
    {
	if (name.equals(REQUEST_GOSSIP_MAX_PARAM)) {
	    requestGossipMaxPerMsg = Integer.parseInt(value);
	}
    }

    private MessageAddress agentNode(MessageAddress agentAddr) {
	String agent = agentAddr.getAddress();
	try {
	    //only look at WP cache, do not block
	    AddressEntry entry = wpService.get(agent, TOPOLOGY, -1);
	    if (entry == null) {
		if (loggingService.isDebugEnabled())
		    loggingService.debug("WhitePages returned null entry for agent " 
					 +agent);
		return LIMBO;
	    } else {
		String node = entry.getURI().getPath().substring(1);
		return MessageAddress.getMessageAddress(node);
	    }
	} catch (Exception ex) {
	    if (loggingService.isDebugEnabled())
		loggingService.debug("", ex);
	    return LIMBO;
	}
    }

    private void addRequest(String key, int propagationDistance)
    {
	ensureQualifierService();
	if (qualifierService == null || qualifierService.shouldForwardRequest(key)) {
	    boolean new_entry = allRequests.add(key, propagationDistance);
	    if (new_entry) {
		// add to all pendingRequests
		synchronized (pendingRequests) {
		    Iterator itr = pendingRequests.values().iterator();
		    KeyGossip gossip = null;
		    while (itr.hasNext()) {
			gossip = (KeyGossip) itr.next();
			gossip.add(key, propagationDistance);
		    }
		}
	    }
	}
    }


    private void addNeighborRequest(KeyGossip gossip)
    {
	Iterator itr = gossip.iterator();
	Map.Entry entry = null;
	String key = null;
	GossipPropagation propagation = null;
	while (itr.hasNext()) {
	    entry = (Map.Entry) itr.next();
	    key = (String) entry.getKey();
	    propagation = (GossipPropagation) entry.getValue();
	    int distance = propagation.getDistance();
	    // This is a non-local request, so the propagation count
	    // is relative to the originator.  Given which, the
	    // appropriate distance test is 1, not 0.  If a local
	    // requests ever went through here, this wouldn't work
	    // properly.
	    if (distance > 1) addRequest(key, --distance);
	}
    }

    private KeyGossip findOrMakePendingRequests(MessageAddress neighbor)
    {
	KeyGossip result = null;
	synchronized (pendingRequests) {
	    result = (KeyGossip) pendingRequests.get(neighbor);
	    if (result == null) {
		result = allRequests.cloneGossip();
		pendingRequests.put(neighbor, result);
	    }
	}
	return result;
	
    }

    // A neighbor wants us to notify him if we see this key
    private void handleRequestGossip(MessageAddress agent, KeyGossip gossip) {
	MessageAddress neighbor = agentNode(agent);
	if (loggingService.isInfoEnabled())
	    loggingService.info("Received gossip requests from " 
				+neighbor+ "="
				+gossip.prettyPrint());

	// Add Neighbors requests to our nodes request's
	addNeighborRequest(gossip);
	synchronized(this) {

	    // Remember requests from neighbor
	   KeyGossip old = (KeyGossip) neighborsRequests.get(neighbor);
	    if (old == null) {
		old = new KeyGossip();
		neighborsRequests.put(neighbor, old);
	    }
	    old.add(gossip);
	    
	    // Add Subscriptions for request
	    GossipSubscription sub = (GossipSubscription) 
		neighborsSubscriptions.get(neighbor);
	    if (sub == null) {
		ensureQualifierService();
		sub = new GossipSubscription(neighbor, metricsService,
					     qualifierService);
		neighborsSubscriptions.put(neighbor, sub);
	    }
	    if (loggingService.isInfoEnabled()) {
		loggingService.info("Adding subscriptions for requests from "
				    +neighbor+ "\n" + gossip.prettyPrint());
	    }
	    sub.add(gossip);
	}
    }

    // A neighbor has provided us with a value we asked for
    private void handleValueGossip(MessageAddress agent, 
				   ValueGossip gossip)
    {
	MessageAddress neighbor = agentNode(agent);
	if (loggingService.isInfoEnabled())
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


    private void addGossipValues(MessageAddress neighbor,
				 AttributedMessage message)
    {
	GossipSubscription sub = (GossipSubscription) 
	    neighborsSubscriptions.get(neighbor);
	if (sub != null) {
	    ValueGossip changes = sub.getChanges();
	    if (changes != null) {
		if (loggingService.isInfoEnabled())
		    loggingService.info("Adding gossip data for "
					+neighbor+
					"="
					+changes.prettyPrint());
		message.setAttribute(VALUE_GOSSIP_ATTR, changes);
	    }
	}
    }

    private boolean getGossipLock(MessageAddress neighbor)
    {
	boolean result = false;
	synchronized (neighbor) {
	    Boolean locked = (Boolean) neighborLocks.get(neighbor);
	    if (locked == null || !locked.booleanValue()) {
		neighborLocks.put(neighbor, Boolean.TRUE);
		result = true;
	    }
	}
	return true; // result;
    }

    private void releaseGossipLock(MessageAddress neighbor)
    {
	synchronized (neighbor) {
	    neighborLocks.put(neighbor, Boolean.FALSE);
	}
    }

    private void addGossipRequests(MessageAddress neighbor,
			       AttributedMessage message)
    {
	KeyGossip pending = findOrMakePendingRequests(neighbor);
	KeyGossip clone = null;

	// Limit the size
	synchronized (pending) {
	    clone = pending.cloneGossip(requestGossipMaxPerMsg);
	}
	if (loggingService.isInfoEnabled()) {
	    if (clone.size() < pending.size()) {
		loggingService.info("Truncated pending requests to " 
				    +neighbor);
	    }
	    loggingService.info("About to send requests to " +neighbor+
				"\n" + clone.prettyPrint());
	}
	if (!clone.isEmpty()) message.setAttribute(REQUEST_GOSSIP_ATTR, clone);
    }

    private void commitValues(MessageAddress neighbor,
			      AttributedMessage message)
    {
	GossipSubscription sub = (GossipSubscription) 
	    neighborsSubscriptions.get(neighbor);
	ValueGossip sent = (ValueGossip) 
	    message.getAttribute(VALUE_GOSSIP_ATTR);
	if (sub != null && sent != null) sub.commitChanges(sent);
    }

    private void commitRequests(MessageAddress neighbor,
				AttributedMessage message)
    {
	KeyGossip pending = findOrMakePendingRequests(neighbor);
	KeyGossip sent = (KeyGossip) 
	    message.getAttribute(REQUEST_GOSSIP_ATTR);
	if (sent != null && !sent.isEmpty()) pending.commitChanges(sent);
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
	    // Neighbor requests (excluding the recipient)
	    MessageAddress neighbor = agentNode(message.getTarget());
	    MessageAttributes result = null;

	    // Check for other in-flight messages to this neighbor. 
	    boolean locked = getGossipLock(neighbor);
	    
	    if (locked) {
		addGossipRequests(neighbor, message);
		addGossipValues(neighbor, message);
	    }

	    try {
		result = super.forwardMessage(message);

		// If the forward succeeds, commit changes.
		// If there was an exception, we won't get here
		commitRequests(neighbor, message);
		commitValues(neighbor, message);
	    } finally {
		if (locked) releaseGossipLock(neighbor);
	    }
	    
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
	    Object requestGossip = 	message.getAttribute(REQUEST_GOSSIP_ATTR);
	    if (requestGossip != null) {
		if (requestGossip instanceof KeyGossip) {
		    handleRequestGossip(message.getOriginator(),
				    (KeyGossip) requestGossip);
		} else {
		    loggingService.error("Weird gossip request in " 
					 +REQUEST_GOSSIP_ATTR+
					 "="
					 +requestGossip);
		}
		message.removeAttribute(REQUEST_GOSSIP_ATTR);
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

	public void addKey(String key, int propagationDistance) {
	    if (loggingService.isInfoEnabled())
		loggingService.info("GossipKeyDistributionService.addKey " 
				    +key);
	    localRequests.add(key, propagationDistance);
	    addRequest(key, propagationDistance);
	}

	public void removeKey(String key) {
	    localRequests.removeEntry(key);
	}

    }

}
