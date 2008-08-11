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

package org.cougaar.mts.std;

import java.net.URI;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.mts.base.NameSupport;
import org.cougaar.mts.base.NameSupportDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;

/**
 * This debugging Aspect logs URIs and stubs of local and remote references,
 * respectively.
 */
public class StubDumperAspect
        extends StandardAspect {

    public StubDumperAspect() {
        super();
    }

    public Object getDelegate(Object delegate, Class type) {
        if (type == NameSupport.class) {
            return new NameSupportDelegate((NameSupport) delegate);
        } else {
            return null;
        }
    }

    public class NameSupportDelegate
            extends NameSupportDelegateImplBase {

        public NameSupportDelegate(NameSupport nameSupport) {
            super(nameSupport);
        }

        public void registerAgentInNameServer(URI reference, MessageAddress address, String protocol) {
            super.registerAgentInNameServer(reference, address, protocol);

            loggingService.info("\nRegistering " + address + " for " + protocol + " = ["
                    + reference + "]");
        }

        public URI lookupAddressInNameServer(MessageAddress address, String protocol) {
            URI result = super.lookupAddressInNameServer(address, protocol);
            loggingService.info("\nLookup " + address + " for " + protocol + " = [" + result + "]");
            return result;
        }

    }

}
