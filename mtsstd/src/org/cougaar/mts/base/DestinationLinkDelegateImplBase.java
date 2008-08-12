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
import org.cougaar.mts.std.AttributedMessage;

/**
 * Convenience class for aspects which define {@link DestinationLink} delegate
 * classes. It implements all methods by delegating to another instance, given
 * in the constructor. Aspect inner classes which extend this need only
 * implement specific methods that are relevant to that aspect,
 * 
 */
abstract public class DestinationLinkDelegateImplBase
        implements DestinationLink {
    private final DestinationLink link;

    protected DestinationLinkDelegateImplBase(DestinationLink link) {
        this.link = link;
    }

    public MessageAttributes forwardMessage(AttributedMessage message)
            throws UnregisteredNameException, NameLookupException, CommFailureException,
            MisdeliveredMessageException {
        return link.forwardMessage(message);
    }

    public boolean isValid(AttributedMessage message) {
        return link.isValid(null);
    }

    public int cost(AttributedMessage message) {
        return link.cost(message);
    }

    public Class<? extends LinkProtocol> getProtocolClass() {
        return link.getProtocolClass();
    }

    public boolean retryFailedMessage(AttributedMessage message, int retryCount) {
        return true;
    }

    public MessageAddress getDestination() {
        return link.getDestination();
    }

    public Object getRemoteReference() {
        return link.getRemoteReference();
    }

    public void addMessageAttributes(MessageAttributes attrs) {
        link.addMessageAttributes(attrs);
    }

}
