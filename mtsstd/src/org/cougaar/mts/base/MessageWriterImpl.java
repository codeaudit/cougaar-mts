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
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import org.cougaar.mts.std.AttributedMessage;


public  class MessageWriterImpl
    implements MessageWriter 
{


    static class SimpleObjectOutputStream extends ObjectOutputStream {
	private ObjectOutput out;

	SimpleObjectOutputStream(ObjectOutput out) 
	    throws java.io.IOException
	{
	    this.out = out;
	}

	public void close()
	    throws java.io.IOException
	{
	    out.close();
	}


	public void flush() 
	    throws java.io.IOException
	{
	    out.flush();
	}

	public void write(int b)
	    throws java.io.IOException
	{
	    out.write(b);
	}

	public void write(byte[] b)
	    throws java.io.IOException
	{
	    out.write(b);
	}

	public void write(byte[] b, int off, int len)
	    throws java.io.IOException
	{
	    out.write(b, off, len);
	}


    }


    public void finalizeAttributes(AttributedMessage msg) {
    }

    public void preProcess() {
    }


    public OutputStream getObjectOutputStream(ObjectOutput out)
	throws java.io.IOException
    {
	if (out instanceof ObjectOutputStream) {
	    return (OutputStream) out;
	} else {
	    return new SimpleObjectOutputStream(out);
	}
    }

    public void finishOutput() {
    }

    public void postProcess() {
    }


}

