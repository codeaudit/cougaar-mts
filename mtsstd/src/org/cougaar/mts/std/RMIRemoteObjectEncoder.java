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

import java.io.ObjectOutput;
import java.net.URI;
import java.rmi.server.RemoteObject;
import java.rmi.server.RemoteRef;
import sun.rmi.server.UnicastRef;

/**
 * Encode an RMI RemoteObject as a URI.
 *
 * @see RMIRemoteObjectDecoder
 */
public final class RMIRemoteObjectEncoder {

  private RMIRemoteObjectEncoder() { }

  /**
   * Encode an rmi RemoteObject as a URI.
   * <p>
   * The object must be a RemoteObject, and the ".getRef()" must be
   * of type UnicastRef.  This implementation is <i>strongly</i> tied
   * to the serialization format of the RMI infrastructure.  However,
   * Sun strives to keep RMI stable across JDK releases, so I don't
   * expect this to be a problem...
   * <p>
   * For example, given an object with a ".toString()" of:<pre>
   *   "com.foo.Bar_Stub[RemoteStub [ref: [endpoint:[127.0.0.1:45123](remote),objID:[189d0c:f2389d89ac:-8000, 0]]]]"
   * </pre>the generated URI will look like:<pre>
   *   "rmi://127.0.0.1:45123/com.foo.Bar_Stub/1_189d0c_f2389d89ac_-8000_0"
   * </pre>.  Note that the server-side object's ".toString()" will
   * hide the ObjID and look more like:<pre>
   *   "com.foo.Bar_Stub[RemoteStub [ref: [endpoint:[127.0.0.1:45123](local),objID:[0]]]]"
   * </pre> but we work around that here.
   *
   * @param o a RemoteObject to encode
   * @return the URI representation
   * @see RMIRemoteObjectDecoder#decode(URI)
   */
  public static URI encode(Object o) throws Exception {
    return encode(o, true);
  }

  /**
   * @param forceRemote force remote flag -- I think this only makes
   *    a difference if a client passed a decoded object off to a
   *    third party.
   */
  public static URI encode(Object o, boolean forceRemote) throws Exception {
    RemoteObject ro = (RemoteObject)o;
    String clname = o.getClass().getName();
    RemoteRef ref = ro.getRef();
    MyObjectOutput oo = new MyObjectOutput();
    ((UnicastRef) ref).writeExternal(oo);
    // Later add the two csf flags use_ssl and use_aspects
    StringBuffer buf = new StringBuffer();
    buf.append("rmi://");
    buf.append(oo.tcp_host);
    buf.append(':');
    buf.append(oo.tcp_port);
    buf.append('/');
    buf.append(clname);
    buf.append('/');
    buf.append((forceRemote || oo.ref_remote)?"1":"0");
    buf.append('_');
    buf.append(Integer.toString(oo.uid_unique,16));
    buf.append('_');
    buf.append(Long.toString(oo.uid_time,16));
    buf.append('_');
    buf.append(Integer.toString(oo.uid_count,16));
    buf.append('_');
    buf.append(oo.oid_num);
    buf.append('/');
    if (oo.has_csf) {
	buf.append(oo.use_ssl ? "1" : "0");
	buf.append(oo.use_aspects ? "1" : "0");
    }
    return new URI(buf.toString());
  }

  /** custom object output, exactly matching UnicastRef */
  private static class MyObjectOutput implements ObjectOutput {

      public String tcp_host;
      public int tcp_port;
      public long oid_num;
      public int uid_unique;
      public long uid_time;
      public short uid_count;
      public boolean ref_remote;
      public boolean has_csf;
      public boolean use_ssl;
      public boolean use_aspects;

      private int state = 0;

      private int state() {
	  return has_csf ? state-1 : state;
      }

      public void writeInt(int v) {
	  ++state;
	  if (state() == 2) {
	      tcp_port=v;
	  } else if (state() == 4) {
	      uid_unique=v;
	  } else {
	      die();
	  }
      }
      public void writeLong(long v) {
	  ++state;
	  if (state() == 3) {
	      oid_num=v;
	  } else if (state() == 5) {
	      uid_time=v;
	  } else {
	      die();
	  }
      }
      public void writeShort(int v) {
	  ++state;
	  if (state() == 6) {
	      uid_count=(short)v;
	  } else {
	      die();
	  }
      }
      public void writeUTF(String str) {
	  ++state;
	  if (state() == 1) {
	      tcp_host=str;
	  } else {
	      die();
	  }
      }
      public void writeBoolean(boolean v) {
	  ++state;
	  if (state() == 7) {
	      ref_remote=v;
	  } else {
	      die();
	  }
      }

      public void writeByte(int v) {
	  // Only called if the object has a csf
	  ++state;
	  if (state == 1) {
	      has_csf = true;
	  } else {
	      die();
	  }
      }


      public void writeObject(Object obj) {
	  if (!has_csf) die();
	  if (!(obj instanceof SocketFactory)) die();
	  if (state != 3) die();
	  SocketFactory csf = (SocketFactory) obj;
	  use_ssl = csf.use_ssl;
	  use_aspects = csf.use_aspects;
      }

      // die:
      private void die() {

	  throw new RuntimeException(
				     "RemoteObject writer has changed (state="+state+")");
      }
      // DataOutput:
      public void write(int b) {die();}
      public void write(byte b[]) {die();}
      public void write(byte b[], int off, int len) {die();}
      public void writeChar(int v) {die();}
      public void writeFloat(float v) {die();}
      public void writeDouble(double v) {die();}
      public void writeBytes(String s) {die();}
      public void writeChars(String s) {die();}
      // ObjectOutput:
      public void flush() {die();}
      public void close() {die();}
  }
}
