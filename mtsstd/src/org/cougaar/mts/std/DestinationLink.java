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
 * The fourth stop for outgoing messages. Each transport has its own
 * implementation class.  DestinationLinks are made directly by the
 * transports, without the use of factories.  */
public interface DestinationLink
{

    /**
     * This method is used to request the associated transport to do
     * its thing with the given message.  Ordinarily only called by a
     * LinkSender.  */
    public void forwardMessage(Message message) 
	throws UnregisteredNameException, 
	       NameLookupException, 
	       CommFailureException;

    /**
     * This method returns a simple measure of the cost of sending the
     * given message via the associated transport. Ordinarily only
     * called by a LinkSender. */
    public int cost(Message message);
}
