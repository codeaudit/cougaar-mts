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

import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectOutput;

public  class ObjectWriterImpl
    implements ObjectWriter 
{


    static class SimpleObjectOutputStream extends ObjectOutputStream {
	private ObjectOutput out;

	SimpleObjectOutputStream(ObjectOutput out) 
	    throws java.io.IOException
	{
	    this.out = out;
	}

	public void close()
	    throws java.io.IOException
	{
	    out.close();
	}


	public void flush() 
	    throws java.io.IOException
	{
	    out.flush();
	}

	public void write(int b)
	    throws java.io.IOException
	{
	    out.write(b);
	}

	public void write(byte[] b)
	    throws java.io.IOException
	{
	    out.write(b);
	}

	public void write(byte[] b, int off, int len)
	    throws java.io.IOException
	{
	    out.write(b, off, len);
	}


    }



    public OutputStream getObjectOutputStream(ObjectOutput out)
	throws java.io.IOException
    {
	if (out instanceof ObjectOutputStream) {
	    return (OutputStream) out;
	} else {
	    return new SimpleObjectOutputStream(out);
	}
    }

    public void preProcess(ObjectOutput out) {
    }


    public void postProcess(ObjectOutput out) {
    }


}

