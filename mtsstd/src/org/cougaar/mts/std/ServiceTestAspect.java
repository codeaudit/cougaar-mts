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
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;

import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MessageDelivererDelegateImplBase;
import org.cougaar.mts.base.MessageReader;
import org.cougaar.mts.base.MessageReaderDelegateImplBase;
import org.cougaar.mts.base.MessageWriter;
import org.cougaar.mts.base.MessageWriterDelegateImplBase;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.UnregisteredNameException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.LinkProtocol; // javadoc
import org.cougaar.mts.base.LinkProtocolService;
import org.cougaar.mts.base.RMILinkProtocol;
import org.cougaar.mts.base.StandardAspect;

/**
 *  This test Aspect is an example of using a {@link
 *  LinkProtocol}-specific service.
 */
public class ServiceTestAspect extends StandardAspect
{
    private LinkProtocolService svc;

    public ServiceTestAspect() {
    }

    private void test(String text, MessageAddress addr) {
	synchronized (this) {
	    if (svc == null) {
		ServiceBroker sb = getServiceBroker();
		Object raw = sb.getService(this,
					   RMILinkProtocol.Service.class, 
					   null);
		svc = (LinkProtocolService) raw;
	    }
	}

	if (svc != null && loggingService.isInfoEnabled()) {
	    loggingService.info("LinkProtocol Service " + text + ":" + 
				     addr + "->" + svc.addressKnown(addr));
	}
			       
    }

    private AttributedMessage send(AttributedMessage message) {
	test("send", message.getTarget());
	return message;
    }

    private AttributedMessage receive(AttributedMessage message) {
	test("receive", message.getOriginator());
	return message;
    }



    public Object getDelegate(Object delegate, Class type) 
    {
	if (type ==  DestinationLink.class) {
	    DestinationLink link = (DestinationLink) delegate;
	    return new TestDestinationLink(link);
	} else {
	    return null;
	}
    }


    public Object getReverseDelegate(Object delegate, Class type) 
    {
	if (type == MessageDeliverer.class) {
	    return new TestDeliverer((MessageDeliverer) delegate);
	} else {
	    return null;
	}
    }
    


    private class TestDestinationLink 
	extends DestinationLinkDelegateImplBase 
    {
	private TestDestinationLink(DestinationLink link) {
	    super(link);
	}

	public MessageAttributes forwardMessage(AttributedMessage message) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException
	{
	    return super.forwardMessage(send(message));
	}


    }



    private class TestDeliverer extends MessageDelivererDelegateImplBase {
	private TestDeliverer(MessageDeliverer deliverer) {
	    super(deliverer);
	}

	public MessageAttributes deliverMessage(AttributedMessage m, 
						MessageAddress dest) 
	    throws MisdeliveredMessageException
	{
	    return super.deliverMessage(receive(m), dest);
	}

    }
}
