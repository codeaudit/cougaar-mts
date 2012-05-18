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

import java.util.Iterator;

import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.MulticastMessageAddress;
import org.cougaar.core.mts.SimpleMessageAttributes;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MessageDelivererDelegateImplBase;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.SendLink;
import org.cougaar.mts.base.SendLinkDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;

/**
 * This Aspect provides support for multicast messages.
 */
public class MulticastAspect
        extends StandardAspect {

    private static final String MCAST = "org.cougaar.message.transport.is-multicast";

    @Override
   public Object getDelegate(Object delegatee, Class<?> type) {
        if (type == SendLink.class) {
            return new SendLinkDelegate((SendLink) delegatee);
        } else {
            return null;
        }
    }

    @Override
   public Object getReverseDelegate(Object delegatee, Class<?> type) {
        if (type == MessageDeliverer.class) {
            return new MessageDelivererDelegate((MessageDeliverer) delegatee);
        } else {
            return null;
        }
    }

    public class SendLinkDelegate
            extends SendLinkDelegateImplBase {

        public SendLinkDelegate(SendLink link) {
            super(link);
        }

        @Override
      public void sendMessage(AttributedMessage msg) {
            MessageAddress destination = msg.getTarget();
            if (destination instanceof MulticastMessageAddress) {
                msg.setAttribute(MCAST, destination);
                AttributedMessage copy;
                MessageAddress nodeAddr;
                if (destination.equals(MessageAddress.MULTICAST_LOCAL)) {
                    if (loggingService.isDebugEnabled()) {
                        loggingService.debug("MCAST: Local multicast");
                    }
                    nodeAddr = getNameSupport().getNodeMessageAddress();
                    copy = new AttributedMessage(msg);
                    copy.setTarget(nodeAddr);
                    super.sendMessage(copy);
                } else {
                    if (loggingService.isDebugEnabled()) {
                        loggingService.debug("MCAST: Remote multicast to " + destination);
                    }
                    MulticastMessageAddress dst = (MulticastMessageAddress) destination;
                    Iterator<MessageAddress> itr = getRegistry().findRemoteMulticastTransports(dst);
                    while (itr.hasNext()) {
                        nodeAddr = itr.next();
                        if (loggingService.isDebugEnabled()) {
                            loggingService.debug("MCAST: next address = " + nodeAddr);
                        }
                        copy = new AttributedMessage(msg);
                        copy.setTarget(nodeAddr);
                        super.sendMessage(copy);
                    }
                }
            } else {
                super.sendMessage(msg);
            }
        }

    }

    public class MessageDelivererDelegate
            extends MessageDelivererDelegateImplBase {

        public MessageDelivererDelegate(MessageDeliverer deliverer) {
            super(deliverer);
        }

        @Override
      public MessageAttributes deliverMessage(AttributedMessage msg, MessageAddress destination)
                throws MisdeliveredMessageException {
            MulticastMessageAddress mcastAddr = (MulticastMessageAddress) msg.getAttribute(MCAST);

            if (mcastAddr != null) {
                if (loggingService.isDebugEnabled()) {
                    loggingService.debug("MCAST: Received multicast to " + mcastAddr);
                }
                Iterator<MessageAddress> i = getRegistry().findLocalMulticastReceivers(mcastAddr);
                MessageAddress localDestination = null;
                AttributedMessage copy = new AttributedMessage(msg.getRawMessage(), msg);
                while (i.hasNext()) {
                    localDestination = i.next();
                    if (loggingService.isDebugEnabled()) {
                        loggingService.debug("MCAST: Delivering to " + localDestination);
                    }
                    super.deliverMessage(copy, localDestination);
                }
                // Hmm...
                MessageAttributes meta = new SimpleMessageAttributes();
                meta.setAttribute(AttributeConstants.DELIVERY_ATTRIBUTE,
                                  AttributeConstants.DELIVERY_STATUS_DELIVERED);
                return meta;
            } else {
                return super.deliverMessage(msg, destination);
            }
        }

    }

}
