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
import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MessageReply;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.RPCLinkProtocol;
import org.cougaar.mts.base.UnregisteredNameException;

/**
 * Multicast via UDP.
 * <p>
 * This protocol only knows how to send to an {@link InetMessageAddress}, which
 * is assumed to be contain a multicast address.
 * <p>
 * For reading, it will poll every {@link InetMessageAddress} to which it's
 * currently joined.
 */
public class UdpMulticastLinkProtocol
        extends RPCLinkProtocol {
    private static final int SOCKET_TIMEOUT_SECONDS = 5;
    
    private static final int MAX_PAYLOAD_SIZE = 64 * 1024; // notional, 64K

    private URI servantUri;
    private final Timer timer = new Timer("Multicast Data Poller");
    private final Map<InetSocketAddress,MulticastSocket> multicastAddresses = 
        new HashMap<InetSocketAddress,MulticastSocket>();
    private final Map<InetSocketAddress,TimerTask> tasks =
        new HashMap<InetSocketAddress,TimerTask>();

    /**
     * Support only true multicast addresses
     */
    public boolean supportsAddressType(Class<? extends MessageAddress> addressType) {
        return InetMessageAddress.class.isAssignableFrom(addressType);
    }
    
    /**
     * Join the given multicast group. Plugins only have access to this method
     * indirectly, via {@link org.cougaar.core.agent.service.MessageSwitchService#joinGroup}
     */
    public void join(InetSocketAddress multicastAddress) throws IOException {
        synchronized (multicastAddresses) {
            TimerTask task = tasks.get(multicastAddress);
            if (task == null) {
                MulticastSocket skt = new MulticastSocket(multicastAddress.getPort());
                skt.setSoTimeout(SOCKET_TIMEOUT_SECONDS*1000);  // notional timeout
                skt.joinGroup(multicastAddress.getAddress());
                multicastAddresses.put(multicastAddress, skt);
                task = new InputSocketPoller(skt, multicastAddress);
                tasks.put(multicastAddress, task);
                timer.schedule(task, 0);
            }
        }
    }
    
    /**
     * Leave the given multicast group. Plugins only have access to this method
     * indirectly, via {@link org.cougaar.core.agent.service.MessageSwitchService#leaveGroup}
     */
    public void leave(InetSocketAddress multicastAddress) throws IOException {
        MulticastSocket socket;
        synchronized (multicastAddresses) {
            socket = multicastAddresses.get(multicastAddress);
            multicastAddresses.remove(multicastAddress);
            TimerTask task = tasks.get(multicastAddress);
            if (task != null) {
                task.cancel();
                tasks.remove(multicastAddress);
            }
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
            for (TimerTask task : tasks.values()) {
                task.cancel();
            }
            tasks.clear();
            timer.cancel();
        }
        servantUri = null;
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
        // Deseriaize the message
        AttributedMessage message = null;
        ObjectInputStream ois = null;

        try {
            ois = new ObjectInputStream(stream);
            Object rawObject = ois.readObject();
            if (rawObject instanceof AttributedMessage) {
                message = (AttributedMessage) rawObject;
            } else {
                loggingService.warn("Expected " +AttributedMessage.class
                                    + " found " +rawObject.getClass());
                return;
            }
        } catch (IOException e) {
            loggingService.warn("Processing Incoming message, stream error :" + e.getMessage());
            return;
        } catch (ClassNotFoundException e) {
            loggingService.warn("Processing Incoming message, unknown object type :"
                                + e.getMessage());
            return;
        } 

        // Deliver to each joined Agent
        MessageDeliverer deliverer = getDeliverer();
        Iterable<MessageAddress> destinations = lookupAddresses(socketAddress);
        if (destinations == null || !destinations.iterator().hasNext()) {
            if (loggingService.isInfoEnabled()) {
                loggingService.info("No agents have joined group " + socketAddress);
            }
            return;
        }
        for (MessageAddress destination : destinations) {
            if (loggingService.isInfoEnabled()) {
                loggingService.info("Dispatching received multicast message to " + destination);
            }
           
            try {
                deliverer.deliverMessage(message, destination);
                // no further use for the return value
            } catch (MisdeliveredMessageException e) {
                if (loggingService.isWarnEnabled()) {
                    loggingService.warn("Misdelivered from " + message.getOriginator() + " to "
                                        + message.getTarget() + ": " + e.getMessage()
                                        + "\n" + message);
                }
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
                loggingService.warn("Unable to join multicast group " + address
                                    + ": " + e.getMessage());
            }
        }
        
        public int cost(AttributedMessage msg) {
            return 1;
        }

        public boolean isValid(AttributedMessage message) {
            return outputConnection != null;
        }
        
        /**
         * This {@link RPCLinkProtocol#Link} method is meaningless here
         * but must return non-null.  So override and return junk.
         */
        protected URI getRemoteURI() {
            try {
                return new URI("junk://never-used");
            } catch (URISyntaxException e) {
                loggingService.warn("This is impossible");
                return null;
            }
        }
        
        protected Object decodeRemoteRef(URI ref)
                throws Exception {
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
     * Periodically check a multicast address for new data
     */
    private class InputSocketPoller
            extends TimerTask {
        private final DatagramPacket incoming;
        private final MulticastSocket socket;
        private final InetSocketAddress address;
        private boolean cancelled;
        
        public InputSocketPoller(MulticastSocket socket, InetSocketAddress address) {
            byte[] data = new byte[MAX_PAYLOAD_SIZE];
            incoming = new DatagramPacket(data, MAX_PAYLOAD_SIZE);
            this.socket = socket;
            this.address = address;
        }

        public boolean cancel() {
            cancelled = true;
            return super.cancel();
        }
        
        public void run() {
            while (!cancelled) {
                if (!isServantAlive()) {
                    // too early?
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // don't care
                    }
                    continue;
                }
                if (!cancelled) {
                    try {
                        SchedulableStatus.beginNetIO("Multicast Receive packet");
                        if (loggingService.isInfoEnabled()) {
                            loggingService.info("Waiting for datagram packet from " + address);
                        }
                        socket.receive(incoming);
                        int length = incoming.getLength();
                        byte[] payload = incoming.getData();
                        if (loggingService.isInfoEnabled()) {
                            loggingService.info("Received datagram packet of size " + length
                                    + " from " + address);
                        }
                        InputStream byteStream = new ByteArrayInputStream(payload, 0, length);
                        processingIncomingMessage(byteStream, address);
                    } catch (SocketTimeoutException e) {
                        // waited too long
                        if (loggingService.isInfoEnabled()) {
                            loggingService.info("No data from " +address
                                                + " for " +SOCKET_TIMEOUT_SECONDS+ " seconds");
                        }
                    } catch (IOException e) {
                        // XXX: Should we just quit at this point?
                        loggingService.warn(e.getMessage());
                    } finally {
                        SchedulableStatus.endBlocking();
                    }
                }
            }
        }

    }

}
