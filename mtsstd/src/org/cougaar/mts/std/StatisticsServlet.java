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
import java.text.DecimalFormat;

import javax.servlet.http.HttpServletRequest;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageStatistics;
import org.cougaar.core.service.MessageStatisticsService;
import org.cougaar.core.servlet.ServletFrameset;

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
	if (statisticsService == null) {
	    out.print("<p><b>");
	    out.print("ERROR: Message Statistics Service is not Available\n");
	    out.print("</b><p> org.cougaar.mts.std.StatisticsAspect ");
	    out.print("should be loaded into Node \n");
	    return;
	} else {
	    stats = statisticsService.getMessageStatistics(reset);
	    if (stats == null) {
		out.print("<p><b>");
		out.print("ERROR: Message Statistics Service returned null statistics\n");
		return;
	    }
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
