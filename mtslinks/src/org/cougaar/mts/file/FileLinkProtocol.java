/*
 *
 * Copyright 2008 by BBN Technologies Corporation
 *
 */

package org.cougaar.mts.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
 * Send messages via file-sharing.
 */
public class FileLinkProtocol extends RPCLinkProtocol {
    private static final String DATA_SUBDIRECTORY = "msgs";
    private static final String TMP_SUBDIRECTORY = "temp";

    // manager for receiving messages
    private MessageReceiver receiver;
    
    // manager for sending messages and waiting for replies
    private ReplySync sync;

    private URI servantUri;

    @Cougaar.Arg(name = "rootDirectory", required = true)
    private String rootDirectory;

    @Cougaar.ObtainService
    private ThreadService threadService;

    URI getServantUri() {
        return servantUri;
    }

    ReplySync getReplySync() {
         if (sync == null) {
             sync = new ReplySync(this);
         }
         return sync;
    }

    private File getDataSubdirectory(URI uri) {
        File rootDirectory = new File(uri.getPath());
        return new File(rootDirectory, DATA_SUBDIRECTORY);
    }

    private File getTmpSubdirectory(URI uri) {
        File rootDirectory = new File(uri.getPath());
        return new File(rootDirectory, TMP_SUBDIRECTORY);
    }
    
    private void deleteFile(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteFile(child);
            }
        }
        file.delete();
    }
    
    private void cleanup() {
        if (servantUri != null) {
            File root = new File(servantUri.getPath());
            deleteFile(root);
        }
    }
    
    public void unload() {
        super.unload();
        cleanup();
    }

    private MessageSender makeMessageSender() {
        return new MessageSender(this);
    }

    private MessageReceiver makeMessageReceiver(MessageDeliverer deliverer) {
        return new MessageReceiver(this, deliverer);
    }

    private URI makeURI(String myServantId) throws URISyntaxException {
        File file = new File(rootDirectory, myServantId);
        file.mkdirs();
        if (file.isDirectory() && file.canWrite() && file.canRead()) {
            return new URI("file", "", file.getAbsolutePath(), null, null);
        } else {
            throw new URISyntaxException(file.getAbsolutePath(), "Bogus path '");
        }
    }
    
    /**
     * Override to use a medium other than, or in addition to, local files.
     */
    protected void processOutgoingMessage(URI destination, MessageAttributes message) 
            throws IOException {
        // serialize message to a temp file
        File tempDir = getTmpSubdirectory(destination);
        File dataDir = getDataSubdirectory(destination);
        tempDir.mkdirs();
        dataDir.mkdir();

        File temp = File.createTempFile("FileLinkProtocol", ".msg", tempDir);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(temp);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(message);
            oos.flush();
        } finally {
            if (fos != null) {
                fos.close();
            }
        }

        // rename the temp file to a unique name in the directory
        File messageFile = new File(dataDir, temp.getName());
        temp.renameTo(messageFile);
        if (loggingService.isDebugEnabled()) {
            loggingService.debug("Wrote message to " + messageFile);
        }
    }
    
    /**
     * Override to poll something other than the local file system.
     * Each poll should invoke {@link processIncomingMessage} for
     * each new item.
     */
    protected Runnable makePollerTask() {
        return new FilePoller();
    }
    
    /**
     * Read and dispatch an incoming message on a stream.
     * 
     * This is protected so that it can be invoked by
     * a subclass, not (typically) to be overridden.
     */
    protected void processingIncomingMessage(InputStream stream) 
            throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(stream);
        Object rawObject = ois.readObject();
        if (rawObject instanceof MessageAttributes) {
            receiver.handleIncomingMessage((MessageAttributes) rawObject);
        } else {
            throw new IllegalStateException(rawObject + " is not MessageAttributes");
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
            loggingService.error("Failed to make URI for node " + node, e);
            return;
        }
        if (receiver == null) {
            ServiceBroker sb = getServiceBroker();
            MessageDeliverer deliverer = sb.getService(this, MessageDeliverer.class, null);
            receiver = makeMessageReceiver(deliverer);
            Runnable pollerTask = makePollerTask();
            Schedulable poller = threadService.getThread(this ,pollerTask, "Message Poller");
            poller.schedule(0, 1);
        }
        setNodeURI(servantUri);

    }

    protected String getProtocolType() {
        return "-FILE";
    }

    protected void releaseNodeServant() {
        // Maybe delete the incoming message directory?
        cleanup();
    }

    protected void remakeNodeServant() {
        // no-op
    }

    protected Boolean usesEncryptedSocket() {
        return false;
    }

    private class FileLink extends Link {
        private final MessageSender sender;
        private URI uri;

        FileLink(MessageAddress addr) {
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
            return FileLinkProtocol.this.getClass();
        }
    }
    
    private class FilePoller implements Runnable {
        private final File directory;

        FilePoller() {
            directory =  getDataSubdirectory(getServantUri());
        }

        public void run() {
            if (directory.exists()) {
                File[] contents = directory.listFiles();
                for (File file : contents) {
                    processFile(file);
                    file.delete();
                }
            }
        }

        private void processFile(File file) {
            if (loggingService.isDebugEnabled()) {
                loggingService.debug("Handling message in " + file);
            }
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                processingIncomingMessage(fis);
            } catch (Exception e) {
                loggingService.error("Error reading '" + file + "': " + e.getMessage(), e);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        // don't care
                    }
                }
            }
        }
    }
}
