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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

abstract public class SocketDelegateImplBase extends Socket
{
    private Socket socket;

    protected SocketDelegateImplBase(Socket socket) {
	this.socket = socket;
    }

    public void sendUrgentData(int data) 
	throws java.io.IOException
    {
	socket.sendUrgentData(data);
    }

    public void bind(SocketAddress bindpoint)
	throws java.io.IOException
    {
	socket.bind(bindpoint);
    }

    public void connect(SocketAddress endpoint) 
	throws java.io.IOException
    {
	socket.connect(endpoint);
    }

    public void connect(SocketAddress endpoint, int timeout) 
	throws java.io.IOException
    {
	socket.connect(endpoint, timeout);
    }

    public SocketChannel getChannel() {
	return socket.getChannel();
    }


    public SocketAddress getLocalSocketAddress() {
	return socket.getLocalSocketAddress();
    }


    public SocketAddress getRemoteSocketAddress() {
	return socket.getRemoteSocketAddress();
    }


    public boolean getOOBInline() 
	throws SocketException
    {
	return socket.getOOBInline();
    }

    public void setOOBInline(boolean  on) 
	throws SocketException
    {
	socket.setOOBInline(on);
    }


    public boolean getReuseAddress() 
	throws SocketException
    {
	return socket.getReuseAddress();
    }

    public void setReuseAddress(boolean  on) 
	throws SocketException
    {
	socket.setReuseAddress(on);
    }


    public int getTrafficClass() 
	throws SocketException
    {
	return socket.getTrafficClass();
    }


    public void setTrafficClass(int tc) 
	throws SocketException
    {
	socket.setTrafficClass(tc);
    }


    public boolean isBound() {
	return socket.isBound();
    }

    public boolean isClosed() {
	return socket.isClosed();
    }

    public boolean isConnected() {
	return socket.isConnected();
    }


    public boolean isInputShutdown() {
	return socket.isInputShutdown();
    }

    public boolean isOutputShutdown() {
	return socket.isOutputShutdown();
    }

    public InetAddress getInetAddress() {
	return socket.getInetAddress();
    }

    public InetAddress getLocalAddress() {
	return socket.getLocalAddress();
    }

    public int getPort() {
	return socket.getPort();
    }

    public int getLocalPort() {
	return socket.getLocalPort();
    }

    public InputStream getInputStream() 
	throws IOException
    {
	return socket.getInputStream();
    }

    public OutputStream getOutputStream() 
	throws IOException
    {
	return socket. getOutputStream();
    }

    public void setTcpNoDelay(boolean flag) 
	throws SocketException
    {
	socket.setTcpNoDelay(flag);
    }

    public boolean getTcpNoDelay() 
	throws SocketException
    {
	return socket.getTcpNoDelay();
    }

    public void setSoLinger(boolean flag, int linger) 
	throws SocketException
    {
	socket.setSoLinger(flag, linger);
    }

    public int getSoLinger()
	throws SocketException
    {
	return socket.getSoLinger();
    }

    public void setSoTimeout(int timeout)
	throws SocketException
    {
	socket.setSoTimeout(timeout);
    }

    public int getSoTimeout() 
	throws SocketException
    {
	return socket.getSoTimeout();
    }

    public void setSendBufferSize(int size) 
	throws SocketException
    {
	socket.setSendBufferSize(size);
    }

    public int getSendBufferSize() 
	throws SocketException
    {
	return socket.getSendBufferSize();
    }

    public void setReceiveBufferSize(int size) 
	throws SocketException
    {
	socket.setReceiveBufferSize(size);
    }

    public int getReceiveBufferSize() 
	throws SocketException
    {
	return socket.getReceiveBufferSize();
    }

    public void setKeepAlive(boolean flag)
	throws SocketException
    {
	socket.setKeepAlive(flag);
    }

    public boolean getKeepAlive() 
	throws SocketException
    {
	return socket.getKeepAlive();
    }

    public void close() 
	throws IOException
    {
	socket.close();
    }

    public void shutdownInput() 
	throws IOException
    {
	socket.shutdownInput();
    }

    public void shutdownOutput() 
	throws IOException
    {
	socket.shutdownOutput();
    }

    public String toString() {
	return socket.toString();
    }

}
