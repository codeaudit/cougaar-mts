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

package org.cougaar.mts.std;

import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.base.UnregisteredNameException;

/**
 * This debugging Aspect deliberately delays message processing by wasting CPU
 * for pseudo-random durations.
 */
public class WasteCPUAspect
        extends StandardAspect {
    // This was taken from TrafficGenerator should be a math utils
    private static class ExpRandom
            extends java.util.Random {

        ExpRandom() {
            // super is uniform distribution
            super();
        }

        // period is the average period,
        // the range can go from zero to ten times the period
        public int nextInt(int period) {
            double raw = -(period * Math.log(super.nextDouble()));
            // clip upper tail
            if (raw > 10 * period) {
                return 10 * period;
            } else {
                return (int) Math.round(raw);
            }
        }
    }

    public Object getDelegate(Object object, Class<?> type) {
        if (type == DestinationLink.class) {
            DestinationLink link = (DestinationLink) object;
            return new WasteCPUDestinationLink(link);
        } else {
            return null;
        }
    }

    ExpRandom expRandom = new ExpRandom();

    private class WasteCPUDestinationLink
            extends DestinationLinkDelegateImplBase

    {

        WasteCPUDestinationLink(DestinationLink link) {
            super(link);
        }

        public synchronized MessageAttributes forwardMessage(AttributedMessage message)
                throws UnregisteredNameException, NameLookupException, CommFailureException,
                MisdeliveredMessageException

        {
            // Serialize into the stream rather than pushing on the
            // queue.
            long count = 0;
            long startTime = System.currentTimeMillis();
            int wasteTime = expRandom.nextInt(166);
            while (System.currentTimeMillis() - startTime < wasteTime) {
                count++;
            }

            // CougaarThread.yield();

            startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < wasteTime) {
                count++;
            }

            // CougaarThread.yield();

            startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < wasteTime) {
                count++;
            }

            return super.forwardMessage(message);

        }
    }
}
