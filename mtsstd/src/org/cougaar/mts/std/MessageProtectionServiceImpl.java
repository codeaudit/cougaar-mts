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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.MessageProtectionService;

class MessageProtectionServiceImpl 
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
	return bos.toByteArray();
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
