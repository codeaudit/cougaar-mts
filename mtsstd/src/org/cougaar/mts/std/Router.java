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


import org.cougaar.core.society.Message;


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
interface Router
{

    /** Used by SendQueue implementations to route an outgoing
     * message, ordinarily to the DestinatonQueue corresponding to the
     * message's destintion.  */
    public void routeMessage(Message message);
}
