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

import org.cougaar.core.service.*;

import org.cougaar.core.mts.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.node.*;

import java.io.*;

/**
 * A plain old MessageAddress
 **/

public class SimpleMessageAddress 
  extends MessageAddress 
{
  protected transient byte[] addressBytes;
  protected transient int _hc = 0;
  protected transient String _as = null;

  // public for externalizable use
  public SimpleMessageAddress() {}

  protected SimpleMessageAddress(String address) {
    this.addressBytes = address.getBytes();
    _as = address.intern();
    _hc = _as.hashCode();
  }

  public final String getAddress() {
    return _as;
  }

  public boolean equals(SimpleMessageAddress ma ){
    return (ma != null && _as == ma._as);
  }

  public boolean equals(Object o ){
    if (this == o) return true;
    // use == since the strings are interned.
    if (o instanceof MessageAddress) {
      MessageAddress oma = (MessageAddress) o;
      return (_as== oma.toAddress());
    } else {
      return false;
    }
  }
  public String toAddress() {
    return _as;
  }

  public final int hashCode() { 
    return _hc;
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    int l = addressBytes.length;
    out.writeByte(l);
    out.write(addressBytes,0,l);
  }

  public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException {
    int l = in.readByte();
    addressBytes=new byte[l];
    in.readFully(addressBytes,0,l);
    _as = new String(addressBytes).intern();
    _hc = _as.hashCode();
  }

  protected Object readResolve() {
    return cacheSimpleMessageAddress(this);
  }

  private static java.util.HashMap cache = new java.util.HashMap(89);

  public static SimpleMessageAddress getSimpleMessageAddress(String as) {
    as = as.intern();
    synchronized (cache) {
      SimpleMessageAddress a = (SimpleMessageAddress) cache.get(as);
      if (a != null) {
        return a;
      } else {
        a = new SimpleMessageAddress(as);
        cache.put(as, a);
        return a;
      }
    }
  }

  public static SimpleMessageAddress cacheSimpleMessageAddress(SimpleMessageAddress a) {
    synchronized (cache) {
      String as = a._as;
      SimpleMessageAddress x = (SimpleMessageAddress) cache.get(as);
      if (x != null) {
        return x;
      } else {
        cache.put(as, a);
        return a;
      }
    }
  }

}
