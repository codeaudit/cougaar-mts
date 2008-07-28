/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.SimpleMessageAttributes;
import org.cougaar.core.thread.SchedulableStatus;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.std.AttributedMessage;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * This utility class does the low-level work to force the file linkprotocol to
 * behave like a synchronous rpc. In particular it blocks the sending thread
 * until a reply for the outgoing message arrives, generates and sends replies
 * for incoming messages, and processes received replies by waking the
 * corresponding thread.
 */
public class ReplySync {
    public static final int DEFAULT_TIMEOUT = 5000;
    private static final String ID_PROP = "MTS_MSG_ID";
    private static final String IS_MTS_REPLY_PROP = "MTS_REPLY";
    private static final String ORIGINATING_URI_PROP = "ORIGINATING_URI";
    private static final String DELIVERY_EXCEPTION_PROP = "DELIVERY_EXCEPTION";
    private static int ID = 0;

    protected final FileLinkProtocol lp;
    private final Map<Integer, Object> pending;
    private final Map<Integer, Object> replyData;
    private final int timeout;
    protected final Logger log;

    public ReplySync(FileLinkProtocol lp) {
        this(lp, DEFAULT_TIMEOUT);
    }

    public ReplySync(FileLinkProtocol lp, int timeout) {
        this.lp = lp;
        this.pending = new HashMap<Integer, Object>();
        this.replyData = new HashMap<Integer, Object>();
        this.log = Logging.getLogger(getClass().getName());
        this.timeout = timeout;
    }

    private void writeMessage(URI directory, AttributedMessage message) 
            throws IOException {
        File directoryPath = new File(directory.getPath());
        
        // serialize message to a temp file
        File tempDir = new File(directoryPath, "tmp");
        File dataDir = new File(directoryPath, "msgs");
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
    }
    
    protected void setMessageProperties(AttributedMessage message,
                                        Integer id,
                                        URI uri) {
        message.setAttribute(ID_PROP, id.intValue());
        message.setAttribute(IS_MTS_REPLY_PROP, false);
        message.setAttribute(ORIGINATING_URI_PROP, lp.getServantUri());
    }

    public MessageAttributes sendMessage(AttributedMessage message,
                                         URI uri)
            throws CommFailureException,
                MisdeliveredMessageException {
        Integer id = new Integer(++ID);
        setMessageProperties(message, id, uri);

        Object lock = new Object();
        pending.put(id, lock);
        long startTime = System.currentTimeMillis();
        SchedulableStatus.beginNetIO("FILE RPC");
        synchronized (lock) {
            try {
                writeMessage(uri, message);
            } catch (IOException e) {
                throw new CommFailureException(e);
            }
            while (true) {
                try {
                    lock.wait(timeout); // TODO: timeout should be set dynamically
                    break;
                } catch (InterruptedException ex) {

                }
            }
        }
        SchedulableStatus.endBlocking();
        long sendTime = System.currentTimeMillis() - startTime;
        Object result = replyData.remove(id);
        pending.remove(id);
        if (result instanceof MessageAttributes) {
            return (MessageAttributes) result;
        } else if (result instanceof MisdeliveredMessageException) {
            MisdeliveredMessageException ex = (MisdeliveredMessageException) result;
            throw ex;
        } else if (sendTime >= timeout) {
            throw new CommFailureException(new RuntimeException("Timeout waiting for reply = "
                    + sendTime));
        } else {
            throw new CommFailureException(new RuntimeException("Weird Reply"
                    + result));
        }
    }

    protected void setReplyProperties(AttributedMessage omsg, 
                                      AttributedMessage replyMsg) {
        replyMsg.setAttribute(IS_MTS_REPLY_PROP, true);
        replyMsg.setAttribute(ID_PROP, omsg.getAttribute(ID_PROP));
    }

    public void replyToMessage(AttributedMessage originalMsg, 
                               MessageAttributes replyData) {
        AttributedMessage replyMsg = new AttributedMessage(null, replyData);
        setReplyProperties(originalMsg, replyMsg);
        URI originatingUri = (URI) originalMsg.getAttribute(ORIGINATING_URI_PROP);
        try {
            writeMessage(originatingUri, replyMsg);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void replyToMessage(AttributedMessage originalMessage,
                               MisdeliveredMessageException exception) {
        MessageAttributes attrs = new SimpleMessageAttributes();
        attrs.setAttribute(DELIVERY_EXCEPTION_PROP, exception);
        replyToMessage(originalMessage, attrs);
    }   
    
    public boolean isReply(AttributedMessage msg) {
        boolean isReply = (Boolean) msg.getAttribute(IS_MTS_REPLY_PROP);
        if (log.isDebugEnabled()) {
            log.debug("Value of " + IS_MTS_REPLY_PROP + " property is "
                      + isReply);
        }
        if (!isReply) {
            return false;
        }
        Integer id =  (Integer) msg.getAttribute(ID_PROP);
        if (log.isDebugEnabled()) {
            log.debug("Value of " + ID_PROP + " property is " + id);
        }
        Object exception = msg.getAttribute(DELIVERY_EXCEPTION_PROP);
        if (exception != null) {
            replyData.put(id, exception);
        } else {
            replyData.put(id, msg);
        }
        Object lock = pending.get(id);
        if (lock != null) {
            synchronized (lock) {
                lock.notify();
            }
        } else {
            if (log.isWarnEnabled()) {
                log.warn("Got reply for message we timed out, id=" + id
                         + " msg=" + msg);
            }
        }
        return true;
    }

}
