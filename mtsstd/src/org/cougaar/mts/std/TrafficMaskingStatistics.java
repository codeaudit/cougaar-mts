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

import org.cougaar.core.mts.MessageAddress;

  /** TrafficMaskingStatistics is an object that keeps track of various
   *  statistics about Fake-Request messages that have been sent to 
   *  a specific Node.
   **/
  public class TrafficMaskingStatistics {
    private MessageAddress node;
    private int requestPeriod, requestSize;
    private int msgCount = 0;
    private int totalBytesSent = 0;
    
    public TrafficMaskingStatistics(MessageAddress nodeAddress, int avgPeriod, int avgSize) {
      node = nodeAddress;
      requestPeriod = avgPeriod;
      requestSize = avgSize;
    }

    /** The statistics in this object are for fake request 
     *  messages sent to this Node.
     *  @return MessageAddress The Node the messages were sent to.
     **/
    public MessageAddress getNode() {return node;}
    
    /** Get the Average period of time (in ms) between each
     *  request being sent.
     *  @return int The request period in ms.
     **/
    public int getAvgRequestPeriod() {return requestPeriod;}
    
    /** Get the Average Size (in bytes) of the contents of the
     *  request messages.
     *  @return int The size in bytes.
     **/
    public int getAvgRequestSize() {return requestSize;}

    /** Get the Total number of fake request messages sent to
     *  to the Node in getNode()
     *  @return int The total number of request messages
     **/
    public int getTotalMessageCount() {return msgCount;}
    
    /** Get the total number of bytes sent to the Node
     *  in getNode() - note this is the total bytes of the contents
     *  of the request messages... not the bytes of the entire
     *  message (overhead for source, destination etc)
     *  @return int The number of bytes sent.
     **/
    public int getTotalBytesSent() {return totalBytesSent;}

    /** Increment Message Count by 1 (one) **/
    public void incrementCount() {
      msgCount = msgCount + 1;
    }

    /** Increment the total byte count **/
    public void incrementTotalBytes(int moreBytes) {
      totalBytesSent = totalBytesSent + moreBytes;
    }

  }
    
