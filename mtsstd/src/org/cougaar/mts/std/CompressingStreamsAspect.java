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

    public Object getDelegate(Object delegatee, Class type) {
	if (type == ObjectWriter.class) {
	    ObjectWriter wtr = (ObjectWriter) delegatee;
	    return new CompressingObjectWriter(wtr);
	} else if (type == ObjectReader.class) {
	    ObjectReader rdr = (ObjectReader) delegatee;
	    return new CompressingObjectReader(rdr);
	} else {
	    return null;
	}
    }


    private class CompressingObjectWriter extends ObjectWriterDelegateImplBase
    {

	private DeflaterOutputStream def_os;
	private Deflater deflater;

	CompressingObjectWriter(ObjectWriter delegatee) {
	    super(delegatee);
	}

	// Join point
	public OutputStream getObjectOutputStream(ObjectOutput out)
	    throws java.io.IOException
	{
	    OutputStream raw_os = super.getObjectOutputStream(out);
	    deflater = new Deflater();
	    def_os = new DeflaterOutputStream(raw_os, deflater);
	    return def_os;
	}

	public void postProcess(ObjectOutput out) 
	    throws java.io.IOException
	{
	    def_os.finish();
	    def_os.flush();
	    super.postProcess(out);
	}



    }





    private class CompressingObjectReader extends ObjectReaderDelegateImplBase
    {

	CompressingObjectReader(ObjectReader delegatee) {
	    super(delegatee);
	}


	public InputStream getObjectInputStream(ObjectInput in) 
	    throws java.io.IOException, ClassNotFoundException
	{
	    InputStream raw_is = super.getObjectInputStream(in);
	    Inflater inflator = new Inflater();
	    return new InflaterInputStream(raw_is);
	}


    }

}
