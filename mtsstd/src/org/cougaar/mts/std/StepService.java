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
import org.cougaar.core.component.Service;
import org.cougaar.core.mts.MessageAddress;

/**
 * This MTS-internal Service can be used to 'single-step' a message
 * through the stations.  Its only real use so far is through a Swing
 * gui, but in principle it could have other uses (e.g., @{link
 * StepperControlExampleAspect}).  The implementation is in the {@link
 * StepperAspect}.
 */
public interface StepService extends Service
{
    /** 
     * Pause processing to all destinations.  The next message to each
     * destination will be frozen until a subsequent resume or
     * step. */
    void pauseAll();

    /** 
     * Pause processing to the given destination.  The next message to
     * that destination will be frozen until a subsequent resume or
     * step. */
    void pause(MessageAddress destination);


    /** Resume processing to all destinations. */
    void resumeAll();

    /** Resume processing to the given destination. */
    void resume(MessageAddress destination);


    /** Allow the frozen message (if any) to proceed, for all
     * destinations. */ 
    void stepAll();

    /** Allow the frozen message (if any) to proceed, for the given
     * destination. */ 
    void step(MessageAddress destination);
}
