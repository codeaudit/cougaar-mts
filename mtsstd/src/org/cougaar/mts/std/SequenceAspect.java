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

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.cougaar.core.mts.AgentState;
import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.mts.SimpleMessageAttributes;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.OutOfBandMessageService;
import org.cougaar.mts.base.ReceiveLink;
import org.cougaar.mts.base.ReceiveLinkDelegateImplBase;
import org.cougaar.mts.base.SendLink;
import org.cougaar.mts.base.SendLinkDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;

/**
 * This Aspect adds sequence numbers to messages, and enforces sequencing based
 * on those numbers.
 */
public class SequenceAspect
        extends StandardAspect {
    private static final String SEQ = "org.cougaar.message.transport.sequencenumber";
    private static final String SEQ_SEND_MAP_ATTR = "org.cougaar.message.transport.sequence.send";
    private static final String SEQ_RECV_MAP_ATTR = "org.cougaar.message.transport.sequence.recv";

    private OutOfBandMessageService oobs;

    @Override
   public Object getDelegate(Object delegate, Class<?> type) {
        if (type == SendLink.class) {
            return new SequencedSendLink((SendLink) delegate);
        } else {
            return null;
        }
    }

    @Override
   public Object getReverseDelegate(Object delegate, Class<?> type) {
        if (type == ReceiveLink.class) {
            return new SequencedReceiveLink((ReceiveLink) delegate);
        } else {
            return null;
        }
    }

    @Override
   public void start() {
        super.start();
        oobs = getServiceBroker().getService(this, OutOfBandMessageService.class, null);
    }

    private int getSequenceNumber(AttributedMessage message) {
        return ((Integer) message.getAttribute(SEQ)).intValue();
    }

    private class SequencedSendLink
            extends SendLinkDelegateImplBase {
        
        private Map<MessageAddress, Integer> sequenceNumbers;

        private SequencedSendLink(SendLink link) {
            super(link);
        }

        // This can't be done in the constructor, since that runs when
        // the client first requests the MessageTransportService (too
        // early). Wait for registration.
        @Override
      public synchronized void registerClient(MessageTransportClient client) {
            super.registerClient(client);

            MessageAddress myAddress = getAddress();
            AgentState myState = getRegistry().getAgentState(myAddress);

            synchronized (myState) {
                @SuppressWarnings("unchecked") // unavoidable
                Map<MessageAddress, Integer> map =
                        (Map<MessageAddress, Integer>) myState.getAttribute(SEQ_SEND_MAP_ATTR);
                if (map != null) {
                    sequenceNumbers = map;
                } else {
                    sequenceNumbers = new HashMap<MessageAddress, Integer>();
                    myState.setAttribute(SEQ_SEND_MAP_ATTR, sequenceNumbers);
                }
            }
        }

        private int nextSeq(AttributedMessage msg) {
            // Verify that msg.getOriginator() == getAddress() ?
            MessageAddress dest = msg.getTarget();
            MessageAddress primary = dest.getPrimary();
            Integer nextInt = sequenceNumbers.get(primary);
            int next = nextInt == null ? 1 : nextInt.intValue();
            sequenceNumbers.put(primary, next + 1);
            return next;
        }

        @Override
      public void sendMessage(AttributedMessage message) {
            int sequenceNumber = nextSeq(message);
            message.setAttribute(SEQ, sequenceNumber);
            super.sendMessage(message);
        }
    }

    private class SequencedReceiveLink
            extends ReceiveLinkDelegateImplBase {
        
        private final Map<MessageAddress, ConversationState> conversationState;

        private SequencedReceiveLink(ReceiveLink link) {
            super(link);
            MessageAddress myAddress = getClient().getMessageAddress();
            AgentState myState = getRegistry().getAgentState(myAddress);
            synchronized (myState) {
                @SuppressWarnings("unchecked") // unavoidable
                Map<MessageAddress, ConversationState> map =
                        (Map<MessageAddress, ConversationState>) myState.getAttribute(SEQ_RECV_MAP_ATTR);
                if (map != null) {
                    conversationState = map;
                } else {
                    conversationState = new HashMap<MessageAddress, ConversationState>();
                    myState.setAttribute(SEQ_RECV_MAP_ATTR, conversationState);
                }
            }
        }

        private void superDeliverMessage(AttributedMessage message) {
            super.deliverMessage(message);
        }

        @Override
      public MessageAttributes deliverMessage(AttributedMessage message) {
            if (oobs != null && oobs.isOutOfBandMessage(message)) {
                // ignore out of band messages
                return super.deliverMessage(message);
            }
            Object seq = message.getAttribute(SEQ);
            if (seq != null) {
                MessageAddress src = message.getOriginator();
                ConversationState conversation = conversationState.get(src);
                if (conversation == null) {
                    conversation = new ConversationState();
                    conversationState.put(src, conversation);
                }
                return conversation.handleNewMessage(message, this);
            } else {
                if (loggingService.isWarnEnabled()) {
                    loggingService.warn("No Sequence tag: " + message);
                }
                return super.deliverMessage(message);
            }

        }
    }

    private class MessageComparator
            implements Comparator<AttributedMessage>, Serializable {
        /**
       * 
       */
      private static final long serialVersionUID = 1L;

      public int compare(AttributedMessage msg1, AttributedMessage msg2) {
            int seq1 = getSequenceNumber(msg1);
            int seq2 = getSequenceNumber(msg2);
            return seq1-seq2;
        }
    }

    private class ConversationState
            implements Serializable {
        
        /**
       * 
       */
      private static final long serialVersionUID = 1L;
      private int nextSeqNum;
        private final TreeSet<AttributedMessage> heldMessages;
        private final Comparator<AttributedMessage> comparator = new MessageComparator();
        
        public ConversationState() {
            nextSeqNum = 1;
            heldMessages = new TreeSet<AttributedMessage>(comparator);
        }

        private void stripAndDeliver(AttributedMessage message, SequencedReceiveLink link) {
            // message.removeAttribute(SEQ);
            link.superDeliverMessage(message);
            nextSeqNum++;
        }

        private MessageAttributes handleNewMessage(AttributedMessage message,
                                                   SequencedReceiveLink link) {
            String destination = link.getClient().getMessageAddress().getAddress();
            String source = message.getOriginator().getAddress();
            MessageAttributes meta = new SimpleMessageAttributes();
            String deliveryStatus = null;
            int msgSeqNum = getSequenceNumber(message);
            if (nextSeqNum > msgSeqNum) {
                // We're already beyond this one -- drop it
                Message contents = message.getRawMessage();
                if (loggingService.isInfoEnabled()) {
                    loggingService.info("Dropping duplicate " + " <"
                            + contents.getClass().getName() + " " + contents.hashCode() + " "
                            + message.getOriginator() + "->" + message.getTarget() + " #"
                            + msgSeqNum);
                }
                deliveryStatus = AttributeConstants.DELIVERY_STATUS_DROPPED_DUPLICATE;
            } else if (nextSeqNum == msgSeqNum) {
                // In sync now -- deliver this message along with any held messages
                // that follow immediately without gaps.
                if (loggingService.isInfoEnabled()) {
                    loggingService.info("Delivered message #" + nextSeqNum
                                        + " from " +source+ " to " +destination);
                }
                stripAndDeliver(message, link);
                Iterator<AttributedMessage> itr = heldMessages.iterator();
                while (itr.hasNext()) {
                    AttributedMessage next = itr.next();
                    if (getSequenceNumber(next) == nextSeqNum) {
                        if (loggingService.isInfoEnabled()) {
                            loggingService.info("Delivered held message #" + nextSeqNum
                                                + " from " +source+ " to " +destination);
                        }
                        stripAndDeliver(next, link);
                        itr.remove();
                    }
                }
                deliveryStatus = AttributeConstants.DELIVERY_STATUS_DELIVERED;
            } else {
                if (loggingService.isInfoEnabled()) {
                    loggingService.info("Holding out of sequence message #" + msgSeqNum
                                        + " from " +source+ " to " +destination);
                }
                heldMessages.add(message);
                deliveryStatus = AttributeConstants.DELIVERY_STATUS_HELD;
            }

            meta.setAttribute(AttributeConstants.DELIVERY_ATTRIBUTE, deliveryStatus);
            return meta;
        }
    }

}
