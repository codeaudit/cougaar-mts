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
	if (type == MessageWriter.class) {
	    MessageWriter wtr = (MessageWriter) delegatee;
	    return new CachingMessageWriter(wtr);
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
	    out.write(b);
	    other.write(b);
	}

	public void write(byte[] b, int off, int len)
	    throws java.io.IOException 
	{
	    out.write(b, off, len);
	    other.write(b, off, len);
	}


	public void write(byte[] b)
	    throws java.io.IOException 
	{
	    out.write(b);
	    other.write(b);
	}

    }




    private class CachingMessageWriter extends MessageWriterDelegateImplBase
    {

	private ByteArrayOutputStream byte_os;
	private byte[] cache;

	CachingMessageWriter(MessageWriter delegatee) {
	    super(delegatee);
	}

	public OutputStream getObjectOutputStream(ObjectOutput out)
	    throws java.io.IOException
	{
	    OutputStream raw_os = super.getObjectOutputStream(out);
	    byte_os = new ByteArrayOutputStream();
	    return new TeeOutputStream(raw_os, byte_os);
	}

	public void finishOutput() 
	    throws java.io.IOException
	{
	    super.finishOutput();
	    byte_os.flush();
	}

	public void postProcess() 
	{
	    super.postProcess();
	    cache = byte_os.toByteArray();
	}

    }






}
