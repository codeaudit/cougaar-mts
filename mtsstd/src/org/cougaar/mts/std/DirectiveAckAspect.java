/*
 *
 * Copyright 2008 by BBN Technologies Corporation
 *
 */

package org.cougaar.mts.std;

import java.util.HashSet;
import java.util.Set;

import org.cougaar.core.blackboard.DirectiveMessage;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.relay.RelayDirective;
import org.cougaar.core.relay.RelayDirectiveUtil;
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
        oobs = sb.getService(this, OutOfBandMessageService.class, null);
        if (oobs == null) {
            String msg = "DirectAckAspect requires OutOfBandMessageService, which is not available!";
            loggingService.error(msg);
            throw new IllegalStateException(msg);
        }
        NodeIdentificationService nis = 
            sb.getService(this, NodeIdentificationService.class, null);
        nodeAddress = nis.getMessageAddress();
        sb.releaseService(this, NodeIdentificationService.class, nis);
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
     * Whenever a message enters the MTS, keep track of it if it's 
     * a DirectiveMessage at least one of whose Directives is a Relay.
     */
    private class SendQueueDelegate extends SendQueueDelegateImplBase {
        SendQueueDelegate(SendQueue queue) {
            super(queue);
        }
        
        public void sendMessage(AttributedMessage message) {
            if (message.getAttribute(RECEIPT_REQUESTED) != null 
                    && RelayDirectiveUtil.hasRelayDirectives(message.getRawMessage())) {
                outstandingMessages.add(message);
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
                Message receipt = 
                    RelayDirectiveUtil.makeReceiptMessage(nodeAddress, original, reply.cloneAttributes());
                oobs.sendOutOfBandMessage(receipt, null, message.getOriginator());
                outstandingMessages.remove(message);
            }
            return reply;
        }
    }
}
