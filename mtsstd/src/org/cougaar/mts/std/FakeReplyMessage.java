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

import org.cougaar.core.mts.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.node.*;

import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;

/** Class is a fake message sent in reply to a fake request 
 *  to mask real message traffic 
**/
public class FakeReplyMessage extends Message {
  private byte[] stuff;
  
  /** constructor that takes, source, destination and the fake contents**/
  public FakeReplyMessage(MessageAddress source, MessageAddress dest, byte[] contents) {
    super(source, dest);
    this.stuff = contents;
  }

  // this may go away. .. but for now use the contents for the reply
  protected byte[] getContents() {
    return stuff;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("<FakeReplyMessage "+super.toString());
    //don't add the contents for now
    return sb.toString();
  }

}
