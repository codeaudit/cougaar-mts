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


import org.cougaar.core.thread.CougaarThread;

/**
 * This is a debugging aspect.  By attaching it in a single-node
 * society (ie one in which all messages go through the Loopback
 * transport), we can check for issues related to serialization that
 * wouldn't arise otherwise.  */
public class WasteCPUAspect extends StandardAspect
{
   //This was taken from TrafficGenerator should be a math utils
    private static class ExpRandom extends java.util.Random {
	
	ExpRandom() {
	    //super is uniform distribution
	    super();
	}
	// period is the average period, 
	// the range can go from zero to ten times the period
	public int nextInt(int period) {
	    double raw = - (period * Math.log(super.nextDouble()));
	    // clip upper tail
	    if (raw > 10 * period) {
		return 10 * period;
	    }
	    else return (int) Math.round(raw);
	}
    }

    public Object getDelegate(Object object, Class type) 
    {
	if (type == DestinationLink.class) {
	    DestinationLink link = (DestinationLink) object;
	    return new WasteCPUDestinationLink(link);
	} else {
	    return null;
	}
    }
    
    ExpRandom expRandom = new ExpRandom();

    private class WasteCPUDestinationLink 
	extends DestinationLinkDelegateImplBase

    {
	
	WasteCPUDestinationLink(DestinationLink link) {
	    super(link);
	}


	public synchronized void forwardMessage(AttributedMessage message) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException

	{
	    // Serialize into the stream rather than pushing on the
	    // queue.
	    long count = 0;
	    long startTime = System.currentTimeMillis();
	    int wasteTime= expRandom.nextInt(166);
	    while (System.currentTimeMillis() - startTime < wasteTime) {
		count++;
	    }

	    CougaarThread.yield();

	    startTime = System.currentTimeMillis();
	    while (System.currentTimeMillis() - startTime < wasteTime) {
		count++;
	    }

	    CougaarThread.yield();

	    startTime = System.currentTimeMillis();
	    while (System.currentTimeMillis() - startTime < wasteTime) {
		count++;
	    }

	    super.forwardMessage(message);

	}
    }
}
