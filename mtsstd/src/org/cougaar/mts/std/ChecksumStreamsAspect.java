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
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MessageDelivererDelegateImplBase;
import org.cougaar.mts.base.MessageReader;
import org.cougaar.mts.base.MessageReaderDelegateImplBase;
import org.cougaar.mts.base.MessageWriter;
import org.cougaar.mts.base.MessageWriterDelegateImplBase;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.UnregisteredNameException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.SendLink;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.SendLinkDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;

/**
 * This class provides an example of adding trailers to serialized
 * AttributedMessages.  The Writer computes a checksum (as a long) and
 * sends the eight bytes after the Message content.  The Reader
 * computes its own checksum and compares to the one that was sent.
 * The CHECKSUM_VALID_ATTR records whether or not they matched.  This
 * attributes is stored in the AttributedMessage and is available to
 * all receive-side Aspects.
 */

public class ChecksumStreamsAspect extends StandardAspect 
{

    private static final String CHECKSUM_ENABLE_ATTR =
	"org.cougaar.core.security.checksum.enable";
    private static final String CHECKSUM_VALID_ATTR =
	"org.cougaar.core.security.checksum.valid";


    public Object getDelegate(Object delegatee, Class type) {
	if (type == MessageWriter.class) {
	    MessageWriter wtr = (MessageWriter) delegatee;
	    return new ChecksumMessageWriter(wtr);
	} else if (type == MessageReader.class) {
	    MessageReader rdr = (MessageReader) delegatee;
	    return new ChecksumMessageReader(rdr);
	} else if (type == DestinationLink.class) {
	    DestinationLink link = (DestinationLink) delegatee;
	    return new ForceChecksum(link);
	} else if (type == MessageDeliverer.class) {
	    MessageDeliverer deliverer = (MessageDeliverer) delegatee;
	    return new ViewReceiveAttribute(deliverer);
	} else {
	    return null;
	}
    }


    // Raw i/o of a long.  Not used anymore
//     void writeLong(OutputStream stream, long x) throws IOException  {
// 	byte[] bytes = new byte[8];
// 	for (int i=7; i>=0; i--) {
// 	    bytes[i] = (byte) (x & 0xFF);
// 	    x = x >>> 8;
// 	}
// 	stream.write(bytes);
//     }

//     long readLong(InputStream stream) throws IOException {

// 	byte[] bytes = new byte[8];
// 	int count = stream.read(bytes);
// 	long result = 0;
// 	for (int i=0; i<8; i++) {
// 	    result = result << 8;
// 	    // watch out for sign-extension
// 	    result = result | (((long) bytes[i]) & 0xFF);
// 	}
// 	return result;

//     }




    private class ViewReceiveAttribute 
	extends MessageDelivererDelegateImplBase {
	ViewReceiveAttribute(MessageDeliverer delegatee) {
	    super(delegatee);
	}

	public MessageAttributes deliverMessage(AttributedMessage message, 
						MessageAddress dest) 
	    throws MisdeliveredMessageException
	{

	    System.out.println("Message Checksum Valid = " +
			       message.getAttribute(CHECKSUM_VALID_ATTR));

	    return super.deliverMessage(message, dest);
	}
	
    }


    private class ForceChecksum extends DestinationLinkDelegateImplBase {
	ForceChecksum(DestinationLink delegatee) {
	    super(delegatee);
	}
	
	public MessageAttributes forwardMessage(AttributedMessage message) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException
	{
	    // Register checksum Aspect as a Message Streaming filter
	    message.addFilter(ChecksumStreamsAspect.this);
	    // Force on checksum 
	    message.setAttribute(CHECKSUM_ENABLE_ATTR,
				 Boolean.TRUE);

	    return super.forwardMessage(message);
	}
	
    }



    private class ChecksumMessageWriter
	extends MessageWriterDelegateImplBase
    {
	private ObjectOutputStream stream;
	private long checksum = 0;

	private class ChecksumOutputStream extends FilterOutputStream {

	    private ChecksumOutputStream(OutputStream wrapped) {
		super(wrapped);
	    }


	    public void write(int b) throws IOException {
		super.write(b);
		checksum += b;
	    }


 	    public void write(byte[] b, int off, int len)
		throws java.io.IOException 
	    {
 		out.write(b, off, len);
		int end = Math.min(off+len, b.length);
		for (int i=off; i<end; i++) checksum += b[i];
 	    }


 	    public void write(byte[] b)
		throws java.io.IOException 
	    {
 		out.write(b);
		int end = b.length;
		for (int i=0; i<end; i++) checksum += b[i];
 	    }

	}

	ChecksumMessageWriter(MessageWriter delegatee) {
	    super(delegatee);
	}



	public OutputStream getObjectOutputStream(ObjectOutput out)
	    throws java.io.IOException
	{
	    OutputStream raw_os = super.getObjectOutputStream(out);
	    stream = new ObjectOutputStream(new ChecksumOutputStream(raw_os));
	    return stream;
	}

	public void finishOutput() 
	    throws java.io.IOException
	{
	    //Send the Checksum as a tailer
	    try {
		// writeLong(stream, checksum);
		stream.writeObject(new Long(checksum));
	    } catch (java.io.IOException iox) {
		throw iox;
	    }
	    System.err.println("Checksum output finished");
	    super.finishOutput();


	}
    }




    private class ChecksumMessageReader
	extends MessageReaderDelegateImplBase
    {
	ObjectInputStream stream;
	AttributedMessage msg;
	private long checksum = 0;

	private class ChecksumInputStream extends FilterInputStream {

	    private ChecksumInputStream (InputStream wrapped) {
		super(wrapped);
	    }


	    public int read() throws IOException {
		int b = in.read();
		checksum += b;
		return b;
	    }

	    public int read(byte[] b, int off, int len) throws IOException {
		int count = in.read(b, off, len);

		// Even though these are bytes rather than ints (as in
		// read()), we don't need to worry about sign
		// extension, presumably because the ints as written
		// are also sign extended.  If true, this should
		// ensure that the reader and writer compute the same
		// checksum.  Otherwise, add code for sign extension,
		// but do it everywhere, not just here.
		for (int i=0; i<count; i++) checksum += b[i+off];

		return count;
	    }



	    public int read(byte[] b) throws IOException {
		int count = in.read(b);
		for (int i=0; i<count; i++) checksum += b[i];

		return count;
	    }


	}

	ChecksumMessageReader(MessageReader delegatee) {
	    super(delegatee);
	}

	public void finalizeAttributes(AttributedMessage msg) {
	    super.finalizeAttributes(msg);
	    this.msg = msg;
	}

	public InputStream getObjectInputStream(ObjectInput in) 
	    throws java.io.IOException, ClassNotFoundException
	{
	    InputStream raw_is = super.getObjectInputStream(in);
	    stream = new ObjectInputStream(new ChecksumInputStream(raw_is));
	    return stream;
	}

	public void finishInput() 
	    throws java.io.IOException
	{
	    System.err.println("Entering ChecksumStreamsAspect finishInput");
	    // The tailer itself wasn't included in the checksum
	    // computed by the sender.  So grab the computed value
	    // before reading the remote value.
	    long sum = checksum; 
	    long checksum_as_read = 0; // readLong(stream);
	    try {
		checksum_as_read = ((Long) stream.readObject()).longValue();
	    } catch (ClassNotFoundException cnf) {
		cnf.printStackTrace();
	    }
 	    System.out.println("Read checksum=" +  checksum_as_read +
 			       "  Computed checksum=" + sum);

	    // added Checksum validity to message attributes
	    if  (checksum_as_read ==  sum) 
		msg.setAttribute(CHECKSUM_VALID_ATTR,
				 Boolean.TRUE);
	    else
		msg.setAttribute(CHECKSUM_VALID_ATTR,
				 Boolean.FALSE);

	    super.finishInput();

	}
    }

}
