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

import java.util.List;

import org.cougaar.core.mts.AgentState;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageTransportClient;

/**
 * Convenience class for aspects which define {@link SendLink} delegate classes.
 * It implements all methods by delegating to another instance, given in the
 * constructor. Aspect inner classes which extend this need only implement
 * specific methods that are relevant to that aspect,
 * 
 */
abstract public class SendLinkDelegateImplBase
        implements SendLink {

    private SendLink link;

    protected SendLinkDelegateImplBase(SendLink link) {
        this.link = link;
    }

    public void sendMessage(AttributedMessage message) {
        link.sendMessage(message);
    }

    public void flushMessages(List<Message> droppedMessages) {
        link.flushMessages(droppedMessages);
    }

    public MessageAddress getAddress() {
        return link.getAddress();
    }

    public void release() {
        link.release();
        link = null;
    }

    public boolean okToSend(AttributedMessage message) {
        return link.okToSend(message);
    }

    public void registerClient(MessageTransportClient client) {
        link.registerClient(client);
    }

    public void unregisterClient(MessageTransportClient client) {
        link.unregisterClient(client);
    }

    public String getIdentifier() {
        return link.getIdentifier();
    }

    public boolean addressKnown(MessageAddress address) {
        return link.addressKnown(address);
    }

    public AgentState getAgentState() {
        return link.getAgentState();
    }

}
