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

import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;

import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 *  This utility class does the low-level work to force
 *  the jms linkprotocol to behave like a synchronous rpc.
 *  In particular it blocks the sending thread until 
 *  a reply for the outgoing message arrives, generates and sends replies
 *  for incoming messages, and processes received replies by waking
 *  the corresponding thread.
 */
public class ReplySync {
    public static final int DEFAULT_TIMEOUT = 5000;
    private static final String ID_PROP = "MTS_MSG_ID";
    private static final String IS_MTS_REPLY_PROP = "MTS_REPLY";
    private static int ID = 0;
    
    private final JMSLinkProtocol lp;
    private final Map pending;
    private final Map replyData;
    private final int timeout;
    private final Logger log;
    
    public ReplySync(JMSLinkProtocol lp) {
	this(lp, DEFAULT_TIMEOUT);
    }
    
    public ReplySync(JMSLinkProtocol lp, int timeout) {
	this.lp = lp;
	this.pending = new HashMap();
	this.replyData = new HashMap();
	this.log = Logging.getLogger(getClass().getName());
	this.timeout = timeout;
    }
    
    protected void setMessageProperties(Message message, 
	    Integer id,
	    URI uri,
	    Destination destination) 
    throws JMSException {
	message.setIntProperty(ID_PROP, id.intValue());
	message.setBooleanProperty(IS_MTS_REPLY_PROP, false);
    }
    
    public MessageAttributes sendMessage(Message message, 
	    URI uri,
	    Destination destination) 
    throws JMSException,CommFailureException,MisdeliveredMessageException {
	message.setJMSReplyTo(lp.getServant());
	message.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
	Integer id = new Integer(++ID);
	setMessageProperties(message, id, uri, destination);
	
	Object lock = new Object();
	pending.put(id, lock);
	synchronized (lock) {
	    lp.getGenericProducer().send(destination, message);
	    while (true) {
		try {
		    lock.wait(timeout); // TODO:  Should be set dynamically
		    break;
		} catch (InterruptedException ex) {
		    
		}
	    }
	}
	Object result = replyData.remove(id);
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

    protected void setReplyProperties(ObjectMessage omsg, ObjectMessage replyMsg) 
    throws JMSException {
	replyMsg.setBooleanProperty(IS_MTS_REPLY_PROP, true);
	replyMsg.setIntProperty(ID_PROP, omsg.getIntProperty(ID_PROP));
    }
    
    public void replyToMessage(ObjectMessage omsg, Object replyData) throws JMSException {
	ObjectMessage replyMsg = lp.getSession().createObjectMessage((Serializable) replyData);
	replyMsg.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
	setReplyProperties(omsg, replyMsg);
	Destination dest = omsg.getJMSReplyTo();
	lp.getGenericProducer().send(dest,replyMsg);
    }

    
    
   public boolean isReply(ObjectMessage msg) {
	try {
	    boolean isReply = msg.getBooleanProperty(IS_MTS_REPLY_PROP);
	    if (log.isDebugEnabled()) {
		log.debug("Value of " +IS_MTS_REPLY_PROP+ " property is " + isReply);
	    }
	    if (!isReply) {
		return false;
	    }
	    Integer id = new Integer(msg.getIntProperty(ID_PROP));
	    log.debug("Value of " +ID_PROP+ " property is " + id);
	    replyData.put(id, msg.getObject());
	    Object lock = pending.get(id);
	    if (lock != null) {
		synchronized (lock) {
		    lock.notify();
		}
	    }
	    return true;
	} catch (JMSException e) {
	   log.error("Error checking reply status: " + e.getMessage(), e);
	   return false;
	}
    }

}
