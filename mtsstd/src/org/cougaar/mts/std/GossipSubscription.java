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
import org.cougaar.core.qos.metrics.MetricNotificationQualifier;
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
	    MetricNotificationQualifier qualifier = null;
	    if (qualifier_svc != null) {
		qualifier = qualifier_svc.getNotificationQualifier(key);
	    }
	    subscription_uid = svc.subscribeToValue(path, this,
						    qualifier);
	}

	public void update(Observable ignore, Object value) {
	    this.current = (Metric) value;
	    addChange(key, this.current);
	}

	void unsubscribe() {
	    svc.unsubscribeToValue(subscription_uid);
	}
    }

    private GossipQualifierService qualifier_svc;
    private MessageAddress neighbor;
    private MetricsService svc;
    private HashMap callbacks;
    private ValueGossip changes;

    GossipSubscription(MessageAddress neighbor, 
		       MetricsService svc,
		       GossipQualifierService qualifierService)
    {
	this.svc = svc;
	this. qualifier_svc = qualifierService;
	this.neighbor = neighbor;
	callbacks = new HashMap();
	changes = null;
    }


    private synchronized void addChange(String key, Metric metric) {
	if (changes == null) changes = new ValueGossip();
	changes.add(key, metric);
    }

    synchronized ValueGossip getChanges() {
	if (changes == null) return null;
	ValueGossip result = new ValueGossip();
	Iterator itr = changes.iterator();
	Map.Entry entry = null;
	while (itr.hasNext()) {
	    entry = (Map.Entry) itr.next();
	    result.addEntry(entry.getKey(), entry.getValue());
	}
	return result;
    }

    synchronized void commitChanges(ValueGossip uncommitted_changes) {
	if (uncommitted_changes == null) return;
	Iterator itr = uncommitted_changes.iterator();
	Map.Entry entry = null;
	Object key = null;
	Object data = null;
	while(itr.hasNext()) {
	    entry = (Map.Entry) itr.next();
	    key = entry.getKey();
	    data = entry.getValue();
	    Object old_data = changes.lookupValue(key);
	    if (old_data != null && data.equals(old_data)) 
		changes.removeEntry(key);
	}
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
	    String key = (String) entry.getKey();
	    addKey(key);
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

    
