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
 * 
 * @see org.cougaar.core.agent.ClusterIdentifier
 */

/**
 * An address for a Message sender or receiver.
 **/

public class MessageAddress implements Externalizable {
  protected transient byte[] addressBytes;
  protected transient int _hc = 0;
  protected transient String _as = null;
    private transient MessageAttributes qosAttributes;

  // don't allow subclasses access to default constructor
  public MessageAddress() {}

  public MessageAddress( String address) {
    this.addressBytes = address.getBytes();
    _as = address.intern();
    _hc = _as.hashCode();
  }

    public MessageAddress(MessageAttributes qosAttributes,
			  String address)
    {
	this(address);
	this.qosAttributes = qosAttributes;
    }

    public MessageAddress(MessageAttributes qosAttributes)
    {
	this();
	this.qosAttributes = qosAttributes;
    }


    public final MessageAttributes getQosAttributes() {
	return qosAttributes;
    }


  /** @return The address of a society member.  This is Society-centric and
   * may not be human readable or parsable.
   **/
  public final String getAddress() {
    return _as;
  }

  public boolean equals(MessageAddress ma ){
    return (_as== ma._as);
  }

  public boolean equals(Object o ){
    if (this == o) return true;
    // use == since the strings are interned.
    if (o instanceof MessageAddress) {
      MessageAddress oma = (MessageAddress) o;
      return (_as== oma._as);
    } else {
      return false;
    }
  }

  public String toString() {
    return _as;
  }


  /** @return the object address part of a URL describing the entity on
   * the COUGAAR society's pseudo-web.  e.g. the URL of an entity could be 
   * contstructed with something like protocol+"://"+host+":"+port+"/"+getAddress()+"/";
   **/
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

  public static final MessageAddress SOCIETY = new MulticastMessageAddress("SOCIETY");
  public static final MessageAddress COMMUNITY = new MulticastMessageAddress("COMMUNITY");
  public static final MessageAddress LOCAL = new MulticastMessageAddress("LOCAL");

  
}
