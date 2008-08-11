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

package org.cougaar.mts.base;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.cougaar.core.service.LoggingService;
import org.cougaar.mts.std.AspectFactory;

/**
 * This abstraction provides 'listener' support to inform interested parties
 * when objects are removed from message queues.
 */
abstract class QueueFactory
        extends AspectFactory {
    private final HashSet listeners = new HashSet();

    public void addListener(QueueListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
        LoggingService lsvc = getLoggingService();
        if (lsvc.isInfoEnabled()) {
            lsvc.info("Add listener " + listener);
        }
    }

    public void removeListener(QueueListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
        LoggingService lsvc = getLoggingService();
        if (lsvc.isInfoEnabled()) {
            lsvc.info("Remove listener " + listener);
        }
    }

    protected void notifyListeners(List messages) {
        if (messages.isEmpty()) {
            return;
        }
        LoggingService lsvc = getLoggingService();
        if (lsvc.isInfoEnabled()) {
            lsvc.info("Notify listeners");
        }
        synchronized (listeners) {
            Iterator itr = listeners.iterator();
            QueueListener listener;
            while (itr.hasNext()) {
                listener = (QueueListener) itr.next();
                if (lsvc.isInfoEnabled()) {
                    lsvc.info("Notify listener " + listener);
                }
                listener.messagesRemoved(messages);
            }
        }
    }

}
