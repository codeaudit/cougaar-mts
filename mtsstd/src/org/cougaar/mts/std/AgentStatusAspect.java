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

import org.cougaar.core.qos.metrics.Constants;
import org.cougaar.core.qos.metrics.MetricImpl;
import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.qos.metrics.MetricsUpdateService;

import java.util.HashMap;

public class AgentStatusAspect 
    extends StandardAspect
    implements AgentStatusService, Constants, AttributeConstants
{

    private static final double SEND_CREDIBILITY = 
	Constants.SECOND_MEAS_CREDIBILITY;


    private HashMap states;
    private MetricsUpdateService metricsUpdateService;
    
    public AgentStatusAspect() {
	states = new HashMap();
    }

    public void load() {
	super.load();
	metricsUpdateService = (MetricsUpdateService)
	    getServiceBroker().getService(this, MetricsUpdateService.class, null);
    }
    
   private synchronized AgentState ensureState(MessageAddress address)
    {
	AgentState state = (AgentState) states.get(address);
	if (state == null) {
	    state = new AgentState();
	    System.out.println("Initializing State Agent"+address);
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

	    states.put(address, state);
	}
	return state;
    }

    // JAZ must be a better way to clone an object
    public AgentState snapshotState(AgentState state) {
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

    // must snapshot state or caller will get a dynamic value.
    public AgentState getAgentState(MessageAddress address) {
	//AgentState state = (AgentState) states.get(address);
	AgentState state = ensureState(address);
	System.out.println("#########Agent Status="+address+
			   "messageOut="+state.sendCount+
			   "states="+states.size());
	if (state != null) return snapshotState(state);
	else return null;
    }



    public class AgentStatusDestinationLink 
	extends DestinationLinkDelegateImplBase
    {
	private String spoke_key, heard_key;

	public AgentStatusDestinationLink(DestinationLink link)
	{
	    super(link);
	    String agent = link.getDestination().getAddress();
	    spoke_key = "Agent" +KEY_SEPR+ agent +KEY_SEPR+ "SpokeTime";
	    heard_key = "Agent" +KEY_SEPR+ agent +KEY_SEPR+ "HeardTime";
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
	    MessageAddress addr = message.getTarget();
	    AgentState state = ensureState(addr);
	    
	    try {
		long startTime = System.currentTimeMillis();
		metricsUpdateService.updateValue(spoke_key, 
						 longMetric(startTime));
		synchronized (state) {
		    state.lastLinkProtocolTried=getProtocolClass().getName();
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
		double alpha = 0.333;
		synchronized (state) {
		    state.status =  AgentStatusService.ACTIVE;
		    state.timestamp = System.currentTimeMillis();
		    state.deliveredCount++;
		    state.deliveredBytes+=msgBytes;
		    state.lastDeliveredBytes=msgBytes;
		    state.queueLength--;
		    state.lastDeliveredLatency = (int) latency;
		    state.deliveredLatencySum +=  latency;
		    state.averageDeliveredLatency = (alpha * latency) +
			((1-alpha) * latency);
		    state.lastLinkProtocolSuccess=getProtocolClass().getName();
		}
		System.out.println("Wrote Status="+addr+
				   "messageOut="+state.sendCount+
				   "states="+states.size());
		return meta;

	    } catch (UnregisteredNameException unreg) {
		synchronized (state) {
		    state.status = UNREGISTERED;
		    state.timestamp = System.currentTimeMillis();
		    state.unregisteredNameCount++;
		}
		throw unreg;
	    } catch (NameLookupException namex) {
		synchronized (state) {
		    state.status =UNKNOWN;
		    state.timestamp = System.currentTimeMillis();
		    state.nameLookupFailureCount++;
		}
		throw namex;
	    } catch (CommFailureException commex) {
		synchronized (state) {
		    state.status =UNREACHABLE;
		    state.timestamp = System.currentTimeMillis();
		    state.commFailureCount++;
		}
		throw commex;
	    } catch (MisdeliveredMessageException misd) {
		synchronized (state) {
		    state.status =UNREGISTERED;
		    state.timestamp = System.currentTimeMillis();
		    state.misdeliveredMessageCount++;
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
	    String agent= message.getOriginator().getAddress();
	    String heard_key = "Agent" +KEY_SEPR+ agent +KEY_SEPR+ "HeardTime";
	    long receiveTime = System.currentTimeMillis();
	    metricsUpdateService.updateValue(heard_key, 
					     longMetric(receiveTime));

	    int msgBytes=0;
	    Object attr= message.getAttribute(MESSAGE_BYTES_ATTRIBUTE);
	    if (attr!=null && (attr instanceof Number) )
		msgBytes=((Number) attr).intValue();

	    AgentState state = ensureState(message.getOriginator());
	    synchronized (state) {
		state.receivedCount++;
		state.receivedBytes+=msgBytes;
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
	    MessageAddress addr = message.getTarget();
	    AgentState state = ensureState(addr);
	    synchronized (state) {
		state.sendCount++;
		state.queueLength++;
	    }	
	    //Local agent sending message means that the MTS has
	    //"heard from" the local agent
	    String agent= message.getOriginator().getAddress();
	    String heard_key = "Agent" +KEY_SEPR+ agent +KEY_SEPR+ "HeardTime";
	    long receiveTime = System.currentTimeMillis();
	    metricsUpdateService.updateValue(heard_key, longMetric(receiveTime));

	    super.sendMessage(message);
	}
	
    }

}
