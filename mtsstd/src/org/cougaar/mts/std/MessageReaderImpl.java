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
import java.io.ObjectInputStream;
import java.io.ObjectInput;

public  class MessageReaderImpl
    implements MessageReader 
{

    static class SimpleObjectInputStream extends ObjectInputStream
    {
	private ObjectInput in;

	SimpleObjectInputStream(ObjectInput in) 
	    throws java.io.IOException
	{
	    this.in = in;
	}

	public int available() 
	    throws java.io.IOException
	{
	    return in.available();
	}

	public void close() 
	    throws java.io.IOException
	{
	    in.close();
	}

	public boolean markSupported() {
	    return false;
	}

	public int read() 
	    throws java.io.IOException 
	{
	    return in.read();
	}

	public int read(byte[] b) 
	    throws java.io.IOException 
	{
	    return in.read(b);
	}

	public int read(byte[] b, int off, int len)
	    throws java.io.IOException
	{
	    return in.read(b, off, len);
	}

	public synchronized void reset() 
	    throws java.io.IOException
	{
	}

	public long skip (long n)
	    throws java.io.IOException
	{
	    return in.skip(n);
	}

    }


    public void finalizeAttributes(AttributedMessage msg) {
    }



    public void preProcess() {
    }


    public InputStream getObjectInputStream(ObjectInput in) 
	throws java.io.IOException, ClassNotFoundException
    {
	if (in instanceof ObjectInputStream) {
	    return (InputStream) in;
	} else {
	    return new SimpleObjectInputStream(in);
	}
    }


    public void finishInput() {
    }


    public void postProcess() {
    }


}

