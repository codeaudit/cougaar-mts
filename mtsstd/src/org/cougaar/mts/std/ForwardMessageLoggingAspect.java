/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.mts.std;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.UnregisteredNameException;
import org.cougaar.mts.base.StandardAspect;

/**
 * This test Aspect logs all outgoing messages, including in each log
 * the source and destination Agent, the destination Node, and the
 * destination Host. 
 */
public class ForwardMessageLoggingAspect extends StandardAspect
{
    static final String TOPOLOGY = "topology";

    private static final char SEPR = '\t';

    private PrintStream log;
    private boolean madeLog;
    private WhitePagesService wp;

    public void load() {
	super.load();
	ServiceBroker sb = getServiceBroker();
	wp = (WhitePagesService)
	    sb.getService(this, WhitePagesService.class, null);
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
	String dst_agent = dst.getAddress();
	Class pclass = link.getProtocolClass();
	long now = System.currentTimeMillis();
	Object remote = link.getRemoteReference();
	String remoteIP = 
	    remote == null ? null : extractInetAddr(remote.toString());
        String dst_node =  null;
	try {
	    AddressEntry entry = wp.get(dst_agent, TOPOLOGY, 10);
	    if (entry == null) {
		dst_node = "???";
	    } else {
		dst_node = entry.getURI().getPath().substring(1);
	    }
	} catch (Exception ex) {
	    getLoggingService().error("", ex);
	    dst_node = "???";
	}


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



    
