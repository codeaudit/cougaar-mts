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

/**
 * This is a utility class which supports loading aspects
 */
public interface AspectSupport {
    public MessageTransportAspect findAspect(String classname);
    public ArrayList getAspects();
    public void add(MessageTransportAspect aspect) ;
    public void readAspects();
}
