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


import java.io.FilterOutputStream;
import java.io.FilterInputStream;
import java.io.ObjectInput;
import java.io.InputStream;
import java.io.ObjectOutput;
import java.io.OutputStream;


public class CountBytesStreamsAspect extends StandardAspect 
{

    private static final String COUNT_ATTR =
	"org.cougaar.core.message.count";
    private static final String THIS_CLASS =
	"org.cougaar.core.mts.CountBytesStreamsAspect";


    public Object getDelegate(Object delegatee, Class type) {
	if (type == MessageWriter.class) {
	    MessageWriter wtr = (MessageWriter) delegatee;
	    return new CountingMessageWriter(wtr);
	} else if (type == MessageReader.class) {
	    MessageReader rdr = (MessageReader) delegatee;
	    return new CountingMessageReader(rdr);
	} else if (type == DestinationLink.class) {
	    DestinationLink link = (DestinationLink) delegatee;
	    Class cls = link.getProtocolClass();
	    if (RMILinkProtocol.class.isAssignableFrom(cls))
		return new BandwidthDestinationLink(link);
	}
	 
	return null;
    }



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
 	    message.addValue(MessageAttributes.FILTERS_ATTRIBUTE,
 			     THIS_CLASS);

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


    private class CountingMessageWriter
	extends MessageWriterDelegateImplBase
    {

	private AttributedMessage msg;
	private int count = 0;

	private class CountingOutputStream extends FilterOutputStream {

	    private CountingOutputStream(OutputStream wrapped) {
		super(wrapped);
	    }


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



	public OutputStream getObjectOutputStream(ObjectOutput out)
	    throws java.io.IOException
	{
	    OutputStream raw_os = super.getObjectOutputStream(out);
	    return new CountingOutputStream(raw_os);
	}

	public void finalizeAttributes(AttributedMessage msg) {
	    super.finalizeAttributes(msg);
	    this.msg = msg;
	}

	public void postProcess() 
	{
	    super.postProcess();
	    if (msg != null) msg.setAttribute(COUNT_ATTR, new Integer(count));
	}
    }


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
