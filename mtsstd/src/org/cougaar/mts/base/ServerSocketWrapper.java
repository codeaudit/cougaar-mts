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

package org.cougaar.mts.base;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;

/**
 * This class is a delegating {@link ServerSocket} that simply passes
 * all methods to another socket.  It's handy as a base class for
 * whatever wrapper class, if any, the {@link SocketFactory} is using.
 */
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


