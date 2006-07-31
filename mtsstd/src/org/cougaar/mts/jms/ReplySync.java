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
package org.cougaar.mts.jms;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.MisdeliveredMessageException;

/**
 *  This utility class does the low-level work to force
 *  the jms linkprotocol to behave like a synchronous rpc.
 *  In particular it blocks the sending thread until 
 *  a reply for the outgoing message arrives, generates and sends replies
 *  for incoming messages, and processes received replies by waking
 *  the corresponding thread.
 */
public class ReplySync {
    private static final String ID_PROP = "JMS_MSG_ID";
    private static int ID = 0;
    
    private final Destination originator;
    private final Session session;
    private final Map producers;
    private final Map pending;
    private final Map replyData;
    
    ReplySync(Destination originator, Session session) {
	this.originator = originator;
	this.session = session;
	this.producers = new HashMap();
	this.pending = new HashMap();
	this.replyData = new HashMap();
    }
    
    MessageAttributes sendMessage(Message msg, MessageProducer producer) 
    throws JMSException,CommFailureException,MisdeliveredMessageException {
	msg.setJMSReplyTo(originator);
	Integer id = new Integer(++ID);
	msg.setIntProperty(ID_PROP, id.intValue());
	Object lock = new Object();
	pending.put(id, lock);
	synchronized (lock) {
	    producer.send(msg);
	    while (true) {
		try {
		    lock.wait(10000); // TODO:  Set a maximum wait time?
		    break;
		} catch (InterruptedException ex) {
		    
		}
	    }
	}
	Object result = replyData.get(id);
	replyData.remove(id);
	pending.remove(id);
	if (result instanceof MessageAttributes) {
	    return (MessageAttributes) result;
	} else if (result instanceof MisdeliveredMessageException) {
	    MisdeliveredMessageException ex = (MisdeliveredMessageException) result;
	    throw ex;
	} else {
	    throw new CommFailureException(new RuntimeException("Weird data " + result));
	}
    }
    
    void replyToMessage(ObjectMessage omsg, Object replyData) throws JMSException {
	Reply reply = new Reply(replyData, omsg.getIntProperty(ID_PROP));
	ObjectMessage replyMsg = session.createObjectMessage(reply);
	Destination dest = omsg.getJMSReplyTo();
	MessageProducer producer = (MessageProducer) producers.get(dest);
	if (producer == null) {
	    producer = session.createProducer(dest);
	    producers.put(dest, producer);
	}
	producer.send(replyMsg);
    }
    
    boolean isReply(ObjectMessage msg) {
	try {
	    Object raw = msg.getObject();
	    if (raw instanceof Reply) {
		Reply reply = (Reply) raw;
		Integer id = new Integer(reply.getId());
		replyData.put(id, reply.getData());
		Object lock = pending.get(id);
		if (lock != null) {
		    synchronized (lock) {
			lock.notify();
		    }
		}
		return true;
	    } else {
		return false;
	    }
	} catch (JMSException e) {
	   return false;
	}
    }

}
