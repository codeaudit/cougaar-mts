/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.mts.std;

import java.util.ArrayList;
import java.util.Iterator;

import org.cougaar.core.mts.MessageTransportWatcher;
import org.cougaar.core.service.MessageWatcherService;
import org.cougaar.mts.base.MessageTransportServiceProvider;

/**
 * This entity implements the {@link MessageWatcherService}. It's used in
 * conjunction with the {@link WatcherAspect}. Both are instantiated by the
 * {@link MessageTransportServiceProvider}, which is the provider of the
 * {@link MessageWatcherService}. The actual "watching" happens in the Aspect.
 * This service is a core front-end.
 */
public class MessageWatcherServiceImpl
        implements MessageWatcherService {
    private WatcherAspect aspect;
    private ArrayList watchers;

    public MessageWatcherServiceImpl(WatcherAspect aspect) {
        this.aspect = aspect;
        this.watchers = new ArrayList();
    }

    public synchronized void release() {
        Iterator itr = watchers.iterator();
        while (itr.hasNext()) {
            MessageTransportWatcher watcher = (MessageTransportWatcher) itr.next();
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

    public void removeMessageTransportWatcher(MessageTransportWatcher watcher) {
        aspect.removeWatcher(watcher);
        synchronized (this) {
            watchers.remove(watcher);
        }
    }

}
