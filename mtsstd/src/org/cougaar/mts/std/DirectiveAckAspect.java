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
import org.cougaar.core.component.ServiceBroker;
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
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.OutOfBandMessageService;
import org.cougaar.mts.base.SendQueue;
import org.cougaar.mts.base.SendQueueDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.base.UnregisteredNameException;

/**
 * After delivering a {@link DirectiveMessage} with a non-null value for the
 * {@link #RECEIPT_REQUESTED} attribute, try to inject a receipt message back to
 * the sending Agent contain the reply status for each {@link RelayDirective} it
 * contains. Use the directive's <code>reply</code> field to hold the delivery
 * status {@link MessageAttributes}.
 */
public class DirectiveAckAspect 
        extends StandardAspect
        implements AttributeConstants {
    
    private final Set<AttributedMessage> outstandingMessages = new HashSet<AttributedMessage>();
    private MessageAddress nodeAddress;
    private OutOfBandMessageService oobs;
    
    public void start() {
        super.start();
        ServiceBroker sb = getServiceBroker();
        NodeIdentificationService nis = 
            sb.getService(this, NodeIdentificationService.class, null);
        nodeAddress = nis.getMessageAddress();
        sb.releaseService(this, NodeIdentificationService.class, nis);
        oobs = sb.getService(this, OutOfBandMessageService.class, null);
    }
    
    /**
     * The delegate for {@link SendQueue} remembers each DirectiveMessage that
     * requests a receipt. The delegate for {@DestinationLink}
     * checks for successful delivery (ie, no exceptions), and in that case
     * injects a receipt message back to the sending Agent.
     */
    public Object getDelegate(Object delegate, Class<?> type) {
        if (type == SendQueue.class) {
            return new SendQueueDelegate((SendQueue) delegate);
        } else if (type == DestinationLink.class) {
            return new DestinationLinkDelegate((DestinationLink) delegate);
        }
        return null;
    }
    
    /**
     * Return the given Directive as RelayDirective if possible. Otherwise
     * return null. Usually this just involves a downcast. If the given
     * Directive has change reports we need to extract the true Directive it
     * holds and operate on that one instead.
     */
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
    
    /**
     * Make a receipt for the given Directive, using the reply status as the
     * content.
     */
    private Directive makeReceiptDirective(RelayDirective original, MessageAttributes reply) {
       UID requestorUID = original.getUID();
       RelayDirective.Response dir = 
           new RelayDirective.Response(requestorUID, reply.cloneAttributes());
       dir.setSource(original.getDestination());
       dir.setDestination(original.getSource());
       return dir;
    }
    
    /**
     * Whenever a message enters the MTS, keep track of it if it's 
     * a DirectiveMessage at least one of whose Directives is a Relay.
     */
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
    
    /**
     * Whenever a DirectiveMessage we're keeping track of is sent without exceptions,
     * inject a receipt message back to the originator for each RelayDirective
     * it contains.
     */
    private class DestinationLinkDelegate extends DestinationLinkDelegateImplBase {
        DestinationLinkDelegate(DestinationLink link) {
            super(link);
        }
        
        public MessageAttributes forwardMessage(AttributedMessage message) 
                throws UnregisteredNameException,
                NameLookupException,
                CommFailureException,
                MisdeliveredMessageException {
            MessageAttributes reply = super.forwardMessage(message);
            if (outstandingMessages.contains(message)) {
                DirectiveMessage original = (DirectiveMessage) message.getRawMessage();
                Directive[] originalDirectives = original.getDirectives();
                MessageAddress dest = message.getOriginator();
                
                // Construct and collect receipt directives for each RelayDirective
                List<RelayDirective> relevantDirectives = 
                    new ArrayList<RelayDirective>(originalDirectives.length);
                for (Directive directive : originalDirectives) {
                    RelayDirective relayDirective = getRelayDirective(directive);
                    if (relayDirective != null) {
                        relevantDirectives.add(relayDirective);
                    }
                }
                Directive[] receipts = new Directive[relevantDirectives.size()];
                for (int i=0; i<receipts.length; i++) {
                    RelayDirective requestDirective = relevantDirectives.get(i);
                    Directive responseDirective = makeReceiptDirective(requestDirective, reply);
                    receipts[i] = responseDirective;
                }
                
                // Construct and send the receipt message
                long incarnation = original.getIncarnationNumber(); // XXX: Is this right?
                Message receipt = new DirectiveMessage(nodeAddress, dest, incarnation, receipts);
                oobs.sendOutOfBandMessage(receipt, reply, dest);
                
                outstandingMessages.remove(message);
            }
            return reply;
        }
    }
}
