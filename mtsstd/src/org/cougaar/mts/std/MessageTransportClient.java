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
import org.cougaar.core.society.MessageAddress;

/**
 * Abstract MessageTransport layer for Society interaction.
 *
 **/

public interface MessageTransportClient {

  /** Receive a message, presumably from a MessageTransportServer.
   * message.getTarget() should generally be our MessageAddress.
   **/

  void receiveMessage(Message message);

  /** @return this client's MessageAddress. */
  MessageAddress getMessageAddress();

}

