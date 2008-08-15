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

import org.cougaar.core.component.Service;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;

/**
 * The first or second station for an outgoing message on the receive side is a
 * MessageDeliverer (first if no serialization, second otherwise).
 * <p>
 * In theory a given Message Transport subsystem can have multiple
 * MessageDeliverers. For this release we only make one, instantiated as a
 * MessageDelivererImpl. Either way, the MessageDeliverers are instantiated by a
 * MessageDelivererFactory, accessible as the MTS-internal
 * <ff>MessageDeliver</ff> service,
 * 
 * <p>
 * The <strong>deliverMessage</strong> method is used to pass the messages onto
 * the next stop, a ReceiveLink. The LinkProtocol is responsible for calling the
 * MessageDeliverer's deliverMessage after it reaches the destination node.
 * 
 * <p>
 * The previous station is MessageReader if the Java serialization was used or
 * DestinationLink on the sender side otherwise. The next station is
 * ReceiveLink.
 * 
 * @see MessageDelivererFactory
 * @see SendLink
 * @see SendQueue
 * @see Router
 * @see DestinationQueue
 * @see DestinationLink
 * @see MessageWriter
 * @see MessageReader
 * @see ReceiveLink Javadoc contributions from George Mount.
 */

public interface MessageDeliverer
        extends Service {
    /**
     * Called by the LinkProtocol on the receiving side. Chooses the correct
     * ReceiveLink associated with the target address and calls the
     * deliverMessage.
     * 
     * @param message The mesage to be delivered.
     * @param dest The target MessageTransportClient address.
     * @see LinkProtocol
     * @see ReceiveLink#deliverMessage(AttributedMessage)
     */
    MessageAttributes deliverMessage(AttributedMessage message, MessageAddress dest)
            throws MisdeliveredMessageException;

    /**
     * Returns true iff this MessageDeliverer is associated with the given name.
     * Currently there is only one MessageDeliverer per node. Used by
     * MessageDelivererFactory should there be multiple MessageDeliverers.
     * 
     * @param name The MessageDeliverer name to compare against the one
     *        associated with this MessageDeliverer
     */
    boolean matches(String name);

}
