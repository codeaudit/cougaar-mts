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

import java.util.Iterator;

import org.cougaar.mts.std.AttributedMessage;

/**
 * A cost-based {@link LinkSelectionPolicy} that chooses the cheapest link. If
 * no other poliy is loaded, this is the one that will be used.
 */
public class MinCostLinkSelectionPolicy
        extends AbstractLinkSelectionPolicy {

    // Example of using MTS services in a selection policy
    //
    // public void load() {
    // super.load();
    // System.out.println("ID=" +getRegistry().getIdentifier());
    // }

    public DestinationLink selectLink(Iterator links,
                                      AttributedMessage message,
                                      AttributedMessage failedMessage,
                                      int retryCount,
                                      Exception lastException) {
        int min_cost = -1;
        DestinationLink cheapest = null;
        while (links.hasNext()) {
            DestinationLink link = (DestinationLink) links.next();
            int cost = link.cost(message);

            // If a link reports 0 cost, use it. With proper
            // ordering, this allows us to skip relatively expensive
            // cost calculations (eg rmi) that can't be any better
            // anyway.
            if (cost == 0) {
                return link;
            }

            // If a link reports MAX_VALUE, ignore it.
            if (cost == Integer.MAX_VALUE) {
                continue;
            }

            if (cheapest == null || cost < min_cost) {
                cheapest = link;
                min_cost = cost;
            }
        }
        return cheapest;
    }
}
