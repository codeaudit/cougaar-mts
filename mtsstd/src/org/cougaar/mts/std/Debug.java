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

import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.LoggingService;

import java.util.StringTokenizer;

/** 
 * MTS debugging support.
 * 
 * @property org.cougaar.message.transport.debug  If specified, may
 * be "aspects","flush", "comm", "multicast", "policy", "security",
 * "service", "statistics", "watcher", or a comma-separated list of
 * these options.  Case is not considered. The values "true" and "all"
 * enable all options.  The values "false" and "none" disable all
 * options.
 *
 **/
public final class Debug
{

    Debug(ServiceBroker sb) {
	LoggingService loggingService = (LoggingService)
	    sb.getService(this, LoggingService.class, null);
	Provider provider = new Provider(loggingService);
	sb.addService(DebugService.class, provider);
	
    }


    private static class ServiceImpl implements DebugService, DebugFlags
    {
	private int flags;
	private LoggingService loggingService;

	private ServiceImpl(LoggingService loggingService) {
	    this.loggingService = loggingService;

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
		    } else if (dbg.equalsIgnoreCase("watcher")) {
			flags |= WATCHER;
		    } else {
			loggingService.error("Ignoring unknown MTS debug key "
					     + dbg);
		    }
		}
	    }
	}

	public boolean isDebugEnabled(int mask) {
	    return loggingService.isDebugEnabled() && ((flags & mask) == mask);
	}


	// Everything else is just delegation

	public boolean isEnabledFor(int level) {
	    return loggingService.isEnabledFor(level);
	}

	public void log(int level, String message) {
	    loggingService.log(level, message);
	}

	public void log(int level, String message, Throwable t) {
	    loggingService.log(level, message, t);
	}

	public boolean isDebugEnabled() {
	    return loggingService.isDebugEnabled();
	}

	public boolean isInfoEnabled() {
	    return loggingService.isInfoEnabled();
	}

	public boolean isWarnEnabled() {
	    return loggingService.isWarnEnabled();
	}

	public boolean isErrorEnabled() {
	    return loggingService.isErrorEnabled();
	}

	public boolean isFatalEnabled() {
	    return loggingService.isFatalEnabled();
	}


	public void debug(String message) {
	    loggingService.debug(message);
	}

	public void debug(String message, Throwable t) {
	    loggingService.debug(message, t);
	}


	public void info(String message) {
	    loggingService.info(message);
	}

	public void info(String message, Throwable t) {
	    loggingService.info(message, t);
	}

	public void warn(String message) {
	    loggingService.warn(message);
	}

	public void warn(String message, Throwable t) {
	    loggingService.warn(message, t);
	}

	public void error(String message) {
	    loggingService.error(message);
	}

	public void error(String message, Throwable throwable) {
	    loggingService.error(message, throwable);
	}

	public void fatal(String message) {
	    loggingService.fatal(message);
	}

	public void fatal(String message, Throwable throwable) {
	    loggingService.fatal(message, throwable);
	}

    }

    private static class Provider implements ServiceProvider {
	private DebugService service;

	private Provider(LoggingService loggingService) {
	    service = new ServiceImpl(loggingService);
	}

	public Object getService(ServiceBroker sb, 
				 Object requestor, 
				 Class serviceClass) 
	{
	    if (serviceClass == DebugService.class) {
		return service;
	    } else {
		return null;
	    }
	}

	public void releaseService(ServiceBroker sb, 
				   Object requestor, 
				   Class serviceClass, 
				   Object service)
	{
	}
    }

}
