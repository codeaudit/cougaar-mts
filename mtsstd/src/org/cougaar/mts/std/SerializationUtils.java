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

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;


public class SerializationUtils
{

    public static byte[] toByteArray(Object data) {
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	ObjectOutputStream oos = null;

	try {
	    oos = new ObjectOutputStream(baos);
	    oos.writeObject(data);
	} catch (IOException ioe) {
	    return null;
	}

	try {
	    oos.close();
	} catch (IOException ioe2) {
	}

	return baos.toByteArray();
    }


    public static Object fromByteArray(byte[] data) {
	ByteArrayInputStream bais = new ByteArrayInputStream(data);
	ObjectInputStream ois = null;
	Object udata = null;

	try {
	    ois = new ObjectInputStream(bais);
	    udata = ois.readObject();
	} catch (IOException ioe) {
	    return null;
	} catch (ClassNotFoundException cnf) {
	    return null;
	}
	
	try {
	    ois.close();
	} catch (IOException ioe2) {
	}

	return udata;
    }

}
