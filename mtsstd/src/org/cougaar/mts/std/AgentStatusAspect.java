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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.qos.metrics.Constants;
import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.qos.metrics.MetricImpl;
import org.cougaar.core.qos.metrics.MetricsUpdateService;

public class AgentStatusAspect 
    extends StandardAspect
    implements AgentStatusService, Constants, AttributeConstants
{

    private static final double SEND_CREDIBILITY = 
	Constants.SECOND_MEAS_CREDIBILITY;


    private HashMap remoteStates;
    private HashMap localStates;
    private MetricsUpdateService metricsUpdateService;
    
    public AgentStatusAspect() {
	remoteStates = new HashMap();
	localStates = new HashMap();
    }

    public void load() {
	super.load();
	metricsUpdateService = (MetricsUpdateService)
	    getServiceBroker().getService(this, MetricsUpdateService.class, null);
    }
    
    private AgentState ensureRemoteState(MessageAddress address) {
	AgentState state = null;
	synchronized(remoteStates){
	    state = (AgentState) remoteStates.get(address);
	    if (state == null) {
		state = newAgentState();
		remoteStates.put(address, state);
	    }
	}
	return state;
    }

    private  AgentState getRemoteState(MessageAddress address){
	AgentState state = null;
	synchronized(remoteStates){
	    state = (AgentState) remoteStates.get(address);
	}
	return state;
    }

    private AgentState ensureLocalState(MessageAddress address) {
	AgentState state = null;
	synchronized(localStates){
	    state = (AgentState) localStates.get(address);
	    if (state == null) {
		state = newAgentState();
		localStates.put(address, state);
	    }
	}
	return state;
    }

    private  AgentState getLocalState(MessageAddress address){
	AgentState state = null;
	synchronized(localStates){
	    state = (AgentState) localStates.get(address);
	}
	return state;
    }

    private AgentState newAgentState() {
	AgentState state = new AgentState();
	// JAZ must be a better way to initialize an object
	state.timestamp = System.currentTimeMillis();
	state.status = UNREGISTERED;
	state.queueLength=0;
	state.receivedCount=0;
	state.receivedBytes=0;
	state.lastReceivedBytes=0;
	state.sendCount = 0;
	state.deliveredCount = 0;
	state.deliveredBytes=0;
	state.lastDeliveredBytes=0;
	state.deliveredLatencySum = 0;
	state.lastDeliveredLatency = 0;
	state.averageDeliveredLatency = 0;
	state.unregisteredNameCount = 0;
	state.nameLookupFailureCount = 0;
	state.commFailureCount = 0;
	state.misdeliveredMessageCount = 0;	
	state.lastLinkProtocolTried = null;
	state.lastLinkProtocolSuccess=null;
	return state;
    }


    // JAZ must be a better way to clone an object
    private AgentState snapshotState(AgentState state) {
	AgentState result = new AgentState();
	synchronized (state) {
	    result.timestamp = state.timestamp;
	    result.status = state.status;
	    result.queueLength = state.queueLength;
	    result.receivedCount=state.receivedCount;
	    result.receivedBytes=state.receivedBytes;
	    result.lastReceivedBytes=state.lastReceivedBytes;
	    result.sendCount =state.sendCount ;
	    result.deliveredCount =state.deliveredCount ;
	    result.deliveredBytes=state.deliveredBytes;
	    result.lastDeliveredBytes=state.lastDeliveredBytes;
	    result.deliveredLatencySum = state.deliveredLatencySum ;
	    result.lastDeliveredLatency =state.lastDeliveredLatency ;
	    result.averageDeliveredLatency =state.averageDeliveredLatency ;
	    result.unregisteredNameCount =state.unregisteredNameCount ;
	    result.nameLookupFailureCount =state.nameLookupFailureCount ;
	    result.commFailureCount =state.commFailureCount ;
	    result.misdeliveredMessageCount =state.misdeliveredMessageCount ;
	    result.lastLinkProtocolTried =state.lastLinkProtocolTried ;
	    result.lastLinkProtocolSuccess=state.lastLinkProtocolSuccess;
	}
	return result;
    }
 
    private Metric longMetric(long value) {
	return new MetricImpl(new Long(value),
			      SEND_CREDIBILITY,
			      "",
			      "AgentStatusAspect");
    }

    //
    // Agent Status Service Public Interface

    // Deprecated: For backwards compatibility
    public AgentState getAgentState(MessageAddress address) {
	return getRemoteAgentState(address);
    }

    public AgentState getLocalAgentState(MessageAddress address) {
	AgentState state = getLocalState(address);
	// must snapshot state or caller will get a dynamic value.
	if (state != null) return snapshotState(state);
	else return null;
    }

  public AgentState getRemoteAgentState(MessageAddress address) {
	AgentState state = getRemoteState(address);
	// must snapshot state or caller will get a dynamic value.
	if (state != null) return snapshotState(state);
	else return null;
    }


    public Set getLocalAgents() {
	Set result = new  java.util.HashSet();
	synchronized (localStates) {
	    result.addAll(localStates.keySet());
	}
	return result;
    }


    public Set getRemoteAgents() {	
	Set result = new java.util.HashSet();
	synchronized (remoteStates) {
	    result.addAll(remoteStates.keySet());
	}
	return result;
    }


    // 
    // Aspect Code to implement Sensors

    public Object getDelegate(Object object, Class type) {
	if (type == DestinationLink.class) {
	    return new AgentStatusDestinationLink((DestinationLink) object);
	} else 	if (type == SendQueue.class) {
	    return new SendQueueDelegate((SendQueue) object);
	} else 	if (type == MessageDeliverer.class) {
	    return new MessageDelivererDelegate((MessageDeliverer) object);
	} else {
	    return null;
	}
    }

 
    public class AgentStatusDestinationLink 
	extends DestinationLinkDelegateImplBase
    {
	private String spoke_key, heard_key;

	public AgentStatusDestinationLink(DestinationLink link)
	{
	    super(link);
	    String remoteAgent = link.getDestination().getAddress();
	    spoke_key = "Agent" +KEY_SEPR+ remoteAgent +KEY_SEPR+ "SpokeTime";
	    heard_key = "Agent" +KEY_SEPR+ remoteAgent +KEY_SEPR+ "HeardTime";
	}
	
	boolean delivered(MessageAttributes attributes) {
	    return 
		attributes != null &
		attributes.getAttribute(DELIVERY_ATTRIBUTE).equals(DELIVERY_STATUS_DELIVERED);
	}

	public MessageAttributes forwardMessage(AttributedMessage message) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException

	{
	    MessageAddress remoteAddr = message.getTarget();
	    AgentState remoteState = ensureRemoteState(remoteAddr);
	    MessageAddress localAddr = message.getOriginator();
	    AgentState localState = ensureLocalState(localAddr);
	    
	    try {
		long startTime = System.currentTimeMillis();
		metricsUpdateService.updateValue(spoke_key, 
						 longMetric(startTime));
		synchronized (remoteState) {
		    remoteState.lastLinkProtocolTried=
			getProtocolClass().getName();
		}
		// Attempt to Deliver message
		MessageAttributes meta = super.forwardMessage(message);

		//successful Delivery
		long endTime = System.currentTimeMillis();
		
		if (delivered(meta))
		    metricsUpdateService.updateValue(heard_key, 
						     longMetric(endTime));
		int msgBytes=0;
		Object attr= message.getAttribute(MESSAGE_BYTES_ATTRIBUTE);
		if (attr!=null && (attr instanceof Number) )
		    msgBytes=((Number) attr).intValue();

		long latency = endTime - startTime;
		double alpha = 0.20;
		synchronized (remoteState) {
		    remoteState.status =  AgentStatusService.ACTIVE;
		    remoteState.timestamp = System.currentTimeMillis();
		    remoteState.deliveredCount++;
		    remoteState.deliveredBytes+=msgBytes;
		    remoteState.lastDeliveredBytes=msgBytes;
		    remoteState.queueLength--;
		    remoteState.lastDeliveredLatency = (int) latency;
		    remoteState.deliveredLatencySum +=  latency;
		    remoteState.averageDeliveredLatency = (alpha * latency) +
			((1-alpha) * remoteState.averageDeliveredLatency);
		    remoteState.lastLinkProtocolSuccess=
			getProtocolClass().getName();
		}
		synchronized (localState) {
		    localState.status =  AgentStatusService.ACTIVE;
		    localState.timestamp = System.currentTimeMillis();
		    localState.deliveredCount++;
		    localState.deliveredBytes+=msgBytes;
		    localState.lastDeliveredBytes=msgBytes;
		}


		return meta;

	    } catch (UnregisteredNameException unreg) {
		synchronized (remoteState) {
		    remoteState.status = UNREGISTERED;
		    remoteState.timestamp = System.currentTimeMillis();
		    remoteState.unregisteredNameCount++;
		}
		throw unreg;
	    } catch (NameLookupException namex) {
		synchronized (remoteState) {
		    remoteState.status =UNKNOWN;
		    remoteState.timestamp = System.currentTimeMillis();
		    remoteState.nameLookupFailureCount++;
		}
		throw namex;
	    } catch (CommFailureException commex) {
		synchronized (remoteState) {
		    remoteState.status =UNREACHABLE;
		    remoteState.timestamp = System.currentTimeMillis();
		    remoteState.commFailureCount++;
		}
		throw commex;
	    } catch (MisdeliveredMessageException misd) {
		synchronized (remoteState) {
		    remoteState.status =UNREGISTERED;
		    remoteState.timestamp = System.currentTimeMillis();
		    remoteState.misdeliveredMessageCount++;
		}	
		throw misd;
	    }
	}
	
    }

    public class  MessageDelivererDelegate 
	extends MessageDelivererDelegateImplBase 
    {

	MessageDelivererDelegate(MessageDeliverer delegatee) {
	    super(delegatee);
	}

	public MessageAttributes deliverMessage(AttributedMessage message,
						MessageAddress dest)
	    throws MisdeliveredMessageException
	{  
	    String remoteAgent= message.getOriginator().getAddress();
	    String heard_key = "Agent" +KEY_SEPR+ remoteAgent 
		+KEY_SEPR+ "HeardTime";
	    long receiveTime = System.currentTimeMillis();
	    metricsUpdateService.updateValue(heard_key, 
					     longMetric(receiveTime));

	    int msgBytes=0;
	    Object attr= message.getAttribute(MESSAGE_BYTES_ATTRIBUTE);
	    if (attr!=null && (attr instanceof Number) )
		msgBytes=((Number) attr).intValue();

	    AgentState remoteState = 
		ensureRemoteState(message.getOriginator());
	    synchronized (remoteState) {
		remoteState.receivedCount++;
		remoteState.receivedBytes+=msgBytes;
	    }
		    
	    AgentState localState = 
		ensureLocalState(message.getTarget());
	    synchronized (localState) {
		localState.receivedCount++;
		localState.receivedBytes+=msgBytes;
	    }

	    return super.deliverMessage(message, dest);
	}

    }


    public class SendQueueDelegate 
	extends SendQueueDelegateImplBase
    {
	public SendQueueDelegate (SendQueue queue) {

	    super(queue);
	}
	
	public void sendMessage(AttributedMessage message) {
	    MessageAddress remoteAddr = message.getTarget();
	    AgentState remoteState = ensureRemoteState(remoteAddr);
	    MessageAddress localAddr = message.getOriginator();
	    AgentState localState = ensureLocalState(localAddr);

	    synchronized (remoteState) {
		remoteState.sendCount++;
		remoteState.queueLength++;
	    }	

	    synchronized (localState) {
		localState.sendCount++;
	    }	

	    //Local agent sending message means that the MTS has
	    //"heard from" the local agent
	    String localAgent = localAddr.getAddress();
	    String heard_key = "Agent" +KEY_SEPR+ localAgent 
		+KEY_SEPR+ "HeardTime";
	    long receiveTime = System.currentTimeMillis();
	    metricsUpdateService.updateValue(heard_key, 
					     longMetric(receiveTime));

	    super.sendMessage(message);
	}
    }
}
