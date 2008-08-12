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
package org.cougaar.mts.rmi;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.SerializationUtils;
import org.cougaar.mts.base.CougaarIOException;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.SocketFactory;
import org.cougaar.mts.std.AttributedMessage;

/**
 * Implementation of the {@link SerializedMT} rmi interface. It accepts a
 * byte-array that can be deserialized into an AttributedMessage, rather than an
 * actual AttributedMessage.
 */
public class SerializedMTImpl
        extends MTImpl
        implements SerializedMT {
    public SerializedMTImpl(MessageAddress addr, ServiceBroker sb, SocketFactory socfac) {
        super(addr, sb, socfac);
    }

    public byte[] rerouteMessage(byte[] messageBytes)
            throws MisdeliveredMessageException, CougaarIOException {
        AttributedMessage message = null;
        try {
            message = (AttributedMessage) SerializationUtils.fromByteArray(messageBytes);
        } catch (CougaarIOException mex) {
            throw mex;
        } catch (java.io.IOException deser_ex) {
        } catch (ClassNotFoundException cnf) {
        }

        MessageAttributes meta = super.rerouteMessage(message);

        byte[] result = null;
        try {
            result = SerializationUtils.toByteArray(meta);
        } catch (CougaarIOException mex) {
            throw mex;
        } catch (java.io.IOException ser_ex) {
        }
        return result;
    }

}
