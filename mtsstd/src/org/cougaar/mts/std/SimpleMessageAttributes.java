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

import java.util.ArrayList;
import java.util.HashMap;

public class SimpleMessageAttributes    
    extends  HashMap
    implements MessageAttributes
{
    public SimpleMessageAttributes() {
    }

    public SimpleMessageAttributes(AttributedMessage msg) {
	super();
	MessageAttributes attr = msg.getRawAttributes();
	copyAttributes(attr);
    }


    public SimpleMessageAttributes(MessageAttributes attr) {
	super();
	copyAttributes(attr);
    }


    private void copyAttributes (MessageAttributes attr) {
	if (attr instanceof SimpleMessageAttributes) {
	    putAll((SimpleMessageAttributes) attr);
	} else {
	    System.err.println("#### THIS SHOULD NEVER HAPPEN");
	    Thread.dumpStack();
	}
    }


    // MessageAttributes interface


    public void clearAttributes() {
	clear();
    }

    public void restoreAttributes(MessageAttributes snapshot) {
	clear();
	copyAttributes(snapshot);
    }

    /**
     * Returns the current value of the given attribute.
     */
    public Object getAttribute(String attribute) {
	return get(attribute);
    }

    /**
     * Modifies or sets the current value of the given attribute to the
     * given value.
     */
    public void setAttribute(String attribute, Object value) {
	put(attribute, value);
    }


    /**
     * Removes the given attribute.
     */
    public void removeAttribute(String attribute) {
	remove(attribute);
    }
	
    /**
     * Add a value to an attribute.  If the current raw value of the
     * attribute is an ArrayList, the new value will be added to it.
     * If the current raw value is not an ArrayList, a new ArrayList
     * will be created and the current value (if non-null) as well as
     * the new value will be added to it.
     */
    public void addValue(String attribute, Object value) {
	Object old = get(attribute);
	if (old == null) {
	    ArrayList list = new ArrayList();
	    list.add(value);
	    put(attribute, list);
	} else if (old instanceof ArrayList) {
	    ((ArrayList) old).add(value);
	} else {
	    ArrayList list = new ArrayList();
	    list.add(old);
	    list.add(value);
	    put(attribute, list);
	}
    }

    /**
     * Remove a value to an attribute.  The current raw value should
     * either be an ArrayList or a singleton equal to the given value.
     */
    public void removeValue(String attribute, Object value) {
	Object old = get(attribute);
	if (old == null) {
	} else if (old instanceof ArrayList) {
	    ((ArrayList) old).remove(value);
	} else if (value.equals(old)) {
	    remove(attribute);
	}
    }




}

