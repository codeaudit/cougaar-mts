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

import org.cougaar.core.component.ServiceBroker;


import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.rmi.server.RMISocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;


/**
 * This is not really a factory, it just needs to use AspectFactory's
 * attachAspects method.  The purpose of this class is to create an
 * RMISocketFactory with aspectizable streams.
 *
 * Instantiating this class creates the socket factory and makes it
 * the default.
 */
public class SocketFactory  
    extends RMISocketFactory
    implements java.io.Serializable
{

    // The factory will be serialized along with the MTImpl, and we
    // definitely don't want to include the AspectSupport when that
    // happens.  Instead, the aspect delegation will be handled by a
    // special static call.
    private boolean use_ssl;

    public SocketFactory(boolean use_ssl) {
	this.use_ssl = use_ssl;
    }

    
    public Socket createSocket(String host, int port) 
	throws IOException, UnknownHostException 
    {
	Socket s = null;
	if (use_ssl) {
	    try {
		s = SSLSocketFactory.getDefault().createSocket(host, port);
	    } catch (IOException ex) {
		ex.printStackTrace();
		return null;
	    }
	} else {
	    s = new Socket(host, port);
	}
 	return AspectSupportImpl.attachRMISocketAspects(s);
    }
    
    public ServerSocket createServerSocket(int port) 
	throws IOException 
    {
	ServerSocket s = null;
	if (use_ssl) {
	    try {
		s=SSLServerSocketFactory.getDefault().createServerSocket(port);
	    } catch (IOException ex) {
		ex.printStackTrace();
		return null;
	    }
	} else {
	    s = new ServerSocket(port);
	}
	return s;
    }


}
