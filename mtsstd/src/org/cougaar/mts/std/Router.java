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
