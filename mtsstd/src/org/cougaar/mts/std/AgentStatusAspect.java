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

import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;

import java.util.HashMap;

public class AgentStatusAspect 
    extends StandardAspect
    implements AgentStatusService
{

    private HashMap states;
    
    public AgentStatusAspect() {
	states = new HashMap();
    }
    

    public synchronized AgentState getAgentState(MessageAddress address) {
	AgentState state = (AgentState) states.get(address);
	if (state == null) {
	    state = new AgentState();
	    state.status = UNREGISTERED;
	    state.timestamp = System.currentTimeMillis();
	    states.put(address, state);
	}
	
	return state;
    }

    private synchronized void updateState(MessageAddress address, int status) {
	AgentState state = (AgentState) states.get(address);
	if (state == null) {
	    state = new AgentState();
	    states.put(address, state);
	}
	state.timestamp = System.currentTimeMillis();
	state.status = status;
    }

    public Object getDelegate(Object object, Class type) {
	if (type == DestinationLink.class) {
	    return new AgentStatusDestinationLink((DestinationLink) object);
	} else {
	    return null;
	}
    }


    public class AgentStatusDestinationLink implements DestinationLink
    {
	private DestinationLink server;
	
	public AgentStatusDestinationLink(DestinationLink server)
	{
	    this.server = server;
	}
	
	public void forwardMessage(Message message) 
	    throws UnregisteredNameException, 
	    NameLookupException, 
	    CommFailureException,
	    MisdeliveredMessageException

	{
	    MessageAddress addr = message.getTarget();
	    try {
		server.forwardMessage(message);
		updateState(addr, ACTIVE);
	    } catch (UnregisteredNameException unreg) {
		updateState(addr, UNREGISTERED);
		throw unreg;
	    } catch (NameLookupException namex) {
		updateState(addr, UNKNOWN);
		throw namex;
	    } catch (CommFailureException commex) {
		updateState(addr, UNREACHABLE);
		throw commex;
	    } catch (MisdeliveredMessageException misd) {
		updateState(addr, UNREGISTERED);
		throw misd;
	    }
	}
	
	public int cost(Message message){
	    return server.cost(message);
	}
    }
   
}
