/*
 * =====================================================================
 * (c) Copyright 2001  BBNT Solutions, LLC
 * =====================================================================
 */

/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ULTRALOG (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.mts.corba;

import org.cougaar.mts.corba.idlj.*;

import org.cougaar.mts.std.AttributedMessage;
import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.DontRetryException;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.SerializationUtils;


public class MTImpl extends MTPOA
{
    private MessageAddress address;
    private MessageDeliverer deliverer;


    public MTImpl(MessageAddress addr,  MessageDeliverer deliverer) 
    {
	super();
	address = addr;
	this.deliverer = deliverer;
    }


    private void dontRetryException(DontRetryException mex)
	throws CorbaDontRetryException
    {
	try {
	    byte[] exception = SerializationUtils.toByteArray(mex);
	    throw new CorbaDontRetryException(exception);
	} catch  (java.io.IOException iox) {
	}
	
	throw new CorbaDontRetryException();
    }

    public byte[] rerouteMessage(byte[] message_bytes) 
	throws CorbaMisdeliveredMessage, CorbaDontRetryException
    {
	AttributedMessage message = null;
	try {
	    message = (AttributedMessage) 
		SerializationUtils.fromByteArray(message_bytes);
	} catch (DontRetryException mex) {
	    dontRetryException(mex);
	} catch (java.io.IOException iox) {
	} catch (ClassNotFoundException cnf) {
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
