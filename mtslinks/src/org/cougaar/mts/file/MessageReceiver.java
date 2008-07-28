/*
 * <copyright>
 *  
 *  Copyright 1997-2006 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */
package org.cougaar.mts.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URI;

import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.std.AttributedMessage;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * This utility class handles incoming JMS messages
 */
public class MessageReceiver {
    protected final Logger log;
    private final MessageDeliverer deliverer;
    private final ReplySync sync;
    private final URI servantUri;
    private Schedulable poller;
    public MessageReceiver(ReplySync sync, MessageDeliverer deliverer,
                           URI servantUri, ThreadService threads) {
        this.sync = sync;
        this.deliverer = deliverer;
        this.servantUri = servantUri;
        this.log = Logging.getLogger(getClass().getName());
        poller = threads.getThread(this, new FilePoller(), "File Poller");
        poller.schedule(0, 1);
    }
    
    public void handleIncomingMessage(AttributedMessage msg) {
        if (log.isDebugEnabled())
            log.debug("Received JMS message=" + msg);
        if (deliverer == null) {
            log.error("Message arrived before MessageDelivererService was available");
            return;
        }
        if (sync.isReply(msg)) {
            // it's an ack -- Work is done in isReply
            return;
        } else {
            try {
                MessageAttributes reply = deliverer.deliverMessage(msg, msg.getTarget());
                sync.replyToMessage(msg, reply);
            } catch (MisdeliveredMessageException e) {
                log.error(e.getMessage(), e);
                sync.replyToMessage(msg, e);
            }
        }
    }

    private class FilePoller implements Runnable {
        private File directory;
        
        FilePoller() {
            File rootDirectory = new File(servantUri.getPath());
            directory = new File(rootDirectory, "msgs");
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
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                ObjectInputStream ois = new ObjectInputStream(fis);
                Object rawObject = ois.readObject();
                if (rawObject instanceof AttributedMessage) {
                    handleIncomingMessage((AttributedMessage) rawObject);
                } else if (rawObject instanceof Exception) {
                    
                } else {
                    throw new IllegalStateException(rawObject + " is not an AttributedMessage");
                }
            } catch (Exception e) {
                log.error("Error reading '" +file+ "': " + e.getMessage(), e);
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
