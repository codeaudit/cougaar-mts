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

import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.qos.metrics.GossipUpdateService;

import java.util.Iterator;
import java.util.Map;

class ValueGossip 
    extends Gossip
{

    private static class Data implements java.io.Serializable {
	Data(Metric metric) {
	    this.metric = metric;
	}

	private Metric metric;
    }

    synchronized void update(GossipUpdateService updateService) {
	// get the key/metric pairs and pass them through
	Iterator itr = iterator();
	while (itr.hasNext()) {
	    Map.Entry elt = (Map.Entry) itr.next();
	    String key = (String) elt.getKey();
	    Data data = (Data) elt.getValue();
	    updateService.updateValue(key, data.metric);
	}
    }


    synchronized void add(String key, Metric data) {
	addEntry(key, new Data(data));
    }


    synchronized String prettyPrint() {
	StringBuffer buf = new StringBuffer();
	Iterator itr = iterator();
	while (itr.hasNext()) {
	    Map.Entry entry = (Map.Entry) itr.next();
	    String key = (String) entry.getKey();
	    Data data = (Data) entry.getValue();
	    buf.append("\n\t");
	    buf.append(key);
	    buf.append("->");
	    buf.append(data.metric.toString());
	}
	return buf.toString();
    }

}



