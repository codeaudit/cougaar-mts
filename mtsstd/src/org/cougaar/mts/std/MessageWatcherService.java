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

import org.cougaar.core.component.Service;

public interface MessageWatcherService extends Service
{

  /**
   * add a MessageTransportWatcher to the server.
   **/
  void addMessageTransportWatcher(MessageTransportWatcher watcher);

}

