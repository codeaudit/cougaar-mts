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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.core.mts.InetMessageAddress;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
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
        extends RPCLinkProtocol { // NO NO NO No No No
    private static final int MAX_PAYLOAD_SIZE = 64 * 1024; // notional, 64K

    private URI servantUri;
    private final Timer timer = new Timer("Multicast Data Poller");
    private final Map<InetSocketAddress,MulticastSocket> multicastAddresses = 
        new HashMap<InetSocketAddress,MulticastSocket>();

    /**
     * Support only true multicast addresses
     */
    public boolean supportsAddressType(Class<? extends MessageAddress> addressType) {
        return InetMessageAddress.class.isAssignableFrom(addressType);
    }
    
    public void join(InetSocketAddress multicastAddress) throws IOException {
        synchronized (multicastAddresses) {
            MulticastSocket skt = new MulticastSocket(multicastAddress.getPort());
            skt.setSoTimeout(100);
            skt.joinGroup(multicastAddress.getAddress());
            multicastAddresses.put(multicastAddress,skt);
        }
    }
    
    public void leave(InetSocketAddress multicastAddress) throws IOException {
        MulticastSocket socket;
        synchronized (multicastAddresses) {
            socket = multicastAddresses.get(multicastAddress);
            multicastAddresses.remove(multicastAddress);
        }
        if (socket != null) {
            socket.leaveGroup(multicastAddress.getAddress());
        }
    }

    protected int computeCost(AttributedMessage message) {
        return 1;
    }

    protected DestinationLink createDestinationLink(MessageAddress address) {
        if (!(address instanceof InetMessageAddress)) {
            throw new RuntimeException(address + " is not a SocketMessageAddress");
        }
        return new MulticastLink((InetMessageAddress) address);
    }

    protected void ensureNodeServant() {
        if (servantUri != null) {
            return;
        }

        String node = getNameSupport().getNodeMessageAddress().getAddress();

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
        synchronized (multicastAddresses) {
            for (Map.Entry<InetSocketAddress,MulticastSocket> entry : multicastAddresses.entrySet()) {
                try {
                    entry.getValue().leaveGroup(entry.getKey().getAddress());
                } catch (IOException e) {
                    loggingService.error("Error closing multicast socket", e);
                }
            }
            multicastAddresses.clear();
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
        return servantUri != null;
    }

    protected String getProtocolType() {
        return "-UDP-MULTICAST";
    }

    protected Boolean usesEncryptedSocket() {
        return false;
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
    private MessageAttributes processOutgoingMessage(MulticastSocket destination,
                                                     AttributedMessage message, 
                                                     InetSocketAddress address)
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
                DatagramPacket packet = new DatagramPacket(payload, 0, payload.length, address);
                destination.send(packet);
                if (loggingService.isInfoEnabled()) {
                    loggingService.info("Sent packet of " + payload.length + " bytes from "
                            + servantUri + " to " + destination);
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
    
    private Iterable<MessageAddress> lookupAddresses(InetSocketAddress multicastAddress) {
        return getRegistry().getGroupListeners(multicastAddress);
    }

    /**
     * Read and dispatch an incoming message on a stream.
     */
    private void processingIncomingMessage(InputStream stream, InetSocketAddress socketAddress) {
        
        Object rawObject = null;
        ObjectInputStream ois = null;

        try {
            ois = new ObjectInputStream(stream);
        } catch (IOException e) {
            loggingService.warn("Processing Incoming message, stream error :" + e.getMessage());
            return;
        }

        Iterable<MessageAddress> destinations = lookupAddresses(socketAddress);
        for (MessageAddress destination : destinations) {
            try {
                rawObject = ois.readObject();
            } catch (ClassNotFoundException e) {
                loggingService.warn("Processing Incoming message, unknown object type :"
                        + e.getMessage());
                continue;
            } catch (IOException e) {
                loggingService.warn("Processing Incoming message, deserializing error :"
                        + e.getMessage());
                continue;
            }
            if (rawObject instanceof MessageAttributes) {
                AttributedMessage message = (AttributedMessage) rawObject;
                if (loggingService.isInfoEnabled()) {
                    loggingService.info("Delivering from " + message.getOriginator() + " to "
                            + message.getTarget() + "\n" + message);
                }
                try {
                    getDeliverer().deliverMessage(message, destination);
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
    }

    private class MulticastLink
            extends Link {
        private MulticastSocket outputConnection;
        private final InetSocketAddress address;
        
        private MulticastLink(InetMessageAddress destination) {
            super(destination);
            address = destination.getSocketAddress();
            try {
                outputConnection = new MulticastSocket(address.getPort());
                outputConnection.joinGroup(address.getAddress());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        public int cost(AttributedMessage msg) {
            return 1;
        }

        public boolean isValid(AttributedMessage message) {
            return outputConnection != null;
        }
        
        protected URI getRemoteURI() {
            try {
                return new URI("crap://from-crapola");
            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }
        }
        
        protected Object decodeRemoteRef(URI ref)
                throws Exception {
            if (loggingService.isInfoEnabled()) {
                loggingService.info("Remote URI for " + getDestination() + " is " + ref);
            }
            return ref;
        }

        protected MessageAttributes forwardByProtocol(Object remote, AttributedMessage message)
                throws NameLookupException, UnregisteredNameException, CommFailureException,
                MisdeliveredMessageException {
            try {
                return processOutgoingMessage(outputConnection, message, address);
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
            Map<InetSocketAddress,MulticastSocket> map = 
                new HashMap<InetSocketAddress,MulticastSocket>();
            synchronized (multicastAddresses) {
                map.putAll(multicastAddresses);
            }
            try {
                SchedulableStatus.beginNetIO("Multicast Receive packet");
                for (Map.Entry<InetSocketAddress,MulticastSocket> entry : map.entrySet()) {
                    MulticastSocket skt = entry.getValue();
                    try {
                        skt.receive(incoming); // need this to timeout
                    } catch (SocketTimeoutException e) {
                        // no data, continue on other sockets
                        continue;
                    }
                    int length = incoming.getLength();
                    byte[] payload = incoming.getData();
                    if (loggingService.isInfoEnabled()) {
                        loggingService.info("Received datagram packet of size " + length + " from "
                                            + skt.getInetAddress());
                    }
                    InputStream byteStream = new ByteArrayInputStream(payload, 0, length);
                    processingIncomingMessage(byteStream, entry.getKey());
                }
            } catch (IOException e) {
                loggingService.warn(e.getMessage());
                releaseNodeServant(); // ????
            } finally {
                SchedulableStatus.endBlocking();
            }
        }

    }

}
