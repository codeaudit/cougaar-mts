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
import org.cougaar.core.qos.metrics.MetricsService;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

class GossipSubscription
{
    private class Callback implements Observer {
	String key;
	Metric current;
	Object subscription_uid;

	Callback(String key) {
	    this.key = key;
	    String path = "GossipIntegrater(" +key+ "):GossipFormula";
	    subscription_uid = svc.subscribeToValue(path, this);
	}

	public void update(Observable ignore, Object value) {
	    this.current = (Metric) value;
	    addChange(key, this.current);
	}

	void unsubscribe() {
	    svc.unsubscribeToValue(subscription_uid);
	}
    }

    private MessageAddress neighbor;
    private MetricsService svc;
    private HashMap callbacks;
    private ValueGossip changes;

    GossipSubscription(MessageAddress neighbor, MetricsService svc) {
	this.svc = svc;
	this.neighbor = neighbor;
	callbacks = new HashMap();
	changes = null;
    }


    synchronized void addChange(String key, Metric metric) {
	if (changes == null) changes = new ValueGossip();
	changes.add(key, metric);
    }

    synchronized ValueGossip getChanges() {
	ValueGossip old_changes = changes;
	changes = null;
	return old_changes;
    }

    private void addKey(String key) {
	Callback cb = (Callback) callbacks.get(key);
	if (cb == null) {
	    cb = new Callback(key);
	    callbacks.put(key, cb);
	}
    }

    synchronized void add (KeyGossip gossip) {
	Iterator  itr = gossip.iterator();
	while (itr.hasNext()) {
	    Map.Entry entry = (Map.Entry) itr.next();
	    addKey((String) entry.getKey());
	}
    }

    private synchronized void removeKey(String key) {
	Callback cb = (Callback) callbacks.get(key);
	if (cb != null) {
	    cb.unsubscribe();
	    callbacks.remove(key);
	}
    }

}

    
