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


import java.io.*;



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
    private static final String THIS_CLASS =
	"org.cougaar.core.mts.ChecksumStreamsAspect";


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


    void writeLong(OutputStream stream, long x) throws IOException  {
	byte[] bytes = new byte[8];
	for (int i=7; i>=0; i--) {
	    bytes[i] = (byte) (x & 0xFF);
	    x = x >>> 8;
	}
	stream.write(bytes);
    }

    long readLong(InputStream stream) throws IOException {

	byte[] bytes = new byte[8];
	int count = stream.read(bytes);
	long result = 0;
	for (int i=0; i<8; i++) {
	    result = result << 8;
	    // watch out for sign-extension
	    result = result | (((long) bytes[i]) & 0xFF);
	}
	return result;

    }




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
	    message.addValue(MessageAttributes.FILTERS_ATTRIBUTE,
			     THIS_CLASS);
	    // Force on checksum 
	    message.setAttribute(CHECKSUM_ENABLE_ATTR,
				 Boolean.TRUE);

	    return super.forwardMessage(message);
	}
	
    }



    private class ChecksumMessageWriter
	extends MessageWriterDelegateImplBase
    {
	private OutputStream stream;
	private long checksum = 0;

	private class ChecksumOutputStream extends FilterOutputStream {

	    private ChecksumOutputStream(OutputStream wrapped) {
		super(wrapped);
	    }


	    public void write(int b) throws IOException {
		super.write(b);
		checksum += b;
	    }

	}

	ChecksumMessageWriter(MessageWriter delegatee) {
	    super(delegatee);
	}



	public OutputStream getObjectOutputStream(ObjectOutput out)
	    throws java.io.IOException
	{
	    OutputStream raw_os = super.getObjectOutputStream(out);
	    stream = new ChecksumOutputStream(raw_os);
	    return stream;
	}

	public void finishOutput() 
	    throws java.io.IOException
	{
	    //Send the Checksum as a tailer
	    writeLong(stream, checksum);

	    super.finishOutput();


	}
    }




    private class ChecksumMessageReader
	extends MessageReaderDelegateImplBase
    {
	InputStream stream;
	AttributedMessage msg;
	private long checksum = 0;

	private class ChecksumInputStream extends FilterInputStream {

	    private ChecksumInputStream (InputStream wrapped) {
		super(wrapped);
	    }


	    public int read() throws IOException {
		int b = super.read();
		checksum += b;
		return b;
	    }

	    public int read(byte[] b, int off, int len) throws IOException {
		int count = super.read(b, off, len);

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
	    stream = new ChecksumInputStream(raw_is);
	    return stream;
	}

	public void finishInput() 
	    throws java.io.IOException
	{
	    
	    // The tailer itself wasn't included in the checksum
	    // computed by the sender.  So grab the computed value
	    // before reading the remote value.
	    long sum = checksum; 
	    long checksum_as_read = readLong(stream);
// 	    System.out.println("Read checksum=" +  checksum_as_read +
// 			       "  Computed checksum=" + sum);

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
