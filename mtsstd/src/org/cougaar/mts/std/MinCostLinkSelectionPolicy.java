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

package org.cougaar.mts.std;
import org.cougaar.core.mts.*;

import java.util.Iterator;

/**
 * A cost-based selection policy that chooses the cheapest link.  */
public class MinCostLinkSelectionPolicy 
    extends AbstractLinkSelectionPolicy
{


    // Example of using MTS services in a selection policy
    //
//     public void load() {
// 	super.load();
// 	System.out.println("ID=" +getRegistry().getIdentifier());
//     }


    public DestinationLink selectLink (Iterator links, 
				       AttributedMessage message,
				       AttributedMessage failedMessage,
				       int retryCount,
				       Exception lastException)
    {
	int min_cost = -1;
	DestinationLink cheapest = null;
	while (links.hasNext()) {
	    DestinationLink link = (DestinationLink) links.next();
	    int cost = link.cost(message);
	    
	    // If a link reports 0 cost, use it.  With proper
	    // ordering, this allows us to skip relatively expensive
	    // cost calculations (eg rmi) that can't be any better
	    // anyway.
	    if (cost == 0) return link;

	    // If a link reports MAX_VALUE, ignore it.
	    if (cost == Integer.MAX_VALUE) continue;

	    if (cheapest == null || cost < min_cost) {
		cheapest = link;
		min_cost = cost;
	    }
	}
	return cheapest;
    }
}
