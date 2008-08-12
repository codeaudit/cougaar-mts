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

package org.cougaar.mts.corba;

import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.std.AttributedMessage;

/**
 * This test Aspect prefers CORBA over RMI after a delay, by cutting the cost of
 * the former 10 seconds in.
 */
public class EnableCorbaAspect
        extends StandardAspect {
    private long cutover_time;
    private final long startup_period = 10000; // make this a parameter

    public Object getDelegate(Object object, Class<?> type) {
        if (type == DestinationLink.class && object instanceof CorbaLinkProtocol) {
            return new Delegate((DestinationLink) object);
        }
        return null;
    }

    public void start() {
        cutover_time = System.currentTimeMillis() + startup_period;
        super.start();
    }

    private boolean timeToCutover() {
        long now = System.currentTimeMillis();
        return now > cutover_time;
    }

    private class Delegate
            extends DestinationLinkDelegateImplBase {
        Delegate(DestinationLink link) {
            super(link);
        }

        public int cost(AttributedMessage message) {
            System.currentTimeMillis();
            int cost = super.cost(message);
            return timeToCutover() ? cost / 10 : cost;
        }

    }

}
