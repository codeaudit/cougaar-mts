/*
 * Copyright (c) 2007 BBN Technologies Corp.
 * 
 * Restricted Release - Code is proprietary to BBN Technologies Corp. and
 * is to be used only as part of the DARPA DTN Program. 
 * 
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any research purpose within the DARPA DTN Program
 * only is hereby granted, provided that  
 * (1) the above copyright notice and this permission appear in all
 * copies and in supporting documentation 
 * (2) the name of BBN Technologies Corp. not be used in advertising or
 * publicity pertaining to distribution of the software without specific,
 * prior written permission  
 * (3) all rights, title and interest in any modifications shall remain
 * with BBN technologies Corp. 
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted only within the DARPA DTN Program provided
 * that the following conditions are met: 
 * (1) Redistributions of source code must retain the above copyright
 * notice, this list of conditions, and the following disclaimer. 
 * (2) Redistribution in binary form must include this file containing
 * the above copyright notice, this list of conditions, and the following
 * disclaimer in the documentation and/or other materials provided with
 * the distribution. 
 * (3) The names of the authors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission 
 * 
 * THIS SOFTWARE IS PROVIDED "AS IS" AND WITHOUT ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. 
 * 
 * This material is based upon work supported by the U. S. Army CECOM and
 * the Department of Defense under Contract No. W15P7T-06-C-P638.  Any
 * opinions, findings and conclusions or recommendations expressed in
 * this material are those of the author(s) and do not necessarily
 * reflect the views of U. S. Army CECOM and the Department of Defense. 
 *
 * $Id: XmppLinkProtocol.java,v 1.1 2008-08-06 20:56:28 jzinky Exp $
 */

package org.cougaar.mts.xmpp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.thread.SchedulableStatus;
import org.cougaar.mts.std.AttributedMessage;
import org.cougaar.mts.stream.PollingStreamLinkProtocol;
import org.cougaar.util.annotations.Cougaar;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;


/**
 * Simplified preliminary implementation of a Cougaar link protocol
 * that uses DTN to send MTS messages.
 */
public class XmppLinkProtocol extends PollingStreamLinkProtocol<Chat> {
    /**
     * Max time in seconds before a bundle will be dropped by the BPA
     */
    private static final int REPLY_EXPIRATION_SECS = 7;

    private XMPPConnection serverConnection;
    
    @Cougaar.Arg(name="server")
    private String serverHost;
    
    @Cougaar.Arg(name="user")
    private String username;
    
    protected String getProtocolType() {
        return "-XMPP";
    }

    protected int computeCost(AttributedMessage message) {
        return 1;
    }

    protected void releaseNodeServant() {
        if (serverConnection != null && serverConnection.isConnected()) {
            serverConnection.disconnect();
            serverConnection = null;
        }
        super.releaseNodeServant();
    }

    /**
     * We must have a connected, registered input connection, a connected
     * output connection and a non-null servant
     */
    protected boolean isServantAlive() {
        return 
        serverConnection != null && serverConnection.isAuthenticated()  && super.isServantAlive();
    }

    protected int getReplyTimeoutMillis() {
        return REPLY_EXPIRATION_SECS*1000;
    }
    
    protected boolean establishConnections(String node) {
        serverConnection = new XMPPConnection(serverHost);
        try {
            serverConnection.connect();
            serverConnection.login(username, username);
            serverConnection.addPacketListener(new Listener(), null);
            return true;
        } catch (XMPPException e) {
            loggingService.warn("Couldn't connect to jabber server " + serverHost
                                + "as " +username+ ": " + e.getMessage());
            releaseNodeServant();
            return false;
        }
    }
    
    protected URI makeURI(String myServantId) throws URISyntaxException {
        String input = "xmpp://" + serverHost+ "/" + username;
        return new URI(input);
    }
    
    protected Runnable makePollerTask() {
        return null;
    }
    
    /**
     * Send a base64'ized message to a buddy.  The payload can either be
     * an MTS message or an ack.
     */
    protected Chat processOutgoingMessage(URI destination, MessageAttributes message) 
            throws IOException {
        if (!isServantAlive()) {
            if (loggingService.isDebugEnabled()) {
                loggingService.debug("Output connection has closed");
            }
            throw new IOException("xmpp connection is not available");
        }
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(message);
        oos.flush();
        bos.close();
        byte[] payload = bos.toByteArray();
        
        String base64msg = Base64.encode(payload);
        
        String path = destination.getPath();
        // ignore the initial slash in the path
        String userJID = path.substring(1)+"@"+destination.getHost();
        
        // A given connection should not be accessed by more than one thread
        synchronized (destination) {
            try {
                SchedulableStatus.beginNetIO("XMPP Send Bundle");
                Message msg = new Message(userJID, Message.Type.chat);
                msg.setBody(base64msg);
                serverConnection.sendPacket(msg);
                if (loggingService.isInfoEnabled()) {
                    loggingService.info("Sent message of " + base64msg.length() + " chars from "
                            + getServantUri() + " to "
                            + userJID);
                }
                return null;
            } finally {
                SchedulableStatus.endBlocking();
            }
        }
    }
    
    
    private class Listener implements PacketListener {
        public void processPacket(Packet pkt) {
            if (pkt instanceof Message) {
                Message msg = (Message) pkt;
                String text = msg.getBody();
                XMPPError err = msg.getError();
                if (err != null) {
                    loggingService.warn("xmpp error: " +err.getCondition() 
                                        + " " +err.getMessage());
                } else if (text != null) {
                    if (loggingService.isDebugEnabled()) {
                        loggingService.debug("Processing msg " + text);
                    }
                    byte[] payload = Base64.decode(text);
                    InputStream stream = new ByteArrayInputStream(payload);
                    processingIncomingMessage(stream);
                }
            }
        }
        
    }

}
