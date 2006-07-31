/*
 * <copyright>
 *  
 *  Copyright 1997-2006 BBNT Solutions, LLC
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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.std.AttributedMessage;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 *  This utility class handles incoming JMS messages
 */
public class MessageReceiver {
    private final Logger log;
    private final MessageDeliverer deliverer;
    private final ReplySync sync;
    
    MessageReceiver(Session session, ReplySync sync, MessageDeliverer deliverer) {
	// no use for the Session now, but we'll need it later
	// to send the acks
	this.sync = sync;
	this.deliverer = deliverer;
	this.log = Logging.getLogger(getClass().getName());
    }
    
    
    void handleIncomingMessage(Message msg) 
    throws MisdeliveredMessageException {
	if (deliverer == null) {
	    log.error("Message arrived before MessageDelivererService was available");
	    return;
	}
	if (msg instanceof ObjectMessage) {
	    ObjectMessage omsg = (ObjectMessage) msg;
	    if (sync.isReply(omsg))  {
		// it's an ack -- no further work here
		return;
	    }
	    try {
		Object domainObject = omsg.getObject();
		if (domainObject instanceof AttributedMessage) {
		    AttributedMessage message = (AttributedMessage) domainObject;
		    MessageAttributes reply = deliverer.deliverMessage(message, message.getTarget());
		    sync.replyToMessage(omsg, reply);
		} else {
		    log.warn(domainObject + " is not an AttributedMessage");
		}
	    } catch (JMSException e) {
		log.warn("JMS error: ", e);
	    }
	} else {
	    log.warn("Received a JMS message that wasn't an ObjectMessage");
	}
    }

}
