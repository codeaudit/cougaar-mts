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
import org.cougaar.core.component.Service;

/**
 * The third station for an outgoing message.  In theory a
 * given Message Transport subsystem can have multiple Routers.
 * For this release we only make one, instantiated as a RouterImpl.
 * Either way, the Routers are instantiated by a RouterFactory,
 * accesible to SendQueueImpls as the internal MTS service
 * <ff>Router</ff>.
 * <p>
 * The <strong>routeMessage</strong> method is used to requesting
 * routing for a given message, which would ordinarily mean passing
 * the message on to a DestinationQueue for the message's destination.
 * Ordinarily this method would only be called from a SendQueue
 * implementation.
 * <p>
 * The previous station is SendQueue. The next station is
 * DestinationQueue.
 *
 * @see RouterFactory
 * @see SendLink
 * @see SendQueue
 * @see DestinationQueue
 * @see DestinationLink
 * @see MessageWriter
 * @see MessageReader
 * @see MessageDeliverer
 * @see ReceiveLink
 *
 * Javadoc contributions from George Mount.
 */
public interface Router extends Service
{

    /** 
     * Used by SendQueue implementations to route an outgoing
     * message, ordinarily to the DestinatonQueue corresponding to the
     * message's destintion.
     *
     * @see DestinationQueue#holdMessage(AttributedMessage)
     */
    void routeMessage(AttributedMessage message);
}
