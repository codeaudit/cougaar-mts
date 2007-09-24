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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.MessageReader;
import org.cougaar.mts.base.MessageReaderDelegateImplBase;
import org.cougaar.mts.base.MessageWriter;
import org.cougaar.mts.base.MessageWriterDelegateImplBase;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.RPCLinkProtocol;
import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.base.UnregisteredNameException;

/**
 * This test Aspect preserializes messages into a byte array, sends
 * the array instead of the message over the RMI stream, and then
 * deserializes on the other end.
 */
public class PreserializingStreamsAspect extends StandardAspect 
{

    // Return delegates for MessageReader, MessageWriter and
    // DestinationLink.
    public Object getDelegate(Object delegatee, Class type) {
	if (type == MessageWriter.class) {
	    MessageWriter wtr = (MessageWriter) delegatee;
	    return new PSMessageWriter(wtr);
	} else if (type == MessageReader.class) {
	    MessageReader rdr = (MessageReader) delegatee;
	    return new PSMessageReader(rdr);
	} else if (type == DestinationLink.class) {
	    DestinationLink link = (DestinationLink) delegatee;
	    // Only RPC is relevant here
	    Class cls = link.getProtocolClass();
	    if (RPCLinkProtocol.class.isAssignableFrom(cls))
		return new PSDestinationLink(link);
	}
	 
	return null;
    }



    // The DestinationLink delegate
    private class PSDestinationLink 
	extends DestinationLinkDelegateImplBase
    {
	PSDestinationLink(DestinationLink delegatee) {
	    super(delegatee);
	}


	public MessageAttributes forwardMessage(AttributedMessage message) 
	    throws NameLookupException, 
		   UnregisteredNameException, 
		   CommFailureException,
		   MisdeliveredMessageException
	{
	    // Register Aspect as a Message Streaming filter
 	    message.addFilter(PreserializingStreamsAspect.this);

	    return super.forwardMessage(message);
	}
    }


    private class PSMessageWriter
	extends MessageWriterDelegateImplBase
    {

	private ByteArrayOutputStream byte_os;
	private OutputStream next;
	private byte[] bytes;

	PSMessageWriter(MessageWriter delegatee) {
	    super(delegatee);
	}



	// Cache the next stream in the chain and return a standalone
	// ByteArrayOutputStream.  Nothing downstream will see any
	// data at all until the byte-stream is closed at
	// finishOutput.
	public OutputStream getObjectOutputStream(ObjectOutput out)
	    throws java.io.IOException
	{
	    next = super.getObjectOutputStream(out);
	    byte_os =  new ByteArrayOutputStream();
	    return byte_os;
	}



	// Done writing to the ByteArrayOutputStream.  Extract the
	// byte array and write it to the next filter.
	public void finishOutput() 
	    throws java.io.IOException
	{
	    byte_os.flush();
	    byte_os.close();
	    bytes = byte_os.toByteArray();

	    System.err.println("Preserialized " +bytes.length+ " bytes");

	    ObjectOutputStream object_out = null;
	    // 'out' should be an ObjectOutputStream but might just be an
	    // OutputStream.  In the latter case, wrap it here.
	    if (next instanceof ObjectOutputStream)
		object_out = (ObjectOutputStream) next;
	    else
		object_out = new ObjectOutputStream(next);
	    
	    object_out.writeObject(bytes);
	    super.finishOutput();
	}

    }



    // MessageReader delegate. 
    private class PSMessageReader
	extends MessageReaderDelegateImplBase
    {


	PSMessageReader(MessageReader delegatee) {
	    super(delegatee);
	}

	// At this point we should get a byte array from the next
	// stream in the chain.  Make a ByteArrayInputStream out of
	// it.  Earlier filters will be reading from that.
	public InputStream getObjectInputStream(ObjectInput in)
	    throws java.io.IOException, ClassNotFoundException
	{
	    InputStream raw_is = super.getObjectInputStream(in);
	    ObjectInputStream object_in = null;
	    if (raw_is instanceof ObjectInputStream)
		object_in = (ObjectInputStream) raw_is;
	    else
		object_in = new ObjectInputStream(raw_is);
	    byte[] bytes = (byte []) object_in.readObject();

// 	    for (int i=0; i<bytes.length; i++) {
// 		System.out.print(' ');
// 		System.out.print(Integer.toHexString(0xFF & bytes[i]));
// 	    }
// 	    System.out.println("");

	    System.err.println("Read " +bytes.length+ " bytes");

	    return new ByteArrayInputStream(bytes);
	}
    }



}
