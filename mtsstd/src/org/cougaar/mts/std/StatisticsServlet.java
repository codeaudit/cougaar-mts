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
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.servlet.ServletService;
import org.cougaar.core.service.MessageStatisticsService;

public class StatisticsServlet extends HttpServlet 
{

    protected MessageStatisticsService statisticsService;
    protected String nodeID;
    protected DecimalFormat f4_2;

    public StatisticsServlet(ServiceBroker sb) {
	ServletService servletService = (ServletService)
	    sb.getService(this, ServletService.class, null);
	if (servletService == null) {
	    throw new RuntimeException("Unable to obtain ServletService");
	}


	statisticsService = (MessageStatisticsService)
	    sb.getService(this, MessageStatisticsService.class, null);

	NodeIdentificationService node_id_svc = (NodeIdentificationService)
	    sb.getService(this, NodeIdentificationService.class, null);
	nodeID = node_id_svc.getNodeIdentifier().toString();
	

	// register our servlet
	try {
	    servletService.register(myPath(), this);
	} catch (Exception e) {
	    throw new RuntimeException("Unable to register servlet at path <"
				       +myPath()+ ">: " +e.getMessage());
	}

	f4_2 = new DecimalFormat("#0.00");
    }

    protected String myPath() {
	return "/message/statistics";
    }

    protected String myTitle() {
	return nodeID + " Message Statistics";
    }

    protected void outputPage(PrintWriter out,
			      MessageStatistics.Statistics stats)
    {
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

	out.println("</table><p><h2>Message Length Histogram</h2>");

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
    }


    public void doGet(HttpServletRequest request,
		      HttpServletResponse response) 
	throws java.io.IOException 
    {

	String refresh = request.getParameter("refresh");
	int refreshSeconds = 
	    ((refresh != null) ?
	     Integer.parseInt(refresh) :
	     0);
	String reset_string = request.getParameter("reset");
	boolean reset = reset_string != null && 
	    reset_string.equalsIgnoreCase("true");

	MessageStatistics.Statistics stats = 
	    statisticsService.getMessageStatistics(reset);

	response.setContentType("text/html");
	PrintWriter out = response.getWriter();

	out.print("<html><HEAD>");
	if (refreshSeconds > 0) {
	    out.print("<META HTTP-EQUIV=\"refresh\" content=\"");
	    out.print(refreshSeconds);
	    out.print("\">");
	}
	out.print("<TITLE>");
	out.print(myTitle());
	out.print("</TITLE></HEAD><body><H1>");
	out.print(myTitle());
	out.print("</H1>");

	out.print("Date: ");
	out.print(new java.util.Date());
	
	outputPage(out, stats);
	out.print("<p><p><br>RefreshSeconds: ");	
	out.print(refreshSeconds);

	out.print("</body></html>\n");

	out.close();
    }
}
