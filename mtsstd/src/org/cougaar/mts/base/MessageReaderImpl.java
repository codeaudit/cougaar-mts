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
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;

import org.cougaar.mts.std.AttributedMessage;

/**
 * Default implementatiom of {@link MessageReader} that uses a trivial
 * {@link ObjectInputStream} extension to delegate calls to the
 * original {@link ObjectInput}.
 */
public  class MessageReaderImpl
    implements MessageReader 
{

    static class SimpleObjectInputStream extends ObjectInputStream
    {
	private ObjectInput in;

	SimpleObjectInputStream(ObjectInput in) 
	    throws java.io.IOException
	{
	    this.in = in;
	}

	public int available() 
	    throws java.io.IOException
	{
	    return in.available();
	}

	public void close() 
	    throws java.io.IOException
	{
	    in.close();
	}

	public boolean markSupported() {
	    return false;
	}

	public int read() 
	    throws java.io.IOException 
	{
	    return in.read();
	}

	public int read(byte[] b) 
	    throws java.io.IOException 
	{
	    return in.read(b);
	}

	public int read(byte[] b, int off, int len)
	    throws java.io.IOException
	{
	    return in.read(b, off, len);
	}

	public synchronized void reset() 
	    throws java.io.IOException
	{
	}

	public long skip (long n)
	    throws java.io.IOException
	{
	    return in.skip(n);
	}

    }


    public void finalizeAttributes(AttributedMessage msg) {
    }



    public void preProcess() {
    }


    public InputStream getObjectInputStream(ObjectInput in) 
	throws java.io.IOException, ClassNotFoundException
    {
	if (in instanceof ObjectInputStream) {
	    return (InputStream) in;
	} else {
	    return new SimpleObjectInputStream(in);
	}
    }


    public void finishInput() {
    }


    public void postProcess() {
    }


}

