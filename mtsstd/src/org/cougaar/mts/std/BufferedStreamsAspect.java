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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;

import org.cougaar.mts.base.MessageReader;
import org.cougaar.mts.base.MessageReaderDelegateImplBase;
import org.cougaar.mts.base.MessageWriter;
import org.cougaar.mts.base.MessageWriterDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;

/**
 * This Aspect uses buffered streams in the {@link MessageReader} and
 * {@link MessageWriter}. 
 */
public class BufferedStreamsAspect extends StandardAspect
{

    public Object getDelegate(Object delegatee, Class type) {
	if (type == MessageWriter.class) {
	    MessageWriter wtr = (MessageWriter) delegatee;
	    return new BufferedMessageWriter(wtr);
	} else if (type == MessageReader.class) {
	    MessageReader rdr = (MessageReader) delegatee;
	    return new BufferedMessageReader(rdr);
	} else {
	    return null;
	}
    }



    private class BufferedMessageWriter extends MessageWriterDelegateImplBase
    {

	BufferedMessageWriter(MessageWriter delegatee) {
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





    private class BufferedMessageReader extends MessageReaderDelegateImplBase
    {

	BufferedMessageReader(MessageReader delegatee) {
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
