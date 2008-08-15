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

import org.cougaar.core.component.ParameterizedComponent;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;

/**
 * Root class of Components of the MTS.
 */
abstract public class BoundComponent
        extends ParameterizedComponent {

    private MessageTransportRegistryService registry;
    private NameSupport nameSupport;
    private AspectSupport aspectSupport;
    private ServiceBroker sb;

    protected LoggingService loggingService;
    protected ThreadService threadService;

    protected BoundComponent() {

    }

    public void load() {
        super.load();
        getLoggingService();
        getThreadService();
        getRegistry();
        getNameSupport();
        getAspectSupport();
    }

    protected MessageTransportRegistryService getRegistry() {
        if (registry == null) {
            ServiceBroker sb = getServiceBroker();
            registry = sb.getService(this, MessageTransportRegistryService.class, null);
        }
        return registry;
    }

    protected NameSupport getNameSupport() {
        if (nameSupport == null) {
            ServiceBroker sb = getServiceBroker();
            nameSupport = sb.getService(this, NameSupport.class, null);
        }
        return nameSupport;
    }

    protected AspectSupport getAspectSupport() {
        if (aspectSupport == null) {
            ServiceBroker sb = getServiceBroker();
            aspectSupport = sb.getService(this, AspectSupport.class, null);
        }
        return aspectSupport;
    }

    protected LoggingService getLoggingService() {
        if (loggingService == null) {
            ServiceBroker sb = getServiceBroker();
            loggingService = sb.getService(this, LoggingService.class, null);
        }
        return loggingService;
    }

    protected ThreadService getThreadService() {
        if (threadService == null) {
            ServiceBroker sb = getServiceBroker();
            threadService = sb.getService(this, ThreadService.class, null);
        }
        return threadService;
    }

    public final void setServiceBroker(ServiceBroker sb) {
        this.sb = sb;
    }

    public ServiceBroker getServiceBroker() {
        return sb;
    }

}
