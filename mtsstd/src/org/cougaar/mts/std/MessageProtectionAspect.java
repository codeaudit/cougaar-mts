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
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;

import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.ProtectedInputStream;
import org.cougaar.core.mts.ProtectedOutputStream;
import org.cougaar.core.service.MessageProtectionService;

import org.cougaar.mts.base.MessageProtectionServiceImpl;
import org.cougaar.mts.base.MessageReader;
import org.cougaar.mts.base.MessageReaderDelegateImplBase;
import org.cougaar.mts.base.MessageWriter;
import org.cougaar.mts.base.MessageWriterDelegateImplBase;
import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MessageDelivererDelegateImplBase;
import org.cougaar.mts.base.DestinationQueueProviderService;
import org.cougaar.mts.base.SendQueueImpl;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.UnregisteredNameException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.SendLink;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.SendLinkDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;

/**
   This Aspect puts the {@link MessageProtectionService} streams in
   place.  It also instantiates a default implementation for that
   service if no other one is loaded.
 */
public class MessageProtectionAspect extends StandardAspect 
{

    private static MessageProtectionService svc;

    static MessageProtectionService getMessageProtectionService() {
	return svc;
    }

    public void load() {
	super.load();
	svc = (MessageProtectionService)
	    getServiceBroker().getService(this, MessageProtectionService.class,
					  null);
	// System.err.println("Got " +svc+ " from service broker");
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
	    // System.err.println("Got " +stream+ " from " +svc);
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
	    // System.err.println("Got " +stream+ " from " +svc);
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
	    message.addFilter(MessageProtectionAspect.this);


	    return super.forwardMessage(message);
	}

    }





}
