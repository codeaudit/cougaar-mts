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

package org.cougaar.mts.base;

import java.util.ArrayList;
import java.util.List;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.AgentState;
import org.cougaar.core.mts.GroupMessageAddress;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.service.MessageTransportService;

/**
 * Currently the only implementation of MessageTransportService. It does almost
 * nothing by itself - its work is accomplished by redirecting calls to the
 * corresponding {@link SendLink}.
 */
public class MessageTransportServiceProxy
        implements MessageTransportService {
    private final ServiceBroker sb;
    private SendLink link;
    private MessageTransportClient client;
    private boolean registered = false;

    public MessageTransportServiceProxy(MessageTransportClient client, SendLink link,
                                        ServiceBroker sb) {
        this.client = client;
        this.link = link;
        this.sb = sb;
    }

    synchronized long getIncarnationNumber() {
        return client != null ? client.getIncarnationNumber() : 0;
    }

    void release() {
        synchronized (this) {
            if (registered) {
                unregisterClient(client);
            }
            client = null;
        }
        link.release();
        link = null;
    }

    /**
     * Redirects the sendMessage to the SendQueue.
     */
    public void sendMessage(Message rawMessage) {
        MessageAttributes attrs = rawMessage.getTarget().getMessageAttributes();
        AttributedMessage message = new AttributedMessage(rawMessage, attrs);
        if (link.okToSend(message)) {
            link.sendMessage(message);
        }
    }

    /**
     * Wait for all queued messages for our client to be either delivered or
     * dropped.
     * 
     * @return the list of dropped messages, which could be null.
     */
    public synchronized List<Message> flushMessages() {
        List<Message> droppedMessages = new ArrayList<Message>();
        link.flushMessages(droppedMessages);
        List<Message> rawMessages = new ArrayList<Message>(droppedMessages.size());
        for (Message message : droppedMessages) {
            if (message instanceof AttributedMessage) {
                AttributedMessage m = (AttributedMessage) message;
                rawMessages.add(m.getRawMessage());
            } else {
                rawMessages.add(message);
            }
        }
        return rawMessages;
    }

    public AgentState getAgentState() {
        return link.getAgentState();
    }

    /**
     * Redirects the request to the MessageTransportRegistry.
     */
    public synchronized void registerClient(MessageTransportClient client) {
        // Should throw an exception of client != this.client
        if (!registered) {
            link.registerClient(client);
            registered = true;
        } else {
            throw new IllegalStateException("Client " + client.getMessageAddress()
                    + " is already registered in the MTS");
        }
    }

    /**
     * Redirects the request to the MessageTransportRegistry.
     */
    public synchronized void unregisterClient(MessageTransportClient client) {
        // Should throw an exception of client != this.client
        if (registered) {
            link.unregisterClient(client);
            registered = false;
        } else {
            throw new IllegalStateException("Client " + client.getMessageAddress()
                    + " is not registed in the MTS");
        }

        // NB: The proxy (as opposed to the client) CANNOT be
        // unregistered here. If it were, messageDelivered callbacks
        // wouldn't be delivered and flush could block forever.
        // Unregistering the proxy can only happen as part of
        // releasing the service (see release());

    }

    /**
     * Redirects the request to the MessageTransportRegistry.
     */
    public String getIdentifier() {
        return link.getIdentifier();
    }

    /**
     * Redirects the request to the MessageTransportRegistry.
     */
    public boolean addressKnown(MessageAddress a) {
        return link.addressKnown(a);
    }

    // Multicast
    public void joinGroup(MessageTransportClient client, GroupMessageAddress multicastAddress) {
        MessageTransportRegistryService mtrs = 
            sb.getService(this, MessageTransportRegistryService.class, null);
        mtrs.joinGroup(client, multicastAddress);
    }

    public void leaveGroup(MessageTransportClient client, GroupMessageAddress multicastAddress) {
        MessageTransportRegistryService mtrs = 
            sb.getService(this, MessageTransportRegistryService.class, null);
        mtrs.leaveGroup(client, multicastAddress);
    }

}
