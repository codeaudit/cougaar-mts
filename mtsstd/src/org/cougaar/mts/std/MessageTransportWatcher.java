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

import org.cougaar.core.society.Message;

/**
 * API for Metrics analysis of MessageTransport traffic analysis.
 *
 * It is extremely important that implementations of this code not
 * block or crash.
 **/

public interface MessageTransportWatcher {

  /** called whenever a message is sent (to the society) **/
  void messageSent(Message m);

  /** called whenever a message is received (from the society) **/
  void messageReceived(Message m);
}

