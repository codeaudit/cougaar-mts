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
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.ObjectOutput;
import java.io.OutputStream;

import org.cougaar.mts.base.MessageWriter;
import org.cougaar.mts.base.MessageWriterDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;

/**
 * This Aspect caches the serialized message in a byte array as its
 * btyes pass by.
 */
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
