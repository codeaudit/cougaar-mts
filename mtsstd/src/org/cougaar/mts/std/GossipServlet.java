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
import java.text.DecimalFormat;
import javax.servlet.http.HttpServletRequest;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.servlet.ServletFrameset;
import org.cougaar.core.service.MessageStatisticsService;

public class GossipServlet 
    extends ServletFrameset
{

    private GossipStatisticsService statisticsService;

    public GossipServlet(ServiceBroker sb) {
	super(sb);
	statisticsService = (GossipStatisticsService)
	    sb.getService(this, GossipStatisticsService.class, null);
    }

    public String getPath() {
	return "/metrics/gossip";
    }

    public String getTitle() {
	return getNodeID() + " Gossip Statistics";
    }

    private void printRow(PrintWriter out,String label, int count) {
	out.print("<tr><b>");
	out.print("<td><b>");
	out.print(label);
	out.print("</b></td>");
	out.print("<td><b>");
	out.print(count);
	out.print("</b></td>");
	out.print("</b></tr>");
    }
    public void printPage(HttpServletRequest request,
			  PrintWriter out)
    {
	String reset_string = request.getParameter("reset");
	boolean reset = reset_string != null && 
	    reset_string.equalsIgnoreCase("true");

	GossipTrafficRecord stats = null;
	if (statisticsService!=null) {
	    stats = statisticsService.getStatistics();
	}
	if (stats == null) {
	    out.print("<p><b>");
	    out.print("ERROR: Gossip Statistics Service is not Available\n");
	    out.print("</b><p> org.cougaar.core.mts.GossipStatisticsServiceAspect ");
	    out.print("should be loaded into Node \n");
	    return;
	}
	out.print("<h2>Gossip for node ");
	out.print(getNodeID());
	out.println("</h2>");
	out.print("<table border=1>\n");

	printRow(out,"Requests Sent",stats.getRequestsSent());
	printRow(out,"Requests Received",stats.getRequestsReceived());
	printRow(out,"Values Sent",stats.getValuesSent());
	printRow(out,"Values Received",stats.getValuesReceived());
	printRow(out,"Messages with Gossip Sent",
		 stats.getMessagesWithGossipSent());
	printRow(out,"Messages with Gossip Received",
		 stats.getMessagesWithGossipReceived());
	printRow(out,"Total Messages Sent",
		 stats.getMessagesSent());
	printRow(out,"Total Messages Received",
		 stats.getMessagesReceived());
	out.println("</table>");

    }
}
