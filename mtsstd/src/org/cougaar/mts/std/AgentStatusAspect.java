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
import java.util.Set;

import org.cougaar.core.mts.AgentStatusService;
import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.qos.metrics.Constants;
import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.qos.metrics.MetricImpl;
import org.cougaar.core.qos.metrics.MetricsUpdateService;
import org.cougaar.core.service.LoggingService;

import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MessageDelivererDelegateImplBase;
import org.cougaar.mts.base.SendQueue;
import org.cougaar.mts.base.SendQueueDelegateImplBase;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.UnregisteredNameException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.SendLink;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.SendLinkDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;

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

    // To gather sensible send-side statistics, this aspect's
    // delegates need to run very late on the SendQueue (so as to
    // count any internal messages added to the queue by other aspect
    // delegates) but very early on the DestinationLink (because the
    // delegate on that side is processing the return).  The aspect
    // mechanism doesn't provide for station-specific ordering.  But
    // it does provide an implicit early-vs-late switch, since
    // reverse delegates always run early.  Use that here.
    public Object getDelegate(Object object, Class type) {
	if (type == SendQueue.class) {
	    return new SendQueueDelegate((SendQueue) object);
	} else 	if (type == SendLink.class) {
	    return new SendLinkDelegate((SendLink) object);
	} else 	if (type == MessageDeliverer.class) {
	    return new MessageDelivererDelegate((MessageDeliverer) object);
	} else {
	    return null;
	}
    }

    public Object getReverseDelegate(Object object, Class type) {
	if (type == DestinationLink.class) {
	    return new DestinationLinkDelegate((DestinationLink) object);
	} else {
	    return null;
	}
    }

 
    private class SendLinkDelegate
	extends SendLinkDelegateImplBase
    {
	SendLinkDelegate(SendLink link) {
	    super(link);
	}

	public void release() {
	    MessageAddress addr = getAddress().getPrimary();
	    synchronized (localStates) {
		localStates.remove(addr);
	    }
	}


	public void registerClient(MessageTransportClient client)
	{
	    super.registerClient(client);
	    ensureLocalState(getAddress().getPrimary());
	}


	public void flushMessages(ArrayList messages) {
	    super.flushMessages(messages);
	    Iterator i = messages.iterator();
	    while (i.hasNext()) {
		Message message = (Message) i.next();
		MessageAddress remoteAddr = message.getTarget().getPrimary();
		AgentState remoteState = ensureRemoteState(remoteAddr);
		synchronized (remoteState) {
		    remoteState.queueLength--;
		}
	    }
	}
    }

    private class DestinationLinkDelegate
	extends DestinationLinkDelegateImplBase
    {
	private String spoke_key, heard_key,error_key;

	public DestinationLinkDelegate(DestinationLink link)
	{
	    super(link);
	    String remoteAgent = link.getDestination().getAddress();
	    spoke_key = "Agent" +KEY_SEPR+ remoteAgent +KEY_SEPR+ "SpokeTime";
	    heard_key = "Agent" +KEY_SEPR+ remoteAgent +KEY_SEPR+ "HeardTime";
	    error_key = "Agent" +KEY_SEPR+ remoteAgent +KEY_SEPR+ 
		"SpokeErrorTime";
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
	    MessageAddress remoteAddr = message.getTarget().getPrimary();
	    AgentState remoteState = ensureRemoteState(remoteAddr);
	    MessageAddress localAddr = message.getOriginator().getPrimary();
	    AgentState localState = getLocalState(localAddr);
	    
	    if (localState == null) {
		// Leftover message from an unregistered agent
		LoggingService lsvc = getLoggingService();
		if (lsvc.isErrorEnabled())
		    lsvc.error("Forwarding leftover message from unregistered agent " 
			       +localAddr);
		return super.forwardMessage(message);
	    }

	    try {
		long startTime = System.currentTimeMillis();
		synchronized (remoteState) {
		    remoteState.lastLinkProtocolTried=
			getProtocolClass().getName();
		}
		// Attempt to Deliver message
		MessageAttributes meta = super.forwardMessage(message);

		//successful Delivery
		long endTime = System.currentTimeMillis();
		
		if (delivered(meta)) {
		    metricsUpdateService.updateValue(heard_key, 
						     longMetric(endTime));
		    metricsUpdateService.updateValue(spoke_key, 
						 longMetric(endTime));
		}

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
		long now=System.currentTimeMillis();
		synchronized (remoteState) {
		    remoteState.status = UNREGISTERED;
		    remoteState.timestamp = now;
		    remoteState.unregisteredNameCount++;
		}
		metricsUpdateService.updateValue(error_key, 
						 longMetric(now));
		throw unreg;
	    } catch (NameLookupException namex) {
		long now=System.currentTimeMillis();
		synchronized (remoteState) {
		    remoteState.status =UNKNOWN;
		    remoteState.timestamp = now;
		    remoteState.nameLookupFailureCount++;
		}
		metricsUpdateService.updateValue(error_key, 
						 longMetric(now));
		throw namex;
	    } catch (CommFailureException commex) {
		long now=System.currentTimeMillis();
		synchronized (remoteState) {
		    remoteState.status =UNREACHABLE;
		    remoteState.timestamp = now;
		    remoteState.commFailureCount++;
		}
		metricsUpdateService.updateValue(error_key, 
						 longMetric(now));
		throw commex;
	    } catch (MisdeliveredMessageException misd) {
		long now=System.currentTimeMillis();
		synchronized (remoteState) {
		    remoteState.status =UNREGISTERED;
		    remoteState.timestamp = now;
		    remoteState.misdeliveredMessageCount++;
		}	
		metricsUpdateService.updateValue(error_key, 
						 longMetric(now));
		throw misd;
	    }
	}
	
    }

    private class  MessageDelivererDelegate 
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
		ensureRemoteState(message.getOriginator().getPrimary());
	    synchronized (remoteState) {
		remoteState.receivedCount++;
		remoteState.receivedBytes+=msgBytes;
	    }
		    
	    AgentState localState = 
		getLocalState(message.getTarget().getPrimary());
	    if (localState != null) {
		synchronized (localState) {
		    localState.receivedCount++;
		    localState.receivedBytes+=msgBytes;
		}
	    }else {
		LoggingService lsvc = getLoggingService();
		if (lsvc.isInfoEnabled())
		    lsvc.info("Received message for non-local agent "
			       +message.getTarget());
	    }


	    return super.deliverMessage(message, dest);
	}

    }


    private class SendQueueDelegate 
	extends SendQueueDelegateImplBase
    {
	public SendQueueDelegate (SendQueue queue) {

	    super(queue);
	}
	
	public void sendMessage(AttributedMessage message) {
	    MessageAddress remoteAddr = message.getTarget().getPrimary();
	    AgentState remoteState = ensureRemoteState(remoteAddr);
	    MessageAddress localAddr = message.getOriginator().getPrimary();
	    AgentState localState = getLocalState(localAddr);

	    synchronized (remoteState) {
		remoteState.sendCount++;
		remoteState.queueLength++;
	    }	

	    if (localState != null) {
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
	    } else {
		LoggingService lsvc = getLoggingService();
		if (lsvc.isErrorEnabled())
		    lsvc.error("SendQueue sending leftover message from " 
			       +localAddr);
	    }

	    super.sendMessage(message);
	}
    }
}
