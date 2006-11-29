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

package org.cougaar.mts.base;

import java.util.ArrayList;

import org.cougaar.core.component.Container;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.util.UnaryPredicate;

/**
 * This {@link ServiceProvider} provides and implements the {@link
 * SendQueueProviderService}.  Its jobs is to find or make a {@link
 * SendQueue} for a given address.  For now all addresses use a
 * singleton queue, in order to enforce ordering across senders.
 */
public class SendQueueFactory 
    extends QueueFactory
    implements ServiceProvider, SendQueueProviderService
{
    private SendQueue queue; // singleton
    private SendQueueImpl impl;
    private Container container;
    private String id;

    SendQueueFactory(Container container, String id)
    {
	this.container = container;
	this.id = id;
    }

    public void load() {
	super.load();
	impl = new SendQueueImpl(id+"/OutQ", container);
	queue = (SendQueue) attachAspects(impl, SendQueue.class);
    }


    public SendQueue getSendQueue(MessageAddress sender)
    {
	return queue;
    }

    public void removeMessages(UnaryPredicate pred, ArrayList removed) 
    {
	impl.removeMessages(pred, removed);
	notifyListeners(removed);
    }

   public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	if (serviceClass == SendQueueProviderService.class) {
	    return this;
	}
	return null;
    }

    public void releaseService(ServiceBroker sb, 
			       Object requestor, 
			       Class serviceClass, 
			       Object service)
    {
    }



}
