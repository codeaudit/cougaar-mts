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

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.AgentStatusService;
import org.cougaar.core.mts.MessageAddress;

/**
 * This Servlet displays the remote MTS statistics, as returned by the
 * {@link AgentStatusService}, for the Agent in which it's loaded. It's created
 * by the {@link StatisticsPlugin}.
 * 
 * @see AgentLocalStatusServlet
 */
public class AgentRemoteStatusServlet
        extends AgentStatusServlet {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public AgentRemoteStatusServlet(ServiceBroker sb) {
        super(sb);
    }

    @Override
   public String getPath() {
        return "/message/between-Node-and-Agent";
    }

    @Override
   public String getDescription(MessageAddress agent) {
        return "Message Transport statistics between all agents on node " + getNodeID()
                + " and agent " + agent;
    }

    @Override
   public String getTitle() {
        return "Message Transport statistics for agents communicating with node " + getNodeID();
    }

    @Override
   protected boolean isRemote() {
        return true;
    }

    @Override
   protected AgentStatusService.AgentState getState(MessageAddress agent) {
        AgentStatusService.AgentState state = null;
        if (agentStatusService != null) {
            state = agentStatusService.getRemoteAgentState(agent);
        }
        return state;
    }
}
