/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.MessageStatisticsService;

/**
 * This aspect adds simple statistics gathering to the client side
 * OutputStream of RMI connections.
 */
public class StatisticsAspect 
    extends StandardAspect
    implements MessageStatistics, MessageStatisticsService,AttributeConstants

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


    public Object getDelegate(Object delegatee, Class type) 
    {
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
	    // Only RMI is relevant here
	    Class cls = link.getProtocolClass();
	    if (RMILinkProtocol.class.isAssignableFrom(cls))
		return new StatisticsLink(link);
	}

	return null;
    }


    private class StatisticsLink 
	extends DestinationLinkDelegateImplBase
    {
	StatisticsLink(DestinationLink delegatee) {
	    super(delegatee);
	}


	public MessageAttributes forwardMessage(AttributedMessage message) 
	    throws NameLookupException, 
		   UnregisteredNameException, 
		   CommFailureException,
		   MisdeliveredMessageException
	{
	    // Register Aspect as a Message Streaming filter
 	    message.addFilter(StatisticsAspect.this);
	    return super.forwardMessage(message);
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


	public void holdMessage(AttributedMessage m) {
	    ++current_total_size;
	    super.holdMessage(m);
	    accumulateStatistics();
	    countMessage();
	}

	public void dispatchNextMessage(AttributedMessage message) {
	    --current_total_size;
	    accumulateStatistics();
	    if (loggingService.isDebugEnabled()) {
		MessageStatistics.Statistics result = 
		    getMessageStatistics(false);
		loggingService.debug("Count=" + result.totalMessageCount
				   + " Bytes=" + result.totalMessageBytes
				   + " Average Message Queue Length=" +
				   result.averageMessageQueueLength);
	    }
	    super.dispatchNextMessage(message);
	}


    }





    private static class StatisticsOutputStream extends FilterOutputStream {
	int byteCount = 0;

	public StatisticsOutputStream(OutputStream out) {
	    super(out);
	}


	public void write(int b) 
	    throws java.io.IOException 
	{
	    out.write(b);
	    byteCount += 1;
	}

	public void write(byte[] b) 
	    throws java.io.IOException 
	{
	    out.write(b);
	    byteCount += b.length;
	}

	public void write(byte[] b, int o, int len) 
	    throws java.io.IOException 
	{
	    out.write(b, o, len);
	    byteCount += len;
	}
    }

    private class StatisticsWriter
	extends MessageWriterDelegateImplBase
    {

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
	    throws java.io.IOException
	{
	    OutputStream raw_os = super.getObjectOutputStream(out);
	    wrapper = new StatisticsOutputStream(raw_os);
	    return wrapper;
	}


	public void finishOutput() 
	    throws java.io.IOException
	{
	    super.finishOutput();
	    int byteCount = wrapper.byteCount;
	    int bin = 0;
	    int maxBin = MessageStatistics.NBINS - 1;
	    while (bin < maxBin && 
		   byteCount >= MessageStatistics.BIN_SIZES[bin]) {
		bin++;
	    }
	    messageLengthHistogram[bin]++;
	    statisticsTotalBytes += byteCount;
	    
	    //This attribute is not actually sent remotely, but is put
	    //onto the local message's header.  The Attribute can be
	    //read by other local Aspects after message is delivered,
	    //but before it is garbage collected
	    msg.setAttribute(MESSAGE_BYTES_ATTRIBUTE, new Integer(byteCount));

	    if (loggingService.isDebugEnabled())
		loggingService.debug("byteCount = " + byteCount);
	}

    }


    // MessageReader delegate.  In this case it does nothing.
    // Nonetheless it has to be here, since for reasons we don't yet
    // understand, the filtered streams have to match exactly on the
    // reader and writer.
	// Does absolutely nothing but has to be here.
    private static class DummyInputStream extends FilterInputStream {

	private DummyInputStream(InputStream wrapped) {
	    super(wrapped);
	}

	public int read() throws java.io.IOException {
	    return in.read();
	}

	public int read(byte[] b, int off, int len) 
	    throws java.io.IOException
	{
	    return in.read(b, off, len);
	}

	public int read(byte[] b) 
	    throws java.io.IOException
	{
	    return in.read(b);
	}
    }

    private static class StatisticsReader
	extends MessageReaderDelegateImplBase
    {


	StatisticsReader(MessageReader delegatee) {
	    super(delegatee);
	}

	public InputStream getObjectInputStream(ObjectInput in)
	    throws java.io.IOException, ClassNotFoundException
	{
	    InputStream raw_is = super.getObjectInputStream(in);
	    return new DummyInputStream(raw_is);
	}
    }

}
