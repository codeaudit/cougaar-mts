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

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.MessageSecurityManager;
import org.cougaar.core.node.DummyMessageSecurityManager;
import org.cougaar.core.node.SecureMessage;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MessageDelivererDelegateImplBase;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.base.UnregisteredNameException;

/**
 * This Aspect uses the (obsolete?) {@link MessageSecurityManager} interface to
 * secure message traffic in a simple way.
 * 
 * @property org.cougaar.message.security specifies the implementation class of
 *           the {@link MessageSecurityManager}. If unspecified the dummy
 *           implementation {@link DummyMessageSecurityManager} is used.
 */
public class SecurityAspect
        extends StandardAspect {
    private static final String SECURITY_CLASS_PROPERTY = "org.cougaar.message.security";
    private static MessageSecurityManager msm = null;

    private static synchronized MessageSecurityManager ensure_msm() {
        if (msm != null) {
            return msm;
        }

        String name = SystemProperties.getProperty(SECURITY_CLASS_PROPERTY);
        if (name != null && !name.equals("") && !name.equals("none")) {
            try {
                // Object raw = Beans.instantiate(null, name);
                Object raw = Class.forName(name).newInstance();
                msm = (MessageSecurityManager) raw;
            } catch (Exception ex) {
            }
        } else {
            msm = new DummyMessageSecurityManager();
        }
        return msm;
    }

    private boolean enabled = false;

    public SecurityAspect() {
        enabled = ensure_msm() != null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    // Temporarily package access, rather than private, until we get
    // rid of MessageTransportClassic
    AttributedMessage secure(AttributedMessage message) {
        if (msm != null) {
            if (loggingService.isDebugEnabled()) {
                loggingService.debug("Securing message " + message);
            }
            Message rawMessage = message.getRawMessage();
            Message secureMsg = msm.secureMessage(rawMessage);
            return new AttributedMessage(secureMsg, message);
        } else {
            return message;
        }
    }

    // Temporarily package access, rather than private, until we get
    // rid of MessageTransportClassic
    AttributedMessage unsecure(AttributedMessage message) {
        if (msm == null) {
            if (loggingService.isErrorEnabled()) {
                loggingService.error("MessageTransport " + this + " received SecureMessage "
                        + message + " but has no MessageSecurityManager.");
            }
            return null;
        } else {
            if (loggingService.isDebugEnabled()) {
                loggingService.debug("Unsecuring message " + message);
            }
            SecureMessage rawMessage = (SecureMessage) message.getRawMessage();
            Message originalMessage = msm.unsecureMessage(rawMessage);
            AttributedMessage msg = new AttributedMessage(originalMessage, message);
            if (msg == null && loggingService.isErrorEnabled()) {
                loggingService.error("MessageTransport " + this
                        + " received an unverifiable SecureMessage " + message);
            }
            return msg;
        }
    }

    public Object getDelegate(Object delegate, Class type) {
        if (type == DestinationLink.class) {
            DestinationLink link = (DestinationLink) delegate;
            return new SecureDestinationLink(link);
        } else {
            return null;
        }
    }

    public Object getReverseDelegate(Object delegate, Class type) {
        if (type == MessageDeliverer.class) {
            return new SecureDeliverer((MessageDeliverer) delegate);
        } else {
            return null;
        }
    }

    private class SecureDestinationLink
            extends DestinationLinkDelegateImplBase {
        private SecureDestinationLink(DestinationLink link) {
            super(link);
        }

        public MessageAttributes forwardMessage(AttributedMessage message)
                throws UnregisteredNameException, NameLookupException, CommFailureException,
                MisdeliveredMessageException {
            return super.forwardMessage(secure(message));
        }

    }

    private class SecureDeliverer
            extends MessageDelivererDelegateImplBase {
        private SecureDeliverer(MessageDeliverer deliverer) {
            super(deliverer);
        }

        public MessageAttributes deliverMessage(AttributedMessage m, MessageAddress dest)
                throws MisdeliveredMessageException {
            return super.deliverMessage(unsecure(m), dest);
        }

    }
}
