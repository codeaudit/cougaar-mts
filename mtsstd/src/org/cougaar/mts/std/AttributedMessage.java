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
import java.util.HashMap;
import java.util.StringTokenizer;



/**
 * An AttributedMessage is a Message with metadata, the latter
 * represented as HashMap with String keys.  When a Message enters the
 * MTS it will be wrapped in an AttributedMessage, passed through the
 * MTS in that form, and then unwrapped and delivered at the point of
 * final delivery.  AttributedMessages should not be construed as
 * envelopes.  In particular, it's not generally appropriate to
 * introduce further levels of 'wrapping' beyond the initial one.  If
 * you think you need the contents of an AttributedMessage to be
 * another AttributedMessage, you've probably misunderstood...
 */
public class AttributedMessage 
    extends  Message
    implements Externalizable, MessageAttributes
{

    private Message contents;
    private HashMap attributes;

    /** 
     * Only invoked by server-side RMI when it's creating one of these
     * to correspond to one that was sent as data.  */ 
    public AttributedMessage() { }


    /**
     * Make an AttributedMessage whose content is the given message
     * and whose source and destination are those of the contents.
     * The resulting AttributedMessage will have no attributes.
     */
    public AttributedMessage(Message contents) 
    {
	super(contents.getOriginator(), contents.getTarget());
	this.contents = contents;
	attributes = new HashMap();
	attributes.put(FILTERS_ATTRIBUTE, new ArrayList());
    }


    /**
     * Make an AttributedMessage whose content, source and destination
     * are copied from the argument, and whose initial set of
     * attributes is a snapshot of the argument's current set of
     * attributes.
     */
    public AttributedMessage(AttributedMessage contents) 
    {
	super(contents.getOriginator(), contents.getTarget());
	this.contents = contents.contents;
	attributes = new HashMap();
	attributes.putAll(contents.attributes);
    }


    /**
     * Make an AttributedMessage whose content, source and destination
     * are copied from the first argument, and whose initial set of
     * attributes is a snapshot of the second argument's current set
     * of attributes.
     */
    public AttributedMessage(Message contents,
			     AttributedMessage initialAttributes) 
    {
	super(contents.getOriginator(), contents.getTarget());
	this.contents = contents;
	attributes = new HashMap();
	attributes.putAll(initialAttributes.attributes);
    }


    /**
     * Returns the raw (unattributed) message.
     */
    Message getRawMessage() {
	return contents;
    }





    // MessageAttributes interface

    /**
     * Returns the current value of the given attribute.
     */
    public Object getAttribute(String attribute) {
	return attributes.get(attribute);
    }

    /**
     * Modifies or sets the current value of the given attribute to the
     * given value.
     */
    public void setAttribute(String attribute, Object value) {
	attributes.put(attribute, value);
    }


    /**
     * Removes the given attribute.
     */
    public void removeAttribute(String attribute) {
	attributes.remove(attribute);
    }
	
    /**
     * Add a value to an attribute.  If the current raw value of the
     * attribute is an ArrayList, the new value will be added to it.
     * If the current raw value is not an ArrayList, a new ArrayList
     * will be created and the current value (if non-null) as well as
     * the new value will be added to it.
     */
    public void addValue(String attribute, Object value) {
	Object old = attributes.get(attribute);
	if (old == null) {
	    ArrayList list = new ArrayList();
	    list.add(value);
	    attributes.put(attribute, list);
	} else if (old instanceof ArrayList) {
	    ((ArrayList) old).add(value);
	} else {
	    ArrayList list = new ArrayList();
	    list.add(old);
	    list.add(value);
	    attributes.put(attribute, list);
	}
    }

    /**
     * Remove a value to an attribute.  The current raw value should
     * either be an ArrayList or a singleton equal to the given value.
     */
    public void removeValue(String attribute, Object value) {
	Object old = attributes.get(attribute);
	if (old == null) {
	} else if (old instanceof ArrayList) {
	    ((ArrayList) old).remove(value);
	} else if (value.equals(old)) {
	    attributes.remove(attribute);
	}
    }





    // Externalizable interface

    /**
     * First, write special metadata directly to the output stream.
     * Currently the only special metadata is the list of Aspect
     * classes for the filtering streams.  Next, generate the nested
     * filtering streams, using those Aspects.  Next, allow the Aspect
     * delegates to perform any preprocessing they might need to do.
     * Next, write the raw message and the raw attributes through the
     * nested filtering streams.  Finally, allow the Attributes to do
     * postprocessing.
     */
    public void writeExternal(ObjectOutput rawOut) 
	throws java.io.IOException
    {
	MessageStreamsFactory factory =  MessageStreamsFactory.getFactory();
	ArrayList aspectNames = (ArrayList) attributes.get(FILTERS_ATTRIBUTE);
	MessageWriter writer = factory.getMessageWriter(aspectNames);
	
	writer.finalizeAttributes(this);

	rawOut.writeObject(attributes);

	writer.preProcess();

	OutputStream out = writer.getObjectOutputStream(rawOut);
	ObjectOutputStream object_out = null;
	// 'out' should be an ObjectOutputStream but might just be an
	// OutputStream.  In the latter case, wrap it here.
	if (out instanceof ObjectOutputStream)
	    object_out = (ObjectOutputStream) out;
	else
	    object_out = new ObjectOutputStream(out);


	object_out.writeObject(getOriginator());
	object_out.writeObject(getTarget());
	object_out.writeObject(contents);

	writer.finishOutput();

	writer.postProcess();

    }


    /**
     * First, read special metadata directly from the output stream.
     * Currently the only special metadata is the list of Aspect
     * classes for the filtering streams.  Next, generate the nested
     * filtering streams, using those Aspects.  Next, allow the Aspect
     * delegates to perform any preprocessing they might need to do.
     * Next, read the raw message and the raw attributes through the
     * nested filtering streams.  Finally, allow the Attributes to do
     * postprocessing.
     */
    public void readExternal(ObjectInput rawIn) 
	throws java.io.IOException, ClassNotFoundException
    {
	
	attributes = (HashMap) rawIn.readObject();
	ArrayList aspectNames = (ArrayList) attributes.get(FILTERS_ATTRIBUTE);
	MessageStreamsFactory factory =  MessageStreamsFactory.getFactory();  
	MessageReader reader = factory.getMessageReader(aspectNames);

	reader.finalizeAttributes(this);

	reader.preProcess();

	InputStream in = reader.getObjectInputStream(rawIn);
	ObjectInputStream object_in = null;
	if (in instanceof ObjectInputStream)
	    object_in = (ObjectInputStream) in;
	else
	    object_in = new ObjectInputStream(in);

	setOriginator((MessageAddress) object_in.readObject());
	setTarget((MessageAddress) object_in.readObject());
	contents = (Message) object_in.readObject();

	reader.finishInput();

	reader.postProcess();

    }


}

