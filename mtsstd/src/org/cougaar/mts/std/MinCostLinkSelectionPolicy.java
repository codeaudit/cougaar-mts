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

import org.cougaar.core.service.*;

import org.cougaar.core.node.*;

import org.cougaar.core.mts.Message;
import java.util.Iterator;

/**
 * A cost-based selection policy that chooses the cheapest link.  */
public class MinCostLinkSelectionPolicy implements LinkSelectionPolicy
{

    public DestinationLink selectLink (Iterator links, 
				       Message message,
				       int retryCount,
				       Exception lastException)
    {
	int min_cost = -1;
	DestinationLink cheapest = null;
	while (links.hasNext()) {
	    DestinationLink link = (DestinationLink) links.next();
	    int cost = link.cost(message);
	    if (cost == Integer.MAX_VALUE) continue; // skip these
	    if (cheapest == null || cost < min_cost) {
		cheapest = link;
		min_cost = cost;
	    }
	}
	return cheapest;
    }
}
