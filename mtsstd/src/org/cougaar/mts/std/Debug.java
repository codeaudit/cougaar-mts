/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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
import org.cougaar.core.service.LoggingService;

/** 
 * MTS debugging support.  Adds an extra level of control over the
 * usual LoggingService isDebugEnabled().
 * 
 * @property org.cougaar.message.transport.debug If specified, may be
 * any of the names defined in DebugFlags, or a comma-separated list
 * of those names.  Case is not considered. The values "true" and
 * "all" enable all options.  The values "false" and "none" disable
 * all options.
 *
 **/
public final class Debug implements DebugFlags
{

    private static boolean loaded = false;
    private static int flags = 0;

    private static void initialize(LoggingService loggingService) {
	    flags = 0;
	    String debug = 
		System.getProperty("org.cougaar.message.transport.debug");
	    if (debug != null) {
		StringTokenizer tk = new StringTokenizer(debug, ",");
		while (tk.hasMoreTokens()) {
		    String dbg = tk.nextToken();
		    if (dbg.equalsIgnoreCase("true")) {
			flags = -1;
			break;
		    } else if (dbg.equalsIgnoreCase("all")) {
			flags = -1;
			break;
		    } else if (dbg.equalsIgnoreCase("false")) {
			flags = 0;
			break;
		    } else if (dbg.equalsIgnoreCase("none")) {
			flags = 0;
			break;
		    } else if (dbg.equalsIgnoreCase("aspects")) {
			flags |= ASPECTS;
		    } else if (dbg.equalsIgnoreCase("flush")) {
			flags |= FLUSH;
		    } else if (dbg.equalsIgnoreCase("comm")) {
			flags |= COMM;
		    } else if (dbg.equalsIgnoreCase("multicast")) {
			flags |= MULTICAST;
		    } else if (dbg.equalsIgnoreCase("policy")) {
			flags |= POLICY;
		    } else if (dbg.equalsIgnoreCase("quo")) {
			flags |= QUO;
		    } else if (dbg.equalsIgnoreCase("rms")) {
			flags |= RMS;
		    } else if (dbg.equalsIgnoreCase("security")) {
			flags |= SECURITY;
		    } else if (dbg.equalsIgnoreCase("service")) {
			flags |= SERVICE;
		    } else if (dbg.equalsIgnoreCase("statistics")) {
			flags |= STATISTICS;
		    } else if (dbg.equalsIgnoreCase("traffic_masking_generator")) {
			flags |= TRAFFIC_MASKING_GENERATOR;
		    } else if (dbg.equalsIgnoreCase("thread")) {
			flags |= THREAD;
		    } else if (dbg.equalsIgnoreCase("watcher")) {
			flags |= WATCHER;
		    } else {
			loggingService.error("Ignoring unknown MTS debug key "
					     + dbg);
		    }
		}
	    }

	    loaded = true;
	}





    static synchronized void load(LoggingService ls) {
	if (!loaded) initialize(ls);
    }

    public static boolean isDebugEnabled(LoggingService ls, int mask) {
	if (ls == null || !ls.isDebugEnabled())
	    return false;
	else
	    return ((flags & mask) == mask);
    }


    public static boolean isInfoEnabled(LoggingService ls, int mask) {
	if (ls == null || !ls.isInfoEnabled())
	    return false;
	else
	    return ((flags & mask) == mask);
    }


    public static boolean isErrorEnabled(LoggingService ls, int mask) {
	if (ls == null || !ls.isErrorEnabled())
	    return false;
	else
	    return ((flags & mask) == mask);
    }

}
