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

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
  
import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;

/** actual RMI remote object providing the implementation of MessageTransport client
 **/

public class MTImpl extends UnicastRemoteObject implements MT 
{
    private MessageAddress address;
    private MessageDeliverer deliverer;


    public MTImpl(MessageAddress addr,  MessageDeliverer deliverer,
		  SocketFactory socfac) 
	throws RemoteException 
    {
	super(0, socfac, socfac);
	address = addr;
	this.deliverer = deliverer;
    }

    public void rerouteMessage(Message message) 
	throws MisdeliveredMessageException
    {
	deliverer.deliverMessage(message, message.getTarget());
    }

    public MessageAddress getMessageAddress() {
	return address;
    }
}
