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

import java.io.FileOutputStream;
import java.io.PrintStream;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.TopologyEntry;
import org.cougaar.core.service.TopologyReaderService;

public class ForwardMessageLoggingAspect extends StandardAspect
{

    private static final char SEPR = '\t';

    private PrintStream log;
    private boolean madeLog;
    private TopologyReaderService topologySvc;

    public void load() {
	super.load();
	ServiceBroker sb = getServiceBroker();
	topologySvc = (TopologyReaderService)
	    sb.getService(this, TopologyReaderService.class, null);
    }

    private synchronized void ensureLog() {
	if (madeLog) return;
	log = System.out;  // default PrintStream
	String logfile = getRegistry().getIdentifier() + 
	    "_MessageForwarding.log";
	try {
	    FileOutputStream fos = new FileOutputStream(logfile);
	    log = new PrintStream(fos);
	} catch (java.io.FileNotFoundException fnf) {
	    // ???
	}
	madeLog = true;
    }

    public Object getDelegate(Object delegate, Class type) 
    {
	if (type == DestinationLink.class) {
	    return new DestinationLinkDelegate((DestinationLink) delegate);
	} else {
	    return null;
	}
    }

    private static final int  UBYTE_MAX = (1<<8) - 1;
    private static final int  USHORT_MAX = (1<<16) - 1;

    private boolean isUnsignedByte(String string, int start, int end) {
	String s = string.substring(start, end);
	try {
	    int x = Integer.parseInt(s);
	    return x >= 0 && x < UBYTE_MAX;
	} catch (NumberFormatException ex) {
	    return false;
	}
    }

    private boolean isUnsignedShort(String string, int start, int end) {
	String s = string.substring(start, end);
	try {
	    int x = Integer.parseInt(s);
	    return x >= 0 && x < USHORT_MAX;
	} catch (NumberFormatException ex) {
	    return false;
	}
    }

    private String extractInetAddr(String raw) {
	if (raw.startsWith("IOR:")) {
	    // CORBA ior; handle this later.  Just return null for
	    // now.
	    return null;
	}

	int i = 0;
	int end = raw.length();
	while (i<end) {
	    if (Character.isDigit(raw.charAt(i))) {
		int start = i;

		int substart = i;
		i = raw.indexOf('.', substart);
		if (i == -1) break;
		if (!isUnsignedByte(raw, substart, i)) continue;

		substart = i+1;
		i = raw.indexOf('.', substart);
		if (i == -1) break;
		if (!isUnsignedByte(raw, substart, i)) continue;

		substart = i+1;
		i = raw.indexOf('.', substart);
		if (i == -1) break;
		if (!isUnsignedByte(raw, substart, i)) continue;

		substart = i+1;
		i = raw.indexOf(':', substart);
		if (i == -1) break;
		if (!isUnsignedByte(raw, substart, i)) continue;

		substart = i+1;
		while (i<end-1 && Character.isDigit(raw.charAt(++i)));
		if (!isUnsignedShort(raw, substart, i)) continue;

		return raw.substring(start, i);

	    } else {
		++i;
	    }
	}

	return null;
    }

    private void logMessage(AttributedMessage msg, 
			    String tag, 
			    DestinationLink link) 
    {
	ensureLog();
	MessageAddress src = msg.getOriginator();
	MessageAddress dst = msg.getTarget();
	Class pclass = link.getProtocolClass();
	LinkProtocol protocol = null;
	long now = System.currentTimeMillis();
	Object remote = link.getRemoteReference();
	String remoteIP = 
	    remote == null ? null : extractInetAddr(remote.toString());
	TopologyEntry entry = topologySvc.getEntryForAgent(dst.getAddress());
        String dst_node = entry != null ? entry.getNode() : null;

	synchronized(this) {
	    log.print(now);
	    log.print(SEPR);
	    log.print(tag);
	    log.print(SEPR);
	    log.print(src);
	    log.print(SEPR);
	    log.print(dst);
	    log.print(SEPR);
	    log.print(dst_node);
	    log.print(SEPR);
	    log.print(pclass.getName());
	    log.print(SEPR);
	    log.print(remoteIP);
	    log.print(SEPR);
	    log.println(remote);
	    log.flush();
	}
    }



    public class DestinationLinkDelegate 
	extends DestinationLinkDelegateImplBase 
    {
	
	public DestinationLinkDelegate (DestinationLink link) {
	    super(link);
	}
	

	public MessageAttributes forwardMessage(AttributedMessage msg) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException
		   
	{
	    try {
		MessageAttributes result = super.forwardMessage(msg);
		logMessage(msg, "Sent", this);
		return result;
	    } catch (UnregisteredNameException ex1) {
		logMessage(msg, "Failed", this);
		throw ex1;
	    } catch (NameLookupException ex2) {
		logMessage(msg, "Failed", this);
		throw ex2;
	    } catch (CommFailureException ex3) {
		logMessage(msg, "Failed", this);
		throw ex3;
	    } catch (MisdeliveredMessageException ex4) {
		logMessage(msg, "Failed", this);
		throw ex4;
	    }
	}

    }




}



    
