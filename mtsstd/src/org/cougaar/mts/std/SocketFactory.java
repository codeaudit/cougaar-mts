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


import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.ServerSocket;
import java.rmi.server.RMISocketFactory;


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
{

    private AspectFactory aspectFactory;

    public SocketFactory(AspectFactory aspectFactory) {
	this.aspectFactory = aspectFactory;
    }

    public Socket createSocket(String host, int port) 
	throws java.io.IOException, java.net.UnknownHostException 
    {
	return new ClientSideSocket(host, port);
    }
    
    public ServerSocket createServerSocket(int port) 
	throws java.io.IOException 
    {
	// return getDefaultSocketFactory().createServerSocket(port);
	return new ServerSocketWithAspects(port);
    }






    private class ClientSideSocket extends Socket {
	ClientSideSocket(String host, int port) 
	    throws java.io.IOException, java.net.UnknownHostException 
	{
	    super(host, port);
	}

	public OutputStream getOutputStream() 
	    throws java.io.IOException 
	{
	    OutputStream s = super.getOutputStream();
	    if (s == null) return null;
	    s = (OutputStream) aspectFactory.attachAspects(s, OutputStream.class);
	    return s;
	}

	public InputStream getInputStream() 
	    throws java.io.IOException 
	{
	    InputStream s = super.getInputStream();
	    if (s == null) return null;
	    return (InputStream) aspectFactory.attachAspects(s, InputStream.class);
	}

    }


    private class ServerSideSocket extends Socket {
	ServerSideSocket() {
	    super();
	}

	public OutputStream getOutputStream() 
	    throws java.io.IOException 
	{
	    OutputStream s = super.getOutputStream();
	    if (s == null) return null;
	    return (OutputStream) aspectFactory.attachAspects(s, OutputStream.class);
	}

	public InputStream getInputStream() 
	    throws java.io.IOException 
	{
	    InputStream s = super.getInputStream();
	    if (s == null) return null;
	    return (InputStream) aspectFactory.attachAspects(s, InputStream.class);
	}

    }


    private class ServerSocketWithAspects extends ServerSocket {

	ServerSocketWithAspects(int port) 
	    throws java.io.IOException 
	{
	    super(port);
	}

	public Socket accept() 
	    throws java.io.IOException 
	{
	    Socket s = new ServerSideSocket();
	    implAccept(s);
	    return s;
	}
    }

}
