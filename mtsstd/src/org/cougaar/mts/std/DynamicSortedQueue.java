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
import java.util.Comparator;
import java.util.Iterator;

public class DynamicSortedQueue
{
    private Comparator comparator;
    private Mapper mapper;
    private ArrayList store;
    

    public DynamicSortedQueue(Comparator comparator, Mapper mapper) {
	store = new ArrayList();
	this.comparator = comparator;
	this.mapper = mapper;
    }


    public void setComparator(Comparator comparator, Mapper mapper) {
	this.comparator = comparator;
	this.mapper = mapper;
    }

    public int size() {
	return store.size();
    }

    public boolean add(Object x) {
	if (store.contains(x)) return false;
	store.add(x);
	return true;
    }


    private void remove(Object x) {
	store.remove(x);
    }
	    

    public boolean isEmpty() {
	return store.isEmpty();
    }

    public Object next() {
	Iterator itr = store.iterator();
	Object min = null;
	while (itr.hasNext()) {
	    Object candidate = itr.next();
	    if (min == null) {
		min = candidate;
	    } else {
		int comp = comparator.compare(mapper.map(min), 
					      mapper.map(candidate));
		if (comp > 0) min = candidate;
	    }
	}
	if (min != null) store.remove(min);
	return min;
    }
	    
}

