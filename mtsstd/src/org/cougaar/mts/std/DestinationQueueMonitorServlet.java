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

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.servlet.ServletFrameset;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


class DestinationQueueMonitorServlet extends ServletFrameset
{

  // values for the FRAME url parameter
  private static final String INFO_FRAME="infoFrame";
  private static final String SELECT_FRAME="selectFrame";

  private static final String DESTINATION_PARAM="agent";
  private static final String FORMAT_PARAM="format";

  private final DestinationQueueMonitorService destinationQueueMonitorService;

  private static final Comparator MESSAGE_ADDRESS_COMPARATOR =
    new Comparator() {
      public int compare(Object a, Object b){
        String sa = ((MessageAddress) a).getAddress();
        String sb = ((MessageAddress) b).getAddress();
        return sa.compareTo(sb);
      }
    };

  public DestinationQueueMonitorServlet(ServiceBroker sb) {
    super(sb);

    destinationQueueMonitorService = (DestinationQueueMonitorService)
      sb.getService(this, DestinationQueueMonitorService.class, null);
  }

  // Implementations of ServletFrameset's abstract methods

  public String getPath() {
    return "/message/queues";
  }

  public String getTitle() {
    return "MTS Queues";
  }

  private void printMessage(
      int count,
      AttributedMessage am,
      PrintWriter out) {
    if (am == null) {
      out.print("<tr><td align=right>"+count+"</td><td>null</tr>\n");
      return;
    }
    out.print(
        "<tr><td align=right>"+count+
        "</td><td align=right>");
    Message m = am.getRawMessage();
    MessageAddress source = m.getOriginator();
    out.print(source.getAddress());
    out.print("</td><td>");
    MessageAttributes sourceAtts = source.getMessageAttributes();
    if (sourceAtts == null) {
      out.print("&nbsp;");
    } else {
      out.print(encodeHTML(sourceAtts.getAttributesAsString()));
    }
    MessageAddress target = m.getTarget();
    out.print("</td><td>");
    out.print(target.getAddress());
    out.print("</td><td>");
    MessageAttributes targetAtts = target.getMessageAttributes();
    if (targetAtts == null) {
      out.print("&nbsp;");
    } else {
      out.print(encodeHTML(targetAtts.getAttributesAsString()));
    }
    out.print("</td><td>");
    out.print(encodeHTML(am.getAttributesAsString()));
    out.print("</td><td>");
    out.print(m.getClass().getName());
    out.print("</td><td>");
    out.print(encodeHTML(m.toString()));
    out.print("</td></tr>");
  }

  private void printMessageXML(AttributedMessage am, PrintWriter out) {
    if (am == null) {
      return;
    }
    out.print(
        "  <message>\n"+
        "    <source>");
    Message m = am.getRawMessage();
    MessageAddress source = m.getOriginator();
    out.print(source.getAddress());
    out.print(
        "</source>\n"+
        "    <source_attributes>");
    MessageAttributes sourceAtts = source.getMessageAttributes();
    if (sourceAtts != null) {
      out.print(encodeHTML(sourceAtts.getAttributesAsString()));
    }
    out.print(
        "</source_attributes>\n"+
        "    <target>");
    MessageAddress target = m.getTarget();
    out.print(target.getAddress());
    out.print(
        "</target>\n"+
        "    <target_attributes>");
    MessageAttributes targetAtts = target.getMessageAttributes();
    if (targetAtts != null) {
      out.print(encodeHTML(targetAtts.getAttributesAsString()));
    }
    out.print(
        "</source_attributes>\n"+
        "    <attributes>");
    out.print(encodeHTML(am.getAttributesAsString()));
    out.print(
        "</attributes>\n"+
        "    <class>");
    out.print(m.getClass().getName());
    out.print(
        "</class>\n"+
        "    <to_string>");
    out.print(encodeHTML(m.toString()));
    out.print(
        "</to_string>\n"+
        "  </message>\n");
  }


  private void beginTable(PrintWriter out) {
    out.print(
        "<p><table border=1>"+
        "<tr>"+
        "<th rowspan=2></th>"+
        "<th colspan=2>Source</th>"+
        "<th colspan=2>Target</th>"+
        "<th rowspan=2>Attributes</th>"+
        "<th rowspan=2>Class</th>"+
        "<th rowspan=2>toString</th>"+
        "</tr>"+
        "<tr>"+
        "<td>Address</td>"+
        "<td>Attributes</td>"+
        "<td>Address</td>"+
        "<td>Attributes</td>"+
        "</tr>");
  }

  private void endTable(PrintWriter out) {
    out.print("</table>\n");
  }

  private long getHeadSendTime(AttributedMessage[] messages) {
    if (messages.length > 0) {
      AttributedMessage m = messages[0];
      if (m != null) {
        Object sendTime = m.getAttribute(
            AttributeConstants.MESSAGE_SEND_TIME_ATTRIBUTE);
        if (sendTime instanceof Long) {
          long t = ((Long) sendTime).longValue();
          return t;
        }
      }
    }
    return -1;
  }

  private MessageAddress[] getDestinations(String dest) {
    MessageAddress[] destinations;
    if (dest == null) {
      destinations = new MessageAddress[0];
    } else if (dest.equals("*")) {
      destinations =
        destinationQueueMonitorService.getDestinations();
      if (destinations == null) {
        destinations = new MessageAddress[0];
      }
      Arrays.sort(destinations, MESSAGE_ADDRESS_COMPARATOR);
    } else {
      final MessageAddress destination = 
        MessageAddress.getMessageAddress(dest);
      destinations = new MessageAddress[] { destination };
    }
    return destinations;
  }

  private AttributedMessage[] getMessages(MessageAddress addr) {
    AttributedMessage[] messages =
      destinationQueueMonitorService.snapshotQueue(
          addr);
    if (messages == null) {
      messages = new AttributedMessage[0];
    }
    return messages;
  }

  private void printQueues(String dest, PrintWriter out) {
    if (dest == null) {
      return;
    }

    out.print("<p><b>"+dest+"</b><p>");

    MessageAddress[] destinations = getDestinations(dest);

    int n = destinations.length;
    if (n <= 0) {
      out.print("Zero targets\n");
      return;
    }

    AttributedMessage[][] queues = new AttributedMessage[n][];
    for (int i = 0; i < n; i++) {
      AttributedMessage[] messages = getMessages(destinations[i]);
      queues[i] = messages;
    }

    int count = 0;
    long minTime = -1;
    int minCount = 0;
    MessageAddress minAddr = null;
    for (int i = 0; i < n; i++) {
      AttributedMessage[] messages = queues[i];
      long t = getHeadSendTime(messages);
      if (t > 0 && (minTime < 0 || t < minTime)) {
        minCount = count;
        minTime = t;
        minAddr = destinations[i];
      }
      count += messages.length;
    }

    if (minTime > 0) {
      out.print(
          "Message["+minCount+" / "+count+"] to "+
          minAddr.getAddress()+
          " has been pending for <b>"+
          (System.currentTimeMillis() - minTime)+
          "</b> millis ("+(new Date(minTime))+")<p>\n");
    } else {
      out.print("Messages["+count+"]:\n");
    }

    beginTable(out);
    count = 0;
    for (int i = 0; i < n; i++) {
      AttributedMessage[] messages = queues[i];
      for (int j = 0, jn = messages.length; j < jn; j++) {
        printMessage(count++, messages[j], out);
      }
    }
    endTable(out);
  }

  public void printPage(HttpServletRequest request, PrintWriter out) {
    String dest = request.getParameter(DESTINATION_PARAM);
    if (dest == null) {
      return;
    }
    printQueues(dest, out);
  }

  protected List getDataSelectionList(HttpServletRequest request) {
    MessageAddress[] destinations = getDestinations("*");
    int n = destinations.length;
    if (n == 0) {
      return Collections.singletonList("*");
    }
    List ret = new ArrayList(n+1);
    ret.add("*");
    for (int i = 0; i < n; i++) {
      String s = destinations[i].getAddress();
      ret.add(s);
    }
    return ret;
  }

  private void printXML(
      HttpServletRequest request, PrintWriter out) {
    String dest = request.getParameter(DESTINATION_PARAM);
    MessageAddress[] destinations = getDestinations(dest);
    int n = destinations.length;
    out.print("<queues length=\""+n+"\">\n");
    for (int i = 0; i < n; i++) {
      MessageAddress destination = destinations[i];
      String s = destination.getAddress();
      AttributedMessage[] messages = getMessages(destination);
      int jn = messages.length;
      out.print("<queue target=\""+s+"\" length=\""+jn+"\"");
      if (jn <= 0) {
        out.print("/>\n");
      } else {
        out.print(">\n");
        for (int j = 0; j < jn; j++) {
          AttributedMessage am = messages[j];
          printMessageXML(am, out);
        }
        out.print("</queue>\n");
      }
    }
    out.print("</queues>\n");
  }

  private void printSelectFrame(
      HttpServletRequest request, PrintWriter out) {
    out.print("<html><body>\n");
    if (destinationQueueMonitorService == null) {
      out.print("Null DestinationQueueMonitorService\n");
    } else {
      out.print("<ol>\n");
      List l = getDataSelectionList(request);
      for (int i = 0, n = l.size(); i < n; i++) {
        String s = (String) l.get(i);
        out.print(
            "<li><a href=\""+
            request.getRequestURL()+
            "?"+FRAME+"="+DATA_FRAME+
            "&"+DESTINATION_PARAM+"="+s+
            "\" target=\""+DATA_FRAME+"\">"+
            s+"</a></li>\n");
      }
      out.print("</ol>\n");
    }
    out.print("</body></html>\n");
  }

  private void printInfoFrame(
      HttpServletRequest request, PrintWriter out) {
    // Header
    out.print("<html><head><title>");
    out.print(getTitle());
    out.print("</title></head>");

    // Frameset
    int percentage=20; 
    out.print("<frameset cols=\"");
    out.print(percentage);
    out.print("%,");
    out.print(100 - percentage);
    out.print("%\">\n");

    // show select frame
    out.print("<frame src=\"");
    out.print(request.getRequestURI());
    out.print("?");
    out.print(FRAME);
    out.print("=");
    out.print(SELECT_FRAME);
    out.print("\" name=\"");
    out.print(SELECT_FRAME);
    out.print("\">\n");

    // show data frame
    out.print("<frame src=\"");
    out.print(request.getRequestURI());
    out.print("?");
    out.print(FRAME);
    out.print("=");
    out.print(DATA_FRAME);
    out.print("\" name=\"");
    out.print(DATA_FRAME);
    out.print("\">\n");

    // End frameset
    out.print("</frameset>\n");

    // Frameless browser hack
    out.print("<noframes>Please enable frame support</noframes>");

    // End
    out.print("</html>\n");
  }

  protected String getMiddleFrame() {
    return INFO_FRAME;
  }

  protected final void printFrame(
      String frame,
      HttpServletRequest request,
      PrintWriter out) {
    String format = request.getParameter(FORMAT_PARAM);
    if (format != null && format.equalsIgnoreCase("xml")) {
      printXML(request, out);
      return;
    }
    if (SELECT_FRAME.equals(frame)){
      printSelectFrame(request, out);
    } else if (INFO_FRAME.equals(frame)){
      printInfoFrame(request, out);
    } else {
      super.printFrame(frame, request, out);
    }
  }

  private static final String encodeUTF8(String s) {
    try {
      return URLEncoder.encode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("UTF-8 not supported?", e);
    }
  }
  private static final String decodeUTF8(String s) {
    try {
      return URLDecoder.decode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("UTF-8 not supported?", e);
    }
  }

  // move me to "org.cougaar.util.StringUtility"!
  private static final String encodeHTML(String s) {
    return encodeHTML(s, false);
  }
  private static final String encodeHTML(String s, boolean noBreakSpaces) {
    StringBuffer buf = null;  // In case we need to edit the string
    int ix = 0;               // Beginning of uncopied part of s
    for (int i = 0, n = s.length(); i < n; i++) {
      String replacement = null;
      switch (s.charAt(i)) {
        case '"': replacement = "&quot;"; break;
        case '<': replacement = "&lt;"; break;
        case '>': replacement = "&gt;"; break;
        case '&': replacement = "&amp;"; break;
        case ' ': if (noBreakSpaces) replacement = "&nbsp;"; break;
      }
      if (replacement != null) {
        if (buf == null) buf = new StringBuffer();
        buf.append(s.substring(ix, i));
        buf.append(replacement);
        ix = i + 1;
      }
    }
    if (buf == null) {
      return s;
    } else {
      buf.append(s.substring(ix));
      return buf.toString();
    }
  }
}
