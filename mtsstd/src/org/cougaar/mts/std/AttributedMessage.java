/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.mts.std;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;

import org.cougaar.core.mts.Attributes;
import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.SimpleMessageAttributes;
import org.cougaar.core.service.MessageProtectionService;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

import org.cougaar.mts.base.MessageReader;
import org.cougaar.mts.base.MessageWriter;
import org.cougaar.mts.base.MessageSecurityException;
import org.cougaar.mts.base.MessageSerializationException;
import org.cougaar.mts.base.MessageStreamsFactory;

/**
 * An AttributedMessage is a Message with metadata, the latter
 * represented as HashMap with String keys.  When a Message enters the
 * MTS it will be wrapped in an AttributedMessage, passed through the
 * MTS in that form, and then unwrapped and delivered at the point of
 * final delivery.  AttributedMessages should not be construed as
 * envelopes.  In particular, it's not appropriate to introduce
 * further levels of 'wrapping' beyond the initial one.  
 */
public class AttributedMessage 
    extends  Message
    implements Externalizable, MessageAttributes, AttributeConstants
{

    private transient Logger logger = Logging.getLogger(getClass().getName());

    private String FILTERS_ATTRIBUTE = "Filters";

    private Message contents;
    private MessageAttributes attributes;

    // We control the externalization, so the 'transient' tag here is
    // really just documentation.
    private transient MessageAttributes snapshot;


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

    public AttributedMessage(Message contents, MessageAttributes initialAttributes) 
    {
	super(contents == null ? null : contents.getOriginator(), 
	      contents == null ? null : contents.getTarget());
	this.contents = contents;
	if (initialAttributes != null)
	    attributes = (MessageAttributes) 
		initialAttributes.cloneAttributes();
	else
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
	attributes = (MessageAttributes)
	    msg.attributes.cloneAttributes();
    }


    
    // Should only be used by MessageReply.  The second argument is
    // only there to distinguish the constructor signature. It's not
    // used for anything.  Since this is a reply, flip the addresses.
    public AttributedMessage(AttributedMessage source, Class msgClass) 
    {
	super(source.getTarget(), source.getOriginator());
	this.contents = null;
	attributes = (MessageAttributes)
	    source.attributes.cloneAttributes();
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
	attributes = (MessageAttributes)
	    initialAttributes.attributes.cloneAttributes();
    }


    public synchronized void snapshotAttributes() {
	snapshot = (MessageAttributes) attributes.cloneAttributes();
    }

    
    public synchronized void restoreSnapshot() {
	if (snapshot != null)
	    attributes = (MessageAttributes) snapshot.cloneAttributes();
    }



  // aspect was previously typed StandardAspect, but this presumes
  // that we are using mtsstd.
  public void addFilter(Object aspect) {
	String name = aspect.getClass().getName();
	if (logger.isDebugEnabled()) {
	    Object old = getAttribute(FILTERS_ATTRIBUTE);
	    if (old != null) {
		if (old instanceof ArrayList) {
		    ArrayList list = (ArrayList) old;
		    if (list.contains(name)) 
			logger.debug("Duplicated filter " +name);
		} else {
		    logger.debug("Filters attribute is not a list!");
		}
	    }
	}
	pushValue(FILTERS_ATTRIBUTE,  name);

    }

    protected boolean replyOnly() {
	return false;
    }


    /**
     * Returns the raw (unattributed) message.
     */
    public Message getRawMessage() {
	return contents;
    }


    // MessageAttributes interface
    // Delegate all calls 

    public String getAttributesAsString() {
        return attributes.getAttributesAsString();
    }

    public Attributes cloneAttributes() {
	return attributes.cloneAttributes();
    }

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

    public void clearAttributes() {
	attributes.clearAttributes();
    }

    public void mergeAttributes(Attributes attributes) {
	this.attributes.mergeAttributes(attributes);
    }



    private byte[] serializeObject(Object object)
    {
	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	try {
	    ObjectOutputStream oos = new ObjectOutputStream(bos);
	    oos.writeObject(object);
	    oos.close();
	} catch (java.io.IOException ex) {
	    logger.error(null, ex);
	}
	return bos.toByteArray();
    }

    private Object deserializeObject(byte[] bytes)
    {
	Object result = null;
	try {
	    ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
	    ObjectInputStream ois = new ObjectInputStream(bis);
	    result = ois.readObject();
	    ois.close();
	} catch (Exception ex) {
	    logger.error(null, ex);
	}

	return result;
    }

    private void sendAttributes(ObjectOutput out) 
	throws java.io.IOException, GeneralSecurityException
    {
 	MessageProtectionService svc =
	    MessageProtectionAspect.getMessageProtectionService();
	byte[] bytes = null;
	if (svc != null) {
	    bytes = svc.protectHeader(attributes, 
				      getOriginator(),
				      getTarget());
	} else {
	    bytes = serializeObject(attributes);
	}
	out.writeObject(bytes);
	// save HeaderLength as local attribute (not sent remotely)
	attributes.setLocalAttribute(HEADER_BYTES_ATTRIBUTE, 
					 new Integer(bytes.length));
    }

    private void readAttributes(ObjectInput in) 
	throws java.io.IOException, GeneralSecurityException, ClassNotFoundException
    {
 	MessageProtectionService svc =
	    MessageProtectionAspect.getMessageProtectionService();
	byte[] rawData = (byte[]) in.readObject();
	if (svc != null) {
	    attributes  = svc.unprotectHeader(rawData, 
					      getOriginator(),
					      getTarget());
	} else {
	    attributes = (SimpleMessageAttributes) deserializeObject(rawData);
	}
	attributes.setLocalAttribute(HEADER_BYTES_ATTRIBUTE, 
					 new Integer(rawData.length));
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
	} catch (java.io.NotSerializableException ex1) {
	    throw new MessageSerializationException(ex1);
	} catch (GeneralSecurityException ex2) {
	    throw new MessageSecurityException(ex2);
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
	} catch (java.io.NotSerializableException ex1) {
	    throwDelayedException(new MessageSerializationException(ex1));
	} catch (GeneralSecurityException ex2) {
	    throwDelayedException(new MessageSecurityException(ex2));
	}
    }

    private void throwDelayedException(java.io.IOException ex) 
	throws java.io.IOException
    {
	    if (logger != null) {
		MessageAddress src = getOriginator();
		MessageAddress dst = getTarget();
		String msg = "Receiver Exception " +src+ "->" +dst;
		logger.error(msg, ex);
	    }

	    // There's a problem here.  If we throw the exception
	    // right away, the sender might still be streaming the
	    // data.  In that case it will see a SocketClosed error,
	    // which it won't recognize as a security exception, and
	    // it will retry the send.  There's no good solution to
	    // this, so use a bad solution: give the sender a second
	    // to get the rest of the data out.  If that's not long
	    // enough, we lose.  In addition, preserialized
	    // notification will be delayed for a second for no
	    // reason.  Bad,
	    try { Thread.sleep(1000); } 
	    catch (InterruptedException xxx) {}

	    throw ex;
    }


    String logString()
    {
	return "<From: " + getOriginator().getAddress()+
	    " To: " + getTarget().getAddress()+
	    " Hash: " + hashCode()+
	    " Id: " + getContentsId()+
	    ">";
    }
}

