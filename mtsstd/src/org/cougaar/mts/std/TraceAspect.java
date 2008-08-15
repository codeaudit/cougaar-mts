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

import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URI;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.DestinationQueue;
import org.cougaar.mts.base.DestinationQueueDelegateImplBase;
import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MessageDelivererDelegateImplBase;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.NameSupport;
import org.cougaar.mts.base.NameSupportDelegateImplBase;
import org.cougaar.mts.base.ReceiveLink;
import org.cougaar.mts.base.ReceiveLinkDelegateImplBase;
import org.cougaar.mts.base.Router;
import org.cougaar.mts.base.RouterDelegateImplBase;
import org.cougaar.mts.base.SendQueue;
import org.cougaar.mts.base.SendQueueDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.base.UnregisteredNameException;

/**
 * This demonstration Aspect provides a simple trace of a message as it passes
 * through the various stages of the message transport subsystem.
 */
public class TraceAspect
        extends StandardAspect {

    // logging support
    private PrintWriter logStream = null;

    public TraceAspect() {
    }

    private PrintWriter getLog() {
        if (logStream == null) {
            try {
                String id = getRegistry().getIdentifier();
                logStream = new PrintWriter(new FileWriter(id + ".cml"), true);
            } catch (Exception e) {
                if (loggingService.isErrorEnabled()) {
                    loggingService.error("Logging required but not possible - exiting", e);
                }
                System.exit(1);
            }
        }
        return logStream;
    }

    protected void log(String key, String info) {
        String id = getRegistry().getIdentifier();
        String cleanInfo = info.replace('\n', '_');
        getLog().println(id + "\t" + System.currentTimeMillis() + "\t" + key + "\t" + cleanInfo);
    }

    public Object getDelegate(Object delegate, Class<?> type) {
        if (type == SendQueue.class) {
            return new SendQueueDelegate((SendQueue) delegate);
        } else if (type == Router.class) {
            return new RouterDelegate((Router) delegate);
        } else if (type == DestinationQueue.class) {
            return new DestinationQueueDelegate((DestinationQueue) delegate);
        } else if (type == DestinationLink.class) {
            return new DestinationLinkDelegate((DestinationLink) delegate);
        } else if (type == MessageDeliverer.class) {
            return new MessageDelivererDelegate((MessageDeliverer) delegate);
        } else if (type == ReceiveLink.class) {
            return new ReceiveLinkDelegate((ReceiveLink) delegate);
        } else if (type == NameSupport.class) {
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

        public MessageAddress getNodeMessageAddress() {
            return super.getNodeMessageAddress();
        }

        public void registerAgentInNameServer(URI reference, MessageAddress addr, String protocol) {
            log("NameSupport", "Register Agent " + addr + " " + reference);
            super.registerAgentInNameServer(reference, addr, protocol);
        }

        public void unregisterAgentInNameServer(URI reference, MessageAddress addr, String protocol) {
            log("NameSupport", "Unregister Agent " + addr + " " + reference);
            super.unregisterAgentInNameServer(reference, addr, protocol);
        }

        public URI lookupAddressInNameServer(MessageAddress address, String protocol) {
            URI res = super.lookupAddressInNameServer(address, protocol);
            log("NameSupport", "Lookup of " + address + " returned " + res);
            return res;
        }

    }

    public class SendQueueDelegate
            extends SendQueueDelegateImplBase {
        public SendQueueDelegate(SendQueue queue) {
            super(queue);
        }

        public void sendMessage(AttributedMessage message) {
            log("SendQueue", message.toString() + " (" + this.size() + ")");
            super.sendMessage(message);
        }
    }

    public class RouterDelegate
            extends RouterDelegateImplBase {
        public RouterDelegate(Router router) {
            super(router);
        }

        public void routeMessage(AttributedMessage message) {
            log("Router", message.getTarget().toString());
            super.routeMessage(message);
        }

    }

    public class DestinationQueueDelegate
            extends DestinationQueueDelegateImplBase {
        public DestinationQueueDelegate(DestinationQueue queue) {
            super(queue);
        }

        public void holdMessage(AttributedMessage message) {
            log("DestinationQueue", message.toString());
            super.holdMessage(message);
        }

        public void dispatchNextMessage(AttributedMessage message) {
            log("DestinationQueue dispatch", message.toString());
            super.dispatchNextMessage(message);
        }

    }

    public class DestinationLinkDelegate
            extends DestinationLinkDelegateImplBase {
        public DestinationLinkDelegate(DestinationLink link) {
            super(link);
        }

        public MessageAttributes forwardMessage(AttributedMessage message)
                throws UnregisteredNameException, NameLookupException, CommFailureException,
                MisdeliveredMessageException

        {
            log("DestinationLink", message.toString());
            return super.forwardMessage(message);
        }

    }

    public class MessageDelivererDelegate
            extends MessageDelivererDelegateImplBase {
        public MessageDelivererDelegate(MessageDeliverer deliverer) {
            super(deliverer);
        }

        public MessageAttributes deliverMessage(AttributedMessage message, MessageAddress dest)
                throws MisdeliveredMessageException {
            log("MessageDeliverer", message.toString());
            return super.deliverMessage(message, dest);
        }

    }

    public class ReceiveLinkDelegate
            extends ReceiveLinkDelegateImplBase {
        public ReceiveLinkDelegate(ReceiveLink link) {
            super(link);
        }

        public MessageAttributes deliverMessage(AttributedMessage message) {
            log("ReceiveLink", message.toString());
            return super.deliverMessage(message);
        }

    }
}
