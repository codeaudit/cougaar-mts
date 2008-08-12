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

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MessageDelivererDelegateImplBase;
import org.cougaar.mts.base.MessageReply;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.SendQueue;
import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.base.UnregisteredNameException;

/**
 * This test Aspect sends periodic 'heartbeat' messages to a specified
 * destination, as given by the <code>dstAddr</code> parameter. Other parameters
 * specify the delay, timeout and sendInterval of the hearbeat.
 */
public class HeartBeatAspect
        extends StandardAspect
        implements AttributeConstants, Runnable {

    private final String I_AM_A_HEARTBEAT_ATTRIBUTE = "i am a heartbeat";

    private MessageAddress hb_dest;
    private long delay;
    private long timeout;
    private long sendInterval;
    private long msgCount = 0;
    private SendQueue sendq;
    private MessageAddress us;

    public HeartBeatAspect() {
        super();
    }

    public void load() {
        super.load();
        String dstAddr = getParameter("dstAddr", "NODE1");
        hb_dest = MessageAddress.getMessageAddress(dstAddr);
        delay = getParameter("delay", 1000);
        timeout = getParameter("timeout", 1000);
        sendInterval = getParameter("sendInterval", 500);

    }

    synchronized void maybeStartSending(SendQueue queue) {
        if (sendq != null) {
            return;
        }

        sendq = queue;

        ServiceBroker sb = getServiceBroker();

        NodeIdentificationService nisvc =
                sb.getService(this, NodeIdentificationService.class, null);
        us = nisvc.getMessageAddress();
        ThreadService tsvc = sb.getService(this, ThreadService.class, null);
        Schedulable sched = tsvc.getThread(this, this, "HeartBeater");
        sched.schedule(10000, sendInterval);
    }

    static class HBMessage
            extends Message {
        HBMessage(MessageAddress src, MessageAddress dest) {
            super(src, dest);
        }
    };

    public void run() {
        if (sendq != null) {
            Message message = new HBMessage(us, hb_dest);
            AttributedMessage a_message = new AttributedMessage(message);
            a_message.setAttribute(MESSAGE_SEND_DEADLINE_ATTRIBUTE,
                                   new Long(System.currentTimeMillis() + timeout));
            a_message.setAttribute(I_AM_A_HEARTBEAT_ATTRIBUTE, new Long(msgCount++));
            sendq.sendMessage(a_message);
            System.out.println("Sending message " + a_message);
        }
    }

    // 
    // Aspect Code to implement TrafficRecord Collection

    public Object getDelegate(Object object, Class<?> type) {
        if (type == DestinationLink.class) {
            return new HeartBeatDestinationLink((DestinationLink) object);
        } else if (type == MessageDeliverer.class) {
            return new MessageDelivererDelegate((MessageDeliverer) object);
        } else if (type == SendQueue.class) {
            // steal the sendqueue
            maybeStartSending((SendQueue) object);

            return null;
        } else {
            return null;
        }
    }

    // Used to added Delay
    public class HeartBeatDestinationLink
            extends DestinationLinkDelegateImplBase {

        public HeartBeatDestinationLink(DestinationLink link) {
            super(link);
        }

        public MessageAttributes forwardMessage(AttributedMessage message)
                throws UnregisteredNameException, NameLookupException, CommFailureException,
                MisdeliveredMessageException {
            // Attempt to Deliver message
            Object count = message.getAttribute(I_AM_A_HEARTBEAT_ATTRIBUTE);
            if (count != null) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ex) {
                }
            }
            MessageAttributes meta = super.forwardMessage(message);
            return meta;
        }
    }

    public class MessageDelivererDelegate
            extends MessageDelivererDelegateImplBase {

        MessageDelivererDelegate(MessageDeliverer delegatee) {
            super(delegatee);
        }

        public MessageAttributes deliverMessage(AttributedMessage message, MessageAddress dest)
                throws MisdeliveredMessageException {
            Object count = message.getAttribute(I_AM_A_HEARTBEAT_ATTRIBUTE);
            if (count != null) {
                System.out.println("Recieved Heartbeat from=" + message.getOriginator() + "count="
                        + count);
                MessageAttributes metadata = new MessageReply(message);
                metadata.setAttribute(AttributeConstants.DELIVERY_ATTRIBUTE,
                                      AttributeConstants.DELIVERY_STATUS_DELIVERED);
                return metadata;
            } else {
                return super.deliverMessage(message, dest);
            }
        }

    }

}
