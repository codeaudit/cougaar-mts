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


/**
 * A marker class for multicasting messages.
 * Used by constant addresses in MessageAddress.
 **/

public class MulticastMessageAddress 
  extends SimpleMessageAddress 
{
  // for Externalizable use only
  public MulticastMessageAddress() {}

  protected MulticastMessageAddress(String address) {
    super(address);
  }

  public boolean hasReceiverClass() { return false; }

  public Class getReceiverClass() { return null; }

  // factory methods

  public static final MulticastMessageAddress getMulticastMessageAddress(String address) {
    return new MulticastMessageAddress(address);
  }


  public static final MulticastMessageAddress getMulticastMessageAddress(Class clientClass) {
    return new MMAWithClass(clientClass);
  }


  public static final MessageAddress getMulticastMessageAddress(MessageAttributes ma) {
    return MessageAddressWithAttributes.getMessageAddressWithAttributes(ma);    
  }

  
  public static final MessageAddress getMulticastMessageAddress(String address, MessageAttributes attrs) {
    MessageAddress ma = MessageAddress.getMessageAddress(address);
    return MessageAddressWithAttributes.getMessageAddressWithAttributes(ma, attrs);    
  }


  public static final MessageAddress getMulticastMessageAddress(Class clientClass, MessageAttributes attrs) {
    MessageAddress ma = getMulticastMessageAddress(clientClass);
    return MessageAddressWithAttributes.getMessageAddressWithAttributes(ma, attrs);    
  }


  // private classes
  private static class MMAWithClass extends MulticastMessageAddress {
    private transient Class _myclass = null;
    public MMAWithClass() {}
    public MMAWithClass(Class clazz) {
      super(clazz.getName());
    }
    public boolean hasReceiverClass() { return true; }
    public synchronized Class getReceiverClass() { 
      if (_myclass != null) {
        return _myclass;
      } else {
        try {
          _myclass = Class.forName(toAddress());
          return _myclass;
        } catch (ClassNotFoundException cnf) {
          return null;
        }
      }
    }
  }
}
