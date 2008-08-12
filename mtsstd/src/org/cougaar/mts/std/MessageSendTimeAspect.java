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

import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.mts.base.SendLink;
import org.cougaar.mts.base.SendLinkDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;

/**
 * Aspect that tags all messages with a "send time" attribute when they enter
 * the MTS SendLink.
 */
public class MessageSendTimeAspect
        extends StandardAspect
        implements AttributeConstants {
    public MessageSendTimeAspect() {
    }

    public Object getDelegate(Object object, Class<?> type) {
        if (type == SendLink.class) {
            return new SendLinkDelegate((SendLink) object);
        } else {
            return null;
        }
    }

    private class SendLinkDelegate
            extends SendLinkDelegateImplBase {
        SendLinkDelegate(SendLink link) {
            super(link);
        }

        public void sendMessage(AttributedMessage message) {
            long now = System.currentTimeMillis();
            message.setAttribute(MESSAGE_SEND_TIME_ATTRIBUTE, new Long(now));
            super.sendMessage(message);
        }
    }
}
