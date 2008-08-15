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

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;

/**
 * The default, and for now only, implementation of {@link MessageDeliverer}.
 * The implementation of <strong>deliverMessage</strong> forwards each message
 * on to the appropriate DestinationLink.
 */
public class MessageDelivererImpl
        implements MessageDeliverer {
    public MessageTransportRegistryService registry;
    public String name;

    public MessageDelivererImpl(String name, MessageTransportRegistryService registry) {
        this.registry = registry;
        this.name = name;
    }

    public boolean matches(String name) {
        return this.name.equals(name);
    }

    /**
     * Forward the message on to the appropriate ReceiveLink, or links in the
     * case of a MulticastMessageAddress. The lookup is handled by the
     * MessageTransportRegistry.
     */
    public MessageAttributes deliverMessage(AttributedMessage message, MessageAddress addr)
            throws MisdeliveredMessageException {
        if (message == null) {
            return null;
        }
        synchronized (registry) {
            // This is locked to prevent the receiver from
            // unregistering between the lookup and the delivery.
            // The corresponding unregister lock is on a private
            // method in MesageTransportRegistry, removeLocalClient.
            ReceiveLink link = registry.findLocalReceiveLink(addr);
            if (link != null) {
                return link.deliverMessage(message);
            }
        }
        throw new MisdeliveredMessageException(message);
    }

}
