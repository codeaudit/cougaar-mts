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

    private static final double SEND_CREDIBILITY = Constants.SECOND_MEAS_CREDIBILITY;


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
	    state.status = UNREGISTERED;
	    state.timestamp = System.currentTimeMillis();
	    state.sendCount = 0;
	    state.deliveredCount = 0;
	    state.lastDeliverTime = 0;
	    state.averageDeliverTime = 0;
	    state.unregisteredNameCount = 0;
	    state.nameLookupFailureCount = 0;
	    state.commFailureCount = 0;
	    state.misdeliveredMessageCount = 0;	  
	    states.put(address, state);
	}
	return state;
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

    public AgentState getAgentState(MessageAddress address) {
	return ensureState(address);
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
		metricsUpdateService.updateValue(spoke_key, longMetric(startTime));
		MessageAttributes meta = super.forwardMessage(message);
		//successful Delivery
		long endTime = System.currentTimeMillis();
		
		if (delivered(meta))
		    metricsUpdateService.updateValue(heard_key, 
						     longMetric(endTime));

		long latency = endTime - startTime;
		double alpha = 0.333;
		synchronized (state) {
		    state.status =  AgentStatusService.ACTIVE;
		    state.timestamp = System.currentTimeMillis();
		    state.deliveredCount++;
		    state.lastDeliverTime = (int) latency;
		    state.averageDeliverTime = (alpha * latency) +
			((1-alpha) * latency);
		}
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
	    metricsUpdateService.updateValue(heard_key, longMetric(receiveTime));

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
