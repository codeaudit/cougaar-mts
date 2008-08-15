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

import org.cougaar.core.thread.Schedulable;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.SendLink;
import org.cougaar.mts.base.SendLinkDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;

/**
 * This test Aspect scrambles the order of messages. Its main purpose is to test
 * the effectiveness of the {@link SequenceAspect}.
 * 
 */
public class ScrambleAspect
        extends StandardAspect {
    public ScrambleAspect() {
    }

    public Object getDelegate(Object delegate, Class<?> type) {
        if (type == SendLink.class) {
            return new ScrambledSendLink((SendLink) delegate);
        } else {
            return null;
        }
    }

    private class ScrambledSendLink
            extends SendLinkDelegateImplBase {

        Schedulable sender;
        AttributedMessage heldMessage;

        int heldMessageCount;
        int flippedMessageCount;
        int forcedMessageCount;
        int messageCount;

        private ScrambledSendLink(SendLink link) {
            super(link);
            // long timeStarted = System.currentTimeMillis();
        }

        private class MessageSender
                implements Runnable {
            public void run() {
                forcedHeldMessage();
            }
        }

        public synchronized void sendMessage(AttributedMessage message) {
            messageCount++;
            if (heldMessage == null) {
                holdMessage(message);
            } else {
                flipMessage(message);
            }
        }

        // ================util methods
        private void holdMessage(AttributedMessage message) {
            heldMessage = message;
            MessageSender sender_body = new MessageSender();
            sender = threadService.getThread(this, sender_body, "Scramble");
            sender.schedule(300);
            heldMessageCount++;
            if (loggingService.isDebugEnabled()) {
                loggingService.debug("Holding message #" + printString() + "  " + heldMessageCount);
            }
        }

        private void flipMessage(AttributedMessage message) {
            sender.cancel();
            // Cancelling the task doesn't guarantee that it won't
            // run. But the only purpose of this aspect is to test
            // weird cases (messages out of order) so it might as well
            // test duplicates sometimes too...

            super.sendMessage(message);
            super.sendMessage(heldMessage);
            heldMessage = null;
            flippedMessageCount++;
            int previousCount = messageCount - 1;
            if (loggingService.isDebugEnabled()) {
                loggingService.debug("Flipping messages #" + previousCount + " and #"
                        + printString() + " and " + message.getTarget() + "  "
                        + flippedMessageCount);
            }
        }

        private synchronized void forcedHeldMessage() {
            if (heldMessage != null) {
                super.sendMessage(heldMessage);
                forcedMessageCount++;
                if (loggingService.isDebugEnabled()) {
                    loggingService.debug("Forcing message #" + printString() + "  "
                            + forcedMessageCount);
                }
                heldMessage = null;
            }
        }

        private String printString() {
            return messageCount + " from " + heldMessage.getOriginator() + " to "
                    + heldMessage.getTarget();
        }
        // ===========================
    }

}
