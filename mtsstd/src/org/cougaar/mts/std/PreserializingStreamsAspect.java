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


import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;


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
	    // Only RMI is relevant here
	    Class cls = link.getProtocolClass();
	    if (RMILinkProtocol.class.isAssignableFrom(cls))
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
