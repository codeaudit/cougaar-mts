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

import org.cougaar.core.component.ServiceBroker;

/**
 * The default, and for now only, implementation of {@link SendQueue}. The
 * implementation of <strong>sendMessage</strong> simply adds the message to the
 * queue. This kind of queue includes its own thread, which invokes
 * <strong>dispatch</strong> as each message is popped off the queue. This, in
 * turn, requests the {@link Router} to route the message to the appropriate
 * {@link DestinationQueue}.
 * 
 */
public final class SendQueueImpl
        extends MessageQueue
        implements SendQueue {
    private Router router;

    public SendQueueImpl(String name) {
        super(name);
    }

    @Override
   public void load() {
        super.load();
        ServiceBroker sb = getServiceBroker();
        router = sb.getService(this, Router.class, null);
    }

    @Override
   /**
     * This is the callback from the internal thread.
     */
    boolean dispatch(AttributedMessage message) {
        router.routeMessage(message);
        return true;
    }

    /**
     * The implementation of this SendQueue method simply adds the message to
     * the internal queue (a CircularQueue).
     */
    public void sendMessage(AttributedMessage message) {
        add(message);
    }

    /**
     * In a system with more than one SendQueue, each would have a unique name.
     * If the SendQueueFactory is ever asked to make a queue with a name that's
     * alreayd in use, it will instead find the existing queue by means of this
     * method.
     */
    public boolean matches(String name) {
        return name.equals(getName());
    }

}
