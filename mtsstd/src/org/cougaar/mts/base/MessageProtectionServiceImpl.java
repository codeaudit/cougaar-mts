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

package org.cougaar.mts.base;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.ProtectedInputStream;
import org.cougaar.core.mts.ProtectedOutputStream;
import org.cougaar.core.service.MessageProtectionService;

public class MessageProtectionServiceImpl 
    implements MessageProtectionService, ServiceProvider
{
    

    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	if (serviceClass == MessageProtectionService.class) {
	    return this;
	} else {
	    return null;
	}
    }

    public void releaseService(ServiceBroker sb, 
			       Object requestor, 
			       Class serviceClass, 
			       Object service)
    {
    }

    public byte[] protectHeader(MessageAttributes attributes, 
				MessageAddress source,
				MessageAddress destination)
	throws java.security.GeneralSecurityException, java.io.IOException
    {
	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	ObjectOutputStream oos = new ObjectOutputStream(bos);
	oos.writeObject(attributes);
	oos.close();
	byte[] header= bos.toByteArray();
	return header;
	// For testing security exception handling
	// throw new java.security.GeneralSecurityException("protectHeader");
    }


    public MessageAttributes unprotectHeader(byte[] rawData, 
					     MessageAddress source,
					     MessageAddress destination)
	throws java.security.GeneralSecurityException, java.io.IOException
    {
	MessageAttributes attributes = null;

	ByteArrayInputStream bis = new ByteArrayInputStream(rawData);
	ObjectInputStream ois = new ObjectInputStream(bis);
	try {
	    attributes = (MessageAttributes) ois.readObject();
	} catch (ClassNotFoundException cnf) {
	    // ???
	}
	ois.close();
	return attributes;
	// For testing security exception handling
	// throw new java.security.GeneralSecurityException("unprotectHeader");
    }


    public ProtectedOutputStream getOutputStream(OutputStream os,
						 MessageAddress src,
						 MessageAddress dst,
						 MessageAttributes attrs)
    {
	return new DummyOutputStream(os);
    }

    public ProtectedInputStream getInputStream(InputStream is,
					       MessageAddress src,
					       MessageAddress dst,
					       MessageAttributes attrs)
    {
	return new DummyInputStream(is);
    }



    private class DummyOutputStream 
	extends ProtectedOutputStream
    {
	DummyOutputStream(OutputStream stream) {
	    super(stream);
	}


	public void finishOutput(MessageAttributes attr) 
	    throws java.io.IOException
	{
	}
    }


    private class DummyInputStream 
	extends  ProtectedInputStream
    {
	DummyInputStream(InputStream stream) {
	    super(stream);
	}

	public void finishInput(MessageAttributes attr) 
	    throws java.io.IOException
	{
	}

    }


}
