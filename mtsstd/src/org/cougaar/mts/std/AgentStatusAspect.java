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

import org.cougaar.core.service.*;

import org.cougaar.core.node.*;

import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;


import java.util.HashMap;

public class AgentStatusAspect 
    extends StandardAspect
    implements AgentStatusService
{

    private HashMap states;
    
    public AgentStatusAspect() {
	states = new HashMap();
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

 
    public Object getDelegate(Object object, Class type) {
	if (type == DestinationLink.class) {
	    return new AgentStatusDestinationLink((DestinationLink) object);
	} else 	if (type == SendQueue.class) {
	    return new SendQueueDelegate((SendQueue) object);
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
	public AgentStatusDestinationLink(DestinationLink link)
	{
	    super(link);
	}
	
	public void forwardMessage(Message message) 
	    throws UnregisteredNameException, 
	    NameLookupException, 
	    CommFailureException,
	    MisdeliveredMessageException

	{
	    MessageAddress addr = message.getTarget();
	    AgentState state = ensureState(addr);
	    
	    try {
		long startTime = System.currentTimeMillis();
		link.forwardMessage(message);
		//successful Delivery
		long endTime = System.currentTimeMillis();
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

    public class SendQueueDelegate 
	extends SendQueueDelegateImplBase
    {
	public SendQueueDelegate (SendQueue queue) {
	    super(queue);
	}
	
	public void sendMessage(Message message) {
	    MessageAddress addr = message.getTarget();
	    AgentState state = ensureState(addr);
	    synchronized (state) {
		state.sendCount++;
	    }	
	    queue.sendMessage(message);
	}
	
    }

}
