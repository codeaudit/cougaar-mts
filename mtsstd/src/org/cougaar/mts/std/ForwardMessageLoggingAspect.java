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

import org.cougaar.core.service.*;

import org.cougaar.core.node.*;

import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.StringTokenizer;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

public class ForwardMessageLoggingAspect extends StandardAspect
{

    private static final char SEPR = '\t';

    private PrintStream log;

    public ForwardMessageLoggingAspect() {
	super();
	log = System.out;  // default PrintStream
	String logfile = 
	    MessageTransportRegistry.getRegistry().getIdentifier() + 
	    "_MessageForwarding.log";
	try {
	    FileOutputStream fos = new FileOutputStream(logfile);
	    log = new PrintStream(fos);
	} catch (java.io.FileNotFoundException fnf) {
	    fnf.printStackTrace();
	}
    }


    public Object getDelegate(Object delegate, Class type) 
    {
	if (type == DestinationLink.class) {
	    return new DestinationLinkDelegate((DestinationLink) delegate);
	} else {
	    return null;
	}
    }

    private String extractInetAddr(String raw) {
	if (raw.startsWith("IOR:")) {
	    // CORBA ior; handle this later.  Just return null for
	    // now.
	    return null;
	}

	int i = 0;
	while (i<raw.length()) {
	    if (Character.isDigit(raw.charAt(i++))) {
		try {
		    // found a digit
		    String sub = raw.substring(i-1);
		    StringTokenizer tk1 = new StringTokenizer(sub, " ;/,]})>");
		    if (tk1.hasMoreTokens()) {
			String data = tk1.nextToken();
			// Now verify it's a.b.c.d:e
			StringTokenizer tk2 = new StringTokenizer(data, ".");
			if (tk2.countTokens() == 4) {
			    Integer.parseInt(tk2.nextToken());
			    Integer.parseInt(tk2.nextToken());
			    Integer.parseInt(tk2.nextToken());
			    // three valid ints (should check 0-255)

			    StringTokenizer tk3 = 
				new StringTokenizer(tk2.nextToken(), ":");
			    if (tk3.countTokens() == 2) {
				Integer.parseInt(tk3.nextToken());
				Integer.parseInt(tk3.nextToken());

				// Done!
				return data;
			    } else {
				// System.out.println("#### More than two tokens");
			    }
			} else {
			    //System.out.println("#### More than four tokens");
			}
		    } else {
			//System.out.println("#### No tokens");
		    }
		} catch (NumberFormatException ex) {
		    // wrong format (most likely not an integer)
		    // ex.printStackTrace();
		}
	    }
	}
	return null;
    }

    private void logMessage(Message msg, String tag, DestinationLink link) {
	MessageAddress src = msg.getOriginator();
	MessageAddress dst = msg.getTarget();
	String dst_node = null;
	Class pclass = link.getProtocolClass();
	LinkProtocol protocol = null;
	long now = System.currentTimeMillis();
	Object remote = link.getRemoteReference();
	String remoteIP = 
	    remote == null ? null : extractInetAddr(remote.toString());
	Attributes match = new BasicAttributes(NameSupport.AGENT_ATTR, dst);
	String attr = NameSupport.NODE_ATTR;
	Iterator itr = 
	    NameSupportImpl.instance().lookupInTopology(match, attr);	   
	if (itr != null && itr.hasNext()) dst_node = (String) itr.next();



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
	

	public void forwardMessage(Message msg) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException
		   
	{
	    try {
		super.forwardMessage(msg);
		logMessage(msg, "Sent", this);
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



    
