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

import org.cougaar.core.component.Component;
import org.cougaar.core.component.Service;
import org.cougaar.mts.std.AttributedMessage;

/**
 * The selection of a {@link DestinationLink} is handled by this MTS-internal
 * service, which is accessible only to MTS components. Its one method is used
 * to select a link for every message processed by every DestinationQueue.
 * 
 * The default implementation simply chooses the cheapest valid link, using the
 * cost and isValid methods. Other policies can be set with the
 * {@link LinkSelectionProvisionService}.
 */
public interface LinkSelectionPolicy
        extends Service, Component {
    /**
     * Selects a DestinationLink from the given set of candidates for the given
     * message. This method will be invoked multiple times on the same message
     * until the DestinationLink it returns succeeds in processing the message.
     * In these retry situations the message will always contains the same set
     * of attributes for every try. Any attributes added in subsequent
     * processing will be stripped before the retry. The @param failedMessage
     * parameter can be used to examine the full set of attributes in this
     * scenario.
     */
    DestinationLink selectLink(Iterator candidate_links,
                               AttributedMessage message,
                               AttributedMessage failedMsg,
                               int retryCount,
                               Exception lastException);
}
