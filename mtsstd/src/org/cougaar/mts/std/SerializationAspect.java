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

import org.cougaar.core.society.Message;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * This is a debugging aspect.  By attaching it in a single-node
 * society (ie one in which all messages go through the Loopback
 * transport), we can check for issues related to serialization that
 * wouldn't arise otherwise.  */
public class SerializationAspect extends StandardAspect
{

    public SerializationAspect() {
    }
    
    public Object getDelegate(Object object,
			      LinkProtocol protocol,
			      Class type) 
    {
	if (!(protocol instanceof LoopbackLinkProtocol)) {
	    return null;
	} else if (type == DestinationLink.class) {
	    return new SerializingDestinationLink((DestinationLink) object);
	} else {
	    return null;
	}
    }
    

    private class SerializingDestinationLink 
	extends DestinationLinkDelegateImplBase
	implements Runnable
	
    {
	PipedInputStream piped_is;
	PipedOutputStream piped_os;
	ObjectInputStream reader;
	ObjectOutputStream writer;
	Thread thread;

	SerializingDestinationLink(DestinationLink link) {
	    super(link);
	    thread = new Thread(this, "SerializingDestinationLink " + link);
	    thread.setDaemon(true);
	    try {
		piped_is = new PipedInputStream();
		piped_os = new PipedOutputStream();
		piped_is.connect(piped_os); 

		BufferedOutputStream bos = new BufferedOutputStream(piped_os);
		writer = new ObjectOutputStream(bos);

		thread.start();

	    } 
	    catch (Exception ex) {
		ex.printStackTrace();
	    }
	}

	public void run() {
	    try {
		BufferedInputStream bis = new BufferedInputStream(piped_is);
		reader = new ObjectInputStream(bis);
	    } catch (Exception ex) {
		ex.printStackTrace();
		return;
	    }

	    Object object;
	    while (true) {
		try {
		    object = reader.readObject();
		    System.out.println("@@@@ Deserialized as " + object);
		    link.forwardMessage((Message) object);
		} catch (Exception ex) {
		    ex.printStackTrace();
		    System.exit(-1);
		}
	    }
	}

	public synchronized void forwardMessage(Message message) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException

	{
	    // Serialize into the stream rather than pushing on the
	    // queue.
	    if (writer != null) {
		try {
		    System.out.println("@@@@ Serializing " + message);
		    writer.writeObject(message);
		    writer.flush();
		    System.out.println("@@@@ Serialized " + message);
		} catch (Exception ex) {
		    ex.printStackTrace();
		    link.forwardMessage(message);
		}
	    } else {
		System.out.println("@@@@ Forwarding " + message);
		link.forwardMessage(message);
	    }
	}

    }



}
