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

import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;

import java.util.ArrayList;

abstract public class ServiceProxyDelegateBaseImpl
    implements MessageTransportService
{

    protected MessageTransportService service;

    protected ServiceProxyDelegateBaseImpl(MessageTransportService service) {
	this.service = service;
    }

    public void sendMessage(Message message) {
	service.sendMessage(message);
    }

    public void registerClient(MessageTransportClient client) {
	service.registerClient(client);
    }

    public void unregisterClient(MessageTransportClient client) {
	service.unregisterClient(client);
    }

    public ArrayList flushMessages() {
	return service.flushMessages();
    }

    public String getIdentifier() {
	return service.getIdentifier();
    }

    public boolean addressKnown(MessageAddress address) {
	return service.addressKnown(address);
    }

}

