/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
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
public class SerializationAspect implements MessageTransportAspect
{

    public SerializationAspect() {
    }
    
    public Object getDelegate(Object object, int cutpoint) {
	switch (cutpoint) {
	case DestinationLink:
	    return new SerializingDestinationLink((DestinationLink) object);
	    

	default:
	    return null;
	}
    }
    

    public boolean rejectTransport(MessageTransport transport, int cutpoint) {
	return (!(transport instanceof LoopbackMessageTransport));
    }

    private class SerializingDestinationLink 
	extends Thread
	implements DestinationLink
	
    {
	PipedInputStream piped_is;
	PipedOutputStream piped_os;
	ObjectInputStream reader;
	ObjectOutputStream writer;
	DestinationLink link;

	SerializingDestinationLink(DestinationLink link) {
	    this.link = link;
	    try {
		piped_is = new PipedInputStream();
		piped_os = new PipedOutputStream();
		piped_is.connect(piped_os); 

		BufferedOutputStream bos = new BufferedOutputStream(piped_os);
		writer = new ObjectOutputStream(bos);

		start();

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
	    throws DestinationLink.UnregisteredNameException, 
		   DestinationLink.NameLookupException, 
		   DestinationLink.CommFailureException

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

	public int cost(Message message) {
	    return link.cost(message);
	}
    }



}
