/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.mts;

import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;

class MulticastMessageEnvelope extends Message
{
    
    private Message contents;

    MulticastMessageEnvelope(Message message, MessageAddress destination) {
	super(message.getOriginator(), destination);
	this.contents = message;
    }

    Message getContents() {
	return contents;
    }
    
}
