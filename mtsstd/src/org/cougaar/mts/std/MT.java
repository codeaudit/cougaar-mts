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

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.*;
import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;
  
/** actual RMI remote interface for MessageTransport clients (clusters).
 **/

public interface MT extends Remote {
  /** receive a message **/
  void rerouteMessage(Message m) throws RemoteException;

  /** @return the message address of this client **/
  MessageAddress getMessageAddress() throws RemoteException;
}
