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
 * A factory which instantiates all MessageTransports.  It will always
 * make at leats two transports: one for local message
 * (LoopbackMessageTransport) and one for remote messages
 * (RMIMessageTransport).  It may also make other transports, one per
 * class, as listed in the property org.cougaar.message.transport.classes.
 *
 * If the property org.cougaar.message.transport.preferred is set, the
 * transport of that class will always be preferred over any others.  */
public final class MessageTransportFactory 
{
    private static final String CLASSES_PROPERTY =
	"org.cougaar.message.transport.classes";
    private static final String PREFERRED_PROPERTY =
	"org.cougaar.message.transport.preferred";


    private ArrayList transports;
    private ArrayList aspects;
    private String id;
    private MessageTransport defaultTransport, loopbackTransport;
    private MessageTransportRegistry registry;
    private MessageDeliverer deliverer;
    private NameSupport nameSupport;

    public MessageTransportFactory(String id, 
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

    private MessageTransport makeTransport(String classname) {
	// Assume for now all transport classes have a constructor of
	// one argument (the id string).
	Class[] types = { String.class, ArrayList.class };
	Object[] args = 
	    { registry.getIdentifier(),
	      aspects
	    };
	MessageTransport transport = null;
	try {
	    Class transport_class = Class.forName(classname);
	    Constructor constructor = 
		transport_class.getConstructor(types);
	    transport = (MessageTransport) constructor.newInstance(args);
	} catch (Exception xxx) {
	    xxx.printStackTrace();
	    return null;
	}
	transport.setDeliverer(deliverer);
	transport.setRegistry(registry);
	transport.setNameSupport(nameSupport);
	transports.add(transport);
	return transport;
    }


    private void makeOtherTransports() {
	String transport_classes = System.getProperty(CLASSES_PROPERTY);
	if (transport_classes == null) return;

	StringTokenizer tokenizer = 
	    new StringTokenizer(transport_classes, ",");
	while (tokenizer.hasMoreElements()) {
	    String classname = tokenizer.nextToken();
	    makeTransport(classname);
	}
    }

    public  ArrayList getTransports() {
	if (transports != null) return transports;

	transports = new ArrayList();

	String preferredClassname = System.getProperty(PREFERRED_PROPERTY);
	if (preferredClassname != null) {
	    MessageTransport transport = makeTransport(preferredClassname);
	    if (transport != null) {
		// If there's a preferred transport, never use any
		// others.
		defaultTransport = transport;
		loopbackTransport = transport;
		return transports;
	    }
	}

	// No preferred transport, make all the usual ones.

	loopbackTransport = new LoopbackMessageTransport(id, aspects);
	loopbackTransport.setDeliverer(deliverer);
	loopbackTransport.setRegistry(registry);
	loopbackTransport.setNameSupport(nameSupport);
	transports.add(loopbackTransport);
	
	defaultTransport = new RMIMessageTransport(id, aspects);
	defaultTransport.setDeliverer(deliverer);
	defaultTransport.setRegistry(registry);
	defaultTransport.setNameSupport(nameSupport);
	transports.add(defaultTransport);


	makeOtherTransports();

	return transports;
    }

    MessageTransport getDefaultTransport() {
	return defaultTransport;
    }

    MessageTransport getLoopbackTransport() {
	return loopbackTransport;
    }



}
