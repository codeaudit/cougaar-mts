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
	    System.out.println(message.getAttribute("test"));
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
	    message.addValue(MessageAttributes.FILTERS_ATTRIBUTE,
			     CompressingStreamsAspect.class.getName());
	    message.setAttribute(LEVEL_ATTR,
				 new Integer(Deflater.BEST_COMPRESSION));
	    return super.forwardMessage(message);
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
	    this.msg = msg;
	    String levelstr = System.getProperty("compression-level", "0");
	    int level = Integer.parseInt(levelstr);
	    msg.setAttribute(LEVEL_ATTR, new Integer(level));
	}

	// Join point
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
	    String test = "test";
	    def_os.write(test.length());
	    def_os.write(test.getBytes());
	    def_os.finish();
	    def_os.flush();

	    System.out.println("Compressed " +deflater.getTotalIn()+
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
	    int count = inf_in.read();
	    byte[] bytes = new byte[count];
	    inf_in.read(bytes);
	    String data = new String(bytes);
	    msg.setAttribute(data, Boolean.TRUE);
	    System.out.println(data);
	    super.finishInput();
	}
    }

}
