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

import java.util.Comparator;

public class PriorityComparator implements Comparator {
    public int compare(Object x, Object y) {
	if (x.equals(y)) return 0;

	if (!(x instanceof Prioritized))
	    throw new RuntimeException(x + " is not Prioritized");
	if (!(y instanceof Prioritized))
	    throw new RuntimeException(y + " is not Prioritized");

	int p1 = ((Prioritized) x).getCougaarPriority();
	int p2 = ((Prioritized) y).getCougaarPriority();
	long t1 = ((Prioritized) x).getTimestamp();
	long t2 = ((Prioritized) y).getTimestamp();
	if (p1 > p2)
	    return -1;
	else if (p1 < p2)
	    return 1;
	else 
	    return  (t1 <= t2) ? -1 : 1;
    }

    public boolean equals(Object x) {
	return x == this;
    }


}
