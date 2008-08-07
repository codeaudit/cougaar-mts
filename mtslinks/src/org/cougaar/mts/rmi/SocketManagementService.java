/*
 *
 * Copyright 2008 by BBN Technologies Corporation
 *
 */

package org.cougaar.mts.rmi;

import org.cougaar.core.component.Service;

/**
 * API for adding and removing {@link SocketManagementListener}s
 */
public interface SocketManagementService extends Service {
    void addListener(SocketManagementListener listener);
    void removeListener(SocketManagementListener listener);
}
