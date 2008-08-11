/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.mts.base;

import java.io.ObjectOutput;
import java.io.OutputStream;

import org.cougaar.core.mts.Attributes;
import org.cougaar.mts.std.AttributedMessage;

/**
 * This is the final station in the sender for serializling LinkProyocols.
 * MessageWriter and MessageReader allow aspect authors to examine and filter
 * the serialized data stream.
 * 
 * <p>
 * Aspect implementers must either call the package-access addFilter
 * AttribuedMessage method or use pushValue("Filter", className) to get the
 * MessageWriter and MessageReader delegates to be attached. Each message has
 * the aspect chain built for it and the target's aspect chain is governed by
 * the list as given by the sender. Normally the attribute is added in the
 * DestinationLink's forwardMessage or addAttributes method.
 * 
 * <p>
 * The previous stop is DestinationLink. The next stop is MessageReader on the
 * receiving side.
 * 
 * @see AttributedMessage#addFilter(Object)
 * @see Attributes#pushValue(String, Object)
 * @see SendLink
 * @see SendQueue
 * @see Router
 * @see DestinationQueue
 * @see DestinationLink
 * @see MessageReader
 * @see MessageDeliverer
 * @see ReceiveLink Javadoc contributions by George Mount.
 */
public interface MessageWriter {
    /**
     * Called during serialization to give the MessageWriter the opportunity to
     * view and modify the message attributes.
     * 
     * @param msg The message for which this MessageWriter is designated.
     * @see #preProcess()
     */
    void finalizeAttributes(AttributedMessage msg);

    /**
     * Called by AttributedMessage during serialization before
     * getObjectOutputStream and after finalizeAttributes.
     * 
     * @see #finalizeAttributes(AttributedMessage)
     * @see #getObjectOutputStream(ObjectOutput)
     */
    void preProcess();

    /**
     * Called by AttributedMessage during serialization. The stream is used to
     * write the serialized message body. The returned OutputStream is usually a
     * filtered stream that modifies the contents before writing them to out.
     * 
     * @param out The next innermost stream in the nesting.
     * @return An OutputStream to be used for serialization of the message.
     * @see #preProcess
     * @see #finishOutput
     */
    OutputStream getObjectOutputStream(ObjectOutput out)
            throws java.io.IOException;

    /**
     * Called during AttributedMessage serialization after the message body has
     * been written to the OutputStream returned from getObjectOutputStream.
     * 
     * @throws java.io.IOException The stream could be cached so an IOException
     *         can be thrown here.
     * @see #getObjectOutputStream(ObjectOutput)
     */
    void finishOutput()
            throws java.io.IOException;

    /**
     * Called after all data is written to the output stream in finishOutput.
     * 
     * @see #finishOutput()
     */
    void postProcess();
}
