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
package org.cougaar.mts.stream;

import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.std.AttributedMessage;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * This utility class handles incoming file messages
 * 
 * @param <I> The class of the ID object for each outgoing message
 */
class MessageReceiver<I> {
    private final Logger log;
    private final MessageDeliverer deliverer;
    private final PollingStreamLinkProtocol<I> protocol;

    MessageReceiver(PollingStreamLinkProtocol<I> protocol, MessageDeliverer deliverer) {
        this.protocol = protocol;
        this.deliverer = deliverer;
        this.log = Logging.getLogger(getClass().getName());
       
    }

    void handleIncomingMessage(MessageAttributes attrs) {
        ReplySync<I> sync = protocol.getReplySync();
        if (sync.isReply(attrs)) {
            // it's an ack -- Work is done in isReply
            return;
        } 
        if (attrs instanceof AttributedMessage) {
            AttributedMessage message = (AttributedMessage) attrs;
            if (log.isInfoEnabled()) {
                log.info("Delivering " + message+ " from " +message.getOriginator() 
                         + " to " + message.getTarget());
            }
            try {
                MessageAttributes reply = deliverer.deliverMessage(message, message.getTarget());
                sync.replyToMessage(message, reply);
            } catch (MisdeliveredMessageException e) {
                if (log.isDebugEnabled()) {
                    log.debug(e.getMessage(), e);
                }
                sync.replyToMessage(message, e);
            }
        }
    }

    
}
