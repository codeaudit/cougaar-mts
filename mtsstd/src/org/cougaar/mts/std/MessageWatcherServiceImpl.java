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
import java.util.Iterator;

class MessageWatcherServiceImpl
    implements MessageWatcherService
{
    private WatcherAspect aspect;
    private ArrayList watchers;

    MessageWatcherServiceImpl(WatcherAspect aspect) {
	this.aspect = aspect;
	this.watchers = new ArrayList();
    }

    synchronized void release() {
	Iterator itr = watchers.iterator();
	while (itr.hasNext()) {
	    MessageTransportWatcher watcher =
		(MessageTransportWatcher) itr.next();
	    aspect.removeWatcher(watcher);
	}
	watchers = null;
	aspect = null;
    }

    public void addMessageTransportWatcher(MessageTransportWatcher watcher) {
	aspect.addWatcher(watcher);
	synchronized (this) {
	    watchers.add(watcher);
	}
    }

    public void removeMessageTransportWatcher(MessageTransportWatcher watcher){
	aspect.removeWatcher(watcher);
	synchronized (this) {
	    watchers.remove(watcher);
	}
    }

}
