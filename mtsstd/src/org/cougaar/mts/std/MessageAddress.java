/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.core.mts;

import org.cougaar.core.service.*;
import org.cougaar.core.node.*;

import java.io.*;

/**
 * An address for a Message sender or receiver.
 **/

public abstract class MessageAddress 
  implements Externalizable
{

  /** MessageAttributes associated with this MessageAddress.  
   * @return MessageAttributes or null
   **/
  public MessageAttributes getMessageAttributes() {
    return null;
  }
  
  /** @return The address of a society member.  This is Society-centric and
   * may not be human readable or parsable.
   **/
  public String getAddress() {
    return toAddress();
  }

  /** @return the object address part of a URL describing the entity on
   * the COUGAAR society's pseudo-web.  e.g. the URL of an entity could be 
   * contstructed with something like protocol+"://"+host+":"+port+"/"+getAddress()+"/";
   **/
  public abstract String toAddress();

  public String toString() {
    return toAddress();
  }

  /** Return the primary MessageAddress associated with this Address.
   * For example, if an address has MessageAttributes, getPrimary() will
   * return the Address without the attributes.
   * @note This is usually an identity operation.
   **/
  public MessageAddress getPrimary() {
    return this;
  }

  //
  // factory items
  //

  public static final MessageAddress NULL_SYNC = getMessageAddress("NULL");
  public static final MessageAddress MULTICAST_SOCIETY = MulticastMessageAddress.getMulticastMessageAddress("SOCIETY");
  public static final MessageAddress MULTICAST_COMMUNITY = MulticastMessageAddress.getMulticastMessageAddress("COMMUNITY");
  public static final MessageAddress MULTICAST_LOCAL = MulticastMessageAddress.getMulticastMessageAddress("LOCAL");


  public static final MessageAddress getMessageAddress(String address) {
    return SimpleMessageAddress.getSimpleMessageAddress(address);
  }

  public static final MessageAddress getMessageAddress(String address, MessageAttributes mas) {
    return MessageAddressWithAttributes.getMessageAddressWithAttributes(address,mas);
  }

  public static final MessageAddress getMessageAddress(MessageAddress address, MessageAttributes mas) {
    return MessageAddressWithAttributes.getMessageAddressWithAttributes(address,mas);
  }

  public static final MessageAddress getMessageAddress(MessageAttributes mas) {
    return MessageAddressWithAttributes.getMessageAddressWithAttributes(mas);
  }
}
