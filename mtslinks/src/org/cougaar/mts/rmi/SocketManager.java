/*
 *
 * Copyright 2008 by BBN Technologies Corporation
 *
 */

package org.cougaar.mts.rmi;

import java.util.ArrayList;
import java.util.List;

/**
 * Strict singleton that binds together WrappedSockets and
 * SocketManagementListeners.
 */
final class SocketManager implements SocketManagementService {
    
    private static final SocketManager singleton = new SocketManager();
    
    static SocketManager getSocketManager() {
        return singleton;
    }
    
    
    private List<SocketManagementListener> listeners = new ArrayList<SocketManagementListener>();
    
    private List<ManagedServerSocket.ManagedSocket> sockets = 
        new ArrayList<ManagedServerSocket.ManagedSocket>();

    private SocketManager() {
    }
    
    public synchronized void addListener(SocketManagementListener listener) {
        // Before adding to the list, inform the new listener of all
        // currently open sockets.
        for (ManagedServerSocket.ManagedSocket socket : sockets) {
            listener.socketAdded(socket, socket.is_ssl());
        }
        listeners.add(listener);
    }

    public synchronized void removeListener(SocketManagementListener listener) {
        listeners.remove(listener);
    }

    synchronized void socketCreated(ManagedServerSocket.ManagedSocket socket) {
        for (SocketManagementListener listener : listeners) {
            listener.socketAdded(socket, socket.is_ssl());
        }
        sockets.add(socket);
    }

    synchronized void socketClosed(ManagedServerSocket.ManagedSocket socket) {
        for (SocketManagementListener listener : listeners) {
            listener.socketRemoved(socket);
        }
        sockets.remove(socket);
    }

   
}
