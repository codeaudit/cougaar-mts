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
import java.util.StringTokenizer;

import org.cougaar.core.component.Container;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.LoggingService;

/**
 * A factory which instantiates all LinkProtocols.  It will always
 * make at least two: one for local message
 * (LoopbackProtocol) and one for remote messages
 * (RMIProtocol).  It may also make others, one per
 * class, as listed in the property org.cougaar.message.protocol.classes.
 * 
 * @property org.cougaar.message.protocol.classes A comma-separated
 * list of LinkProtocol classes to be instantiated.
 */
final class LinkProtocolFactory 
{
    private static final String CLASSES_PROPERTY =
	"org.cougaar.message.protocol.classes";

    private LoggingService loggingService;
    private Container container;

    LinkProtocolFactory(Container container, ServiceBroker sb)
    {
	loggingService = (LoggingService)
	    sb.getService(this, LoggingService.class, null);
	this.container = container;
	loadProtocols(sb);
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

    private void loadProtocols(ServiceBroker sb) {
	String protocol_classes = System.getProperty(CLASSES_PROPERTY);
	if (protocol_classes == null || protocol_classes.equals("")) {

	    MessageTransportRegistryService reg =(MessageTransportRegistryService)
		sb.getService(this, MessageTransportRegistryService.class, null);
	    // If any Protocols are already loaded (via CSMART), don't
	    // make the standard ones.
	    if (reg.hasLinkProtocols()) return;

	    // Make the two standard protocols if none specified.
	    LinkProtocol protocol =new LoopbackLinkProtocol();
	    initProtocol(protocol);
	    protocol = new RMILinkProtocol();
	    initProtocol(protocol);
	} else {
	    StringTokenizer tokenizer = 
		new StringTokenizer(protocol_classes, ",");
	    while (tokenizer.hasMoreElements()) {
		String classname = tokenizer.nextToken();
		makeProtocol(classname);
	    }
	}

    }


}
