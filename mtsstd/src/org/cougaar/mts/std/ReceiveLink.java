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


/**
 * The final stop in the message transport system before an incoming
 * message is delivered to an Agent.  One ReceiveLink is instantiated
 * for each MessageTransportClient.
 *
 */
public interface ReceiveLink
{
    /** Deliver the message to the final recipient (an Agent). */
    public void deliverMessage(Message message);

}
