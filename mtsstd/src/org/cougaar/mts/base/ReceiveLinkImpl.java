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

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.service.LoggingService;

/**
 * The only current implementation of ReceiveLink. The implementation of
 * <strong>deliverMessage</strong> invokes <strong>receiveMessage</strong> on
 * the corresponding MessageTransportClient.
 */
public class ReceiveLinkImpl
        implements ReceiveLink {
    private final MessageTransportClient client;
    private final LoggingService loggingService;

    ReceiveLinkImpl(MessageTransportClient client, ServiceBroker sb) {
        this.client = client;
        loggingService = sb.getService(this, LoggingService.class, null);
    }

    public MessageAttributes deliverMessage(AttributedMessage message) {
        MessageAttributes metadata = new MessageReply(message);
        try {
            client.receiveMessage(message.getRawMessage());
            metadata.setAttribute(AttributeConstants.DELIVERY_ATTRIBUTE,
                                  AttributeConstants.DELIVERY_STATUS_DELIVERED);
            return metadata;
        } catch (Throwable th) {
            if (loggingService.isErrorEnabled()) {
                loggingService.error("MessageTransportClient threw an exception in receiveMessage, not retrying.",
                                     th);
            }
            metadata.setAttribute(AttributeConstants.DELIVERY_ATTRIBUTE,
                                  AttributeConstants.DELIVERY_STATUS_CLIENT_ERROR);
            return metadata;
        }

    }

    public MessageTransportClient getClient() {
        return client;
    }

}
