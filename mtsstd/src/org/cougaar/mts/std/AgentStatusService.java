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

import java.util.Set;
import org.cougaar.core.component.Service;

public interface AgentStatusService extends Service
{
    int UNKNOWN = 0;
    int UNREGISTERED = 1;
    int UNREACHABLE = 2;
    int ACTIVE = 3;

    class AgentState {
	public long timestamp;
	public int status;
	public int queueLength;
	public int receivedCount;
	public long receivedBytes;
	public int lastReceivedBytes;
	public int sendCount;
      	public int deliveredCount;
	public long deliveredBytes;
	public int lastDeliveredBytes;
	public long deliveredLatencySum;
	public int lastDeliveredLatency;
	public double averageDeliveredLatency;
	public int unregisteredNameCount;
	public int nameLookupFailureCount;
	public int commFailureCount;
	public int misdeliveredMessageCount;
	public String lastLinkProtocolTried;
	public String lastLinkProtocolSuccess;
    }

    AgentState getRemoteAgentState(MessageAddress address);
    AgentState getLocalAgentState(MessageAddress address);

    Set getLocalAgents();
    Set getRemoteAgents();

    /**
     * @deprecated Use {@link #getRemoteAgentState}
     */
    AgentState getAgentState(MessageAddress address);

}

