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
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.mts.std.AspectFactory;

/**
 * A factory which makes ReceiveLinks.  The caching for ReceiveLinks
 * is in the registry, not here, since the links need to be cleaned up
 * when agents unregister. 
 * 
 * This Factory is a subclass of AspectFactory, so aspects can be *
 * attached to a ReceiveLink when it's first instantiated.  */
public class ReceiveLinkFactory 
    extends AspectFactory
    implements ReceiveLinkProviderService, ServiceProvider
{

    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	// Could restrict this request to the registry
	if (serviceClass == ReceiveLinkProviderService.class) {
	    if (requestor instanceof MessageTransportRegistry.ServiceImpl) 
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



    /**
     * Make a new ReceiveLinkImpl and attach all relevant aspects.
     * The final object returned is the outermost aspect delegate, or
     * the ReceiveLinkImpl itself if there are no aspects.  */
    public ReceiveLink getReceiveLink(MessageTransportClient client) {
	ReceiveLink link = new ReceiveLinkImpl(client, getServiceBroker());
	return (ReceiveLink) attachAspects(link, ReceiveLink.class);
    }
}

