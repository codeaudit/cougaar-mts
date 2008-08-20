/*
 *
 * Copyright 2008 by BBN Technologies Corporation
 *
 */

package org.cougaar.mts.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.RPCLinkProtocol;
import org.cougaar.mts.base.UnregisteredNameException;

/**
 * Send messages via serialization on abstract reliable Streams, polling for
 * input. Reliability is handled by sending an ack for each message.
 * 
 * @param <I> The class of the ID object for each outgoing message
 */
abstract public class PollingStreamLinkProtocol<I>
        extends RPCLinkProtocol {
    // manager for receiving messages
    private MessageReceiver<I> receiver;

    // manager for sending messages and waiting for replies
    private ReplySync<I> sync;

    private URI servantUri;

    // Check periodically for incoming data
    private Schedulable poller;

    /**
     * Construct a URI that uniquely identifies this node. It will be used by
     * other nodes to contact it.
     * 
     * @param myServantId the node name
     */
    abstract protected URI makeURI(String myServantId)
            throws URISyntaxException;

    /**
     * Send a message or an ack to the given destination.
     */
    abstract protected I processOutgoingMessage(URI destination, MessageAttributes message)
            throws IOException, CommFailureException;

    /**
     * Make a Runnable that will run periodically to look for new messages and
     * process one per run if available.
     */
    abstract protected Runnable makePollerTask();

    /**
     * How long we should wait for the ack to any given message.
     */
    abstract protected int getReplyTimeoutMillis();

    /**
     * If the protocol needs to create any external connections of any kind, do
     * that here.
     * 
     * @return success or failure
     */
    abstract protected boolean establishConnections(String node);

    protected URI getServantUri() {
        return servantUri;
    }

    ReplySync<I> getReplySync() {
        if (sync == null) {
            sync = new ReplySync<I>(this, getReplyTimeoutMillis());
        }
        return sync;
    }

    private MessageSender<I> makeMessageSender() {
        return new MessageSender<I>(this);
    }

    private MessageReceiver<I> makeMessageReceiver() {
        return new MessageReceiver<I>(this, getDeliverer());
    }

    /**
     * Read and dispatch an incoming message on a stream.
     * 
     * This is protected so that it can be invoked by a subclass, not
     * (typically) to be overridden.
     */
    protected void processingIncomingMessage(InputStream stream) {
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
        processIncomingMessage(rawObject);
    }

    protected void processIncomingMessage(Object rawObject) {
        if (rawObject instanceof MessageAttributes) {
            receiver.handleIncomingMessage((MessageAttributes) rawObject);
        } else {
            loggingService.warn("Processing Incoming message is not MessageAttributes");
        }
    }

    protected DestinationLink createDestinationLink(MessageAddress address) {
        return new StreamLink(address);
    }

    protected void ensureNodeServant() {
        if (servantUri != null) {
            return;
        }

        String node = getNameSupport().getNodeMessageAddress().getAddress();
        if (!establishConnections(node)) {
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

        // start polling for input
        if (receiver == null) {
            receiver = makeMessageReceiver();
        }

        Runnable task = makePollerTask();
        if (task != null) {
            int lane = ThreadService.WILL_BLOCK_LANE;
            poller = threadService.getThread(this, task, "Message Poller", lane);
            poller.schedule(0, 1);
        }
    }

    protected void releaseNodeServant() {
        servantUri = null;
        setNodeURI(null);
        if (poller != null) {
            poller.cancelTimer();
            poller = null;
        }
    }

    protected void remakeNodeServant() {
        if (isServantAlive()) {
            releaseNodeServant();
        }
        ensureNodeServant();
    }

    protected Boolean usesEncryptedSocket() {
        return false;
    }

    private class StreamLink
            extends Link {
        private final MessageSender<I> sender;
        private URI uri;

        StreamLink(MessageAddress addr) {
            super(addr);
            this.sender = makeMessageSender();
        }

        public boolean isValid(AttributedMessage message) {
            return ensureNodeServantIsAlive() && super.isValid(message);
        }

        protected Object decodeRemoteRef(URI ref)
                throws Exception {
            return uri = ref;
        }

        protected MessageAttributes forwardByProtocol(Object destination, AttributedMessage message)
                throws NameLookupException, UnregisteredNameException, CommFailureException,
                MisdeliveredMessageException {
            try {
                return sender.handleOutgoingMessage(uri, message);
            } catch (CommFailureException e1) {
                decache();
                throw e1;
            } catch (MisdeliveredMessageException e2) {
                decache();
                throw e2;
            } catch (Exception e3) {
                decache();
                throw new CommFailureException(e3);
            }
        }

        @SuppressWarnings("unchecked") // unavoidable (?) generics warning
        public Class<? extends PollingStreamLinkProtocol> getProtocolClass() {
            return PollingStreamLinkProtocol.this.getClass();
        }
    }
}
