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
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.AgentStatusService;
import org.cougaar.core.mts.BaseServlet;
import org.cougaar.core.mts.MessageAddress;

/**
 * This servlet displays MTS statistics for the Agent in which it's
 * loaded.  Depending on the extension, the data displayed could be
 * either local or remote.
 */
abstract public class AgentStatusServlet
    extends BaseServlet // ServletFrameset
{

    protected AgentStatusService agentStatusService;

    public AgentStatusServlet(ServiceBroker sb) {
	super(sb);
	agentStatusService = (AgentStatusService)
	    sb.getService(this, AgentStatusService.class, null);
    }

    abstract AgentStatusService.AgentState getState(MessageAddress agent);
    abstract String getDescription(MessageAddress agent);
    abstract boolean isRemote();
    

    private void row(PrintWriter out, String name, String value){
	out.print("<tr><b>");
	out.print("<td><b>");
	out.print(name);
	out.print("</b></td><td><b>");
	out.print(value);
	out.print("</b></td>");
    }

    private void row(PrintWriter out, String name, int value){
	row(out,name,Integer.toString(value) );
    }

    private void row(PrintWriter out, String name, long value){
	row(out,name,Long.toString(value) );
    }

    private void row(PrintWriter out, String name, double value){
	row(out,name,Double.toString(value) );
    }



    public void printPage(HttpServletRequest request,
			  PrintWriter out)
    {
	MessageAddress agent = null;
	String agentString= request.getParameter("agent");
	if (agentString != null) {
	    agent = MessageAddress.getMessageAddress(agentString);
	} else {
	    agent = getNodeID();
	}

	AgentStatusService.AgentState state = getState(agent);

	if (state == null) {
	    out.print("<p><b>");
	    out.print("ERROR: Agent Status Service is not Available for Agent ");
	    out.print(agent + "</b><p>");
	    out.println("<p>To Change Agent use cgi parameter: ?agent=agentname<p>");
	    return;
	}
	out.print("<h2>");
	out.print(getDescription(agent));
	out.print("</h2>");
	out.print("<table border=1>\n");
	row(out,"Status", state.status);
	row(out,"Messages Received", state.receivedCount);
	row(out,"Bytes Received", state.receivedBytes);
	row(out,"Last Received Bytes", state.lastReceivedBytes);
	row(out,"Messages Sent", state.sendCount);
	row(out,"Messages Delivered", state.deliveredCount);
	row(out,"Bytes Delivered", state.deliveredBytes);
	row(out,"Last Delivered Bytes", state.lastDeliveredBytes);
	if (isRemote()) {
	    row(out,"Queue Length", state.queueLength);
	    row(out,"Last Delivered Latency", state.lastDeliveredLatency);
	    row(out,"Average Delivered Latency", state.averageDeliveredLatency);
	    row(out,"Unregistered Name Error Count", state.unregisteredNameCount);
	    row(out,"Name Lookup Failure Count", state.nameLookupFailureCount);
	    row(out,"Communication Failure Count", state.commFailureCount);
	    row(out,"Misdelivered Message Count", state.misdeliveredMessageCount);
	    row(out,"Last Link Protocol Tried", state.lastLinkProtocolTried);
	    row(out,"Last Link Protocol Success", state.lastLinkProtocolSuccess);
	}
	out.print("</b></tr>");
	out.println("</table><p>");
	out.println("<p>To Change Agent use cgi parameter: ?agent=agentname<p>");
    }
}
