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
import org.cougaar.core.cluster.ClusterContext;
import org.cougaar.core.component.Service;

/**
 * Abstract MessageTransport layer for Society interaction.
 *
 **/

public interface MessageTransportService extends Service
{

  /** Ask MessageTransport to deliver a message (asynchronously).
   * message.getTarget() names the destination.
   **/

  void sendMessage(Message m);

  /** register a client with MessageTransport.  A client
   * is any object which can receive Messages directed to it
   * as the Target of a message.
   **/
  void registerClient(MessageTransportClient client);


  /** Unregister a client with MessageTransport.  A client
   * is any object which can receive Messages directed to it
   * as the Target of a message.
   **/
  void unregisterClient(MessageTransportClient client);


  /**
   * the name of the entity that this MessageTransport represents.
   * Will usually be the name of a node.
   **/
  String getIdentifier();

  /** @return true IFF the MessageAddress is known to the nameserver **/
  boolean addressKnown(MessageAddress a);
}

