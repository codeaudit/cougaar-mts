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
import java.util.StringTokenizer;

import org.cougaar.core.component.ServiceBroker;

/**
 * This is utility class which supports loading aspects
 */
public class AspectSupportImpl implements AspectSupport
{
    private final static String ASPECTS_PROPERTY = 
	"org.cougaar.message.transport.aspects";
    private static ArrayList aspects;
    private static HashMap aspects_table;
    private static ServiceBroker sb; 

    public AspectSupportImpl(ServiceBroker sb) {
	aspects = new ArrayList();
	aspects_table = new HashMap();
	this.sb =sb;
    }
    
    public MessageTransportAspect findAspect(String classname) {
	return (MessageTransportAspect) aspects_table.get(classname);
    }

 
    public void readAspects() {
	String classes = System.getProperty(ASPECTS_PROPERTY);

	if (classes == null) return;

	StringTokenizer tokenizer = new StringTokenizer(classes, ",");
	while (tokenizer.hasMoreElements()) {
	    String classname = tokenizer.nextToken();
	    try {
		Class aspectClass = Class.forName(classname);
		MessageTransportAspect aspect = 
		    (MessageTransportAspect) aspectClass.newInstance();
		add(aspect);
		aspects_table.put(classname, aspect);
		
		aspect.setServiceBroker(sb);
	    }
	    catch (Exception ex) {
		ex.printStackTrace();
		// System.err.println(ex);
	    }
	}
    }
    public void add(MessageTransportAspect aspect) {
	aspects.add(aspect);
	System.out.println("******* added aspect " + aspect);
    }

    public ArrayList getAspects() {
	return aspects;
    }
}
 


 
