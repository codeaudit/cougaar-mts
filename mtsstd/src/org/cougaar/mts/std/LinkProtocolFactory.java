/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.core.mts;

import org.cougaar.core.component.Container;
import org.cougaar.core.component.ServiceBroker;

import java.util.ArrayList;
import java.util.StringTokenizer;


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
public final class LinkProtocolFactory 
{
    private static final String CLASSES_PROPERTY =
	"org.cougaar.message.protocol.classes";

    private MessageTransportRegistryService registry;
    private MessageDeliverer deliverer;
    private NameSupport nameSupport;
    private Container container;

    public LinkProtocolFactory(Container container, ServiceBroker sb)
    {
	registry = (MessageTransportRegistryService)
	    sb.getService(this, MessageTransportRegistryService.class, null);
	nameSupport = (NameSupport)
	    sb.getService(this, NameSupport.class, null);
	this.container = container;
    }

    void setDeliverer(MessageDeliverer deliverer) {
	this.deliverer = deliverer;
    }

    private void initProtocol(LinkProtocol protocol) {
	protocol.setDeliverer(deliverer);
	registry.addLinkProtocol(protocol);
	container.add(protocol);
    }


    private LinkProtocol makeProtocol(String classname) {
	LinkProtocol protocol = null;
	try {
	    Class protocol_class = Class.forName(classname);
	    protocol = (LinkProtocol) protocol_class.newInstance();
	} catch (Exception xxx) {
	    xxx.printStackTrace();
	    return null;
	}
	initProtocol(protocol);
	return protocol;
    }

    public void loadProtocols() {

	String protocol_classes = System.getProperty(CLASSES_PROPERTY);
	if (protocol_classes == null || protocol_classes.equals("")) {
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
