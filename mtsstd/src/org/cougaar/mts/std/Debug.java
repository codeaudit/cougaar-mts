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
 * MTS debugging support, controlled by the property org.cougaar.message.transport.debug.
 * If specified, may be "aspects","flush", "multicast", "security", "service", "statistics", or "watcher". 
 * A comma-separated list of the options may also be specified, and "true" is interpreted as
 * all of the options enabled.
 * @property org.cougaar.message.transport.debug Enables various MTS debugging options.  
 * See the class description for information.
 **/
class Debug
{
    private static boolean DEBUG_TRANSPORT;
    private static boolean DEBUG_ASPECTS;
    private static boolean DEBUG_FLUSH;
    private static boolean DEBUG_MULTICAST;
    private static boolean DEBUG_SECURITY;
    private static boolean DEBUG_SERVICE;
    private static boolean DEBUG_STATISTICS;
    private static boolean DEBUG_WATCHER;


    static {
	String debug = 
	    System.getProperty("org.cougaar.message.transport.debug");
	if (debug != null) {
	    if (debug.equalsIgnoreCase("true")) {
		DEBUG_TRANSPORT = true;
	    } else {
		StringTokenizer tk = new StringTokenizer(debug, ",");
		while (tk.hasMoreTokens()) {
		    String dbg = tk.nextToken();
		    if (dbg.equalsIgnoreCase("aspects")) 
			DEBUG_ASPECTS = true;
		    else if (dbg.equalsIgnoreCase("flush")) 
			DEBUG_FLUSH = true;
		    else if (dbg.equalsIgnoreCase("multicast")) 
			DEBUG_MULTICAST = true;
		    else if (dbg.equalsIgnoreCase("security")) 
			DEBUG_SECURITY = true;
		    else if (dbg.equalsIgnoreCase("service")) 
			DEBUG_SERVICE = true;
		    else if (dbg.equalsIgnoreCase("statistics")) 
			DEBUG_STATISTICS = true;
		    else if (dbg.equalsIgnoreCase("watcher")) 
			DEBUG_WATCHER = true;
		    else
			System.err.println("### Unknown MTS debug key " + dbg);
		}
	    }
	}
    }

    static boolean debugMulticast() {
	return DEBUG_TRANSPORT || DEBUG_MULTICAST;
    }

    static boolean debugFlush() {
	return DEBUG_TRANSPORT || DEBUG_FLUSH;
    }

    static boolean debugAspects() {
	return DEBUG_TRANSPORT || DEBUG_ASPECTS;
    }


    static boolean debugStatistics() {
	return DEBUG_TRANSPORT || DEBUG_STATISTICS;
    }


    static boolean debugSecurity() {
	return DEBUG_TRANSPORT || DEBUG_SECURITY;
    }

    static boolean debugService() {
	return DEBUG_TRANSPORT || DEBUG_SERVICE;
    }

    static boolean debugWatcher() {
	return DEBUG_TRANSPORT || DEBUG_WATCHER;
    }

}
