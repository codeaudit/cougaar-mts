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

import java.rmi.RemoteException;
  

/** actual RMI remote object providing the implementation of MessageTransport client
 **/

public class SerializedMTImpl extends MTImpl
    implements SerializedMT 
{
    public SerializedMTImpl(MessageAddress addr,  
			    ServiceBroker sb,
			    SocketFactory socfac) 
	throws RemoteException 
    {
	super(addr, sb, socfac);
    }

    public byte[] rerouteMessage(byte[] messageBytes) 
	throws MisdeliveredMessageException, DontRetryException
    {
	AttributedMessage message = null;
	try {
	    message = (AttributedMessage)
		SerializationUtils.fromByteArray(messageBytes);
	} catch (DontRetryException mex) {
	    throw mex;
	} catch (java.io.IOException deser_ex) {
	} catch (ClassNotFoundException cnf) {
	}

	MessageAttributes meta = super.rerouteMessage(message);

	byte[] result = null;
	try {
	    result = SerializationUtils.toByteArray(meta);
	} catch (DontRetryException mex) {
	    throw mex;
	} catch (java.io.IOException ser_ex) {
	}
	return result;
    }

}
