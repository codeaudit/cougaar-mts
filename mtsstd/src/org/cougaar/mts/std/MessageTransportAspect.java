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

/**
 * Abstract specification of a aspect in the message transport
 * subsystem.  An aspect is only required to perform one job: return
 * an aspect delegate for a given object of a given interface, or null
 * if the aspect prefers not to deal with that interface. */
public interface MessageTransportAspect
{
    public Object getDelegate(Object delegate, Class type);
    public boolean rejectTransport(MessageTransport transport, Class type);
}
