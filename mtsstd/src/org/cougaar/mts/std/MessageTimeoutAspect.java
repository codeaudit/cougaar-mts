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

package org.cougaar.mts.std;

import java.util.LinkedList;
import java.util.List;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.DestinationQueueProviderService;
import org.cougaar.mts.base.MessageReply;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.ReceiveLink;
import org.cougaar.mts.base.ReceiveLinkDelegateImplBase;
import org.cougaar.mts.base.SendLink;
import org.cougaar.mts.base.SendLinkDelegateImplBase;
import org.cougaar.mts.base.SendQueueProviderService;
import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.base.UnregisteredNameException;
import org.cougaar.util.PropertyParser;
import org.cougaar.util.UnaryPredicate;

/**
 * Aspect to throw out a timed out message. Necessary for MsgLog et. al. Checks
 * every thread in MTS for timed out attributes on a message:
 * 
 * @property org.cougaar.syncClock Is NTP clock synchronization guaranteed?
 *           default is false.
 */
public final class MessageTimeoutAspect
        extends StandardAspect
        implements AttributeConstants {
    
    private static final boolean SYNC_CLOCK_AVAILABLE =
        PropertyParser.getBoolean("org.cougaar.syncClock", false);
    
    private static final long RECLAIM_PERIOD =
        PropertyParser.getLong("org.cougaar.core.mts.timout.reclaim", 60000);
    
    private SendQueueProviderService sendq_factory;
    
    private DestinationQueueProviderService destq_factory;
    
    private final UnaryPredicate timeoutPredicate = new UnaryPredicate() {
        /**
       * 
       */
      private static final long serialVersionUID = 1L;

      public boolean execute(Object x) {
            AttributedMessage msg = (AttributedMessage) x;
            return timedOut(msg, "Message Timeout Reclaimer");
        }
    };

    @Override
   public void load() {
        super.load();
        Runnable reclaimer = new Runnable() {
            public void run() {
                reclaim();
            }
        };
        Schedulable sched = threadService.getThread(this, reclaimer, "Message Timeout Reclaimer");
        sched.schedule(RECLAIM_PERIOD, RECLAIM_PERIOD);

    }

    @Override
   public void start() {
        super.start();
        ServiceBroker sb = getServiceBroker();
        sendq_factory = sb.getService(this, SendQueueProviderService.class, null);
        destq_factory = sb.getService(this, DestinationQueueProviderService.class, null);
    }

    private void reclaim() {
        // Keep track of the messages we deleted.  Not using this yet.
        List<Message> droppedMessages = new LinkedList<Message>();
        if (sendq_factory != null) {
            sendq_factory.removeMessages(timeoutPredicate, droppedMessages);
        }
        if (destq_factory != null) {
            destq_factory.removeMessages(timeoutPredicate, droppedMessages);
        }
    }

    private long getTimeout(AttributedMessage message) {
        Object attr = message.getAttribute(MESSAGE_SEND_DEADLINE_ATTRIBUTE);
        if (attr instanceof Long) {
            return ((Long) attr).longValue();
        } else {
            // negative values indicate no timeout
            return -1;
        }
    }

    private boolean timedOut(AttributedMessage message, String station) {
        long timeout = getTimeout(message);
        // negative values indicate no timeout
        if (timeout > 0) {
            long now = System.currentTimeMillis();
            if (timeout < now) {
                if (loggingService.isWarnEnabled()) {
                    loggingService.warn(station + " threw away a message=" + message.logString()
                            + " Beyond deadline=" + (now - timeout) + " ms");
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Add aspects to check the timeout at various stations.
     */
    @Override
   public Object getDelegate(Object object, Class<?> type) {
        if (type == SendLink.class) {
            return new SendLinkDelegate((SendLink) object);
        } else if (type == DestinationLink.class) {
            return new DestinationLinkDelegate((DestinationLink) object);
        } else if (type == ReceiveLink.class) {
            // we can only do this if the sender and receiver clocks are sync'd
            if (SYNC_CLOCK_AVAILABLE) {
                return new ReceiveLinkDelegate((ReceiveLink) object);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * When a message is offered, drop it silently if it's already timed out.
     * At one we were also handling relative v absolute times here, but that
     * was removed due to a misunderstanding and lack of documentation.
     */
    private class SendLinkDelegate
            extends SendLinkDelegateImplBase {
        
        SendLinkDelegate(SendLink link) {
            super(link);
        }

        /**
         * Convert relative timeouts to absolute time. A relative time is
         * provided with the attribute MESSAGE_SEND_TIMEOUT_ATTRIBUTE.
         * <p>
         * This method works by side-effecting the absolute timeout attribute,
         * MESSAGE_SEND_DEADLINE_ATTRIBUTE, in the given message.
         */
        private void ensureAbsoluteTimeout(AttributedMessage message) {
            Object timeout = message.getAttribute(MESSAGE_SEND_TIMEOUT_ATTRIBUTE);
            Object deadline = message.getAttribute(MESSAGE_SEND_DEADLINE_ATTRIBUTE);
            if (timeout instanceof Integer) {
                if (deadline == null) {
                    int relativeTimeout = ((Integer) timeout).intValue();
                    long absoluteTimeout = relativeTimeout + System.currentTimeMillis();
                    // store back into absolute attribute value
                    message.setAttribute(MESSAGE_SEND_DEADLINE_ATTRIBUTE, absoluteTimeout);
                    message.removeAttribute(MESSAGE_SEND_TIMEOUT_ATTRIBUTE);
                } else {
                    // Both a timeout and a deadline are set. Prefer the deadline.
                    String msg = "Ignoring timeout attribute since it also has a deadline"
                        +"\n"+ message;
                    loggingService.warn(msg);
                }
            }
        }

        /**
         * If the message is already timed out, just drop it silently.
         */
        @Override
      public void sendMessage(AttributedMessage message) {
            ensureAbsoluteTimeout(message);
            if (!timedOut(message, "SendLink")) {
                super.sendMessage(message);
            }
        }
    }

    /**
     * If we're about to send a message that's already timed out, return the
     * dropped status instead of sending.
     */
    private class DestinationLinkDelegate
            extends DestinationLinkDelegateImplBase {

        DestinationLinkDelegate(DestinationLink delegatee) {
            super(delegatee);
        }

        @Override
      public MessageAttributes forwardMessage(AttributedMessage message)
                throws UnregisteredNameException, NameLookupException, CommFailureException,
                MisdeliveredMessageException {
            if (timedOut(message, "DestinationLink")) {
                // drop message, set delivery status to dropped
                MessageAttributes metadata = new MessageReply(message);
                metadata.setAttribute(AttributeConstants.DELIVERY_ATTRIBUTE,
                                      AttributeConstants.DELIVERY_STATUS_DROPPED);
                return metadata;
            }
            return super.forwardMessage(message);
        }
    }

    /**
     * If a timed out message arrives, return the dropped status instead of
     * delivering.
     */
    private class ReceiveLinkDelegate
            extends ReceiveLinkDelegateImplBase {
        
        ReceiveLinkDelegate(ReceiveLink delegatee) {
            super(delegatee);
        }

        @Override
      public MessageAttributes deliverMessage(AttributedMessage message) {
            if (timedOut(message, "Deliverer")) {
                MessageAttributes metadata = new MessageReply(message);
                metadata.setAttribute(AttributeConstants.DELIVERY_ATTRIBUTE,
                                      AttributeConstants.DELIVERY_STATUS_DROPPED);
                return metadata;
            }
            return super.deliverMessage(message);
        }
    }
}
