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

package org.cougaar.mts.std;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.AgentStatusService;
import org.cougaar.core.mts.MessageAddress;

public class AgentLocalStatusServlet extends AgentStatusServlet
{

    public AgentLocalStatusServlet(ServiceBroker sb) {
	super(sb);
    }

    public String getPath() {
	return "/message/between-Any-agent-and-Local-Agent";
    }

    public String getDescription(MessageAddress agent) {
	return "Message Transport statistics between local agent "+
	    agent + " and any other agent";
    }

    public String getTitle() {
	return "Message Transport statistics for agents on node "+ 
	    getNodeID();
    }

    protected boolean isRemote() {
	return(false);
    }

   protected AgentStatusService.AgentState getState(MessageAddress agent){
       	AgentStatusService.AgentState state = null;
	
	if (agentStatusService!=null) {
	    state = agentStatusService.getLocalAgentState(agent);
	}
	return state;
    }
}
