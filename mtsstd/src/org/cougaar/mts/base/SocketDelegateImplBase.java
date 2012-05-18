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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

/**
 * This class is a delegating {@link Socket} that simply passes all methods to
 * another socket. It's handy as a base class for Socket aspect delegates, as
 * attached by the {@link SocketFactory}.
 */
abstract public class SocketDelegateImplBase
        extends Socket {
    private final Socket socket;

    protected SocketDelegateImplBase(Socket socket) {
        this.socket = socket;
    }

    @Override
   public void sendUrgentData(int data)
            throws java.io.IOException {
        socket.sendUrgentData(data);
    }

    @Override
   public void bind(SocketAddress bindpoint)
            throws java.io.IOException {
        socket.bind(bindpoint);
    }

    @Override
   public void connect(SocketAddress endpoint)
            throws java.io.IOException {
        socket.connect(endpoint);
    }

    @Override
   public void connect(SocketAddress endpoint, int timeout)
            throws java.io.IOException {
        socket.connect(endpoint, timeout);
    }

    @Override
   public SocketChannel getChannel() {
        return socket.getChannel();
    }

    @Override
   public SocketAddress getLocalSocketAddress() {
        return socket.getLocalSocketAddress();
    }

    @Override
   public SocketAddress getRemoteSocketAddress() {
        return socket.getRemoteSocketAddress();
    }

    @Override
   public boolean getOOBInline()
            throws SocketException {
        return socket.getOOBInline();
    }

    @Override
   public void setOOBInline(boolean on)
            throws SocketException {
        socket.setOOBInline(on);
    }

    @Override
   public boolean getReuseAddress()
            throws SocketException {
        return socket.getReuseAddress();
    }

    @Override
   public void setReuseAddress(boolean on)
            throws SocketException {
        socket.setReuseAddress(on);
    }

    @Override
   public int getTrafficClass()
            throws SocketException {
        return socket.getTrafficClass();
    }

    @Override
   public void setTrafficClass(int tc)
            throws SocketException {
        socket.setTrafficClass(tc);
    }

    @Override
   public boolean isBound() {
        return socket.isBound();
    }

    @Override
   public boolean isClosed() {
        return socket.isClosed();
    }

    @Override
   public boolean isConnected() {
        return socket.isConnected();
    }

    @Override
   public boolean isInputShutdown() {
        return socket.isInputShutdown();
    }

    @Override
   public boolean isOutputShutdown() {
        return socket.isOutputShutdown();
    }

    @Override
   public InetAddress getInetAddress() {
        return socket.getInetAddress();
    }

    @Override
   public InetAddress getLocalAddress() {
        return socket.getLocalAddress();
    }

    @Override
   public int getPort() {
        return socket.getPort();
    }

    @Override
   public int getLocalPort() {
        return socket.getLocalPort();
    }

    @Override
   public InputStream getInputStream()
            throws IOException {
        return socket.getInputStream();
    }

    @Override
   public OutputStream getOutputStream()
            throws IOException {
        return socket.getOutputStream();
    }

    @Override
   public void setTcpNoDelay(boolean flag)
            throws SocketException {
        socket.setTcpNoDelay(flag);
    }

    @Override
   public boolean getTcpNoDelay()
            throws SocketException {
        return socket.getTcpNoDelay();
    }

    @Override
   public void setSoLinger(boolean flag, int linger)
            throws SocketException {
        socket.setSoLinger(flag, linger);
    }

    @Override
   public int getSoLinger()
            throws SocketException {
        return socket.getSoLinger();
    }

    @Override
   public void setSoTimeout(int timeout)
            throws SocketException {
        socket.setSoTimeout(timeout);
    }

    @Override
   public int getSoTimeout()
            throws SocketException {
        return socket.getSoTimeout();
    }

    @Override
   public void setSendBufferSize(int size)
            throws SocketException {
        socket.setSendBufferSize(size);
    }

    @Override
   public int getSendBufferSize()
            throws SocketException {
        return socket.getSendBufferSize();
    }

    @Override
   public void setReceiveBufferSize(int size)
            throws SocketException {
        socket.setReceiveBufferSize(size);
    }

    @Override
   public int getReceiveBufferSize()
            throws SocketException {
        return socket.getReceiveBufferSize();
    }

    @Override
   public void setKeepAlive(boolean flag)
            throws SocketException {
        socket.setKeepAlive(flag);
    }

    @Override
   public boolean getKeepAlive()
            throws SocketException {
        return socket.getKeepAlive();
    }

    @Override
   public void close()
            throws IOException {
        socket.close();
    }

    @Override
   public void shutdownInput()
            throws IOException {
        socket.shutdownInput();
    }

    @Override
   public void shutdownOutput()
            throws IOException {
        socket.shutdownOutput();
    }

    @Override
   public String toString() {
        return socket.toString();
    }

}
