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

import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.mts.std.AttributedMessage;

/**
 * The final station in the message transport system before an incoming message
 * is delivered to an Agent. One ReceiveLink is instantiated for each
 * MessageTransportClient.
 * <p>
 * The previous station is MessageDeliverer.
 * 
 * @see org.cougaar.core.service.MessageTransportService#registerClient(MessageTransportClient)
 * @see SendLink
 * @see SendQueue
 * @see Router
 * @see DestinationQueue
 * @see DestinationLink
 * @see MessageWriter
 * @see MessageReader
 * @see MessageDeliverer Javadoc contributions from George Mount.
 */
public interface ReceiveLink {
    /**
     * Deliver the message to the final recipient (an Agent). The message is
     * unwrapped by the implementation before calling MessageTransportClient
     * receiveMessage.
     * 
     * @param message The message to be delivered.
     * @see MessageTransportClient#receiveMessage
     */
    MessageAttributes deliverMessage(AttributedMessage message);

    /**
     * Returns the client associated with this ReceiveLink
     */
    MessageTransportClient getClient();

}
