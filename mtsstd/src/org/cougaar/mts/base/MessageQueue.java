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
import java.util.LinkedList;
import java.util.List;

import org.cougaar.core.mts.Message;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.util.UnaryPredicate;

/**
 * An abstract class which manages a circular queue of messages, and runs its
 * own thread to pop messages off that queue. The method
 * <strong>dispatch</strong>, provided by instantiable subclasses, is invoked on
 * each message as it's popped off.
 */
abstract class MessageQueue
        extends BoundComponent
        implements Runnable {

    // Simplified queue
    private static class SimpleQueue<T>
            extends LinkedList<T> {
        T next() {
            return removeFirst();
        }
    }

    private final SimpleQueue<AttributedMessage> queue;
    private Schedulable thread;
    private final String name;
    private AttributedMessage in_progress;
    private final Object in_progress_lock, queue_processing;

    MessageQueue(String name) {
        this.name = name;
        in_progress_lock = new Object();
        queue_processing = new Object();
        queue = new SimpleQueue<AttributedMessage>();
    }

    int getLane() {
        return ThreadService.BEST_EFFORT_LANE;
    }

    public void load() {
        super.load();
        thread = threadService.getThread(this, this, name, getLane());
    }

    String getName() {
        return name;
    }

    public void removeMessages(UnaryPredicate pred, List<Message> removed) {
        // only one remove can be examining the queue at a time,
        // even if they are looking for orthogonal messages
        synchronized (queue_processing) {
            // Note: We are blocking processing of the queue during
            // this time, which would usually include waiting for an
            // in-progress message to complete. But messages can still
            // be added to the queue.

            AttributedMessage msg = in_progress;
            boolean matches = msg != null && pred.execute(msg);
            if (matches) {
                // Wait for the in-progress message to complete or fail.
                synchronized (in_progress_lock) {
                    if (in_progress == null) {
                        // Between the time we cached the in-progress
                        // message and the time we got the lock, the
                        // message could have been sent
                        // successfully. In that case in_progress is
                        // null and we don't care about it.
                    } else if (in_progress == msg) {
                        // Since we have the lock on queue_processing and
                        // in_progress is not null, it must still be
                        // the message we cached, which we know
                        // should be removed.
                        removed.add(in_progress);
                        in_progress = null;
                    } else {
                        loggingService.error("In Progress Message Changed which is impossible"
                                + msg);
                    }
                }
            }
            synchronized (queue) {
                Iterator<AttributedMessage> itr = queue.iterator();
                while (itr.hasNext()) {
                    msg = itr.next();
                    if (pred.execute(msg)) {
                        removed.add(msg);
                        itr.remove();
                    }
                }
            }
        }
    }

    private static final long HOLD_TIME = 500;

    // Process the last failed message, if any, followed by as many
    // items as possible from the queue, with a max time as given by
    // HOLD_TIME.
    public void run() {
        long endTime = System.currentTimeMillis() + HOLD_TIME;
        // Now process the queued items.
        while (System.currentTimeMillis() <= endTime) {
            if (in_progress == null) {
                synchronized (queue_processing) {
                    synchronized (queue) {
                        if (queue.isEmpty()) {
                            break; // done for now
                        }
                        synchronized (in_progress_lock) {
                            in_progress = queue.next();
                        }
                    }
                }
            }

            // Note that in_progress could have been set to null by
            // the time we get here, presumaly because it was killed
            // via removeMessages, which could have run between the
            // synchronization three lines up and this one. But this
            // is OK. The null will be detected and the thread will
            // simply continue processing the queue.
            synchronized (in_progress_lock) {
                if (in_progress == null) {
                    continue;
                } else if (dispatch(in_progress)) {
                    // Processing succeeded, continue popping the queue
                    in_progress = null;
                    continue;
                } else {
                    // The dispatch code has already scheduled the thread
                    // to run again later.
                    return;
                }
            }
        }

        // Ran out of time or queue is empty. Restart later if any
        // remains on the queue.
        restartIfNotEmpty();
    }

    // Restart the thread immediately if the queue is not empty.
    private void restartIfNotEmpty() {
        synchronized (queue) {
            if (!queue.isEmpty()) {
                thread.start();
            }
        }
    }

    void scheduleRestart(int delay) {
        thread.schedule(delay);
    }

    /**
     * Enqueue a message.
     */
    void add(AttributedMessage message) {
        synchronized (queue) {
            queue.add(message);
        }
        thread.start();
    }

    public AttributedMessage[] snapshot() {
        synchronized (queue) {
            AttributedMessage head = in_progress;
            int size = queue.size();
            if (head != null) {
                size++;
            }
            AttributedMessage[] ret = new AttributedMessage[size];
            int i = 0;
            Iterator<AttributedMessage> iter = queue.iterator();
            if (head != null) {
                ret[i++] = head;
            }
            while (i < size) {
                ret[i++] = iter.next();
            }
            return ret;
        }
    }

    /**
     * Process a dequeued message. Return value indicates success or failure or
     * the dispatch. Failed dispatches will be tried again before any further
     * queue entries are dispatched.
     */
    abstract boolean dispatch(AttributedMessage m);

    /**
     * Number of messages waiting in the queue.
     */
    public int size() {
        return queue.size();
    }

}
