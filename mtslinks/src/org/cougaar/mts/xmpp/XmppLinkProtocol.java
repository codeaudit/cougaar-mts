/*
 *
 * Copyright 2008 by BBN Technologies Corporation
 *
 */

package org.cougaar.mts.xmpp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.thread.SchedulableStatus;
import org.cougaar.mts.std.AttributedMessage;
import org.cougaar.mts.stream.PollingStreamLinkProtocol;
import org.cougaar.util.annotations.Cougaar;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

/**
 * Simplified preliminary implementation of a Cougaar link protocol that uses
 * jabber api (xmpp) to send MTS messages.
 */
public class XmppLinkProtocol
        extends PollingStreamLinkProtocol<Chat> {
    /**
     * Max time in seconds to wait for an ack
     */
    private static final int REPLY_EXPIRATION_SECS = 7;
    private XMPPConnection serverConnection;

    @Cougaar.Arg(name = "server")
    private String serverHost;

    @Cougaar.Arg(name = "jabberId")
    private String jabberId;

    /**
     * Jabber password for the given user. Can be omitted if
     * {@link #passwordsUri} has that information.
     */
    @Cougaar.Arg(name = "password", defaultValue = Cougaar.NULL_VALUE)
    private String password;

    /**
     * Optional reference to a file or web page that contains jabber
     * username/password pairs in Java property file format. This is used if
     * {@link #password} is not provided.
     * 
     * <p>
     * Sample format:
     * 
     * <pre>
     * someNode@gmail.com=somepassword
     *          someOtherNode@gmail.com=someotherpassword
     * </pre>
     */
    @Cougaar.Arg(name = "passwordsUri", defaultValue = Cougaar.NULL_VALUE)
    private URI passwordsUri;

    private final Properties passwords = new Properties();

    public void load() {
        super.load();
        loadPasswordFile();
    }

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
     * We must have a connected, authenticated connection and a non-null servant
     */
    protected boolean isServantAlive() {
        return serverConnection != null && serverConnection.isAuthenticated()
                && super.isServantAlive();
    }

    protected int getReplyTimeoutMillis() {
        return REPLY_EXPIRATION_SECS * 1000;
    }

    protected boolean establishConnections(String node) {
        String[] userinfo = jabberId.split("@");
        String user = userinfo[0];
        String service = userinfo[1];
        ConnectionConfiguration config = new ConnectionConfiguration(serverHost, 5222, service);
        serverConnection = new XMPPConnection(config);
        if (password == null) {
            password = passwords.getProperty(jabberId);
            if (password == null) {
                loggingService.error("No password is available for " + jabberId);
                return false;
            }
        }
        try {
            serverConnection.connect();
            serverConnection.login(user, password);
            serverConnection.addPacketListener(new Listener(), null);
            if (loggingService.isInfoEnabled()) {
                if (serverConnection.isSecureConnection()) {
                    loggingService.info("Jabber connection is secure");
                } else {
                    loggingService.info("Jabber connection is not secure");
                }
            }
            return true;
        } catch (XMPPException e) {
            loggingService.warn("Couldn't connect to jabber server " + serverHost + " as "
                    + jabberId + ": " + e.getMessage());
            releaseNodeServant();
            return false;
        }
    }

    protected URI makeURI(String myServantId)
            throws URISyntaxException {
        String input = "xmpp://" + jabberId;
        return new URI(input);
    }

    protected Runnable makePollerTask() {
        return null;
    }

    /**
     * Send a base64'ized message to a buddy. The message can either be an MTS
     * AttributedMessage or an ack.
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

        String userJID = destination.getAuthority();

        // A given connection should not be accessed by more than one thread
        synchronized (destination) {
            try {
                SchedulableStatus.beginNetIO("XMPP Send Bundle");
                Message msg = new Message(userJID, Message.Type.chat);
                msg.setBody(base64msg);
                serverConnection.sendPacket(msg);
                if (loggingService.isInfoEnabled()) {
                    loggingService.info("Sent message of " + base64msg.length() + " chars from "
                            + getServantUri() + " to " + userJID);
                }
                return null;
            } finally {
                SchedulableStatus.endBlocking();
            }
        }
    }

    private void loadPasswordFile() {
        if (passwordsUri == null) {
            return;
        }
        InputStream stream = null;
        try {
            URL url = passwordsUri.toURL();
            stream = url.openStream();
            passwords.load(stream);
        } catch (FileNotFoundException e) {
            loggingService.warn("Couldn't read passwords from " + passwordsUri + ": "
                    + e.getMessage());
        } catch (IOException e) {
            loggingService.warn("Couldn't read passwords from " + passwordsUri + ": "
                    + e.getMessage());
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // don't care
                }
            }
        }
    }

    private class Listener
            implements PacketListener {
        public void processPacket(Packet pkt) {
            if (pkt instanceof Message) {
                Message msg = (Message) pkt;
                String text = msg.getBody();
                XMPPError err = msg.getError();
                if (err != null) {
                    loggingService.warn("xmpp error: " + err.getCondition() + " "
                            + err.getMessage());
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
