/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

import java.util.Iterator;
import java.util.Map;

class KeyGossip  extends Gossip
{
    public static final int PROPAGATION_DISTANCE = 3;

    private static class Data implements java.io.Serializable {
	int propagation_distance;

	Data(int propagation_distance) {
	    this.propagation_distance = propagation_distance;
	}

    }

    void add(String key) {
	add(key, PROPAGATION_DISTANCE);
    }

    void add(String key, int propagationDistance) {
	Data old = (Data) lookupValue(key);
	if (old == null) {
	    addEntry(key, new Data(propagationDistance));
	} else if (old.propagation_distance < propagationDistance) {
	    old.propagation_distance=propagationDistance;
	}
    }

    // union?
    void add(KeyGossip gossip) { 
	Iterator itr = gossip.iterator();
	while (itr.hasNext()) {
	    Map.Entry entry = (Map.Entry) itr.next();
	    String key = (String) entry.getKey();
	    Data value = (Data) entry.getValue();
	    add(key, value.propagation_distance);
	}
    }

    // Return a gossip set to propagate.  Items are included if the're
    // not already in the addendum and if the propagation count is > 0.
    KeyGossip computeAddendum(KeyGossip addendum) {
	KeyGossip result = null;
	Iterator itr = iterator();
	while (itr.hasNext()) {
	    Map.Entry entry = (Map.Entry) itr.next();
	    String key = (String) entry.getKey();
	    Data data = (Data) entry.getValue();
	    if (data.propagation_distance > 0 && !addendum.hasEntry(key)) {
		if (result == null) result = new KeyGossip();
		Data newdata = new Data(data.propagation_distance-1);
		result.addEntry(key, newdata);
	    }
	}
	return result;
    }

    String prettyPrint() {
	StringBuffer buf = new StringBuffer();
	Iterator itr = iterator();
	while (itr.hasNext()) {
	    Map.Entry entry = (Map.Entry) itr.next();
	    String key = (String) entry.getKey();
	    Data data = (Data) entry.getValue();
	    buf.append("\n\t");
	    buf.append(key);
	    buf.append("->");
	    buf.append(Integer.toString(data.propagation_distance));
	}
	return buf.toString();
    }


}
