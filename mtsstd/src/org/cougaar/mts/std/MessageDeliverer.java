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


/**
 * The fifth stop for an outgoing message, and the first on the
 * receive side, is a MessageDeliverer.  In theory a given Message
 * Transport subsystem can have multiple MessageDeliverers.  For this
 * release we only make one, instantiated as a MessageDelivererImpl.
 * Either way, the MessageDeliverers are instantiated by a
 * MessageDelivererFactory.
 *
 * The <strong>deliverMessage</strong> method is used to pass the
 * messages onto the next stop, a ReceiveLink.
 * */

public interface MessageDeliverer
{
    public void deliverMessage(Message message, MessageAddress dest)
	throws MisdeliveredMessageException;
    public boolean matches(String name);

}
