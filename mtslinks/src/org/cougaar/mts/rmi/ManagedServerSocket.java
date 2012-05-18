/*
 *
 * Copyright 2008 by BBN Technologies Corporation
 *
 */

package org.cougaar.mts.rmi;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.cougaar.mts.base.ServerSocketWrapper;
import org.cougaar.mts.base.SocketDelegateImplBase;

/**
 * A wrapper for ServerSocket. At the moment this isn't handled in the aspect
 * way. Instead, the class name must be provided as the value of the system
 * property:
 * 
 * <pre>
 *  -Dorg.cougaar.message.transport.server_socket_class=org.cougaar.mts.rmi.ManagedServerSocket
 * </pre>
 */
public class ManagedServerSocket
        extends ServerSocketWrapper {

    private boolean is_ssl;

    public ManagedServerSocket()
            throws java.io.IOException {
        super();
    }

    @Override
   public void setDelegate(ServerSocket delegate) {
        super.setDelegate(delegate);
        is_ssl = delegate instanceof javax.net.ssl.SSLServerSocket;
    }

    @Override
   public Socket accept()
            throws IOException {
        ManagedSocket socket = new ManagedSocket(super.accept());
        return socket;
    }

    class ManagedSocket
            extends SocketDelegateImplBase {
        private boolean managerInformedOfClose = false;

        ManagedSocket(Socket delegatee) {
            super(delegatee);
            SocketManager.getSocketManager().socketCreated(this);
        }

        boolean is_ssl() {
            return is_ssl;
        }

        @Override
      public void close()
                throws java.io.IOException {
            // seems excessive to res -- why not use isClosed() ?
            synchronized (this) {
                if (!managerInformedOfClose) {
                    SocketManager.getSocketManager().socketClosed(this);
                    managerInformedOfClose = true;
                }
            }
            super.close();
        }

    }
}
