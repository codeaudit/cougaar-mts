/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.mts;

import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.society.MessageStatistics;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This aspect adds simple statistics gathering to the client side
 * OutputStream of RMI connections.
 */
public class StatisticsAspect 
    extends StandardAspect
    implements MessageStatistics, Debug, MessageStatisticsService

{
    // This variable holds the total current size of ALL
    // destination queues, so it behaves as it did in the original
    // RMIMessageTransport (which had a single outgoing queue).
    static int current_total_size = 0;



    private long statisticsTotalBytes = 0L;
    private long[] messageLengthHistogram = null;
    private long statisticsTotalMessages = 0L;
    private long totalElapsedTime = 0L;
    private long totalQueueLength = 0L;



    public synchronized MessageStatistics.Statistics 
	getMessageStatistics(boolean reset) 
    {
	MessageStatistics.Statistics result =
	    new  MessageStatistics.Statistics((totalElapsedTime == 0 ?
					       0.0 :
					       (0.0 + totalQueueLength) /
					       (0.0 + totalElapsedTime)),
					      statisticsTotalBytes,
					      statisticsTotalMessages,
					      messageLengthHistogram);
	if (reset) {
	    totalElapsedTime = 0L;
	    totalQueueLength = 0L;
	    statisticsTotalBytes = 0L;
	    statisticsTotalMessages = 0L;
	    for (int i = 0; i < messageLengthHistogram.length; i++) {
		messageLengthHistogram[i] = 0;
	    }
	}
	return result;
    }



    private synchronized void accumulateMessageStatistics(long elapsed,
							  int queueLength)
    {
	totalElapsedTime += elapsed;
	totalQueueLength += queueLength;
    }

    private synchronized void countMessage() {
	statisticsTotalMessages++;
    }


    public Object getDelegate(Object object, Class type) {
	if (type == OutputStream.class) {
	    return new StatisticsStreamWrapper((OutputStream) object);
	} else if (type == DestinationQueue.class) {
	    return new StatisticsDestinationQueue((DestinationQueue) object);
	} else {
	    return null;
	}
    }


    private class StatisticsDestinationQueue implements DestinationQueue {
	DestinationQueue queue;
	long then = System.currentTimeMillis();

	StatisticsDestinationQueue(DestinationQueue queue) {
	    this.queue = queue;
	}

	void accumulateStatistics() {
	    long now = System.currentTimeMillis();
	    accumulateMessageStatistics(now - then, current_total_size);
	    then = now;
	}


	public void holdMessage(Message m) {
	    ++current_total_size;
	    queue.holdMessage(m);
	    accumulateStatistics();
	    countMessage();
	}

	public Object next() {
	    --current_total_size;
	    accumulateStatistics();
	    Object next = queue.next();
	    
	    if (DEBUG_TRANSPORT) {
		MessageStatistics.Statistics result = 
		    getMessageStatistics(false);
		System.err.println("###### Count=" + result.totalMessageCount
				   + " Bytes=" + result.totalMessageBytes
				   + " Average Message Queue Length=" +
				   result.averageMessageQueueLength);
	    }

	    return next;
	}


	public boolean matches(MessageAddress addr) {
	    return queue.matches(addr);
	}

	public boolean isEmpty() {
	    return queue.isEmpty();
	}

	public int size() {
	    return queue.size();
	}
    }

    private class StatisticsStreamWrapper extends OutputStream {
	OutputStream wrapped;
	int byteCount = 0;
	public StatisticsStreamWrapper(OutputStream wrapped) {
	    this.wrapped = wrapped;
	    if (messageLengthHistogram == null) {
		messageLengthHistogram = new long[MessageStatistics.NBINS];
	    }
	}
	public void close() throws IOException {
	    wrapped.close();
	}
	public void flush() throws IOException {
	    wrapped.flush();
	    int bin = 0;
	    int maxBin = MessageStatistics.NBINS - 1;
	    while (bin < maxBin && byteCount >= MessageStatistics.BIN_SIZES[bin]) {
		bin++;
	    }
	    messageLengthHistogram[bin]++;
	    statisticsTotalBytes += byteCount;

	    if (DEBUG_TRANSPORT)
		System.err.println("%%%%%%% byteCount = " + byteCount);

	    byteCount = 0;
	}
	public void write(int b) throws IOException {
	    wrapped.write(b);
	    synchronized (StatisticsAspect.this) {
		byteCount += 1;
	    }
	}
	public void write(byte[] b) throws IOException {
	    wrapped.write(b);
	    synchronized (StatisticsAspect.this) {
		byteCount += b.length;
	    }
	}
	public void write(byte[] b, int o, int len) throws IOException {
	    wrapped.write(b, o, len);
	    synchronized (StatisticsAspect.this) {
		byteCount += len;
	    }
	}
    }



}
