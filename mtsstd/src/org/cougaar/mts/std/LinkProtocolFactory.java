/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.mts;


import java.util.ArrayList;
import java.util.StringTokenizer;
import java.lang.reflect.Constructor;


/**
 * A factory which instantiates all LinkProtocols.  It will always
 * make at least two: one for local message
 * (LoopbackProtocol) and one for remote messages
 * (RMIProtocol).  It may also make others, one per
 * class, as listed in the property org.cougaar.message.protocol.classes.
 */
public final class LinkProtocolFactory 
{
    private static final String CLASSES_PROPERTY =
	"org.cougaar.message.protocol.classes";

    private ArrayList protocols;
    private AspectSupport aspectSupport;
    private String id;
    private MessageTransportRegistry registry;
    private MessageDeliverer deliverer;
    private NameSupport nameSupport;

    public LinkProtocolFactory(String id, 
			       MessageTransportRegistry registry,
			       NameSupport nameSupport,
			       AspectSupport aspectSupport)
    {
	this.id = id;
	this.registry = registry;
	this.nameSupport = nameSupport;
	this.aspectSupport = aspectSupport;
    }

    void setDeliverer(MessageDeliverer deliverer) {
	this.deliverer = deliverer;
    }

    private void initProtocol(LinkProtocol protocol) {
	protocol.setDeliverer(deliverer);
	protocol.setRegistry(registry);
	protocol.setNameSupport(nameSupport);
	protocols.add(protocol);
    }


    private LinkProtocol makeProtocol(String classname) {
	// Assume for now all transport classes have a constructor of
	// one argument (the id string).
	Class[] types = { String.class, AspectSupport.class };
	Object[] args = 
	    { registry.getIdentifier(),  aspectSupport };
	LinkProtocol protocol = null;
	try {
	    Class protocol_class = Class.forName(classname);
	    Constructor constructor = 
		protocol_class.getConstructor(types);
	    protocol = (LinkProtocol) constructor.newInstance(args);
	} catch (Exception xxx) {
	    xxx.printStackTrace();
	    return null;
	}
	initProtocol(protocol);
	return protocol;
    }

    public  ArrayList getProtocols() {
	if (protocols != null) return protocols;

	protocols = new ArrayList();

	String protocol_classes = System.getProperty(CLASSES_PROPERTY);
	if (protocol_classes == null || protocol_classes.equals("")) {
	    // Make the two standard protocols if none specified.
	    LinkProtocol protocol =new LoopbackLinkProtocol(id, aspectSupport);
	    initProtocol(protocol);
	    protocol = new RMILinkProtocol(id, aspectSupport);
	    initProtocol(protocol);
	} else {
	    StringTokenizer tokenizer = 
		new StringTokenizer(protocol_classes, ",");
	    while (tokenizer.hasMoreElements()) {
		String classname = tokenizer.nextToken();
		makeProtocol(classname);
	    }
	}


	return protocols;
    }


}
