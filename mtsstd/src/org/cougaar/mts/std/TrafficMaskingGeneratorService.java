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

import java.util.Collection;
import org.cougaar.core.component.Service;

public interface TrafficMaskingGeneratorService extends Service {

  /** Turn on Traffic Masking for a Node with the following params
   *  @param node  The MessageAddress of the Node to send fake messages to
   *  @param avgPeriod How often to send the fake messages in ms
   *  @param avgSize The size in bytes of the contents of the fake messages 
   **/
  void setRequestParameters(MessageAddress node, int avgPeriod, int avgSize);

  /** Set the Think Time and size parameters for Fake Replies coming 
   *  from the local Node.
   *  @param thinkTime The time in ms to wait before sending a reply
   *  @param avgSize The size in bytes of the contents of the fake reply message
   **/
  void setReplyParameters(int thinkTime, int avgSize);

  /** Get information about the fake messages sent from this Node
   *  @return Collection Collection of TrafficMaskingStatistics objects
   *  @see org.cougaar.core.mts.TrafficMaskingStatistics
   **/
  Collection getStatistics();

}  
