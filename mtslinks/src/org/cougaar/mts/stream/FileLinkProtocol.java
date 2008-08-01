/*
 *
 * Copyright 2008 by BBN Technologies Corporation
 *
 */

package org.cougaar.mts.stream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.mts.std.AttributedMessage;
import org.cougaar.util.annotations.Cougaar;

/**
 * Send messages via file-sharing.
 */
public class FileLinkProtocol extends PollingStreamLinkProtocol {
    private static final String DATA_SUBDIRECTORY = "msgs";
    private static final String TMP_SUBDIRECTORY = "temp";

    @Cougaar.Arg(name = "rootDirectory", defaultValue="/tmp/cougaar")
    private String rootDirectory;

    private File getDataSubdirectory(URI uri) {
        File rootDirectory = new File(uri.getPath());
        return new File(rootDirectory, DATA_SUBDIRECTORY);
    }

    private File getTmpSubdirectory(URI uri) {
        File rootDirectory = new File(uri.getPath());
        return new File(rootDirectory, TMP_SUBDIRECTORY);
    }

    protected URI makeURI(String myServantId) throws URISyntaxException {
        File file = new File(rootDirectory, myServantId);
        file.mkdirs();
        if (file.isDirectory() && file.canWrite() && file.canRead()) {
            return new URI("file", "", file.getAbsolutePath(), null, null);
        } else {
            throw new URISyntaxException(file.getAbsolutePath(), "Bogus path '");
        }
    }
    
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
    
    protected Runnable makePollerTask() {
        return new FilePoller();
    }
    
    protected int computeCost(AttributedMessage message) {
        // very cheap
        return 0;
    }
    
    protected int getReplyTimeoutMillis() {
        return 1000;
    }

    protected String getProtocolType() {
        return "-FILE";
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
