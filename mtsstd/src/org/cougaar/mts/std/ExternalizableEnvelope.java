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
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;



public class ExternalizableEnvelope 
    extends  Message
    implements Externalizable, MessageProperties
{

    private Message contents;
    private Properties properties;

    // Only invoked by server-side RMI when it's creating one of these
    // to correspond to one that was sent as data.
    public ExternalizableEnvelope() {
    }

    public ExternalizableEnvelope(Message contents) 
    {
	super(contents.getOriginator(), contents.getTarget());
	this.contents = contents;
	properties = new Properties();
	properties.put(FILTERS_PROPERTY, new ArrayList());
    }


    public Message getMessage() {
	return contents;
    }

    public Object getProperty(String property) {
	return properties.get(property);
    }

    public void setProperty(String property, Object value) {
	properties.put(property, value);
    }

    public void removeProperty(String property) {
	properties.remove(property);
    }
	
    public void addValue(String property, Object value) {
	Object old = properties.get(property);
	if (old == null) {
	    ArrayList list = new ArrayList();
	    list.add(value);
	    properties.put(property, list);
	} else if (old instanceof ArrayList) {
	    ((ArrayList) old).add(value);
	} else {
	    ArrayList list = new ArrayList();
	    list.add(old);
	    list.add(value);
	    properties.put(property, list);
	}
    }

    public void removeValue(String property, Object value) {
	Object old = properties.get(property);
	if (old == null) {
	} else if (old instanceof ArrayList) {
	    ((ArrayList) old).remove(value);
	} else if (value.equals(old)) {
	    properties.remove(property);
	}
    }


    public void writeExternal(ObjectOutput out) 
	throws java.io.IOException
    {
	ArrayList aspectNames = (ArrayList) properties.get(FILTERS_PROPERTY);
	ObjectStreamsFactory factory =  ObjectStreamsFactory.getFactory();    
	ObjectWriter writer = factory.getObjectWriter(aspectNames);

	out.writeObject(aspectNames);
	writer.preProcess(out);

	OutputStream os = writer.getObjectOutputStream(out);
	ObjectOutputStream oos = null;
	if (os instanceof ObjectOutputStream)
	    oos = (ObjectOutputStream) os;
	else
	    oos = new ObjectOutputStream(os);
	oos.writeObject(properties);
	oos.writeObject(contents);

	writer.postProcess(out);

    }


    public void readExternal(ObjectInput in) 
	throws java.io.IOException, ClassNotFoundException
    {
	
	ArrayList aspectNames = (ArrayList) in.readObject();
	ObjectStreamsFactory factory =  ObjectStreamsFactory.getFactory();    
	ObjectReader reader = factory.getObjectReader(aspectNames);

	reader.preProcess(in);

	InputStream is = reader.getObjectInputStream(in);
	ObjectInputStream ois = null;
	if (is instanceof ObjectInputStream)
	    ois = (ObjectInputStream) is;
	else
	    ois = new ObjectInputStream(is);
	properties = (Properties) ois.readObject();
	contents = (Message) ois.readObject();
	setOriginator(contents.getOriginator());
	setTarget(contents.getTarget());

	reader.postProcess(in);

    }

    Message getContents() {
	return contents;
    }

}

