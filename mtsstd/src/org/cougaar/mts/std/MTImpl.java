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


    public MTImpl(MessageAddress addr,  MessageDeliverer deliverer) 
	throws RemoteException 
    {
	super();
	address = addr;
	this.deliverer = deliverer;
    }

    public void rerouteMessage(Message m) 
	throws MisdeliveredMessageException
    {
	deliverer.deliverMessage(m);
    }

    public MessageAddress getMessageAddress() {
	return address;
    }
}
