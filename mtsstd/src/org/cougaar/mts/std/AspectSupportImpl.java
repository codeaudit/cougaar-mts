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
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.cougaar.core.component.ServiceBroker;

/**
 * This is utility class which supports loading aspects
 */
public final class AspectSupportImpl implements AspectSupport
{
    private final static String ASPECTS_PROPERTY = 
	"org.cougaar.message.transport.aspects";

    private ArrayList aspects;
    private HashMap aspects_table;
    private ServiceBroker sb; 

    // Should this be a singleton?
    public AspectSupportImpl(ServiceBroker sb) {
	aspects = new ArrayList();
	aspects_table = new HashMap();
	this.sb =sb;
	readAspects();
    }
    
    public MessageTransportAspect findAspect(String classname) {
	return (MessageTransportAspect) aspects_table.get(classname);
    }

 
    private void readAspects() {
	String classes = System.getProperty(ASPECTS_PROPERTY);

	if (classes == null) return;

	StringTokenizer tokenizer = new StringTokenizer(classes, ",");
	while (tokenizer.hasMoreElements()) {
	    String classname = tokenizer.nextToken();
	    try {
		Class aspectClass = Class.forName(classname);
		MessageTransportAspect aspect = 
		    (MessageTransportAspect) aspectClass.newInstance();
		addAspect(aspect, classname);
	    }
	    catch (Exception ex) {
		ex.printStackTrace();
		// System.err.println(ex);
	    }
	}
    }
    public void addAspect(MessageTransportAspect aspect,
			  String classname)
    {
	aspects.add(aspect);
	aspects_table.put(classname, aspect);
	aspect.setServiceBroker(sb);
	System.out.println("******* added aspect " + aspect);
    }


    /**
     * Loops through the aspects, allowing each one to attach an
     * aspect delegate in a cascaded series.  If any aspects attach a
     * delegate, the final aspect delegate is returned.  If no aspects
     * attach a delegate, the original object, as created by the
     * factory, is returned.  */
    public  Object attachAspects (Object delegate, 
				  Class type, 
				  LinkProtocol protocol)
    {
	if (aspects != null) {
	    Iterator itr = aspects.iterator();
	    while (itr.hasNext()) {
		MessageTransportAspect aspect = 
		    (MessageTransportAspect) itr.next();
		if (protocol != null && aspect.rejectProtocol(protocol, type))
		    continue; //skip it

		Object candidate = aspect.getDelegate(delegate, type);
		if (candidate != null) delegate = candidate;
		if (Debug.debugAspects()) 
		    System.out.println("======> " + delegate);
	    }
	}
	return delegate;
    }

}
 


 
