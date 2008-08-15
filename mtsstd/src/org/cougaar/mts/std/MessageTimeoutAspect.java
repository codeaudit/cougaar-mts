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

import java.util.Collections;
import java.util.List;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.service.ThreadService;
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
 * <pre>
 * -SendLink - DestinationLink - ReceiveLink
 * </pre>
 * 
 * @property org.cougaar.syncClock Is NTP clock synchronization guaranteed?
 *           default is false.
 */
public final class MessageTimeoutAspect
        extends StandardAspect
        implements AttributeConstants {
    private SendQueueProviderService sendq_factory;
    private DestinationQueueProviderService destq_factory;
    private final UnaryPredicate timeoutPredicate = new UnaryPredicate() {
        public boolean execute(Object x) {
            AttributedMessage msg = (AttributedMessage) x;
            return timedOut(msg, "Message Timeout Reclaimer");
        }
    };

    public static final boolean SYNC_CLOCK_AVAILABLE =
            PropertyParser.getBoolean("org.cougaar.syncClock", false);

    public static final long RECLAIM_PERIOD =
            PropertyParser.getLong("org.cougaar.core.mts.timout.reclaim", 60000);

    public MessageTimeoutAspect() {
    }

    public void load() {
        super.load();

        ServiceBroker sb = getServiceBroker();
        ThreadService tsvc = sb.getService(this, ThreadService.class, null);

        Runnable reclaimer = new Runnable() {
            public void run() {
                reclaim();
            }
        };
        Schedulable sched = tsvc.getThread(this, reclaimer, "Message Timeout Reclaimer");
        sched.schedule(RECLAIM_PERIOD, RECLAIM_PERIOD);
        sb.releaseService(this, ThreadService.class, tsvc);

    }

    public void start() {
        super.start();
        ServiceBroker sb = getServiceBroker();
        sendq_factory = sb.getService(this, SendQueueProviderService.class, null);
        destq_factory = sb.getService(this, DestinationQueueProviderService.class, null);
    }

    private void reclaim() {
        List<Message> droppedMessages = Collections.emptyList(); // not using this yet
        if (sendq_factory != null) {
            sendq_factory.removeMessages(timeoutPredicate, droppedMessages);
        }
        if (destq_factory != null) {
            destq_factory.removeMessages(timeoutPredicate, droppedMessages);
        }
    }

    // retrieves absolute timeout that convertTimeout stored for us
    private long getTimeout(AttributedMessage message) {
        long the_timeout = -1;
        Object attr = message.getAttribute(MESSAGE_SEND_DEADLINE_ATTRIBUTE);
        if (attr != null) { // check for, convert to long
            if (attr instanceof Long) {
                the_timeout = ((Long) attr).longValue();
                return the_timeout;
            }
        }
        return -1; // something extraordinarily large so msgs will never time
                   // out
    }

    private boolean timedOut(AttributedMessage message, String station) {
        long the_timeout = getTimeout(message);
        // absolute timeout value of must be greater than 0;
        if (the_timeout > 0) {
            long now = System.currentTimeMillis();
            if (the_timeout < now) {
                // log that the message timed out
                if (loggingService.isWarnEnabled()) {
                    loggingService.warn(station + " threw away a message=" + message.logString()
                            + " Beyond deadline=" + (now - the_timeout) + " ms");
                }
                return true;
            }
        }
        return false;
    }

    /*
     * Aspect Code to hook into all the links in the MTS chain
     */
    public Object getDelegate(Object object, Class<?> type) {
        if (type == SendLink.class) {
            return new SendLinkDelegate((SendLink) object);
        } else if (type == DestinationLink.class) {
            return new DestinationLinkDelegate((DestinationLink) object);
        } else if (type == ReceiveLink.class) {
            if (SYNC_CLOCK_AVAILABLE) {
                return new ReceiveLinkDelegate((ReceiveLink) object);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /*
     * First thread in the msg chain to check timeout values Also computes
     * timeout
     */
    private class SendLinkDelegate
            extends SendLinkDelegateImplBase {
        SendLinkDelegate(SendLink link) {
            super(link);
        }

        // turns relative into absolute timeout
        // stores back into absolute for other delegates to access
        // null means no timeout was used
        long convertTimeout(AttributedMessage message) {

            long the_timeout = -1;

            // Get either the relative or absolute timeout values here
            // One (should) be null
            Object attr = message.getAttribute(MESSAGE_SEND_TIMEOUT_ATTRIBUTE);
            if (attr != null) { // check for relative
                if (attr instanceof Integer) {
                    the_timeout = ((Integer) attr).intValue();
                    the_timeout += System.currentTimeMillis(); // turn into
                                                               // absolute time
                    // store back into absolute attribute value
                    message.setAttribute(MESSAGE_SEND_DEADLINE_ATTRIBUTE, new Long(the_timeout));
                }
            } else {
                attr = message.getAttribute(MESSAGE_SEND_DEADLINE_ATTRIBUTE);
                if (attr != null) { // check for absolute
                    if (attr instanceof Long) {
                        the_timeout = ((Long) attr).longValue();
                    }
                }
            }
            return the_timeout;
        }

        public void sendMessage(AttributedMessage message) {
            // convert relative timeouts to absolute
            // long the_timeout = convertTimeout(message);

            if (timedOut(message, "SendLink")) {
                // drop message silently
                return;
            }
            super.sendMessage(message);
        }
    }

    private class DestinationLinkDelegate
            extends DestinationLinkDelegateImplBase {
        DestinationLinkDelegate(DestinationLink delegatee) {
            super(delegatee);
        }

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
            MessageAttributes metadata = super.forwardMessage(message);
            return metadata;
        }
    }

    private class ReceiveLinkDelegate
            extends ReceiveLinkDelegateImplBase {
        ReceiveLinkDelegate(ReceiveLink delegatee) {
            super(delegatee);
        }

        public MessageAttributes deliverMessage(AttributedMessage message) {
            if (timedOut(message, "Deliverer")) {
                // drop message, set delivery status to dropped
                MessageAttributes metadata = new MessageReply(message);
                metadata.setAttribute(AttributeConstants.DELIVERY_ATTRIBUTE,
                                      AttributeConstants.DELIVERY_STATUS_DROPPED);
                return metadata;
            }
            MessageAttributes metadata = super.deliverMessage(message);
            return metadata;
        }
    }
}
