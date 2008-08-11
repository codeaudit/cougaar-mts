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

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.LinkProtocol;
import org.cougaar.mts.base.MisdeliveredMessageException;

/**
 * This {@link LinkProtocol} is purely a debugging aid. It fails by design,
 * throwing a MisdeliveredMessageException, after serializing.
 */
class FutileSerializingLinkProtocol
        extends LinkProtocol

{

    private final HashMap links;

    public FutileSerializingLinkProtocol() {
        super();
        links = new HashMap();
    }

    public synchronized DestinationLink getDestinationLink(MessageAddress address) {
        DestinationLink link = (DestinationLink) links.get(address);
        if (link == null) {
            link = new Link(address);
            link = attachAspects(link, DestinationLink.class);
            links.put(address, link);
        }
        return link;
    }

    public void registerClient(MessageTransportClient client) {
        // Does nothing because the Database of local clients is held
        // by MessageTransportServerImpl
    }

    public void unregisterClient(MessageTransportClient client) {
        // Does nothing because the Database of local clients is held
        // by MessageTransportServerImpl
    }

    public boolean addressKnown(MessageAddress address) {
        // we know everybody
        return true;
    }

    class Link
            implements DestinationLink {
        private AttributedMessage lastMessage;
        private final MessageAddress address;
        private int count;

        Link(MessageAddress address) {
            this.address = address;
        }

        public MessageAddress getDestination() {
            return address;
        }

        public boolean isValid(AttributedMessage message) {
            return true;
        }

        public int cost(AttributedMessage msg) {
            if (lastMessage != msg) {
                lastMessage = msg;
                count = 1;
                return 400;
            } else if (count < 3) {
                count++;
                return 400;
            } else {
                lastMessage = null;
                return Integer.MAX_VALUE;
            }
        }

        private void serialize(AttributedMessage message) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = null;

            try {
                oos = new ObjectOutputStream(baos);
                oos.writeObject(message);
            } catch (java.io.IOException ioe) {
                if (loggingService.isErrorEnabled()) {
                    loggingService.error(null, ioe);
                }
                return;
            }

            try {
                oos.close();
            } catch (java.io.IOException ioe2) {
                if (loggingService.isErrorEnabled()) {
                    loggingService.error(null, ioe2);
                }
            }

            if (loggingService.isInfoEnabled()) {
                loggingService.info("Serialized " + message);
            }
        }

        public MessageAttributes forwardMessage(AttributedMessage message)
                throws MisdeliveredMessageException {
            serialize(message);
            throw new MisdeliveredMessageException(message);
        }

        public boolean retryFailedMessage(AttributedMessage message, int retryCount) {
            return true;
        }

        public Class getProtocolClass() {
            return FutileSerializingLinkProtocol.class;
        }

        public Object getRemoteReference() {
            return null;
        }

        public void addMessageAttributes(MessageAttributes attrs) {

        }

    }

}
