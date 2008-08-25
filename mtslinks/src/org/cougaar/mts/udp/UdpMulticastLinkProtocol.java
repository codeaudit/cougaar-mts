/*
 *
 * Copyright 2008 by BBN Technologies Corporation
 *
 */

package org.cougaar.mts.udp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.SocketMessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.thread.SchedulableStatus;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.MessageReply;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.RPCLinkProtocol;
import org.cougaar.mts.base.UnregisteredNameException;

/**
 * Simple best-effort UDP link protocol: send messages and hope for the best. No
 * acks.
 */
public class UdpMulticastLinkProtocol
        extends RPCLinkProtocol {
    private static final int MAX_PAYLOAD_SIZE = 64 * 1024; // notional, 64K

    private DatagramSocket inputConnection;
    private URI servantUri;
    private final Timer timer = new Timer("Multicast Data Poller");

    /**
     * Support only true multicast addresses
     */
    public boolean supportsAddressType(Class<? extends MessageAddress> addressType) {
        return SocketMessageAddress.class.isAssignableFrom(addressType);
    }

    protected int computeCost(AttributedMessage message) {
        return 1;
    }

    protected DestinationLink createDestinationLink(MessageAddress address) {
        if (!(address instanceof SocketMessageAddress)) {
            throw new RuntimeException(address + " is not a SocketMessageAddress");
        }
        return new MulticastLink((SocketMessageAddress) address);
    }

    protected void ensureNodeServant() {
        if (servantUri != null) {
            return;
        }

        String node = getNameSupport().getNodeMessageAddress().getAddress();
        if (!openInputSocket(node)) {
            releaseNodeServant();
            return;
        }

        try {
            servantUri = makeURI(node);
            setNodeURI(servantUri);
        } catch (URISyntaxException e) {
            loggingService.error("Failed to make URI for node " + node, e);
            releaseNodeServant();
            return;
        }

        TimerTask task = new InputSocketPoller();
        // FIXME: Workaround for name-server bootstrapping problem
        // Delay reading incoming packets to give the Name Server agent time to
        // initialize
        timer.schedule(task, 5000, 1);
    }

    protected void releaseNodeServant() {
        timer.cancel();
        if (inputConnection != null) {
            inputConnection.close();
            inputConnection = null;
        }
        servantUri = null;
        // XXX: Do we need to close the sockets in outputSockets?
    }

    protected void remakeNodeServant() {
        if (isServantAlive()) {
            releaseNodeServant();
        }
        ensureNodeServant();
    }

    /**
     * We must have an open UDP socket and a non-null servant
     */
    protected boolean isServantAlive() {
        return inputConnection != null && servantUri != null && super.isServantAlive();
    }

    protected String getProtocolType() {
        return "-UDP-MULTICAST";
    }

    protected Boolean usesEncryptedSocket() {
        return false;
    }

    private boolean openInputSocket(String node) {
        try {
            inputConnection = new DatagramSocket();
            return true;
        } catch (SocketException e) {
            loggingService.warn("Couldn't create UDP socket: " + e.getMessage());
            return false;
        }
    }

    private URI makeURI(String myServantId)
            throws URISyntaxException {
        String input = null;
        ServiceBroker sb = getServiceBroker();
        NodeIdentificationService nis = sb.getService(this, NodeIdentificationService.class, null);
        InetAddress localHost = nis.getInetAddress();
        sb.releaseService(this, NodeIdentificationService.class, nis);
        if (localHost == null) {
            throw new URISyntaxException("Local ip address is unavailable", null);
        }
        input = "mcast://" + myServantId + "/";
        return new URI(input);
    }

    /**
     * Send a datagram to the destination.
     */
    protected MessageAttributes processOutgoingMessage(DatagramSocket destination,
                                                       AttributedMessage message)
            throws IOException {
        if (!isServantAlive()) {
            if (loggingService.isDebugEnabled()) {
                loggingService.debug("Output connection has closed");
            }
            throw new IOException("UDP connection is not available");
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(message);
        oos.flush();
        bos.close();
        byte[] payload = bos.toByteArray();

        Object deadline = message.getAttribute(AttributeConstants.MESSAGE_SEND_DEADLINE_ATTRIBUTE);
        int ttl;
        if (deadline instanceof Long) {
            ttl = (int) (((Long) deadline).longValue() - System.currentTimeMillis());
            if (ttl < 0) {
                loggingService.warn("Message already expired");
                MessageAttributes metadata = new MessageReply(message);
                metadata.setAttribute(AttributeConstants.DELIVERY_ATTRIBUTE,
                                      AttributeConstants.DELIVERY_STATUS_DROPPED);
                return metadata;
            }
        }

        if (loggingService.isInfoEnabled()) {
            loggingService.info("Sending from " + message.getOriginator() + " to "
                    + message.getTarget() + "\n" + message);
        }

        // A given connection should not be accessed by more than one thread
        synchronized (destination) {
            try {
                SchedulableStatus.beginNetIO("Multicast Send packet");
                SocketAddress remoteSocketAddress = destination.getRemoteSocketAddress();
                DatagramPacket packet =
                        new DatagramPacket(payload, 0, payload.length, remoteSocketAddress);
                destination.send(packet);
                if (loggingService.isInfoEnabled()) {
                    loggingService.info("Sent packet of " + payload.length + " bytes from "
                            + servantUri + " to " + remoteSocketAddress);
                }
                MessageAttributes metadata = new MessageReply(message);
                metadata.setAttribute(AttributeConstants.DELIVERY_ATTRIBUTE,
                                      AttributeConstants.DELIVERY_STATUS_BEST_EFFORT);
                return metadata;
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                loggingService.error("Uexpected error in udp send", e);
                throw new IOException(e.getLocalizedMessage());
            } finally {
                SchedulableStatus.endBlocking();
            }
        }
    }

    /**
     * Read and dispatch an incoming message on a stream.
     * 
     * This is protected so that it can be invoked by a subclass, not
     * (typically) to be overridden.
     */
    private void processingIncomingMessage(InputStream stream) {
        Object rawObject = null;
        ObjectInputStream ois = null;

        try {
            ois = new ObjectInputStream(stream);
        } catch (IOException e) {
            loggingService.warn("Processing Incoming message, stream error :" + e.getMessage());
            return;
        }

        try {
            rawObject = ois.readObject();
        } catch (ClassNotFoundException e) {
            loggingService.warn("Processing Incoming message, unknown object type :"
                    + e.getMessage());
            return;
        } catch (IOException e) {
            loggingService.warn("Processing Incoming message, deserializing error :"
                    + e.getMessage());
            return;
        }
        if (rawObject instanceof MessageAttributes) {
            AttributedMessage message = (AttributedMessage) rawObject;
            if (loggingService.isInfoEnabled()) {
                loggingService.info("Delivering from " + message.getOriginator() + " to "
                        + message.getTarget() + "\n" + message);
            }
            try {
                getDeliverer().deliverMessage(message, message.getTarget());
                // no further use for the return value
            } catch (MisdeliveredMessageException e) {
                if (loggingService.isWarnEnabled()) {
                    loggingService.warn("Misdelivered from " + message.getOriginator() + " to "
                            + message.getTarget() + ": " + e.getMessage() + "\n" + message);
                }
            }
        } else {
            loggingService.warn("Processing Incoming message is not MessageAttributes");
        }
    }

    private class MulticastLink
            extends Link {
        private MulticastSocket outputConnection;
        private final InetSocketAddress address;
        
        private MulticastLink(SocketMessageAddress destination) {
            super(destination);
            address = destination.getSocketAddress();
        }

        public boolean isValid(AttributedMessage message) {
            return ensureNodeServantIsAlive() && super.isValid(message);
        }

        protected Object decodeRemoteRef(URI ref)
                throws Exception {
            if (loggingService.isInfoEnabled()) {
                loggingService.info("Remote URI for " + getDestination() + " is " + ref);
            }
            outputConnection = new MulticastSocket(address.getPort());
            outputConnection.joinGroup(address.getAddress());
            return ref;
        }

        protected MessageAttributes forwardByProtocol(Object remote, AttributedMessage message)
                throws NameLookupException, UnregisteredNameException, CommFailureException,
                MisdeliveredMessageException {
            try {
                return processOutgoingMessage(outputConnection, message);
            } catch (IOException e) {
                throw new CommFailureException(e);
            }
        }

        public Class<UdpMulticastLinkProtocol> getProtocolClass() {
            return UdpMulticastLinkProtocol.class;
        }

    }

    /**
     * Periodically check the BPA for messages to us.
     */
    private class InputSocketPoller
            extends TimerTask {
        private final DatagramPacket incoming;

        public InputSocketPoller() {
            byte[] data = new byte[MAX_PAYLOAD_SIZE];
            incoming = new DatagramPacket(data, MAX_PAYLOAD_SIZE);
        }

        public void run() {
            if (!isServantAlive()) {
                // too early
                return;
            }
            try {
                SchedulableStatus.beginNetIO("UDP Receive packet");
                inputConnection.receive(incoming);
                int length = incoming.getLength();
                byte[] payload = incoming.getData();
                if (loggingService.isInfoEnabled()) {
                    loggingService.info("Received datagram packet of size " + length + " from "
                            + inputConnection.getInetAddress());
                }
                InputStream byteStream = new ByteArrayInputStream(payload, 0, length);
                processingIncomingMessage(byteStream);
            } catch (IOException e) {
                loggingService.warn(e.getMessage());
                releaseNodeServant(); // ????
            } finally {
                SchedulableStatus.endBlocking();
            }
        }

    }

}
