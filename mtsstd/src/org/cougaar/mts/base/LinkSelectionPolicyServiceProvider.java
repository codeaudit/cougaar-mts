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
 * This {@link ServiceProvider} provides the {@link LinkSelectionPolicy}
 * service. If no implementation of that service has been loaded at the time
 * this singleton is made, it will create a default implementation, using
 * {@link MinCostLinkSelectionPolicy}.
 */
public class LinkSelectionPolicyServiceProvider
        implements ServiceProvider {

    private LinkSelectionPolicy policy;
    private final LoggingService loggingService;

    LinkSelectionPolicyServiceProvider(ServiceBroker sb, Container container) {
        loggingService = sb.getService(this, LoggingService.class, null);
        LinkSelectionProvisionService lsp =
                sb.getService(this, LinkSelectionProvisionService.class, null);
        policy = lsp.getPolicy();
        if (policy == null) {
            policy = new MinCostLinkSelectionPolicy();
            container.add(policy);
        }
    }

    public Object getService(ServiceBroker sb, Object requestor, Class<?> serviceClass) {
        if (serviceClass == LinkSelectionPolicy.class) {
            if (requestor instanceof DestinationQueueImpl) {
                return policy;
            } else if (loggingService.isErrorEnabled()) {
                loggingService.error("Illegal request for LinkSelectionPolicy from " + requestor);
            }
        }
        return null;
    }

    public void releaseService(ServiceBroker sb,
                               Object requestor,
                               Class<?> serviceClass,
                               Object service) {
        if (serviceClass == LinkSelectionPolicy.class) {
            // ??
        }
    }

}
