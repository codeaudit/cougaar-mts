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
import org.cougaar.core.component.Container;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.LoggingService;

/**
 * 
 * @property org.cougaar.message.transport.policy Sets the message
 * transport policy to instance o the specified class.
 */
public class LinkSelectionPolicyServiceProvider
    implements ServiceProvider
{

    private final static String POLICY_PROPERTY =
	"org.cougaar.message.transport.policy";

    private LinkSelectionPolicy policy;
    private LoggingService loggingService;

    LinkSelectionPolicyServiceProvider(ServiceBroker sb,
				       Container container) 
    {
	loggingService = (LoggingService)
	    sb.getService(this, LoggingService.class, null);
	LinkSelectionProvisionService lsp = (LinkSelectionProvisionService)
	    sb.getService(this, LinkSelectionProvisionService.class, null);
	policy = lsp.getPolicy();
	if (policy == null) policy = createSelectionPolicy(container);
    }


    private LinkSelectionPolicy createSelectionPolicy(Container container) {
	String policy_classname = System.getProperty(POLICY_PROPERTY);
	LinkSelectionPolicy selectionPolicy = null;
	if (policy_classname == null) {
	    selectionPolicy = new MinCostLinkSelectionPolicy();
	} else {
	    try {
		Class policy_class = Class.forName(policy_classname);
		selectionPolicy = 
		    (LinkSelectionPolicy) policy_class.newInstance();
		if (loggingService.isDebugEnabled())
		    loggingService.debug("Created " +  policy_classname);
	    } catch (Exception ex) {
		if (loggingService.isErrorEnabled())
		    loggingService.error(null, ex);
		selectionPolicy = new MinCostLinkSelectionPolicy();
	    }
	}

	container.add(selectionPolicy);
	return selectionPolicy;

    }



    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	if (serviceClass == LinkSelectionPolicy.class) {
	    if (requestor instanceof DestinationQueueImpl)
		return policy;
	    else if (loggingService.isErrorEnabled())
		loggingService.error("Illegal request for LinkSelectionPolicy from "
				   + requestor);
	}
	return null;
    }


    public void releaseService(ServiceBroker sb, 
			       Object requestor, 
			       Class serviceClass, 
			       Object service)
    {
	if (serviceClass == LinkSelectionPolicy.class) {
	    // ??
	}
    }




}
