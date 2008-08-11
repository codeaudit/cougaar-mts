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

import java.util.ArrayList;

import org.cougaar.mts.std.AspectFactory;
import org.cougaar.mts.std.AttributedMessage;

/**
 * This factory is used to create MessageReaders and MessageWriters with a
 * specific list of Aspects. It's not a service primarily because the only
 * client is {@link AttributedMessage}, which has no ready access to a
 * ServiceBroker.
 */
public final class MessageStreamsFactory
        extends AspectFactory {
    private static MessageStreamsFactory factory;

    public static synchronized MessageStreamsFactory makeFactory() {
        factory = new MessageStreamsFactory();
        return factory;
    }

    public static synchronized MessageStreamsFactory getFactory() {
        return factory;
    }

    private MessageStreamsFactory() {
    }

    public MessageReader getMessageReader(ArrayList aspectNames) {
        MessageReader rdr = new MessageReaderImpl();
        return (MessageReader) attachAspects(rdr, MessageReader.class, aspectNames);
    }

    public MessageWriter getMessageWriter(ArrayList aspectNames) {
        MessageWriter wtr = new MessageWriterImpl();
        return (MessageWriter) attachAspects(wtr, MessageWriter.class, aspectNames);
    }

    /**
     * Free resources
     */
    public void releaseFactory() {
        // Nullify static variable so it can be reclaimed by the garbage
        // collector.
        factory = null;
    }

}
