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

import org.cougaar.mts.std.AttributedMessage;

/**
 * The second station for an outgoing message is a SendQueue. In theory a given
 * Message Transport subsystem can have multiple SendQueues. For this release we
 * only make one, instantiated as a SendQueueImpl. Either way, the SendQueues
 * are instantiated by a SendQueueFactory, accessible to SendLinkImpl as the
 * internal MTS service <ff>SendQueue</ff>.
 * <p>
 * The <strong>sendMessage</strong> method is used to queue messages in
 * preparation for passing them onto the next station, a Router. Ordinarily this
 * would only be called from a SendLinkImpl. The processing of queued messages
 * is assumed to take place in its own thread, not the caller's thread. In other
 * words, sendMessage is the final call in the original client's call sequence.
 * <p>
 * In a system with multiple SendQueues, the <strong>matches</strong> method
 * would be used by the SendQueueFactory to avoid making any particular queue
 * more than once.
 * <p>
 * The previous stop is SendLink. The next stop is Router.
 * 
 * @see SendQueueFactory
 * @see SendLink
 * @see Router
 * @see DestinationQueue
 * @see DestinationLink
 * @see MessageWriter
 * @see MessageReader
 * @see MessageDeliverer
 * @see ReceiveLink
 */
public interface SendQueue {
    /**
     * Used by SendLinkImpls to queue outgoing messages.
     * 
     * @param message A message to add to the queue.
     * @see SendLink#sendMessage(AttributedMessage) Javadoc contributions from
     *      George Mount.
     */
    void sendMessage(AttributedMessage message);

    /**
     * Used by a SendQueueFactory in its find-or-make algorithm to avoid
     * duplicating SendQueues.
     * 
     * @param name The name of the queue.
     */
    boolean matches(String name);

    /**
     * Number of messages waiting in the queue.
     */
    int size();

}
