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

package org.cougaar.core.mts;

import java.io.IOException;
import org.cougaar.core.service.LoggingService;

/**
 * This is a debugging aspect.  By attaching it in a single-node
 * society (ie one in which all messages go through the Loopback
 * transport), we can check for issues related to serialization that
 * wouldn't arise otherwise.  */
public class SerializationAspect extends StandardAspect
{

    public SerializationAspect() {
    }
    
    public Object getDelegate(Object object, Class type) 
    {
	if (type == DestinationLink.class) {
	    DestinationLink link = (DestinationLink) object;
	    if (link.getProtocolClass() == LoopbackLinkProtocol.class)
		return new SerializingDestinationLink(link);
	    else
		return null;
	} else {
	    return null;
	}
    }
    

    private class SerializingDestinationLink 
	extends DestinationLinkDelegateImplBase
	
    {
	SerializingDestinationLink(DestinationLink link) {
	    super(link);
	}

	public synchronized MessageAttributes
	    forwardMessage(AttributedMessage message) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException

	{
	    byte[] data = null;
	    AttributedMessage clone = null;
	    try {
		if (loggingService.isInfoEnabled())
		    loggingService.info("Serializing " + message);
		data = SerializationUtils.toByteArray(message);
		if (loggingService.isInfoEnabled())
		    loggingService.info("Serialized " + message);
		if (loggingService.isInfoEnabled())
		    loggingService.info("Deserializing");
		clone = (AttributedMessage) SerializationUtils.fromByteArray(data);
		if (loggingService.isInfoEnabled())
		    loggingService.info("Deserialized as " + clone);
	    } catch (CougaarIOException cougaar_iox) {
		loggingService.error("SerializationAspect", cougaar_iox);
		throw new CommFailureException(cougaar_iox);
	    } catch (java.io.IOException iox) {
		loggingService.error("SerializationAspect", iox);
		return null;
	    } catch (ClassNotFoundException cnf) {
		loggingService.error("SerializationAspect", cnf);
		return null;
	    }

	    return super.forwardMessage(clone);

	}

    }



}
