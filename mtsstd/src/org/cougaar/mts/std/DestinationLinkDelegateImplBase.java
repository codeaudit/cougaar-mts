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


/**
 * Convenience class for aspects which define DestinationLink delegate
 * classes. */
abstract public class DestinationLinkDelegateImplBase
    implements DestinationLink
{
    protected DestinationLink link;

    protected DestinationLinkDelegateImplBase(DestinationLink link) {
	this.link = link;
    }

    public void forwardMessage(Message message) 
	throws UnregisteredNameException, 
	NameLookupException, 
	CommFailureException,
	MisdeliveredMessageException
    {
	link.forwardMessage(message);
    }

    public int cost(Message message) {
	return link.cost(message);
    }

    public Class getProtocolClass() {
	return link.getProtocolClass();
    }

    public boolean retryFailedMessage(Message message, int retryCount) {
	return true;
    }

    public MessageAddress getDestination() {
	return link.getDestination();
    }

}
