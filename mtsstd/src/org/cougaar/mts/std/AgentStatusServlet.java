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

package org.cougaar.core.mts;

import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.servlet.ServletFrameset;

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
