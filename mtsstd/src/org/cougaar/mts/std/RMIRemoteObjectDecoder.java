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

import java.io.ObjectInput;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.rmi.server.RemoteRef;
import sun.rmi.server.UnicastRef;
import sun.rmi.transport.LiveRef;

/**
 * Decode an RMI RemoteObject from a URI.
 *
 * @see RMIRemoteObjectEncoder
 */
public final class RMIRemoteObjectDecoder {

  private RMIRemoteObjectDecoder() { }

  /**
   * @param uri the URI representation
   * @return a RemoteObject
   * @see RMIRemoteObjectEncoder#encode(Object) decode is the reverse of encode
   */
  public static Object decode(URI uri) throws Exception {
    MyObjectInput lrIn = new MyObjectInput(uri);
    LiveRef lr = LiveRef.read(lrIn, false);
    final RemoteRef rr = new UnicastRef(lr);
    Class cl = Class.forName(lrIn.clname);
    Constructor cons = cl.getConstructor(new Class[] {RemoteRef.class});
    Object ret = cons.newInstance(new Object[] {rr});
    return ret;
  }
  
  /** custom object input, exactly matching UnicastRef */
  private static class MyObjectInput implements ObjectInput {

    public final String tcp_host;
    public final int tcp_port;
    public final String clname;
    public final boolean ref_remote;
    public final int uid_unique;
    public final long uid_time;
    public final short uid_count;
    public final long oid_num;

    private int state = 0;

    public MyObjectInput(URI uri) {
      try {
        if (!("rmi".equals(uri.getScheme()))) {
          throw new RuntimeException(
              "Expecting scheme to be \"rmi\", not "+uri.getScheme());
        }
        this.tcp_host=uri.getHost();
        this.tcp_port=uri.getPort();
        String s=uri.getPath();
        int i = 0;
        if (s.charAt(0) != '/') {
          throw new RuntimeException("Invalid path");
        }
        i++;
        int j = s.indexOf('/', i);
        this.clname = s.substring(i,j);
        i=j+1;
        j = s.indexOf('_', i);
        this.ref_remote = "1".equals(s.substring(i,j));
        i=j+1;
        j = s.indexOf('_', i);
        this.uid_unique = Integer.parseInt(s.substring(i,j), 16);
        i=j+1;
        j = s.indexOf('_', i);
        this.uid_time = Long.parseLong(s.substring(i,j), 16);
        i=j+1;
        j = s.indexOf('_', i);
        this.uid_count = Short.parseShort(s.substring(i,j), 16);
        i=j+1;
        this.oid_num = Long.parseLong(s.substring(i));
      } catch (Exception e) {
        throw new RuntimeException("Invalid URI: "+uri, e);
      }
    }

    public int readInt() {
      ++state;
      if (state == 2) return tcp_port;
      if (state == 4) return uid_unique;
      die(); return -1;
    }
    public long readLong() {
      ++state;
      if (state == 3) return oid_num;
      if (state == 5) return uid_time;
      die(); return -1;
    }
    public short readShort() {
      ++state;
      if (state == 6) return uid_count;
      die(); return -1;
    }
    public String readUTF() {
      ++state;
      if (state == 1) return tcp_host;
      die(); return null;
    }
    public boolean readBoolean() {
      ++state;
      if (state == 7) return ref_remote;
      die(); return false;
    }
    // die:
    private void die() {
      throw new RuntimeException(
          "LiveRef reader has changed (state="+state+")");
    }
    // DataInput:
    public void readFully(byte b[]) {die();}
    public void readFully(byte b[], int off, int len) {die();}
    public int skipBytes(int n) {die(); return -1;}
    public byte readByte() {die(); return -1;}
    public int readUnsignedByte() {die(); return -1;}
    public int readUnsignedShort() {die(); return -1;}
    public char readChar() {die(); return (char)-1;}
    public float readFloat() {die(); return -1;}
    public double readDouble() {die(); return -1;}
    public String readLine() {die(); return null;}
    // ObjectInput:
    public Object readObject() {die(); return null;}
    public int read() {die(); return -1;}
    public int read(byte b[]) {die(); return -1;}
    public int read(byte b[], int off, int len) {die(); return -1;}
    public long skip(long n) {die(); return -1;}
    public int available() {die(); return -1;}
    public void close() {die();}
  };
}
