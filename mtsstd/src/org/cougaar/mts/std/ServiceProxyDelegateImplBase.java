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

public class ServiceProxyDelegateImplBase
    implements MessageTransportServiceDelegate
{

    private SendQueue sendQ;
    private MessageAddress addr;
    protected MessageTransportServiceDelegate delegate;

    ServiceProxyDelegateImplBase(SendQueue sendQ, MessageAddress addr) {
	this.sendQ = sendQ;
	this.addr = addr;
    }

    protected ServiceProxyDelegateImplBase(MessageTransportServiceDelegate d) {
	this.delegate = d;
    }

    public void sendMessage(Message message) {
	if (delegate != null) 
	    delegate.sendMessage(message);
	else
	    sendQ.sendMessage(message);
    }

    public ArrayList flushMessages() {
	if (delegate != null) 
	    return delegate.flushMessages();
	else
	    return null;
    }

    public MessageAddress getAddress() {
	if (delegate != null)
	    return delegate.getAddress();
	else
	    return addr;
    }

    public void release() {
	if (delegate != null) delegate.release();
	sendQ = null;
    }

    public boolean okToSend(Message message) {
	MessageAddress target = message.getTarget();
	// message is ok as long as the target is not empty or null
	if (delegate != null) {
	    return delegate.okToSend(message);
	} else if (target == null || target.toString().equals("")) {
	    System.err.println("**** Malformed message: "+message);
	    Thread.dumpStack();
	    return false;
	} else {
	    return true;
	}
    }

	

}

