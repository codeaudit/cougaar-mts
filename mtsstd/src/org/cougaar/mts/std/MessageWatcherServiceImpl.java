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

    void release() {
	aspect = null;
	Iterator itr = watchers.iterator();
	while (itr.hasNext()) {
	    MessageTransportWatcher watcher =
		(MessageTransportWatcher) itr.next();
	    aspect.removeWatcher(watcher);
	}
	watchers = null;
    }

    public void addMessageTransportWatcher(MessageTransportWatcher watcher) {
	aspect.addWatcher(watcher);
    }
}
