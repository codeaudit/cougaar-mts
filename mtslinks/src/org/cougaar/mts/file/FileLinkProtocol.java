/*
 *
 * Copyright 2008 by BBN Technologies Corporation
 *
 */

package org.cougaar.mts.file;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.service.ThreadService;
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
 * Send messages via file-sharing.
 */
public class FileLinkProtocol extends RPCLinkProtocol {

 // manager for receiving messages
    private MessageReceiver receiver;
    // manager for sending messages and waiting for replys
    private ReplySync sync;
    
    private URI servantUri;
    
    @Cougaar.Arg(name="rootDirectory", required=true)
    private String rootDirectory;
    
    @Cougaar.ObtainService
    private ThreadService threadService;
    
    public FileLinkProtocol() {
        
    }
    
    URI getServantUri() {
        return servantUri;
    }
    
    protected final ReplySync findOrMakeReplySync() {
        if (sync == null)
            sync = makeReplySync();
        return sync;
    }
    
    protected MessageSender makeMessageSender(ReplySync replySync) {
        return new MessageSender(this, replySync);
    }

    protected MessageReceiver makeMessageReceiver(ReplySync sync,
                                                  MessageDeliverer deliverer,
                                                  URI servantUri) {
        return new MessageReceiver(sync, deliverer, servantUri, threadService);
    }

    protected ReplySync makeReplySync() {
        return new ReplySync(this);
    }
    
    protected URI makeURI(String myServantId) throws URISyntaxException {
        File file = new File(rootDirectory, myServantId);
        file.mkdirs();
        if (file.isDirectory() && file.canWrite() && file.canRead()) {
            return new URI("file", null, file.getAbsolutePath(), null, null);
        } else {
            throw new URISyntaxException(file.getAbsolutePath(), "Bogus path '");
        }
    }
    
    protected int computeCost(AttributedMessage message) {
        // very cheap
        return 0;
    }

    protected DestinationLink createDestinationLink(MessageAddress address) {
        return new FileLink(address);
    }

    protected void findOrMakeNodeServant() {
        if (servantUri != null) {
            return;
        }
        
        // start polling file system
        String node = getNameSupport().getNodeMessageAddress().getAddress();
        try {
            servantUri = makeURI(node);
        } catch (URISyntaxException e) {
            loggingService.error("Failed to make URI for node " +node, e);
            return;
        }
        if (receiver == null) {
            ServiceBroker sb = getServiceBroker();
            MessageDeliverer deliverer = sb.getService(this, MessageDeliverer.class, null);
            receiver = makeMessageReceiver(findOrMakeReplySync(),  deliverer, servantUri);
        }
        setNodeURI(servantUri);

    }

    protected String getProtocolType() {
        return "-FILE";
    }

    protected void releaseNodeServant() {
        // Maybe delete the incoming message directory?
    }

    protected void remakeNodeServant() {
        // no-op
    }

    protected Boolean usesEncryptedSocket() {
        return false;
    }

    
    class FileLink extends Link {
        private final MessageSender sender;
        protected URI uri;

        protected FileLink(MessageAddress addr) {
            super(addr);
            this.sender = makeMessageSender(findOrMakeReplySync());
        }

        public boolean isValid() {
            // Remake our servant if necessary. If that fails, the link is
            // considered invalid,
            // since the remote reference must be unreachable.
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

        protected MessageAttributes forwardByProtocol(Object destination,
                                                      AttributedMessage message)
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
            return FileLinkProtocol.this.getClass();
        }
    }
}
