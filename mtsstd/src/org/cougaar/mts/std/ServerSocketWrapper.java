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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;

public abstract class ServerSocketWrapper extends ServerSocket
{
    private ServerSocket delegate;

    public ServerSocketWrapper () 
	throws java.io.IOException
    {
    }

    public void setDelegate(ServerSocket delegate) {
	this.delegate = delegate;
    }

    protected ServerSocket getDelegate() {
	return delegate;
    }

    public InetAddress getInetAddress() {
	return delegate.getInetAddress();
    }

    public int getLocalPort() {
	return delegate.getLocalPort();
    }

    public int getSoTimeout() 
	throws java.io.IOException
    {
	return delegate.getSoTimeout();
    }

    public void setInetAddress(int to) 
	throws java.net.SocketException
    {
	delegate.setSoTimeout(to);
    }

    public Socket accept() 
	throws java.io.IOException
    {
	return delegate.accept();
    }

    public void close() 
	throws java.io.IOException
    {
	delegate.close();
    }

    public String toString() {
	return delegate.toString();
    }

    public void bind(SocketAddress endpoint) 
	throws java.io.IOException
    {
	delegate.bind(endpoint);
    }

    public void bind(SocketAddress endpoint, int backlog) 
	throws java.io.IOException
    {
	delegate.bind(endpoint, backlog);
    }


    public ServerSocketChannel getChannel() 
    {
	return delegate.getChannel();
    }

    public SocketAddress getLocalSocketAddress() 
    {
	return delegate.getLocalSocketAddress();
    }

    public int getReceiveBufferSize() 
	throws SocketException
    {
	return delegate.getReceiveBufferSize();
    }

    public boolean getReuseAddress() 
	throws SocketException
    {
	return delegate.getReuseAddress();
    }

    public boolean isBound() {
	return delegate.isBound();
    }

    public boolean isClosed() {
	return delegate.isClosed();
    }

    public void setReceiveBufferSize(int size) 
	throws SocketException
    {
	delegate.setReceiveBufferSize(size);
    }

    public void setReuseAddress (boolean on) 
	throws SocketException
    {
	delegate.setReuseAddress(on);
    }

}


