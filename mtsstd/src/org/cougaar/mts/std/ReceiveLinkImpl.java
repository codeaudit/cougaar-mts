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

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.LoggingService;


/**
 * The only current implementation of ReceiveLink.  The implementation
 * of <strong>deliverMessage</strong> invokes
 * <strong>receiveMessage</strong> on the corresponding
 * MessageTransportClient.  */
public class ReceiveLinkImpl implements ReceiveLink
{
    private MessageTransportClient client;
    private LoggingService loggingService;

    ReceiveLinkImpl(MessageTransportClient client, ServiceBroker sb) {
	this.client = client;
	loggingService = (LoggingService)
	    sb.getService(this, LoggingService.class, null);
    }

    public MessageAttributes deliverMessage(AttributedMessage message)
    {
	MessageAttributes metadata = new MessageReply(message);
	try {
	    client.receiveMessage(message.getRawMessage());
	    metadata.setAttribute(MessageAttributes.DELIVERY_ATTRIBUTE, 
				  MessageAttributes.DELIVERY_STATUS_DELIVERED);
	    return metadata;
	} catch (Throwable th) {
	    if (loggingService.isErrorEnabled())
		loggingService.error("MessageTransportClient threw an exception in receiveMessage, not retrying.", th);
	    metadata.setAttribute(MessageAttributes.DELIVERY_ATTRIBUTE, 
				  MessageAttributes.DELIVERY_STATUS_CLIENT_ERROR);
	    return metadata;
	}

    }


    public MessageTransportClient getClient() {
	return client;
    }

}
