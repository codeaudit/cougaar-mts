/*
 * <copyright>
 *  
 *  Copyright 1997-2006 BBNT Solutions, LLC
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
package org.cougaar.mts.rmi;

import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.base.UnregisteredNameException;
import org.cougaar.mts.std.AttributedMessage;

/**
 * Sample Aspect that will disable RMI for 30 seconds (by default) after a
 * failure.
 * 
 */
public class EnableRmiSenderSideAspect
        extends StandardAspect {
    private long failureTimeout;

    public void load() {
        super.load();
        failureTimeout = getParameter("failure-timeout", 30000);
    }

    public Object getDelegate(Object delegatee, Class<?> type) {
        if (type == DestinationLink.class) {
            DestinationLink link = (DestinationLink) delegatee;
            if (link.getProtocolClass() == RMILinkProtocol.class) {
                return new RmiEnableDestinationLink(link);
            }
        }
        return null;
    }

    private class RmiEnableDestinationLink
            extends DestinationLinkDelegateImplBase {
        long last_fail_time = 0;

        RmiEnableDestinationLink(DestinationLink delegatee) {
            super(delegatee);
        }

        public boolean isValid(AttributedMessage message) {
            long now = System.currentTimeMillis();
            if (now - last_fail_time < failureTimeout) {
                return false;
            } else {
                return super.isValid(message);
            }
        }

        public MessageAttributes forwardMessage(AttributedMessage message)
                throws NameLookupException, UnregisteredNameException, CommFailureException,
                MisdeliveredMessageException {
            long now = System.currentTimeMillis();
            try {
                return super.forwardMessage(message);
            } catch (MisdeliveredMessageException ex) {
                last_fail_time = now;
                throw ex;
            } catch (CommFailureException ex) {
                last_fail_time = now;
                throw ex;
            } catch (UnregisteredNameException ex) {
                last_fail_time = now;
                throw ex;
            } catch (NameLookupException ex) {
                last_fail_time = now;
                throw ex;
            }
        }
    }
}
