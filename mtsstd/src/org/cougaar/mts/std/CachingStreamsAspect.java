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

import java.io.ObjectInput;
import java.io.InputStream;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.ObjectOutputStream;

public class CachingStreamsAspect extends StandardAspect
{

    public Object getDelegate(Object delegatee, Class type) {
	if (type == ObjectWriter.class) {
	    ObjectWriter wtr = (ObjectWriter) delegatee;
	    return new CachingObjectWriter(wtr);
	} else if (type == ObjectReader.class) {
	    ObjectReader rdr = (ObjectReader) delegatee;
	    return new CachingObjectReader(rdr);
	} else {
	    return null;
	}
    }



    static class TeeOutputStream extends FilterOutputStream {
	OutputStream other;

	TeeOutputStream(OutputStream stream1, OutputStream stream2) {
	    super(stream1);
	    other = stream2;
	}


	public void write(int b)
	    throws java.io.IOException
	{
	    super.write(b);
	    other.write(b);
	}
    }




    private class CachingObjectWriter extends ObjectWriterDelegateImplBase
    {

	private ByteArrayOutputStream byte_os;
	private byte[] cache;

	CachingObjectWriter(ObjectWriter delegatee) {
	    super(delegatee);
	}

	public OutputStream getObjectOutputStream(ObjectOutput out)
	    throws java.io.IOException
	{
	    OutputStream raw_os = super.getObjectOutputStream(out);
	    byte_os = new ByteArrayOutputStream();
	    return new TeeOutputStream(raw_os, byte_os);
	}

	public boolean proceed() {
	    return cache == null;
	}

	public void preProcess(ObjectOutput out) 
	    throws java.io.IOException
	{
	    out.writeObject(cache);
	    if (cache != null) System.err.println("Sending cache " + cache);
	    super.preProcess(out);
	}


	public void postProcess(ObjectOutput out) 
	    throws java.io.IOException
	{
	    byte_os.flush();
	    cache = byte_os.toByteArray();
	    super.postProcess(out);
	}

    }



    private class CachingObjectReader extends ObjectReaderDelegateImplBase
    {
	Object cache;

	CachingObjectReader(ObjectReader delegatee) {
	    super(delegatee);
	}

	public InputStream getObjectInputStream(ObjectInput in) 
	    throws java.io.IOException, ClassNotFoundException
	{
	    InputStream raw_is = super.getObjectInputStream(in);
	    if (cache != null) {
		System.err.println("Received cache " + cache);
		return new ByteArrayInputStream((byte[]) cache);
	    } else {
		return raw_is;
	    }
	}

	public void preProcess(ObjectInput in) 
	    throws java.io.IOException, ClassNotFoundException
	{
	    cache = in.readObject();
	    super.preProcess(in);
	}


    }



}
