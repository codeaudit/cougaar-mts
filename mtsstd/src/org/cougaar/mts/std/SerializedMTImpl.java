/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.mts.std;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject; // not used but needed by ANT and build process -- DO NOT REMOVE

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.SerializationUtils;
import org.cougaar.mts.base.CougaarIOException;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.SocketFactory;
import org.cougaar.mts.base.MT;
import org.cougaar.mts.base.MTImpl;

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
	throws MisdeliveredMessageException, CougaarIOException
    {
	AttributedMessage message = null;
	try {
	    message = (AttributedMessage)
		SerializationUtils.fromByteArray(messageBytes);
	} catch (CougaarIOException mex) {
	    throw mex;
	} catch (java.io.IOException deser_ex) {
	} catch (ClassNotFoundException cnf) {
	}

	MessageAttributes meta = super.rerouteMessage(message);

	byte[] result = null;
	try {
	    result = SerializationUtils.toByteArray(meta);
	} catch (CougaarIOException mex) {
	    throw mex;
	} catch (java.io.IOException ser_ex) {
	}
	return result;
    }

}
