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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;

/**
 * This is the first station in the receiver for serializling
 * LinkProyocols.  MessageWriter and MessageReader allow aspect
 * authors to examine and filter the serialized data stream.
 *
 * <p>
 * Aspect implementers must either call the package-access addFilter
 * AttribuedMessage method or use pushValue("Filter", className) to
 * get the MessageWriter and MessageReader delegates to be attached.
 * Each message has the aspect chain built for it and the target's
 * aspect chain is governed by the list as given by the sender.
 * Normally the attribute is added in the DestinationLink's
 * forwardMessage or addAttributes method.
 * <p>
 * The previous stop is MessageWriter on the sending side.
 * The next stop is MessageDeliverer.
 * 
 * @see AttributedMessage#addFilter(StandardAspect)
 * @see Attributes#pushValue(String, Object)
 * @see SendLink
 * @see SendQueue
 * @see Router
 * @see DestinationQueue
 * @see DestinationLink
 * @see MessageWriter
 * @see MessageDeliverer
 * @see ReceiveLink
 *
 * Javadoc contributions from George Mount.
 */
public interface MessageReader
{
    
  /**
   * Called during deserialization of an AttributedMessage
   * after the message attributes have been read.
   * Gives the MessageReader
   * the opportunity to view and modify the message attributes.
   *
   * @param msg The message for which this MessageReader is designated.
   * @see #preProcess()
   */
    void finalizeAttributes(AttributedMessage msg);

  /**
   * Called by AttributedMessage during deserialization before
   * getObjectInputStream and after finalizeAttributes.
   *
   * @see #finalizeAttributes(AttributedMessage)
   * @see #getObjectInputStream(ObjectOutput)
   */
    void preProcess();

  /**
   * Called by AttributedMessage during deserialization. The
   * stream is used to read the serialized message body. The
   * returned OutputStream is usually a filtered stream that modifies
   * the contents as they are being read.
   *
   * @param out The next innermost stream in the nesting.
   * @return An OutputStream to be used for serialization of the message.
   * @see #preProcess
   * @see #finishInput
   */
    InputStream getObjectInputStream(ObjectInput in) 
	throws java.io.IOException, ClassNotFoundException;

  /**
   * Called during AttributedMessage deserialization after the message body
   * has been read.
   *
   * @throws java.io.IOException The stream could be cached 
   *                             so an IOException can be thrown here.
   * @see #getObjectInputStream(ObjectOutput)
   */
    void finishInput()
	throws java.io.IOException;

  /**
   * Called after finishInput in the AttributedMessage deserialization.
   *
   * @see #finishInput()
   */
    void postProcess();

}
