/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;

public class FuseServerSocket extends ServerSocketWrapper
{
    private static final int MAX_CONNECTIONS = 10;

    private class FuseSocket extends SocketDelegateImplBase {
	FuseSocket(Socket delegatee) {
	    super(delegatee);
	}

	public void close() 
	    throws java.io.IOException
	{
	    InetAddress client = getInetAddress();
	    boolean alreadyClosed = isClosed();
	    super.close();
	    if (!alreadyClosed) {
		ConnectionStats record = getRecord(client);
		record.decrementCount();
		System.err.println(this + " closed");
	    }
	}
    }



    private static class ConnectionStats {
	String client;
	int count;

	ConnectionStats(String client) {
	    this.client = client;
	    count = 0;
	}

	synchronized int incrementCount() {
	    System.err.println(client +" inc= "+ count);
	    return ++count;
	}

	synchronized int decrementCount() {
	    System.err.println(client +" dec= "+ count);
	    return --count;
	}
    }
	

    private HashMap stats;

    public FuseServerSocket()
	throws java.io.IOException
    {
	super();
	stats = new HashMap();
    }

    private ConnectionStats getRecord(InetAddress address) {
	String client = address.getCanonicalHostName();
	ConnectionStats stat = null;
	synchronized (stats) {
	    stat = (ConnectionStats) stats.get(client);
	    if (stat == null) {
		stat = new ConnectionStats(client);
		stats.put(client, stat);
	    }
	}
	return stat;
    }



    public Socket accept() 
	throws java.io.IOException
    {
	Socket socket = super.accept();
	socket = new FuseSocket(socket);
	InetAddress source = socket.getInetAddress();
	ConnectionStats record = getRecord(source);
	int count = record.incrementCount();
	if (count > MAX_CONNECTIONS) {
	    try { socket.close(); }
	    catch (java.io.IOException io_ex) { }
	} 
	    
	return socket;
    }


}
