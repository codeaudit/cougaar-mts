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



/**
 * Simple extension to RMILinkProtocol which uses pre-serialization.
 */
public class SerializedRMILinkProtocol extends RMILinkProtocol
{

    public SerializedRMILinkProtocol() {
	super();
    }


    protected String getProtocolType() {
	return "-SerializedRMI";
    }

    // Is this different?
    protected int computeCost(AttributedMessage message) {
	return super.computeCost(message);
    }

    protected MTImpl makeMTImpl(MessageAddress myAddress,
				MessageDeliverer deliverer,
				SocketFactory socfac)
	throws java.rmi.RemoteException
    {
	return new SerializedMTImpl(myAddress, deliverer, socfac);
    }

    protected MessageAttributes doForwarding(MT remote, 
					     AttributedMessage message) 
	throws MisdeliveredMessageException, java.rmi.RemoteException
    {
	if (remote instanceof SerializedMT) {
	    byte[] messageBytes = null;
	    try {
		messageBytes = SerializationUtils.toByteArray(message);
	    } catch (MessageSecurityException ex) {
		loggingService.error(ex.getMessage(), ex.getException());
		return null;
	    }
	    byte[] res = ((SerializedMT) remote).rerouteMessage(messageBytes);
	    return (MessageAttributes) SerializationUtils.fromByteArray(res);
	} else {
	    return super.doForwarding(remote, message);
	}
    }

}
