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
    implements MessageTransportCutpoints
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
	    return (OutputStream) aspectFactory.attachAspects(s, RmiClientOutputStream);
	}

	public InputStream getInputStream() 
	    throws java.io.IOException 
	{
	    InputStream s = super.getInputStream();
	    if (s == null) return null;
	    return (InputStream) aspectFactory.attachAspects(s, RmiClientInputStream);
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
	    return (OutputStream) aspectFactory.attachAspects(s, RmiServerOutputStream);
	}

	public InputStream getInputStream() 
	    throws java.io.IOException 
	{
	    InputStream s = super.getInputStream();
	    if (s == null) return null;
	    return (InputStream) aspectFactory.attachAspects(s, RmiServerInputStream);
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
