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

import java.net.URI;

import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.MessageReply;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.std.AttributedMessage;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * This utility class handles outgoing file messages
 */
class MessageSender implements AttributeConstants {
    private final PollingStreamLinkProtocol protocol;
    private final Logger log;

    MessageSender(PollingStreamLinkProtocol protocol) {
        this.protocol = protocol;
        log = Logging.getLogger(getClass().getName());
    }

    MessageAttributes handleOutgoingMessage(URI uri, AttributedMessage mtsMessage) 
            throws CommFailureException,
            MisdeliveredMessageException {
        Object deadline = mtsMessage.getAttribute(MESSAGE_SEND_DEADLINE_ATTRIBUTE);
        if (deadline instanceof Long) {
            long ttl = (Long) deadline - System.currentTimeMillis();
            if (ttl < 0) {
                log.warn("Message already expired");
                MessageAttributes metadata = new MessageReply(mtsMessage);
                metadata.setAttribute(MessageAttributes.DELIVERY_ATTRIBUTE,
                                      MessageAttributes.DELIVERY_STATUS_DROPPED);
                return metadata;
            }
        }
        if (log.isInfoEnabled()) {
            log.info("Sending message " + mtsMessage+ " to " + uri);
        }
        MessageAttributes metadata = protocol.getReplySync().sendMessage(mtsMessage, uri);
        return metadata;
    }
}
