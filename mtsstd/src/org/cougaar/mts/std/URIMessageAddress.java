/*
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

import java.io.*;
import java.net.URI;
import java.net.URL;


/**
 * A MessageAddress which encapsulates a standard URI.  The URI must
 * be directly interpretable by the MTS.
 **/

public class URIMessageAddress 
  extends MessageAddress 
{
  private URI uri;

  /** @return the MessageAddress as a URI **/
  public URI toURI() { 
    return uri;
  }

  // public for externalizable use
  public URIMessageAddress() {}

  protected URIMessageAddress(URI uri) {
    this.uri = uri;
  }

  public final String toAddress() {
    return uri.toString();
  }


  public static URIMessageAddress getURIMessageAddress(URI uri) {
    return new URIMessageAddress(uri);
  }

  public boolean equals(URIMessageAddress ma ){
    return uri.equals(ma.uri);
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof URIMessageAddress) {
      return uri.equals(((URIMessageAddress)o).uri);      
    } else {
      return false;
    }
  }

  public final int hashCode() { 
    return uri.hashCode();
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(uri);
  }

  public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException {
    uri = (URI) in.readObject();
  }

  /*
  protected Object readResolve() {
    return cacheSimpleMessageAddress(this);
  }
  */
}
