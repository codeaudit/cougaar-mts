/*
 *
 * Copyright 2008 by BBN Technologies Corporation
 *
 */

package org.cougaar.mts.rmi;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.ServletService;

/**
 * Servlet that blocks RMI sockets from being created to Blacklist Hosts
 */
public class RmiAccessControlPlugin
        extends ComponentPlugin {
    ServletService servletService;
    RMISocketControlService rmiAccess;

    protected void setupSubscriptions() {

        this.rmiAccess = getServiceBroker().getService(this, RMISocketControlService.class, null);

        try {
            servletService.register("/rmiAccessControl", new MyServlet());
        } catch (Exception ex) {
            System.err.println("Unable to register rmiAccessControl servlet");
            ex.printStackTrace();
        }

    }

    protected void execute() {

    }

    protected void doit(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String ipAddr = req.getParameter("ipAddr");
        String yes_or_no = req.getParameter("blacklist");
        boolean blacklist = yes_or_no != null && yes_or_no.equalsIgnoreCase("true");

        PrintWriter out = res.getWriter();
        out.println("<html><head></head><body>");

        if (rmiAccess == null) {
            out.println("RmiSocketControlServce is null, can't change value");
        } else if (ipAddr != null) {
            try {
                InetAddress addr = InetAddress.getByName(ipAddr);
                if (blacklist) {
                    rmiAccess.rejectSocketsFrom(addr);
                } else {
                    rmiAccess.acceptSocketsFrom(addr);
                }
            } catch (Exception ex) {
                out.println(ex);
            }
        }

        out.println("<FORM METHOD=\"GET\" ACTION=\"rmiAccessControl\">");
        out.println("<table>");
        out.println("<tr><td>IP address </td><td><input type=\"text\" name=\"ipAddr\" size=15 value=\""
                + ipAddr + "\"> </td></tr>");
        out.println("<tr><td>Blacklist </td><td><input type=\"text\" name=\"blacklist\" size=10 value=\""
                + yes_or_no + "\"> </td></tr>");
        out.println("</table>");
        out.println("<INPUT TYPE=\"submit\" Value=\"Submit\">");
        out.println("</FORM>");
        out.println("</body>");
    }

    private class MyServlet
            extends HttpServlet {
        public void doGet(HttpServletRequest req, HttpServletResponse res)
                throws IOException {
            doit(req, res);
        }

        public void doPost(HttpServletRequest req, HttpServletResponse res)
                throws IOException {
            doit(req, res);
        }
    }

    /**
     * Returns the servletService.
     * 
     * @return ServletService
     */
    public ServletService getServletService() {
        return servletService;
    }

    /**
     * Sets the servletService.
     * 
     * @param servletService The servletService to set
     */
    public void setServletService(ServletService servletService) {
        this.servletService = servletService;
    }
}
