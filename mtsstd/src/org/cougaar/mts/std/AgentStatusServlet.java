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

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Set;


import javax.servlet.*;
import javax.servlet.http.*;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.servlet.ServletService;
import org.cougaar.core.service.MessageStatisticsService;

abstract public class AgentStatusServlet extends BaseServlet
{

    protected AgentStatusService agentStatusService;

    public AgentStatusServlet(ServiceBroker sb) {
	super(sb);
	agentStatusService = (AgentStatusService)
	    sb.getService(this, AgentStatusService.class, null);
    }

    abstract AgentStatusService.AgentState getState(MessageAddress agent);	
    abstract String getAdjective();	

    protected String myTitle() {
	return nodeID + " Message Transport <em>"+getAdjective()+
	   "</em> Agent Status";
    }


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

    protected void outputPage(PrintWriter out,
			      HttpServletRequest request )
    {
	String agentString= request.getParameter("agent");
	if (agentString==null) agentString=nodeID;
	MessageAddress agent = MessageAddress.getMessageAddress(agentString);

	AgentStatusService.AgentState state = getState(agent);

	if (state == null) {
	    out.print("<p><b>");
	    out.print("ERROR: Agent <em>"+ getAdjective() +
		      "</em> Status Service is not Available for Agent ");
	    out.print(agentString + "</b><p>");
	    out.println("<p>To Change Agent use cgi parameter: ?agent=agentname<p>");
	    return;
	}
	out.print("<h2> Agent <em> " +getAdjective()+
		  "</em> Status for Agent "+agentString+"</h2>");
	out.print("<table border=1>\n");
	row(out,"Status", state.status);
	row(out,"Queue Length", state.queueLength);
	row(out,"Messages Received", state.receivedCount);
	row(out,"Bytes Received", state.receivedBytes);
	row(out,"Last Received Bytes", state.lastReceivedBytes);
	row(out,"Messages Sent", state.sendCount);
	row(out,"Messages Delivered", state.deliveredCount);
	row(out,"Bytes Delivered", state.deliveredBytes);
	row(out,"Last Delivered Bytes", state.lastDeliveredBytes);
	row(out,"Last Delivered Latency", state.lastDeliveredLatency);
	row(out,"Average Delivered Latency", state.averageDeliveredLatency);
	row(out,"Unregistered Name Error Count", state.unregisteredNameCount);
	row(out,"Name Lookup Failure Count", state.nameLookupFailureCount);
	row(out,"Communication Failure Count", state.commFailureCount);
	row(out,"Misdelivered Message Count", state.misdeliveredMessageCount);
	row(out,"Last Link Protocol Tried", state.lastLinkProtocolTried);
	row(out,"Last Link Protocol Success", state.lastLinkProtocolSuccess);
	out.print("</b></tr>");
	out.println("</table><p>");
	out.println("<p>To Change Agent use cgi parameter: ?agent=agentname<p>");
    }
}
