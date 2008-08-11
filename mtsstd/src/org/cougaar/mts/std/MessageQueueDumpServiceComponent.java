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

import org.cougaar.core.component.ParameterizedComponent;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageQueueDumpService;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.node.StateDumpServiceComponent;
import org.cougaar.mts.base.DestinationQueueMonitorService;
import org.cougaar.util.log.Logger;

/**
 * This class and its related service have a very specialized role. They exist
 * solely to allow one external class, the StateDumpService implementation, to
 * dump the current message queues. No other clients can use it. The
 * StateDumpService impl itself can't easily live in mts for security reasons.
 */
public final class MessageQueueDumpServiceComponent
        extends ParameterizedComponent
        implements ServiceProvider {
    private ServiceBroker sb, rootsb;
    private Impl impl;

    public MessageQueueDumpServiceComponent() {
    }

    public void setNodeControlService(NodeControlService ncs) {
        rootsb = ncs == null ? null : ncs.getRootServiceBroker();
    }

    public void load() {
        super.load();
        impl = new Impl(sb);
        rootsb.addService(MessageQueueDumpService.class, this);
    }

    public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
        if (serviceClass == MessageQueueDumpService.class
                && requestor instanceof StateDumpServiceComponent) {
            return impl;
        } else {
            return null;
        }
    }

    public void releaseService(ServiceBroker sb,
                               Object requestor,
                               Class serviceClass,
                               Object service) {
    }

    public final void setServiceBroker(ServiceBroker sb) {
        this.sb = sb;
    }

    private class Impl
            implements MessageQueueDumpService {
        DestinationQueueMonitorService dqms;
        ServiceBroker sb;

        Impl(ServiceBroker sb) {
            this.sb = sb;
        }

        private void dumpMessage(AttributedMessage msg, int i, Logger logger) {
            logger.warn(i + " " + msg.getOriginator() + " " + msg.getTarget() + " "
                    + msg.getAttributesAsString() + " " + msg.getRawMessage()/*
                                                                              * .getClass
                                                                              * (
                                                                              * )
                                                                              * .
                                                                              * getName
                                                                              * (
                                                                              * )
                                                                              */);
        }

        public int dumpQueues(Logger logger) {
            if (dqms == null) {
                dqms = sb.getService(this, DestinationQueueMonitorService.class, null);
                if (dqms == null) {
                    logger.warn("Couldn't get DestinationQueueMonitorService");
                    return 0;
                }
            }

            int count = 0;
            MessageAddress[] addresses = dqms.getDestinations();
            for (MessageAddress address : addresses) {
                AttributedMessage[] msgs = dqms.snapshotQueue(address);
                for (AttributedMessage msg : msgs) {
                    count++;
                    dumpMessage(msg, count, logger);
                }
            }
            return count;
        }

    }
}
