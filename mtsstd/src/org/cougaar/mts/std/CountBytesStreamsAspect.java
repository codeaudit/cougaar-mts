/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.PrintStream;

public class CountBytesStreamsAspect extends StandardAspect 
{

    // The name of a local attribute which will be used to store the
    // count.
    private static final String COUNT_ATTR =
	"org.cougaar.core.message.count";



    // Return delegates for MessageReader, MessageWriter and
    // DestinationLink.
    public Object getDelegate(Object delegatee, Class type) {
	if (type == MessageWriter.class) {
	    MessageWriter wtr = (MessageWriter) delegatee;
	    return new CountingMessageWriter(wtr);
	} else if (type == MessageReader.class) {
	    MessageReader rdr = (MessageReader) delegatee;
	    return new CountingMessageReader(rdr);
	} else if (type == DestinationLink.class) {
	    DestinationLink link = (DestinationLink) delegatee;
	    // Only RMI is relevant here
	    Class cls = link.getProtocolClass();
	    if (RMILinkProtocol.class.isAssignableFrom(cls))
		return new BandwidthDestinationLink(link);
	}
	 
	return null;
    }



    // The DestinationLink delegate
    private class BandwidthDestinationLink 
	extends DestinationLinkDelegateImplBase
    {
	BandwidthDestinationLink(DestinationLink delegatee) {
	    super(delegatee);
	}


	public MessageAttributes forwardMessage(AttributedMessage message) 
	    throws NameLookupException, 
		   UnregisteredNameException, 
		   CommFailureException,
		   MisdeliveredMessageException
	{
	    // Register Aspect as a Message Streaming filter
 	    message.addFilter(CountBytesStreamsAspect.this);

	    // Compute the latency and print it along with the cached
	    // byte count (the MessageWriter will do the actual
	    // counting).
	    long start = System.currentTimeMillis();
	    MessageAttributes reply = super.forwardMessage(message);
	    long elapsed = System.currentTimeMillis()-start;
	    Integer Count = (Integer) message.getAttribute(COUNT_ATTR);
	    if (Count != null) {
		System.out.println(" Message from " +message.getOriginator()+
				   " to " +message.getTarget()+
				   " has " +Count+ " bytes and took " 
				   +elapsed+ " ms");
	    }

	    return reply;
	}
    }


    // The MessageWriter delegate.  This will do the byte-counting by
    // creating a simple FilterOutputStream that watches all the bytes
    // go past,
    private class CountingMessageWriter
	extends MessageWriterDelegateImplBase
    {

	private AttributedMessage msg;
	private int count = 0;

	private class CountingOutputStream extends FilterOutputStream {

	    private CountingOutputStream(OutputStream wrapped) {
		super(wrapped);
	    }


	    // Count the bytes, whichever method is used to write
	    // them.  Pass the byte or bytes to 'out' rather than
	    // using super, since the default FilterOutputStream
	    // methods aren't very efficient.

 	    public void write(int b) throws java.io.IOException {
 		out.write(b);
 		++count;
 	    }

 	    public void write(byte[] b, int off, int len)
		throws java.io.IOException 
	    {
 		out.write(b, off, len);
 		count += len;
 	    }


 	    public void write(byte[] b)
		throws java.io.IOException 
	    {
 		out.write(b);
 		count += b.length;
 	    }

	}

	CountingMessageWriter(MessageWriter delegatee) {
	    super(delegatee);
	}



	// Create and return the byte-counting FilterOutputStream
	public OutputStream getObjectOutputStream(ObjectOutput out)
	    throws java.io.IOException
	{
	    OutputStream raw_os = super.getObjectOutputStream(out);
	    return new CountingOutputStream(raw_os);
	}


	// Save the message, since we'll need it later (in
	// postProcess).
	public void finalizeAttributes(AttributedMessage msg) {
	    super.finalizeAttributes(msg);
	    this.msg = msg;
	}


	// Stash the count in the saved message's attributes.  Note
	// that we're doing this after the message has been sent.
	// Even if it weren't a local attribute, the receive would
	// never see it.  But other aspect delegates can get at it. In
	// fact the DestinationLink delegate above does so.
	public void postProcess() 
	{
	    super.postProcess();
	    if (msg != null) msg.setLocalAttribute(COUNT_ATTR, new Integer(count));
	}
    }



    // MessageReader delegate.  In this case it does nothing.
    // Nonetheless it has to be here, since for reasons we don't yet
    // understand, the filtered streams have to match exactly on the
    // reader and writer.
    private class CountingMessageReader
	extends MessageReaderDelegateImplBase
    {

	// Does absolutely nothing but has to be here.
	private class CountingInputStream extends FilterInputStream {

	    private CountingInputStream(InputStream wrapped) {
		super(wrapped);
	    }

	    public int read() throws java.io.IOException {
		return in.read();
	    }

	    public int read(byte[] b, int off, int len) 
		throws java.io.IOException
	    {
		return in.read(b, off, len);
	    }

	    public int read(byte[] b) 
		throws java.io.IOException
	    {
		return in.read(b);
	    }
	}

	CountingMessageReader(MessageReader delegatee) {
	    super(delegatee);
	}

	public InputStream getObjectInputStream(ObjectInput in)
	    throws java.io.IOException, ClassNotFoundException
	{
	    InputStream raw_is = super.getObjectInputStream(in);
	    return new CountingInputStream(raw_is);
	}
    }



}
