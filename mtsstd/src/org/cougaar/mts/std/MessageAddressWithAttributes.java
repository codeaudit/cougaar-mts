/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;


import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * A MessageAddress which includes MessageAttributes
 **/

public class MessageAddressWithAttributes 
  extends MessageAddress
{
  private transient MessageAddress delegate;
  private transient MessageAttributes attributes;

  public MessageAddressWithAttributes() {}

  protected MessageAddressWithAttributes (MessageAddress delegate,
                                          MessageAttributes attributes)
  {
    this.delegate = delegate;
    this.attributes = attributes;
  }

    /**
     * @deprecated Why would you want a MessageAddress that only has attributes?
     */
  protected MessageAddressWithAttributes(MessageAttributes attributes)
  {
    Logger logger = Logging.getLogger(getClass().getName());
    if (logger.isErrorEnabled())
	logger.error("Creating a MessageAddress with attributes but no name!");
    delegate = null;
    this.attributes = attributes;
  }

  protected MessageAddressWithAttributes(String addr, MessageAttributes attrs) {
    delegate = MessageAddress.getMessageAddress(addr);
    attributes = attrs;
  }

  /** @return The MessageAddress without the MessageAtributes **/
  public final MessageAddress getPrimary() {
      return delegate == null ? null : delegate.getPrimary();
  }

  /** @return The Parent MessageAddress.  This is usually the same
   * as the result of getPrimary();
   **/
  public final MessageAddress getDelegate() {
    return delegate;
  }

  public final String toAddress() {
      return (delegate == null) ? null : delegate.toAddress();
  }

  public final MessageAttributes getMessageAttributes() {
    return attributes;
  }

  public static final MessageAddress getMessageAddressWithAttributes(MessageAddress ma,
                                                                     MessageAttributes mas) {
    return new MessageAddressWithAttributes(ma, mas);
  }

  public static final MessageAddress getMessageAddressWithAttributes(String address,
                                                                     MessageAttributes mas) {
    MessageAddress ma = MessageAddress.getMessageAddress(address);
    return new MessageAddressWithAttributes(ma, mas);
  }

    /**
     * @deprecated Why would you want a MessageAddress that only has attributes?
     */
  public static final MessageAddress getMessageAddressWithAttributes(MessageAttributes mas) {
    return new MessageAddressWithAttributes(mas);
  }

  //
  // io
  //

  // should never be used - see writeReplace
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(delegate);
    // attributes are transient
  }

  public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException {
    delegate = (MessageAddress) in.readObject();
    // attributes are transient
  }

  private Object writeReplace() {
    return delegate;
  }

}


