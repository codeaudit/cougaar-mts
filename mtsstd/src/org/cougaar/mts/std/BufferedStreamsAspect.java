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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectInput;
import java.io.InputStream;
import java.io.ObjectOutput;
import java.io.OutputStream;

public class BufferedStreamsAspect extends StandardAspect
{

    public Object getDelegate(Object delegatee, Class type) {
	if (type == ObjectWriter.class) {
	    ObjectWriter wtr = (ObjectWriter) delegatee;
	    return new BufferedObjectWriter(wtr);
	} else if (type == ObjectReader.class) {
	    ObjectReader rdr = (ObjectReader) delegatee;
	    return new BufferedObjectReader(rdr);
	} else {
	    return null;
	}
    }



    private class BufferedObjectWriter extends ObjectWriterDelegateImplBase
    {

	BufferedObjectWriter(ObjectWriter delegatee) {
	    super(delegatee);
	}

	// Join point
	public OutputStream getObjectOutputStream(ObjectOutput out)
	    throws java.io.IOException
	{
	    OutputStream raw_oos = super.getObjectOutputStream(out);
	    return new BufferedOutputStream(raw_oos);
	}

    }





    private class BufferedObjectReader extends ObjectReaderDelegateImplBase
    {

	BufferedObjectReader(ObjectReader delegatee) {
	    super(delegatee);
	}


	public InputStream getObjectInputStream(ObjectInput in) 
	    throws java.io.IOException, ClassNotFoundException
	{
	    InputStream raw_ois = super.getObjectInputStream(in);
	    return new BufferedInputStream(raw_ois);
	}


    }

}
