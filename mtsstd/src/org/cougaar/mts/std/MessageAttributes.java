/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

import java.io.Serializable;

public interface MessageAttributes extends Serializable
{
    public static final String FILTERS_ATTRIBUTE = "Filters";

    public static final String DELIVERY_ATTRIBUTE = "DeliveryStatus";
    public static final String DELIVERY_STATUS_DELIVERED = "Delivered";
    public static final String DELIVERY_STATUS_CLIENT_ERROR = 
	"ClientException";
    public static final String DELIVERY_STATUS_DROPPED_DUPLICATE = 
	"DroppedDuplicate";
    public static final String DELIVERY_STATUS_HELD = "Held";
    public static final String DELIVERY_STATUS_STORE_AND_FORWARD  =
	"Store&Forward";
    public static final String DELIVERY_STATUS_BEST_EFFORT = "BestEffort";

    public Object getAttribute(String attribute);
    public void setAttribute(String attribute, Object value);
    public void removeAttribute(String attribute);
    public void addValue(String attribute, Object value);
    public void removeValue(String attribute, Object value);
}
