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

import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class SimpleMessageAttributes    
    implements MessageAttributes, Serializable
{
    private HashMap data;
    private transient HashMap local_data;

    public SimpleMessageAttributes() {
	data = new HashMap();
	local_data = new HashMap();
    }


    private void readObject(ObjectInputStream ois)
	throws java.io.IOException, ClassNotFoundException
    {
	ois.defaultReadObject();
	local_data = new HashMap();
    }


    // MessageAttributes interface

    public MessageAttributes cloneAttributes() {
	SimpleMessageAttributes clone = new SimpleMessageAttributes();
	clone.data.putAll(data);
	clone.local_data.putAll(local_data);
	return clone;
    }


    public Object getAttribute(String attribute) {
	Object value = getAttribute(attribute, local_data);
	if (value != null) return value;
	return getAttribute(attribute, data);
    }

    public void setAttribute(String attribute, Object value) {
	setAttribute(attribute, value, data);
    }

    public void removeAttribute(String attribute) {
	removeAttribute(attribute, data);
    }

    public void addValue(String attribute, Object value) {
	addValue(attribute, value, data);
    }

    public void pushValue(String attribute, Object value) {
	pushValue(attribute, value, data);
    }

    public void removeValue(String attribute, Object value) {
	removeValue(attribute, value, data);
    }

    public void setLocalAttribute(String attribute, Object value) {
	setAttribute(attribute, value, local_data);
    }

    public void removeLocalAttribute(String attribute) {
	removeAttribute(attribute, local_data);
    }

    public void addLocalValue(String attribute, Object value) { 
	addValue(attribute, value, local_data);
   }

    public void pushLocalValue(String attribute, Object value) { 
	pushValue(attribute, value, local_data);
   }

    public void removeLocalValue(String attribute, Object value) {
	removeValue(attribute, value, local_data);
    }



    // Internal functions

    /**
     * Returns the current value of the given attribute.
     */
    private Object getAttribute(String attribute, HashMap map) {
	return map.get(attribute);
    }

    /**
     * Modifies or sets the current value of the given attribute to the
     * given value.
     */
    private void setAttribute(String attribute, Object value, HashMap map) {
	map.put(attribute, value);
    }


    /**
     * Removes the given attribute.
     */
    private void removeAttribute(String attribute, HashMap map) {
	map.remove(attribute);
    }
	
    /**
     * Add a value to an attribute.  If the current raw value of the
     * attribute is an ArrayList, the new value will be added to it.
     * If the current raw value is not an ArrayList, a new ArrayList
     * will be created and the current value (if non-null) as well as
     * the new value will be added to it.
     */
    private void addValue(String attribute, Object value, HashMap map) {
	Object old = map.get(attribute);
	if (old == null) {
	    ArrayList list = new ArrayList();
	    list.add(value);
	    map.put(attribute, list);
	} else if (old instanceof ArrayList) {
	    ((ArrayList) old).add(value);
	} else {
	    ArrayList list = new ArrayList();
	    list.add(old);
	    list.add(value);
	    map.put(attribute, list);
	}
    }


    /**
     * Like addValue but add to the front. 
     */
    private void pushValue(String attribute, Object value, HashMap map) {
	Object old = map.get(attribute);
	if (old == null) {
	    ArrayList list = new ArrayList();
	    list.add(value);
	    map.put(attribute, list);
	} else if (old instanceof ArrayList) {
	    ((ArrayList) old).add(0, value);
	} else {
	    ArrayList list = new ArrayList();
	    list.add(old);
	    list.add(0, value);
	    map.put(attribute, list);
	}
    }

    /**
     * Remove a value to an attribute.  The current raw value should
     * either be an ArrayList or a singleton equal to the given value.
     */
    private void removeValue(String attribute, Object value, HashMap map) {
	Object old = map.get(attribute);
	if (old == null) {
	} else if (old instanceof ArrayList) {
	    ((ArrayList) old).remove(value);
	} else if (value.equals(old)) {
	    map.remove(attribute);
	}
    }






}

