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

abstract public class SendLinkDelegateImplBase
    implements SendLink
{

    protected SendLink link;

    protected SendLinkDelegateImplBase(SendLink link) {
	this.link = link;
    }

    public void sendMessage(Message message) {
	link.sendMessage(message);
    }

    public void flushMessages(ArrayList droppedMessages) {
	link.flushMessages(droppedMessages);
    }

    public MessageAddress getAddress() {
	return link.getAddress();
    }

    public void release() {
	link.release();
	link = null;
    }

    public boolean okToSend(Message message) {
	return link.okToSend(message);
    }

	
    public void registerClient(MessageTransportClient client) {
	link.registerClient(client);
    }

    public void unregisterClient(MessageTransportClient client) {
	link.unregisterClient(client);
    }

    public String getIdentifier() {
	return link.getIdentifier();
    }

    public boolean addressKnown(MessageAddress address) {
	return link.addressKnown(address);
    }

}

