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
 * This utility class handles outgoing JMS messages
 */
public class MessageSender implements AttributeConstants {
    private final ReplySync sync;
    private final Logger log;

    public MessageSender(FileLinkProtocol lp, ReplySync sync) {
        this.sync = sync;
        log = Logging.getLogger(getClass().getName());
    }

    public MessageAttributes handleOutgoingMessage(URI uri,
                                                   AttributedMessage mtsMessage)
            throws CommFailureException,
                MisdeliveredMessageException {
        Object deadline = mtsMessage.getAttribute(MESSAGE_SEND_DEADLINE_ATTRIBUTE);
        long ttl = 0;
        if (deadline != null) {
            if (deadline instanceof Long) {
                ttl = ((Long) deadline).longValue()
                - System.currentTimeMillis();
                if (ttl < 0) {
                    log.warn("Message already expired");
                    MessageAttributes metadata = new MessageReply(mtsMessage);
                    metadata.setAttribute(MessageAttributes.DELIVERY_ATTRIBUTE,
                                          MessageAttributes.DELIVERY_STATUS_DROPPED);
                    return metadata;
                }
            }
        }
        MessageAttributes metadata = sync.sendMessage(mtsMessage, uri);
        return metadata;
    }
}
