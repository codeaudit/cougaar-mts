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
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.mts.std.AspectFactory;

/**
 * This {@link ServiceProvider} provides the {@link MessageDeliverer}
 * service, which is implemented by a singleton MessageDelivererImpl.
 */
public class MessageDelivererFactory 
    extends AspectFactory
    implements ServiceProvider
{
    public MessageDeliverer deliverer;
    public String id;

    MessageDelivererFactory(String id)
    {
	this.id = id;
    }

    public void load() {
	super.load();
	String name = id+"/Deliverer";
	MessageDeliverer d = new MessageDelivererImpl(name, getRegistry());
	deliverer = 
	    (MessageDeliverer) attachAspects(d, MessageDeliverer.class);
    }


    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	// Could restrict this request to LinkProtocols
	if (serviceClass == MessageDeliverer.class) {
	    if (requestor instanceof LinkProtocol || requestor instanceof MTImpl) 
		return deliverer;
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
