/*
 *
 * Copyright 2008 by BBN Technologies Corporation
 *
 */

package org.cougaar.mts.std;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cougaar.core.blackboard.Directive;
import org.cougaar.core.blackboard.DirectiveMessage;
import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.relay.RelayDirective;
import org.cougaar.core.util.UID;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.MessageTransportRegistryService;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.ReceiveLink;
import org.cougaar.mts.base.SendQueue;
import org.cougaar.mts.base.SendQueueDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.base.UnregisteredNameException;

/**
 * Look for RelayDirectives in DirectiveMessages with the
 * {@link #RECEIPT_REQUESTED} attribute, and try to inject a receipt message
 * back to the sender on delivery.
 * <p>
 * Use the reply field of the Relay to hold the delivery status
 * {@link MessageAttributes}.
 * 
 * <p>
 * TODO: The receipt messages are out-of-band and need to be tagged as such so
 * that other aspects (eg {@link SequenceAspect}) ignore them.
 */
public class DirectiveAckAspect 
        extends StandardAspect
        implements AttributeConstants {
    private final Set<AttributedMessage> outstandingMessages = new HashSet<AttributedMessage>();
    private MessageAddress nodeAddress;
    
    
    public void load() {
        super.load();
        NodeIdentificationService nis = 
            getServiceBroker().getService(this, NodeIdentificationService.class, null);
        nodeAddress = nis.getMessageAddress();
    }
    
    public Object getDelegate(Object delegate, Class<?> type) {
        if (type == SendQueue.class) {
            return new SendQueueDelegate((SendQueue) delegate);
        } else if (type == DestinationLink.class) {
            return new DestinationLinkDelegate((DestinationLink) delegate);
        }
        return null;
    }
    
    private RelayDirective getRelayDirective(Directive directive) {
        Directive candidate;
        if (directive instanceof DirectiveMessage.DirectiveWithChangeReports) {
            candidate = ((DirectiveMessage.DirectiveWithChangeReports) directive).getDirective();
        } else {
            candidate = directive;
        }
        if (candidate instanceof RelayDirective) {
            return (RelayDirective) candidate;
        } else {
            return null;
        }
    }
    
    private Directive makeAck(RelayDirective original, MessageAttributes reply) {
       UID requestorUID = original.getUID();
       RelayDirective.Response dir = 
           new RelayDirective.Response(requestorUID, reply.cloneAttributes());
       dir.setSource(original.getDestination());
       dir.setDestination(original.getSource());
       return dir;
    }
    
    private class SendQueueDelegate extends SendQueueDelegateImplBase {
        SendQueueDelegate(SendQueue queue) {
            super(queue);
        }
        
        public void sendMessage(AttributedMessage message) {
            if (message.getAttribute(RECEIPT_REQUESTED) != null) {
                Message raw = message.getRawMessage();
                if (raw instanceof DirectiveMessage) {
                    DirectiveMessage dmesg = (DirectiveMessage) raw;
                    for (Directive directive : dmesg.getDirectives()) {
                        if (getRelayDirective(directive) != null) {
                            outstandingMessages.add(message);
                            break;
                        }
                    }
                }
            }
            super.sendMessage(message);
        }
    }
    
    private class DestinationLinkDelegate extends DestinationLinkDelegateImplBase {
        public MessageAttributes forwardMessage(AttributedMessage message) 
                throws UnregisteredNameException,
                NameLookupException,
                CommFailureException,
                MisdeliveredMessageException {
            MessageAttributes reply = super.forwardMessage(message);
            if (outstandingMessages.contains(message)) {
                DirectiveMessage original = (DirectiveMessage) message.getRawMessage();
                Directive[] originalDirectives = original.getDirectives();
                List<RelayDirective> relevantDirectives = 
                    new ArrayList<RelayDirective>(originalDirectives.length);
                for (Directive directive : originalDirectives) {
                    RelayDirective relayDirective = getRelayDirective(directive);
                    if (relayDirective != null) {
                        relevantDirectives.add(relayDirective);
                    }
                }
                MessageAddress dest = message.getOriginator();
                
                // TODO: Ensure this is the relevant incarnation number
                long incarnation = original.getIncarnationNumber();
                
                Directive[] acks = new Directive[relevantDirectives.size()];
                for (int i=0; i<acks.length; i++) {
                    RelayDirective requestDirective = relevantDirectives.get(i);
                    Directive responseDirective = makeAck(requestDirective, reply);
                    acks[i] = responseDirective;
                }
                Message ack = new DirectiveMessage(nodeAddress, dest, incarnation, acks);
                AttributedMessage attributedAck = new AttributedMessage(ack);
                MessageTransportRegistryService registry = getRegistry();
                
                synchronized (registry) {
                    // This is locked to prevent the receiver from
                    // unregistering between the lookup and the delivery.
                    // The corresponding unregister lock is on a private
                    // method in MesageTransportRegistry, removeLocalClient.
                    ReceiveLink link = registry.findLocalReceiveLink(dest);
                    if (link != null) {
                        link.deliverMessage(attributedAck);
                    }
                }

                outstandingMessages.remove(message);
            }
            return reply;
        }

        protected DestinationLinkDelegate(DestinationLink link) {
            super(link);
        }
    }

}
