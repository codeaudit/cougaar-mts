/* 
 * <copyright> 
 *  Copyright 1999-2004 Cougaar Software, Inc.
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

package org.cougaar.mts.http;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.service.LoggingService;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.std.AttributedMessage;

/**
 * Pulled out of HTTPLinkProtocol to simplify maintenance.
 */
class HTTPLinkProtocolServlet
        extends HttpServlet {
    private final LoggingService logger;
    private final MessageDeliverer deliverer;

    // Logging is from the owner's logger (HTTPLinkProtocol)
    HTTPLinkProtocolServlet(MessageDeliverer deliverer, LoggingService logger) {
        this.deliverer = deliverer;
        this.logger = logger;
    }

    public void usage(HttpServletResponse resp)
            throws IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        out.println("<html><head><title>HTTP MTS Servlet</title></head>");
        out.println("<body><h1>HTTP MTS Servlet</h1>");
        out.println("This Servlet is only for use by the HTTPLinkProtocol.");
        out.println("</body></html>");
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        usage(resp);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Object result = null;

        if (logger.isDebugEnabled()) {
            debugHeaders(req);
        }

        try {
            Object obj = readMessage(req.getInputStream(), getContentLength(req));
            if (!(obj instanceof AttributedMessage)) {
                Exception e =
                        new IllegalArgumentException("send message content of class: "
                                + obj.getClass().getName());
                result = new CommFailureException(e);
                if (logger.isDebugEnabled()) {
                    logger.debug("object not AttributedMessage but is a "
                            + obj.getClass().getName(), e);
                }
            } else {
                AttributedMessage message = (AttributedMessage) obj;
                // deliver the message by obtaining the
                // MessageDeliverer from the LinkProtocol
                result = deliverer.deliverMessage(message, message.getTarget());
                if (logger.isDebugEnabled()) {
                    logger.debug("DELIVERED " + message.getRawMessage().getClass().getName() + "("
                            + message.getOriginator() + "->" + message.getTarget()
                            + ") with result=" + result);
                }
            }
        } catch (MisdeliveredMessageException e) {
            result = e;
        } catch (Exception e) {
            result = new CommFailureException(e);
        } finally {
            // return result
            resp.setContentType("application/x-www-form-urlencoded");
            ObjectOutputStream oos = new ObjectOutputStream(resp.getOutputStream());
            oos.writeObject(result);
            oos.flush();
        }
    }

    // this method reads a serialized java object from the HTTP input
    // stream, but can be overridden to read different message formats
    // (e.g., SOAP messages).
    protected Object readMessage(InputStream is, int mlen)
            throws Exception {
        // NOTE: Not sure if this is a hack or a solution to a
        // problem, but we need to wrap the ServletInputStream in a
        // BufferedInputStream. Otherwise, bytes on the stream
        // disappear(?) and reading from the input stream hangs during
        // readObject->readExternal->finishInput->verifySignature.
        ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(is, mlen));
        Object obj = ois.readObject();
        if (logger.isDebugEnabled()) {
            logger.debug("read object from input stream");
        }
        ois.close();
        return obj;
    }

    private int getContentLength(HttpServletRequest req) {
        int contentLength = 512;
        try {
            String header = req.getHeader("Content-length");
            if (header != null) {
                contentLength = Integer.parseInt(header);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Content-length not available");
                }
            }
        } catch (NumberFormatException nfe) {
            logger.warn("Cannot parse Content-length", nfe);
        }
        return contentLength;
    }

    private void debugHeaders(HttpServletRequest req) {
        logger.debug("########## HTTP HEADERS ##########");
        Enumeration e = req.getHeaderNames();
        while (e.hasMoreElements()) {
            String hdr = (String) e.nextElement();
            logger.debug(hdr + ": " + req.getHeader(hdr));
        }
        logger.debug("##################################");
    }
}