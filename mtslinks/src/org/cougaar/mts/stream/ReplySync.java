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
package org.cougaar.mts.stream;

import java.io.IOException;
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
 * This utility class does the low-level work to force the stream linkprotocol
 * to behave like a synchronous rpc. In particular it blocks the sending thread
 * until a reply for the outgoing message arrives, generates and sends replies
 * for incoming messages, and processes received replies by waking the
 * corresponding thread.
 * 
 * @param <I> The class of the ID object for each outgoing message
 */
class ReplySync<I> {
    private static final String MESSAGE_NUMBER_PROP = "org.cougaar.mts.stream.number";
    private static final String MESSAGE_ID_PROP = "org.cougaar.mts.stream.id";
    private static final String IS_MTS_REPLY_PROP = "org.cougaar.mts.stream.replyp";
    private static final String ORIGINATING_URI_PROP = "org.cougaar.mts.stream.orig";
    private static final String DELIVERY_EXCEPTION_PROP = "org.cougaar.mts.stream.ex";
    private static int MESSAGE_NUMBER = 0;

    private final PollingStreamLinkProtocol<I> protocol;
    private final Map<Integer, Object> pending;
    private final Map<Integer, Object> replyData;
    private final int timeout;
    private final Logger log;

    ReplySync(PollingStreamLinkProtocol<I> protocol, int timeout) {
        this.protocol = protocol;
        this.pending = new HashMap<Integer, Object>();
        this.replyData = new HashMap<Integer, Object>();
        this.log = Logging.getLogger(getClass().getName());
        this.timeout = timeout;
    }

    private void setMessageProperties(AttributedMessage message, Integer id, URI uri) {
        message.setAttribute(MESSAGE_NUMBER_PROP, id.intValue());
        message.setAttribute(IS_MTS_REPLY_PROP, false);
        message.setAttribute(ORIGINATING_URI_PROP, protocol.getServantUri());
    }

    MessageAttributes sendMessage(AttributedMessage message, URI uri)
            throws CommFailureException, MisdeliveredMessageException {
        int messageNumber = ++MESSAGE_NUMBER;
        setMessageProperties(message, messageNumber, uri);

        Object lock = new Object();
        pending.put(messageNumber, lock);
        long startTime = System.currentTimeMillis();
        synchronized (lock) {
            SchedulableStatus.beginNetIO("Stream RPC");
            try {
                I id = protocol.processOutgoingMessage(uri, message);
                message.setLocalAttribute(MESSAGE_ID_PROP, id);
            } catch (IOException e) {
                throw new CommFailureException(e);
            } finally {
                SchedulableStatus.endBlocking();
            }
            while (true) {
                try {
                    lock.wait(timeout); // TODO: timeout should be set
                                        // dynamically
                    break;
                } catch (InterruptedException ex) {
                    // keep waiting
                }
            }
        }
        long sendTime = System.currentTimeMillis() - startTime;
        Object result = replyData.remove(messageNumber);
        pending.remove(messageNumber);
        if (result instanceof MessageAttributes) {
            if (log.isDebugEnabled()) {
                log.debug("Response to message " + messageNumber + " was " + result);
            }
            return (MessageAttributes) result;
        } else if (result instanceof MisdeliveredMessageException) {
            MisdeliveredMessageException ex = (MisdeliveredMessageException) result;
            throw ex;
        } else if (sendTime >= timeout) {
            throw new CommFailureException(new Exception("Timeout waiting for reply = " + sendTime));
        } else {
            throw new CommFailureException(new Exception("Weird Reply" + result));
        }
    }

    private void setReplyProperties(AttributedMessage omsg, MessageAttributes reply) {
        reply.setAttribute(IS_MTS_REPLY_PROP, true);
        reply.setAttribute(MESSAGE_NUMBER_PROP, omsg.getAttribute(MESSAGE_NUMBER_PROP));
    }

    void replyToMessage(AttributedMessage originalMsg, MessageAttributes replyData) {
        setReplyProperties(originalMsg, replyData);
        URI originatingUri = (URI) originalMsg.getAttribute(ORIGINATING_URI_PROP);
        try {
            protocol.processOutgoingMessage(originatingUri, replyData);
        } catch (IOException e) {
            log.warn("Unable to reply to delivered message from " + originatingUri + ": "
                    + e.getMessage());
        }
    }

    void replyToMessage(AttributedMessage originalMessage, MisdeliveredMessageException exception) {
        MessageAttributes attrs = new SimpleMessageAttributes();
        attrs.setAttribute(DELIVERY_EXCEPTION_PROP, exception);
        replyToMessage(originalMessage, attrs);
    }

    boolean isReply(MessageAttributes attrs) {
        boolean isReply = (Boolean) attrs.getAttribute(IS_MTS_REPLY_PROP);

        Integer id = (Integer) attrs.getAttribute(MESSAGE_NUMBER_PROP);
        if (!isReply) {
            if (log.isDebugEnabled()) {
                log.debug("Received message " + id);
            }
            return false;
        }

        if (log.isDebugEnabled()) {
            log.debug("Handling reply to " + id);
        }

        if (id == null) {
            log.error("Ack message has no value for attribute " + MESSAGE_NUMBER_PROP);
            return true;
        }
        Object exception = attrs.getAttribute(DELIVERY_EXCEPTION_PROP);
        if (exception != null) {
            replyData.put(id, exception);
        } else {
            replyData.put(id, attrs);
        }
        Object lock = pending.get(id);
        if (lock != null) {
            synchronized (lock) {
                lock.notify();
            }
        } else {
            if (log.isWarnEnabled()) {
                log.warn("Got reply for message we timed out, id=" + id);
            }
        }
        return true;
    }

}
