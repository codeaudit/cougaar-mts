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
    private static final String PREFERRED_PROPERTY =
	"org.cougaar.message.protocol.preferred";


    private ArrayList protocols;
    private ArrayList aspects;
    private String id;
    private LinkProtocol defaultProtocol, loopbackProtocol;
    private MessageTransportRegistry registry;
    private MessageDeliverer deliverer;
    private NameSupport nameSupport;

    public LinkProtocolFactory(String id, 
				   MessageTransportRegistry registry,
				   NameSupport nameSupport,
				   ArrayList aspects)
    {
	this.id = id;
	this.registry = registry;
	this.nameSupport = nameSupport;
	this.aspects = aspects;
    }

    void setDeliverer(MessageDeliverer deliverer) {
	this.deliverer = deliverer;
    }

    private LinkProtocol makeProtocol(String classname) {
	// Assume for now all transport classes have a constructor of
	// one argument (the id string).
	Class[] types = { String.class, ArrayList.class };
	Object[] args = 
	    { registry.getIdentifier(),
	      aspects
	    };
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
	protocol.setDeliverer(deliverer);
	protocol.setRegistry(registry);
	protocol.setNameSupport(nameSupport);
	protocols.add(protocol);
	return protocol;
    }


    private void makeOtherProtocols() {
	String protocol_classes = System.getProperty(CLASSES_PROPERTY);
	if (protocol_classes == null) return;

	StringTokenizer tokenizer = 
	    new StringTokenizer(protocol_classes, ",");
	while (tokenizer.hasMoreElements()) {
	    String classname = tokenizer.nextToken();
	    makeProtocol(classname);
	}
    }

    public  ArrayList getProtocols() {
	if (protocols != null) return protocols;

	protocols = new ArrayList();

	String preferredClassname = System.getProperty(PREFERRED_PROPERTY);
	if (preferredClassname != null) {
	    LinkProtocol protocol = makeProtocol(preferredClassname);
	    if (protocol != null) {
		// If there's a preferred transport, never use any
		// others.
		defaultProtocol = protocol;
		loopbackProtocol = protocol;
		return protocols;
	    }
	}

	// No preferred transport, make all the usual ones.

	loopbackProtocol = new LoopbackLinkProtocol(id, aspects);
	loopbackProtocol.setDeliverer(deliverer);
	loopbackProtocol.setRegistry(registry);
	loopbackProtocol.setNameSupport(nameSupport);
	protocols.add(loopbackProtocol);
	
	defaultProtocol = new RMILinkProtocol(id, aspects);
	defaultProtocol.setDeliverer(deliverer);
	defaultProtocol.setRegistry(registry);
	defaultProtocol.setNameSupport(nameSupport);
	protocols.add(defaultProtocol);


	makeOtherProtocols();

	return protocols;
    }

    LinkProtocol getDefaultProtocol() {
	return defaultProtocol;
    }

    LinkProtocol getLoopbackProtocol() {
	return loopbackProtocol;
    }



}
