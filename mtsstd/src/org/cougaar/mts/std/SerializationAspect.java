/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.mts.std;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.SerializationUtils;

import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.UnregisteredNameException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.LoopbackLinkProtocol;
import org.cougaar.mts.base.CougaarIOException;
import org.cougaar.mts.base.StandardAspect;

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
