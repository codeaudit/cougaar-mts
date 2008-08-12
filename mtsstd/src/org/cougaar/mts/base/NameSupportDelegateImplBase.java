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

import java.net.URI;
import java.util.Iterator;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MulticastMessageAddress;
import org.cougaar.core.service.wp.Callback;

/**
 * Convenience class for aspects which define {@link NameSupport} delegate
 * classes. It implements all methods of by delegating to another instance,
 * given in the constructor. Aspect inner classes which extend this need only
 * implement specific methods that are relevant to that aspect,
 * 
 */
abstract public class NameSupportDelegateImplBase
        implements NameSupport {
    private final NameSupport nameSupport;

    protected NameSupportDelegateImplBase(NameSupport nameSupport) {
        this.nameSupport = nameSupport;
    }

    public MessageAddress getNodeMessageAddress() {
        return nameSupport.getNodeMessageAddress();
    }

    public void registerAgentInNameServer(URI reference, MessageAddress address, String protocol) {
        nameSupport.registerAgentInNameServer(reference, address, protocol);
    }

    public void unregisterAgentInNameServer(URI reference, MessageAddress address, String protocol) {
        nameSupport.unregisterAgentInNameServer(reference, address, protocol);
    }

    public void lookupAddressInNameServer(MessageAddress address, String protocol, Callback callback) {
        nameSupport.lookupAddressInNameServer(address, protocol, callback);
    }

    public URI lookupAddressInNameServer(MessageAddress address, String protocol) {
        return nameSupport.lookupAddressInNameServer(address, protocol);
    }

    public URI lookupAddressInNameServer(MessageAddress address, String protocol, long timeout) {
        return nameSupport.lookupAddressInNameServer(address, protocol, timeout);
    }

    public Iterator<MessageAddress> lookupMulticast(MulticastMessageAddress address) {
        return nameSupport.lookupMulticast(address);
    }

}
