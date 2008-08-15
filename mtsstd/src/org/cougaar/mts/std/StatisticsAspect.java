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

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;

import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.MessageStatistics;
import org.cougaar.core.service.MessageStatisticsService;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.DestinationQueue;
import org.cougaar.mts.base.DestinationQueueDelegateImplBase;
import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MessageDelivererDelegateImplBase;
import org.cougaar.mts.base.MessageReader;
import org.cougaar.mts.base.MessageReaderDelegateImplBase;
import org.cougaar.mts.base.MessageWriter;
import org.cougaar.mts.base.MessageWriterDelegateImplBase;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.StandardAspect;
import org.cougaar.mts.base.UnregisteredNameException;

/**
 * This Aspect gathers message size and count statistics using the InputStream
 * and OutputStream of streamed protocols. The statistics are made accessible
 * via the {@link MessageStatisticsService}, which this class implements.
 * Per-message size statistics are also added to the AttributedMessage itself,
 * as attributes.
 */
public class StatisticsAspect
        extends StandardAspect
        implements MessageStatistics, MessageStatisticsService, AttributeConstants

{
    // This variable holds the current size of ALL
    // destination queues, so it behaves as it did in the original
    // RMIMessageTransport (which had a single outgoing queue).
    static int currentAllQueuesSize = 0;

    private long lastUpdate = System.currentTimeMillis();
    private long totalElapsedTime = 0L;
    private long totalQueueLength = 0L;
    private long statisticsSentTotalBytes = 0L;
    private long statisticsSentTotalMessages = 0L;
    private long statisticsSentHeaderBytes = 0L;
    private long statisticsSentAckBytes = 0L; // Acks sent for msgs received
    private long statisticsRecvTotalBytes = 0L;
    private long statisticsRecvTotalMessages = 0L;
    private long statisticsRecvHeaderBytes = 0L;
    private long statisticsRecvAckBytes = 0L; // Acks received for msgs sent
    private long[] messageLengthHistogram = null;

    public StatisticsAspect() {
        messageLengthHistogram = new long[MessageStatistics.NBINS];
    }

    public synchronized MessageStatistics.Statistics getMessageStatistics(boolean reset) {
        MessageStatistics.Statistics result =
                new MessageStatistics.Statistics(averageQueueLength(),
                                                 statisticsSentTotalBytes,
                                                 statisticsSentHeaderBytes,
                                                 statisticsSentAckBytes,
                                                 statisticsSentTotalMessages,
                                                 statisticsRecvTotalBytes,
                                                 statisticsRecvHeaderBytes,
                                                 statisticsRecvAckBytes,
                                                 statisticsRecvTotalMessages,
                                                 messageLengthHistogram);
        if (reset) {
            totalElapsedTime = 0L;
            totalQueueLength = 0L;
            statisticsSentTotalBytes = 0L;
            statisticsSentHeaderBytes = 0L;
            statisticsSentAckBytes = 0L;
            statisticsSentTotalMessages = 0L;
            statisticsRecvTotalBytes = 0L;
            statisticsRecvHeaderBytes = 0L;
            statisticsRecvAckBytes = 0L;
            statisticsRecvTotalMessages = 0L;

            for (int i = 0; i < messageLengthHistogram.length; i++) {
                messageLengthHistogram[i] = 0;
            }
        }
        return result;
    }

    private int getIntAttribute(MessageAttributes attrs, String key) {
        int result = 0;
        Object attr = attrs.getAttribute(key);
        if (attr != null && attr instanceof Number) {
            result = ((Number) attr).intValue();
        }
        return result;
    }

    private double averageQueueLength() {
        if (totalElapsedTime == 0) {
            return 0;
        }
        return totalQueueLength / (0.0 + totalElapsedTime);
    }

    private synchronized void updateQueueStatistics() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastUpdate;
        lastUpdate = now;
        totalElapsedTime += elapsed;

        // Queuelength wieghted by time spent at this length.
        totalQueueLength += currentAllQueuesSize * elapsed;
    }

    private synchronized void updateMessageLengthStatistics(int byteCount) {
        int bin = 0;
        int maxBin = MessageStatistics.NBINS - 1;
        while (bin < maxBin && byteCount > MessageStatistics.BIN_SIZES[bin]) {
            bin++;
        }
        messageLengthHistogram[bin]++;
        statisticsSentTotalBytes += byteCount;
    }

    public Object getDelegate(Object delegatee, Class<?> type) {
        if (type == MessageWriter.class) {
            MessageWriter wtr = (MessageWriter) delegatee;
            return new StatisticsWriter(wtr);
        } else if (type == MessageReader.class) {
            MessageReader rdr = (MessageReader) delegatee;
            return new StatisticsReader(rdr);
        } else if (type == DestinationQueue.class) {
            return new StatisticsDestinationQueue((DestinationQueue) delegatee);
        } else if (type == DestinationLink.class) {
            DestinationLink link = (DestinationLink) delegatee;
            return new StatisticsLink(link);
        } else if (type == MessageDeliverer.class) {
            MessageDeliverer deliverer = (MessageDeliverer) delegatee;
            return new StatisticsDeliverer(deliverer);
        }

        return null;
    }

    private class StatisticsLink
            extends DestinationLinkDelegateImplBase {
        StatisticsLink(DestinationLink delegatee) {
            super(delegatee);
        }

        public MessageAttributes forwardMessage(AttributedMessage msg)
                throws NameLookupException, UnregisteredNameException, CommFailureException,
                MisdeliveredMessageException {
            // Register Aspect as a Message Streaming filter
            msg.addFilter(StatisticsAspect.this);
            MessageAttributes result = null;
            try {
                result = super.forwardMessage(msg);
                // successful send
                int msgBytes = getIntAttribute(msg, MESSAGE_BYTES_ATTRIBUTE);
                int hdrBytes = getIntAttribute(msg, HEADER_BYTES_ATTRIBUTE);
                int ackBytes = getIntAttribute(result, HEADER_BYTES_ATTRIBUTE);
                updateMessageLengthStatistics(msgBytes);
                statisticsSentHeaderBytes += hdrBytes;
                statisticsRecvAckBytes += ackBytes;
                updateQueueStatistics();
                --currentAllQueuesSize;
                ++statisticsSentTotalMessages;
                if (loggingService.isDebugEnabled()) {
                    MessageStatistics.Statistics stats = getMessageStatistics(false);
                    loggingService.debug("Send Count=" + stats.totalSentMessageCount + " Bytes="
                            + stats.totalSentMessageBytes + " Average Message Queue Length="
                            + stats.averageMessageQueueLength);
                }
                return result;
            } catch (NameLookupException ex1) {
                throw ex1;
            } catch (UnregisteredNameException ex2) {
                throw ex2;
            } catch (CommFailureException ex3) {
                throw ex3;
            } catch (MisdeliveredMessageException ex4) {
                throw ex4;
            }
        }
    }

    private class StatisticsDeliverer
            extends MessageDelivererDelegateImplBase {
        StatisticsDeliverer(MessageDeliverer deliverer) {
            super(deliverer);
        }

        public MessageAttributes deliverMessage(AttributedMessage msg, MessageAddress dest)
                throws MisdeliveredMessageException

        {
            try {
                MessageAttributes ack = super.deliverMessage(msg, dest);
                // successful send
                int msgBytes = getIntAttribute(msg, MESSAGE_BYTES_ATTRIBUTE);
                int hdrBytes = getIntAttribute(msg, HEADER_BYTES_ATTRIBUTE);
                int ackBytes = getIntAttribute(ack, HEADER_BYTES_ATTRIBUTE);
                statisticsRecvTotalBytes += msgBytes;
                statisticsRecvHeaderBytes += hdrBytes;
                statisticsSentAckBytes += ackBytes;
                ++statisticsRecvTotalMessages;
                if (loggingService.isDebugEnabled()) {
                    MessageStatistics.Statistics stats = getMessageStatistics(false);
                    loggingService.debug("Recv Count=" + stats.totalRecvMessageCount + " Bytes="
                            + stats.totalRecvMessageBytes);
                }
                return ack;
            } catch (MisdeliveredMessageException ex) {
                throw ex;
            }
        }

    }

    private class StatisticsDestinationQueue
            extends DestinationQueueDelegateImplBase {

        StatisticsDestinationQueue(DestinationQueue queue) {
            super(queue);
        }

        public void holdMessage(AttributedMessage m) {
            updateQueueStatistics();
            ++currentAllQueuesSize;
            super.holdMessage(m);
        }

    }

    private static class StatisticsOutputStream
            extends FilterOutputStream {
        int byteCount = 0;

        public StatisticsOutputStream(OutputStream out) {
            super(out);
        }

        public void write(int b)
                throws java.io.IOException {
            out.write(b);
            byteCount += 1;
        }

        public void write(byte[] b)
                throws java.io.IOException {
            out.write(b);
            byteCount += b.length;
        }

        public void write(byte[] b, int o, int len)
                throws java.io.IOException {
            out.write(b, o, len);
            byteCount += len;
        }
    }

    private class StatisticsWriter
            extends MessageWriterDelegateImplBase {

        StatisticsOutputStream wrapper;
        AttributedMessage msg;

        StatisticsWriter(MessageWriter delegatee) {
            super(delegatee);
        }

        public void finalizeAttributes(AttributedMessage msg) {
            super.finalizeAttributes(msg);
            this.msg = msg;
        }

        // Create and return the byte-counting FilterOutputStream
        public OutputStream getObjectOutputStream(ObjectOutput out)
                throws java.io.IOException {
            OutputStream raw_os = super.getObjectOutputStream(out);
            wrapper = new StatisticsOutputStream(raw_os);
            return wrapper;
        }

        public void finishOutput()
                throws java.io.IOException {
            super.finishOutput();
            int msgBytes = wrapper.byteCount;

            msg.setLocalAttribute(MESSAGE_BYTES_ATTRIBUTE, new Integer(msgBytes));
        }

    }

    // MessageReader delegate. Counts the bytes.
    private static class StatisticsInputStream
            extends FilterInputStream {

        int byteCount = 0;

        private StatisticsInputStream(InputStream wrapped) {
            super(wrapped);
        }

        public int read()
                throws java.io.IOException {
            byteCount++;
            return in.read();
        }

        public int read(byte[] b, int off, int len)
                throws java.io.IOException {
            int bytes_read = in.read(b, off, len);
            byteCount += bytes_read;
            return bytes_read;
        }

        public int read(byte[] b)
                throws java.io.IOException {
            int bytes_read = in.read(b);
            byteCount += bytes_read;
            return bytes_read;
        }
    }

    private static class StatisticsReader
            extends MessageReaderDelegateImplBase {

        StatisticsInputStream wrapper;
        AttributedMessage msg;

        StatisticsReader(MessageReader delegatee) {
            super(delegatee);
        }

        public InputStream getObjectInputStream(ObjectInput in)
                throws java.io.IOException, ClassNotFoundException {
            InputStream raw_is = super.getObjectInputStream(in);
            wrapper = new StatisticsInputStream(raw_is);
            return wrapper;
        }

        public void finalizeAttributes(AttributedMessage msg) {
            this.msg = msg;
            super.finalizeAttributes(msg);
        }

        public void finishInput()
                throws java.io.IOException {
            super.finishInput();
            int msgBytes = wrapper.byteCount;
            msg.setLocalAttribute(MESSAGE_BYTES_ATTRIBUTE, new Integer(msgBytes));
        }

    }

}
