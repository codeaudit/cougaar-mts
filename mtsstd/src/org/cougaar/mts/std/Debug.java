/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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

import java.util.StringTokenizer;

/** 
 * MTS debugging support, controlled by the property
 * org.cougaar.message.transport.debug.  If specified, may be
 * "aspects","flush", "multicast", "security", "service",
 * "statistics", or "watcher".  A comma-separated list of the options
 * may also be specified, and "true" is interpreted as all of the
 * options enabled.
 * @property org.cougaar.message.transport.debug Enables various MTS debugging options.  
 * See the class description for information.
 **/
public class Debug implements DebugFlags
{
    private static int Flags = 0;

    public static boolean debug(int mask) {
	return (Flags & mask) == mask;
    }

    static {
	String debug = 
	    System.getProperty("org.cougaar.message.transport.debug");
	if (debug != null) {
	    if (debug.equalsIgnoreCase("true")) {
		Flags = -1;
	    } else {
		StringTokenizer tk = new StringTokenizer(debug, ",");
		while (tk.hasMoreTokens()) {
		    String dbg = tk.nextToken();
		    if (dbg.equalsIgnoreCase("aspects")) 
			Flags |= ASPECTS;
		    else if (dbg.equalsIgnoreCase("flush")) 
			Flags |= FLUSH;
		    else if (dbg.equalsIgnoreCase("comm")) 
			Flags |= COMM;
		    else if (dbg.equalsIgnoreCase("multicast")) 
			Flags |= MULTICAST;
		    else if (dbg.equalsIgnoreCase("policy")) 
			Flags |= POLICY;
		    else if (dbg.equalsIgnoreCase("security")) 
			Flags |= SECURITY;
		    else if (dbg.equalsIgnoreCase("service")) 
			Flags |= SERVICE;
		    else if (dbg.equalsIgnoreCase("statistics")) 
			Flags |= STATISTICS;
		    else if (dbg.equalsIgnoreCase("watcher")) 
			Flags |= WATCHER;
		    else
			System.err.println("### Unknown MTS debug key " + dbg);
		}
	    }
	}
    }


}
