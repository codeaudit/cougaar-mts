/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

package org.cougaar.core.mts;

import org.cougaar.core.service.*;

import org.cougaar.core.node.*;

import org.cougaar.core.mts.Message;


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
	try {
	    client.receiveMessage(message);
	} catch (Throwable th) {
	    System.err.println("% MessageTransportClient threw an exception in receiveMessage: " + th);
	    System.err.println("% Not retrying " + message);
	    th.printStackTrace();
	}

    }


    public MessageTransportClient getClient() {
	return client;
    }

}
