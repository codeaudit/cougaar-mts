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


/**
 * Convenience class for aspects which define {@link MessageWriter} delegate
 * classes. It implements all methods by delegating to another instance, given
 * in the constructor. Aspect inner classes which extend this need only
 * implement specific methods that are relevant to that aspect,
 * 
 */
public class MessageWriterDelegateImplBase
        implements MessageWriter {
    private final MessageWriter delegate;

    public MessageWriterDelegateImplBase(MessageWriter delegate) {
        this.delegate = delegate;
    }

    public void finalizeAttributes(AttributedMessage msg) {
        delegate.finalizeAttributes(msg);
    }

    public void preProcess() {
        delegate.preProcess();
    }

    public OutputStream getObjectOutputStream(ObjectOutput out)
            throws java.io.IOException {
        return delegate.getObjectOutputStream(out);
    }

    public void finishOutput()
            throws java.io.IOException {
        delegate.finishOutput();
    }

    public void postProcess() {
        delegate.postProcess();
    }

}
