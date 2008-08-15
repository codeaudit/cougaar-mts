/*
 * <copyright>
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.mts.corba;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.SerializationUtils;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.DontRetryException;
import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.corba.idlj.CorbaDontRetryException;
import org.cougaar.mts.corba.idlj.CorbaMisdeliveredMessage;
import org.cougaar.mts.corba.idlj.MTPOA;

/**
 * This is the CORBA servant class for the MT idl interface.
 */
public class MTImpl
        extends MTPOA {
    private final MessageDeliverer deliverer;

    public MTImpl(MessageAddress addr, MessageDeliverer deliverer) {
        super();
        this.deliverer = deliverer;
    }

    private void dontRetryException(DontRetryException mex)
            throws CorbaDontRetryException {
        try {
            byte[] exception = SerializationUtils.toByteArray(mex);
            throw new CorbaDontRetryException(exception);
        } catch (java.io.IOException iox) {
        }

        throw new CorbaDontRetryException();
    }

    public byte[] rerouteMessage(byte[] message_bytes)
            throws CorbaMisdeliveredMessage, CorbaDontRetryException {
        AttributedMessage message = null;
        try {
            message = (AttributedMessage) SerializationUtils.fromByteArray(message_bytes);
        } catch (DontRetryException mex) {
            dontRetryException(mex);
            return null;
        } catch (java.io.IOException iox) {
            return null;
        } catch (ClassNotFoundException cnf) {
            return null;
        }

        MessageAttributes metadata = null;
        try {
            metadata = deliverer.deliverMessage(message, message.getTarget());
        } catch (MisdeliveredMessageException ex) {
            throw new CorbaMisdeliveredMessage();
        }

        byte[] reply_bytes = null;
        try {
            reply_bytes = SerializationUtils.toByteArray(metadata);
        } catch (DontRetryException mex) {
            dontRetryException(mex);
        } catch (java.io.IOException iox) {
        }

        return reply_bytes;

    }

}
