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
 * The only current implementation of ReceiveLink.  The implementation
 * of <strong>deliverMessage</strong> invokes
 * <strong>receiveMessage</strong> on the corresponding
 * MessageTransportClient.  */
public class ReceiveLinkImpl implements ReceiveLink
{
    MessageTransportClient client;

    ReceiveLinkImpl( MessageTransportClient client) {
	this.client = client;
    }

    public void deliverMessage(Message message)
    {
	client.receiveMessage(message);
    }

}
