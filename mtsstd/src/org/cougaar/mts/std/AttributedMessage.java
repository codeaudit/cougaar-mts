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

import org.cougaar.core.service.MessageProtectionService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
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

    private String FILTERS_ATTRIBUTE = "Filters";

    private Message contents;
    private SimpleMessageAttributes attributes;

    // We control the externalization, so the 'transient' tag here is
    // really just documentation.
    private transient SimpleMessageAttributes snapshot;


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
	attributes = new SimpleMessageAttributes();
	attributes.setAttribute(FILTERS_ATTRIBUTE, new ArrayList());
    }


    /**
     * Make an AttributedMessage whose content, source and destination
     * are copied from the argument, and whose initial set of
     * attributes is a snapshot of the argument's current set of
     * attributes.
     */
    public AttributedMessage(AttributedMessage msg) 
    {
	super(msg.getOriginator(), msg.getTarget());
	this.contents = msg.contents;
	attributes = new SimpleMessageAttributes(msg.attributes);
    }


    
    // Should only be used by MessageReply.  The second argument is
    // only there to distinguish the constructor signature. It's not
    // used for anything.
    AttributedMessage(AttributedMessage source, Class msgClass) 
    {
	super(source.getOriginator(), source.getTarget());
	this.contents = null;
	attributes = new SimpleMessageAttributes(source.attributes);
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
	super(contents == null ? null : contents.getOriginator(), 
	      contents == null ? null : contents.getTarget());
	this.contents = contents;
	attributes = new SimpleMessageAttributes(initialAttributes.attributes);
    }


    AttributedMessage(Message contents, SimpleMessageAttributes attributes) {
	super(contents == null ? null : contents.getOriginator(), 
	      contents == null ? null : contents.getTarget());
	this.contents = contents;
	this.attributes = new SimpleMessageAttributes(attributes);
    }

    synchronized void snapshotAttributes() {
	snapshot = new SimpleMessageAttributes(attributes);
    }

    
    synchronized void restoreSnapshot() {
	if (snapshot != null) attributes = snapshot;
    }




    void addFilter(StandardAspect aspect) {
	pushValue(FILTERS_ATTRIBUTE, 
		 aspect.getClass().getName());

    }

    boolean replyOnly() {
	return false;
    }


    /**
     * Returns the raw (unattributed) message.
     */
    Message getRawMessage() {
	return contents;
    }




    // MessageAttributes interface
    // Delegate all calls 


    public Object getAttribute(String attribute) {
	return attributes.getAttribute(attribute);
    }


    public void setAttribute(String attribute, Object value) {
	attributes.setAttribute(attribute, value);
    }

    public void removeAttribute(String attribute) {
	attributes.removeAttribute(attribute);
    }
	
    public void addValue(String attribute, Object value) {
	attributes.addValue(attribute, value);
    }

    public void pushValue(String attribute, Object value) {
	attributes.pushValue(attribute, value);
    }

    public void removeValue(String attribute, Object value) {
	attributes.removeValue(attribute, value);
    }



    public void setLocalAttribute(String attribute, Object value) {
	attributes.setLocalAttribute(attribute, value);
    }

    public void removeLocalAttribute(String attribute) {
	attributes.removeLocalAttribute(attribute);
    }
	
    public void addLocalValue(String attribute, Object value) {
	attributes.addLocalValue(attribute, value);
    }

    public void pushLocalValue(String attribute, Object value) {
	attributes.pushLocalValue(attribute, value);
    }

    public void removeLocalValue(String attribute, Object value) {
	attributes.removeLocalValue(attribute, value);
    }






    private void sendAttributes(ObjectOutput out) 
	throws java.io.IOException
    {
 	MessageProtectionService svc =
	    MessageProtectionAspect.getMessageProtectionService();

	if (svc != null) {

	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    ObjectOutputStream oos = new ObjectOutputStream(bos);
	    oos.writeObject(attributes);
	    oos.close();
	    
	    byte[] bytes = svc.protectHeader(bos.toByteArray(), 
					     getOriginator(),
					     getTarget());
	    out.writeObject(bytes);
	} else {
	    out.writeObject(attributes);
	}
    }

    private void readAttributes(ObjectInput in) 
	throws java.io.IOException, ClassNotFoundException

    {
 	MessageProtectionService svc =
	    MessageProtectionAspect.getMessageProtectionService();
	if (svc != null) {

	    byte[] rawData = (byte[]) in.readObject();
	    byte[] data  = svc.unprotectHeader(rawData, 
					       getOriginator(),
					       getTarget());
	    ByteArrayInputStream bis = new ByteArrayInputStream(data);
	    ObjectInputStream ois = new ObjectInputStream(bis);
	    attributes = (SimpleMessageAttributes) ois.readObject();
	    ois.close();
	} else {
	    attributes = (SimpleMessageAttributes) in.readObject();
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
	try {
	    // Source and Destination MUST be in the clear
	    rawOut.writeObject(getOriginator());
	    rawOut.writeObject(getTarget());



	    MessageStreamsFactory factory =  
		MessageStreamsFactory.getFactory();
	    ArrayList aspectNames = (ArrayList)
		attributes.getAttribute(FILTERS_ATTRIBUTE);
	    MessageWriter writer = factory.getMessageWriter(aspectNames);
	
	    writer.finalizeAttributes(this);

	    sendAttributes(rawOut);
	    if (replyOnly()) return;


	    writer.preProcess();

	    OutputStream out = writer.getObjectOutputStream(rawOut);
	    ObjectOutputStream object_out = null;
	    // 'out' should be an ObjectOutputStream but might just be an
	    // OutputStream.  In the latter case, wrap it here.
	    if (out instanceof ObjectOutputStream)
		object_out = (ObjectOutputStream) out;
	    else
		object_out = new ObjectOutputStream(out);


	    object_out.writeObject(contents);

	    writer.finishOutput();
	    writer.postProcess();
	} 
	catch (java.io.IOException ex1) {
	    ex1.printStackTrace();
	    throw ex1;
	}
	catch (Exception ex2) {
	    ex2.printStackTrace();
	    throw new java.io.IOException(ex2.toString());
	}

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
	try {
	    setOriginator((MessageAddress) rawIn.readObject());
	    setTarget((MessageAddress) rawIn.readObject());

	    readAttributes(rawIn);

	    if (replyOnly()) return;

	    MessageStreamsFactory factory =  
		MessageStreamsFactory.getFactory();  
	    ArrayList aspectNames = (ArrayList)
		attributes.getAttribute(FILTERS_ATTRIBUTE);
	    MessageReader reader = factory.getMessageReader(aspectNames);

	    reader.finalizeAttributes(this);

	    reader.preProcess();
	    InputStream in = reader.getObjectInputStream(rawIn);
	    ObjectInputStream object_in = null;
	    if (in instanceof ObjectInputStream)
		object_in = (ObjectInputStream) in;
	    else
		object_in = new ObjectInputStream(in);

	    contents = (Message) object_in.readObject();

	    reader.finishInput();
	    reader.postProcess();
	} 
	catch (java.io.IOException ex1) {
	    ex1.printStackTrace();
	    throw ex1;
	}
	catch (ClassNotFoundException ex2) {
	    ex2.printStackTrace();
	    throw ex2;
	}
	catch (Exception ex3) {
	    ex3.printStackTrace();
	    throw new java.io.IOException(ex3.toString());
	}
    }
}

