/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.core.mts;

import org.cougaar.core.service.*;

import org.cougaar.core.node.*;

import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageStatistics;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * This aspect adds simple statistics gathering to the client side
 * OutputStream of RMI connections.
 */
public class StatisticsAspect 
    extends StandardAspect
    implements MessageStatistics, MessageStatisticsService

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

    public StatisticsAspect() {
	messageLengthHistogram = new long[MessageStatistics.NBINS];
    }

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


    public Object getDelegate(Object object, Class type) 
    {
	if (type == Socket.class) {
	    return new StatisticsSocket((Socket) object);
	} else if (type == DestinationQueue.class) {
	    return new StatisticsDestinationQueue((DestinationQueue) object);
	} else {
	    return null;
	}
    }


    private class StatisticsDestinationQueue 
	extends DestinationQueueDelegateImplBase
    {
	long then = System.currentTimeMillis();

	StatisticsDestinationQueue(DestinationQueue queue) {
	    super(queue);
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

	public void dispatchNextMessage(Message message) {
	    --current_total_size;
	    accumulateStatistics();
	    if (debugService.isDebugEnabled(STATISTICS)) {
		MessageStatistics.Statistics result = 
		    getMessageStatistics(false);
		debugService.debug("Count=" + result.totalMessageCount
				   + " Bytes=" + result.totalMessageBytes
				   + " Average Message Queue Length=" +
				   result.averageMessageQueueLength);
	    }
	    queue.dispatchNextMessage(message);
	}


    }





    private class StatisticsSocket extends SocketDelegateImplBase {
	private StatisticsSocket(Socket socket) {
	    super(socket);
	}


	public OutputStream getOutputStream() 
	    throws IOException 
	{
	    OutputStream s = super.getOutputStream();
	    return (s == null) ? null : new StatisticsStreamWrapper(s);
	}


    }


    private class StatisticsStreamWrapper extends OutputStream {
	OutputStream wrapped;
	int byteCount = 0;
	public StatisticsStreamWrapper(OutputStream wrapped) {
	    this.wrapped = wrapped;
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

	    if (debugService.isDebugEnabled(STATISTICS))
		debugService.debug("byteCount = " + byteCount);

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
