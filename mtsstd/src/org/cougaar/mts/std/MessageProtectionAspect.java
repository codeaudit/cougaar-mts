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

import java.io.*;



/**
 * This class provides an example of adding trailers to serialized
 * AttributedMessages.  The Writer computes a checksum (as a long) and
 * sends the eight bytes after the Message content.  The Reader
 * computes its own checksum and compares to the one that was sent.
 * The CHECKSUM_VALID_ATTR records whether or not they matched.  This
 * attributes is stored in the AttributedMessage and is available to
 * all receive-side Aspects.
 */

public class MessageProtectionAspect extends StandardAspect 
{

    private static final String THIS_CLASS =
	"org.cougaar.core.mts.MessageProtectionAspect";

    private static MessageProtectionService svc;

    static MessageProtectionService getMessageProtectionService() {
	return svc;
    }

    public void load() {
	super.load();
	svc = (MessageProtectionService)
	    getServiceBroker().getService(this, MessageProtectionService.class,
					  null);
	// Temporary, until NAI's service is available
	if (svc == null)  svc = new MessageProtectionServiceImpl();
    }



    public Object getDelegate(Object delegatee, Class type) {
	if (type == MessageWriter.class) {
	    MessageWriter wtr = (MessageWriter) delegatee;
	    return new ProtectedMessageWriter(wtr);
	} else if (type == MessageReader.class) {
	    MessageReader rdr = (MessageReader) delegatee;
	    return new ProtectedMessageReader(rdr);
	} else if (type == DestinationLink.class) {
	    DestinationLink link = (DestinationLink) delegatee;
	    return new ProtectedDestinationLink(link);
	} else if (type == MessageDeliverer.class) {
	    MessageDeliverer deliverer = (MessageDeliverer) delegatee;
	    return new ProtectedDeliverer(deliverer);
	} else {
	    return null;
	}
    }


    private class ProtectedMessageWriter 
	extends	MessageWriterDelegateImplBase
    {
	AttributedMessage msg;
	ProtectedOutputStream stream;
	ProtectedMessageWriter(MessageWriter delegatee) {
	    super(delegatee);
	}

	public void finalizeAttributes(AttributedMessage msg) {
	    this.msg = msg;
	    super.finalizeAttributes(msg);
	}

	public OutputStream getObjectOutputStream(ObjectOutput oo) 
	    throws java.io.IOException
	{
	    OutputStream os = super.getObjectOutputStream(oo);
	    stream = svc.getOutputStream(os, 
					 msg.getOriginator(),
					 msg.getTarget(),
					 msg);
	    return stream;
	}

	public void finishOutput()
	    throws java.io.IOException
	{
	    stream.finishOutput(msg);
	    super.finishOutput();
	}
    }


    private class ProtectedMessageReader
	extends	MessageReaderDelegateImplBase
    {
	AttributedMessage msg;
	ProtectedInputStream stream;
	ProtectedMessageReader(MessageReader delegatee) {
	    super(delegatee);
	}

	public void finalizeAttributes(AttributedMessage msg) {
	    this.msg = msg;
	    super.finalizeAttributes(msg);
	}

	public InputStream getObjectInputStream(ObjectInput oi) 
	    throws java.io.IOException, ClassNotFoundException
	{
	    InputStream is = super.getObjectInputStream(oi);
	    stream = svc.getInputStream(is, 
					msg.getOriginator(),
					msg.getTarget(),
					msg);
	    return stream;
	}

	public void finishInput()
	    throws java.io.IOException
	{
	    stream.finishInput(msg);
	    super.finishInput();
	}

    }

    private class ProtectedDeliverer extends MessageDelivererDelegateImplBase {
	ProtectedDeliverer(MessageDeliverer delegatee) {
	    super(delegatee);
	}

	// Only for debugging
// 	public MessageAttributes deliverMessage(AttributedMessage message,
// 						MessageAddress dest)
// 	    throws MisdeliveredMessageException
// 	{
// 	    System.out.println(" #### is streaming = " +
// 			       message.getAttribute(MessageAttributes.IS_STREAMING_ATTRIBUTE) +
// 			       " is encrypted = " +
// 			       message.getAttribute(MessageAttributes.ENCRYPTED_SOCKET_ATTRIBUTE));
// 	    return super.deliverMessage(message, dest);
// 	}

    }


    private class ProtectedDestinationLink
	extends DestinationLinkDelegateImplBase 
    {
	ProtectedDestinationLink(DestinationLink delegatee) {
	    super(delegatee);
	}
	
	public MessageAttributes forwardMessage(AttributedMessage message) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException
	{
	    // Register Aspect as a Message Streaming filter
	    message.addValue(MessageAttributes.FILTERS_ATTRIBUTE,
			     THIS_CLASS);


	    return super.forwardMessage(message);
	}

    }





}
