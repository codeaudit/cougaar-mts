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

package org.cougaar.mts.std;

import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;

import org.cougaar.mts.base.ServerSocketWrapper;
import org.cougaar.mts.base.SocketDelegateImplBase;

/**
 * This test entity can be used as a wrapper for RMI ServerSockets. If too many
 * outstanding connections are open from any one client host, it will not allow
 * any more.
 */
public class FuseServerSocket
        extends ServerSocketWrapper {
    private static final int MAX_CONNECTIONS = 10;

    private class FuseSocket
            extends SocketDelegateImplBase {
        FuseSocket(Socket delegatee) {
            super(delegatee);
        }

        public void close()
                throws java.io.IOException {
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
            System.err.println(client + " inc= " + count);
            return ++count;
        }

        synchronized int decrementCount() {
            System.err.println(client + " dec= " + count);
            return --count;
        }
    }

    private final HashMap stats;

    public FuseServerSocket()
            throws java.io.IOException {
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
            throws java.io.IOException {
        Socket socket = super.accept();
        socket = new FuseSocket(socket);
        InetAddress source = socket.getInetAddress();
        ConnectionStats record = getRecord(source);
        int count = record.incrementCount();
        if (count > MAX_CONNECTIONS) {
            try {
                socket.close();
            } catch (java.io.IOException io_ex) {
            }
        }

        return socket;
    }

}
