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


import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectOutputStream;

import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;


/**
 * For reasons unknown, the compression aspect doesn't work reliably
 * unless its streams are outermost in the nesting.  This implies that
 * it must be the last stream-filtering aspect in the aspect list.
 */
public class CompressingStreamsAspect extends StandardAspect
{

    private static final String LEVEL_ATTR =
	"org.cougaar.core.compression.level";
    public Object getDelegate(Object delegatee, Class type) {
	if (type == MessageWriter.class) {
	    MessageWriter wtr = (MessageWriter) delegatee;
	    return new CompressingMessageWriter(wtr);
	} else if (type == MessageReader.class) {
	    MessageReader rdr = (MessageReader) delegatee;
	    return new CompressingMessageReader(rdr);
	} else if (type == DestinationLink.class) {
	    DestinationLink link = (DestinationLink) delegatee;
	    return new ForceCompression(link);
	} else if (type == MessageDeliverer.class) {
	    MessageDeliverer deliverer = (MessageDeliverer) delegatee;
	    return new TestAttribute(deliverer);
	} else {
	    return null;
	}
    }

    private class TestAttribute extends MessageDelivererDelegateImplBase {
	TestAttribute(MessageDeliverer delegatee) {
	    super(delegatee);
	}

	public MessageAttributes deliverMessage(AttributedMessage message, 
						MessageAddress dest) 
	    throws MisdeliveredMessageException
	{
	    System.out.println("Compression Receiver Test Attribute=" +
			       message.getAttribute("test"));
	    System.out.println("Compression Receiver Level Attribute=" +
			       message.getAttribute(LEVEL_ATTR));
	    return super.deliverMessage(message, dest);
	}
	
    }


    private class ForceCompression extends DestinationLinkDelegateImplBase {
	ForceCompression(DestinationLink delegatee) {
	    super(delegatee);
	}
	
	public MessageAttributes forwardMessage(AttributedMessage message) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException
	{
	    message.addFilter(CompressingStreamsAspect.this);

	    // Delegate the call down stream
	    MessageAttributes result = super.forwardMessage(message);

	    // The "test attribute was set on the message on the
	    // receiver side and returned in the result
	    System.out.println("Compression Sender Result Test Attribute=" +
			       result.getAttribute("test"));

	    // The local attribute was set down stream and is now
	    // available when the call returns
            Object level = message.getAttribute(LEVEL_ATTR);
	    System.out.println("Compression Sender Local Attribute ="
			       + level);
	    return result;


	}
	
    }


    private class CompressingMessageWriter
	extends MessageWriterDelegateImplBase
    {

	private DeflaterOutputStream def_os;
	private Deflater deflater;
	private AttributedMessage msg;

	CompressingMessageWriter(MessageWriter delegatee) {
	    super(delegatee);
	}

	public void finalizeAttributes(AttributedMessage msg) {
	    super.finalizeAttributes(msg);
	    this.msg = msg;
	    // -D flag from shell script
	    String levelstr = System.getProperty("compression-level", "0");
	    int level = Integer.parseInt(levelstr);
	    msg.setLocalAttribute(LEVEL_ATTR, new Integer(level));
	}


	public OutputStream getObjectOutputStream(ObjectOutput out)
	    throws java.io.IOException
	{
	    OutputStream raw_os = super.getObjectOutputStream(out);
	    int level = ((Integer) msg.getAttribute(LEVEL_ATTR)).intValue();
	    deflater = new Deflater(level);
	    def_os = new DeflaterOutputStream(raw_os, deflater);
	    return def_os;
	}

	public void finishOutput() 
	    throws java.io.IOException
	{
	    // For testing purposes, send a "test" string.
	    // Object Stream does not work (why?), so we have to do the
	    // encoding by hand
	    // note for the string length, only 8 bits is sent.
	    String test = "test";
	    def_os.write(test.length());
	    def_os.write(test.getBytes());

	    //Force the the stream to finish its output
	    def_os.finish();

	    // It appears as though finish() has a somewhat nasty
	    // side-effect: no further data can be written to the
	    // stream on which the DeflaterOutputStream was built.
	    // That means we can't write tailers after compression.
	    // The reason for this side-effect remains a mystery.

	    System.out.println("Compressed Output " +deflater.getTotalIn()+
			       " bytes to " +deflater.getTotalOut()+
			       " bytes");

	    super.finishOutput();
	}
    }




    private class CompressingMessageReader
	extends MessageReaderDelegateImplBase
    {
	InflaterInputStream inf_in;
	AttributedMessage msg;

	CompressingMessageReader(MessageReader delegatee) {
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
	    Inflater inflator = new Inflater();
	    inf_in = new InflaterInputStream(raw_is);
	    return inf_in;
	}


	public void finishInput() 
	    throws java.io.IOException
	{
	    // For test purposes, read the test string tailer
	    int count = inf_in.read();
	    byte[] bytes = new byte[count];
	    inf_in.read(bytes);
	    String data = new String(bytes);
	    System.out.println("Compression Receiver Stream Test String="
			       + data);

	    // For test purposes
	    // Set an message attribute with the tailer string
	    msg.setAttribute(data, Boolean.TRUE);
	    super.finishInput();

	}
    }

}
