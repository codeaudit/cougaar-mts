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

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.RPCLinkProtocol;
import org.cougaar.mts.base.UnregisteredNameException;
import org.cougaar.mts.std.AttributedMessage;
import org.cougaar.util.annotations.Cougaar;

/**
 * Send messages via serialization on abstract reliable Streams,
 * polling for input.  Reliability is handled by sending an 
 * ack for each message.
 * 
 * @param <I> The class of the ID object for each outgoing message
 */
abstract public class PollingStreamLinkProtocol<I> extends RPCLinkProtocol {
    // manager for receiving messages
    private MessageReceiver<I> receiver;
    
    // manager for sending messages and waiting for replies
    private ReplySync<I> sync;

    private URI servantUri;
    
    // Check periodically for incoming data
    private Schedulable poller;

    @Cougaar.ObtainService
    private ThreadService threadService;
    
    /**
     * Construct a URI that uniquely identifies this node. 
     * It will be used by other nodes to contact it.
     * 
     * @param myServantId the node name
     */
    abstract protected URI makeURI(String myServantId) 
        throws URISyntaxException;

    /**
     * Send a message or an ack to the given destination.
     */
    abstract protected I processOutgoingMessage(URI destination, MessageAttributes message)
        throws IOException;

    /**
     * Make a Runnable that will run periodically to look for new messages
     * and process one per run if available.
     */
    abstract protected Runnable makePollerTask();

    /**
     * How long we should wait for the ack to any given message.
     */
    abstract protected int getReplyTimeoutMillis();
    
    /**
     * If the protocol needs to create any external connections of any kind,
     * do that here.
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

    private MessageReceiver<I> makeMessageReceiver(MessageDeliverer deliverer) {
        return new MessageReceiver<I>(this, deliverer);
    }

    /**
     * Read and dispatch an incoming message on a stream.
     * 
     * This is protected so that it can be invoked by
     * a subclass, not (typically) to be overridden.
     */
    protected void processingIncomingMessage(InputStream stream) {
        Object rawObject = null;
        ObjectInputStream ois  = null;
        
        try {
            ois = new ObjectInputStream(stream);
        } catch (IOException e) {
            loggingService.warn("Processing Incoming message, stream error :" 
                                + e.getMessage());
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
            receiver.handleIncomingMessage((MessageAttributes) rawObject);
        } else {
            loggingService.warn("Processing Incoming message is not MessageAttributes");
        }
    }

    protected DestinationLink createDestinationLink(MessageAddress address) {
        return new StreamLink(address);
    }

    
    protected void findOrMakeNodeServant() {
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
            ServiceBroker sb = getServiceBroker();
            MessageDeliverer deliverer = sb.getService(this, MessageDeliverer.class, null);
            receiver = makeMessageReceiver(deliverer);
        }
        
        Runnable task = makePollerTask();
        poller = threadService.getThread(this, task, "Message Poller", ThreadService.WILL_BLOCK_LANE);
        poller.schedule(0, 1);
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
        findOrMakeNodeServant();
    }

    protected Boolean usesEncryptedSocket() {
        return false;
    }

    private class StreamLink extends Link {
        private final MessageSender<I> sender;
        private URI uri;

        StreamLink(MessageAddress addr) {
            super(addr);
            this.sender = makeMessageSender();
        }

        public boolean isValid() {
            // Remake our servant if necessary. If that fails, the link is
            // considered invalid, since the remote reference must be unreachable.
            if (!isServantAlive()) {
                remakeNodeServant();
                if (!isServantAlive()) {
                    return false;
                } else {
                    reregisterClients();
                }
            }
            return super.isValid();
        }

        protected Object decodeRemoteRef(URI ref) throws Exception {
            return uri = ref;
        }

        protected MessageAttributes forwardByProtocol(Object destination, AttributedMessage message)
                throws NameLookupException,
                UnregisteredNameException,
                CommFailureException,
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

        public Class<?> getProtocolClass() {
            return PollingStreamLinkProtocol.this.getClass();
        }
    }
}
