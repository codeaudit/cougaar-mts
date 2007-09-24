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
import org.cougaar.core.service.LoggingService;

/**
 * This class was originally designed to instantiate {@link
 * LinkProtocol}s, given a list of class names.  Now that LinkProtocol
 * Components are loaded in the usual way, its only remaining function
 * is to handle the case when none are loaded.  If that happens it
 * will create two default LinkProtocol instances, for RMI and Loopback.
 */
final class LinkProtocolFactory 
{

    private LoggingService loggingService;
    private Container container;

    LinkProtocolFactory(Container container, ServiceBroker sb)
    {
	loggingService = (LoggingService)
	    sb.getService(this, LoggingService.class, null);
	this.container = container;
	MessageTransportRegistryService reg =(MessageTransportRegistryService)
	    sb.getService(this, MessageTransportRegistryService.class, null);
	// If any Protocols are already loaded (via CSMART), don't
	// make the standard ones.
	if (!reg.hasLinkProtocols()) {
	LinkProtocol protocol =new LoopbackLinkProtocol();
	initProtocol(protocol);
	makeProtocol("org.cougaar.mts.rmi.RMILinkProtocol");
	}
    }


    private void initProtocol(LinkProtocol protocol) {
	container.add(protocol);
    }


    private LinkProtocol makeProtocol(String classname) {
	LinkProtocol protocol = null;
	try {
	    Class protocol_class = Class.forName(classname);
	    protocol = (LinkProtocol) protocol_class.newInstance();
	} catch (Exception xxx) {
	    if (loggingService.isErrorEnabled())
		loggingService.error(null, xxx);
	    return null;
	}
	initProtocol(protocol);
	return protocol;
    }


}
