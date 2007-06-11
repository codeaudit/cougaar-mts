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
package org.cougaar.mts.jms;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;

import org.cougaar.core.mts.MessageAttributes;
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

    public MessageReceiver(ReplySync sync, MessageDeliverer deliverer) {
        this.sync = sync;
        this.deliverer = deliverer;
        this.log = Logging.getLogger(getClass().getName());
    }

    public void handleIncomingMessage(Message msg) {
        if (log.isDebugEnabled())
            log.debug("Received JMS message=" + msg);
        if (deliverer == null) {
            log.error("Message arrived before MessageDelivererService was available");
            return;
        }
        if (msg instanceof ObjectMessage) {
            ObjectMessage omsg = (ObjectMessage) msg;
            if (sync.isReply(omsg)) {
                // it's an ack -- Work is done in isReply
                return;
            }
            try {
                Object domainObject = omsg.getObject();
                if (domainObject instanceof AttributedMessage) {
                    AttributedMessage message = (AttributedMessage) domainObject;
                    try {
                        MessageAttributes reply = deliverer.deliverMessage(message,
                                                                           message.getTarget());
                        sync.replyToMessage(omsg, reply);
                    } catch (MisdeliveredMessageException e) {
                        sync.replyToMessage(omsg, e);
                    }
                } else {
                    if (log.isWarnEnabled())
                        log.warn(domainObject + " is not an AttributedMessage");
                }
            } catch (JMSException e1) {
                if (log.isWarnEnabled())
                    log.warn("JMS Error handling new message: Cause="
                            + e1.getMessage());
            } catch (Exception e2) {
                if (log.isWarnEnabled())
                    log.warn("Error handling new message: Cause="
                            + e2.getMessage());
            }
        } else {
            if (log.isWarnEnabled())
                log.warn("Received a JMS message that wasn't an ObjectMessage");
        }
    }

}
