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
 * The second stop for an outgoing message.  In theory a
 * given Message Transport subsystem can have multiple Routers.
 * For this release we only make one, instantiated as a RouterImpl.
 * Either way, the Routers are instantiated by a RouterFactory.
 *
 * The <strong>routeMessage</strong> method is used to requesting
 * routing foe a given message, which would ordinarily mean passing
 * the message on to a DestinationQueue for the message's destination.
 * Ordinarily this method would only be called from a SendQueue
 * implementation.
 **/
interface Router extends Service
{

    /** Used by SendQueue implementations to route an outgoing
     * message, ordinarily to the DestinatonQueue corresponding to the
     * message's destintion.  */
    void routeMessage(AttributedMessage message);
}
