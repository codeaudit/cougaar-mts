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

public class StatisticsServlet 
    extends ServletFrameset
{

    private final DecimalFormat f4_2 = new DecimalFormat("#0.00");


    private MessageStatisticsService statisticsService;

    public StatisticsServlet(ServiceBroker sb) {
	super(sb);
	statisticsService = (MessageStatisticsService)
	    sb.getService(this, MessageStatisticsService.class, null);
    }

    public String getPath() {
	return "/message/statistics";
    }

    public String getTitle() {
	return getNodeID() + " Message Statistics";
    }

    public void printPage(HttpServletRequest request,
			  PrintWriter out)
    {
	String reset_string = request.getParameter("reset");
	boolean reset = reset_string != null && 
	    reset_string.equalsIgnoreCase("true");

	MessageStatistics.Statistics stats = null;
	if (statisticsService!=null) {
	    stats = statisticsService.getMessageStatistics(reset);
	}
	if (stats == null) {
	    out.print("<p><b>");
	    out.print("ERROR: Message Statistics Service is not Available\n");
	    out.print("</b><p> org.cougaar.core.mts.StatisticsAspect ");
	    out.print("should be loaded into Node \n");
	    return;
	}
	out.print("<h2>Messages from all agents on node ");
	out.print(getNodeID());
	out.println("</h2>");
	out.print("<table border=1>\n");
	out.print("<tr><b>");
	out.print("<td><b>AvgQueueLength</b></td>");
	out.print("<td><b>");
	out.print(f4_2.format(stats.averageMessageQueueLength));
	out.print("</b></td>");
	out.print("</b></tr>");
	out.print("<tr><b>");
	out.print("<td><b>TotalBytes</b></td>");
	out.print("<td><b>");
	out.print(stats.totalMessageBytes);
	out.print("</b></td>");
	out.print("</b></tr>");
	out.print("<tr><b>");
	out.print("<td><b>TotalCount</b></td>");
	out.print("<td><b>");
	out.print(stats.totalMessageCount);
	out.print("</b></td>");
	out.print("</b></tr>");
	out.println("</table>");
	out.print("Note: Intra-Node messages have a length of zero bytes");

	out.print("<p><h2>Message Length Histogram</h2>");
	out.print("<table border=1>\n");
	out.print("<tr><b>");
	out.print("<td><b>Size</b></td>");
	out.print("<td><b>Count</b></td>");
	out.print("</b></tr>");

	for (int i=0; i<stats.histogram.length; i++) {
	    out.print("<tr><b>");

	    out.print("<td><b>");
	    out.print(MessageStatistics.BIN_SIZES[i]);
	    out.print("</b></td>");

	    out.print("<td><b>");
	    out.print(stats.histogram[i]);
	    out.print("</b></td>");

	    out.print("</b></tr>");
	}
	out.print("</table>");
	out.print("Note: Histagram table does not include intra-Node traffic.");
    }
}
