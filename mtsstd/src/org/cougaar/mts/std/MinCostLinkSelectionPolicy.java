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
import java.util.Iterator;

/**
 * A cost-based selection policy that chooses the cheapest link.  */
public class MinCostLinkSelectionPolicy implements LinkSelectionPolicy
{

    public DestinationLink selectLink (Iterator links, Message message)
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
