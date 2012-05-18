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

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * Special kind of IOException whose stack trace shouldn't be logged. See
 * DestinationQueueImpl.
 */

public class TransientIOException
        extends CougaarIOException {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private transient final Logger logger =
            Logging.getLogger("org.cougaar.mts.base.TransientIOException");

    public TransientIOException() {
        super();
    }

    public TransientIOException(String message) {
        super(message);
    }

    // Make these very quiet
    @Override
   public void printStackTrace() {
        if (logger.isDebugEnabled()) {
            super.printStackTrace();
        }
    }

    @Override
   public void printStackTrace(java.io.PrintStream s) {
        if (logger.isDebugEnabled()) {
            super.printStackTrace(s);
        }
    }

    @Override
   public void printStackTrace(java.io.PrintWriter w) {
        if (logger.isDebugEnabled()) {
            super.printStackTrace(w);
        }
    }

}
