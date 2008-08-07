/*
 *
 * Copyright 2008 by BBN Technologies Corporation
 *
 */

package org.cougaar.mts.rmi;

import java.net.Socket;

/**
 * API for getting callbacks when an incoming RMI connection opens and closes
 */
public interface SocketManagementListener {
    void socketAdded(Socket socket, boolean is_ssl);
    void socketRemoved(Socket socket);
}
