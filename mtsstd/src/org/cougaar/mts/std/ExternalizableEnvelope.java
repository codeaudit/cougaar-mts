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


import java.io.Externalizable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;


public class ExternalizableEnvelope 
    extends  Message
    implements Externalizable
{
    private Message contents;
    private transient ObjectReader reader;
    private transient ObjectWriter writer;

    public ExternalizableEnvelope() {
	makeStreams();
    }

    public ExternalizableEnvelope(Message contents) 
    {
	super(contents.getOriginator(), contents.getTarget());
	this.contents = contents;
	makeStreams();
    }

    private void makeStreams() {
	ObjectStreamsFactory factory =  ObjectStreamsFactory.getFactory();
	reader = factory.getObjectReader();
	writer = factory.getObjectWriter();
    }


    public void writeExternal(ObjectOutput out) 
	throws java.io.IOException
    {
	writer.preProcess(out);
	if (writer.proceed()) {
	    OutputStream os = writer.getObjectOutputStream(out);
	    ObjectOutputStream oos = null;
	    if (os instanceof ObjectOutputStream)
		oos = (ObjectOutputStream) os;
	    else
		oos = new ObjectOutputStream(os);
	    oos.writeObject(contents);
	    writer.postProcess(out);
	}
    }


    public void readExternal(ObjectInput in) 
	throws java.io.IOException, ClassNotFoundException
    {
	reader.preProcess(in);
	if (reader.proceed()) {
	    InputStream is = reader.getObjectInputStream(in);
	    ObjectInputStream ois = null;
	    if (is instanceof ObjectInputStream)
		ois = (ObjectInputStream) is;
	    else
		ois = new ObjectInputStream(is);
	    contents = (Message) ois.readObject();
	    setOriginator(contents.getOriginator());
	    setTarget(contents.getTarget());
	    reader.postProcess(in);
	}
    }

    Message getContents() {
	return contents;
    }

}

