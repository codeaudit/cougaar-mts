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
 *
 */
public class MessageReceiver {
    private final Logger log;
    private final MessageDeliverer deliverer;
    
    MessageReceiver(Session session, MessageDeliverer deliverer) {
	// no use for the Session now, but we'll need it later
	// to send the acks
	this.deliverer = deliverer;
	this.log = Logging.getLogger(getClass().getName());
    }
    
    
    void handleIncomingMessage(Message msg) {
	if (deliverer == null) {
	    log.error("Message arrived before MessageDelivererService was available");
	    return;
	}
	// If it's a data message, extract it, do more or less what MTImpl does
	// and send an ack with the result.
	//
	// If it's an ack, wake up the waiter.
	//
	// For now ignore the acks
	if (msg instanceof ObjectMessage) {
	    ObjectMessage omsg = (ObjectMessage) msg;
	    try {
		Object domainObject = omsg.getObject();
		if (domainObject instanceof AttributedMessage) {
		    AttributedMessage message = (AttributedMessage) domainObject;
		    try {
			MessageAttributes reply = deliverer.deliverMessage(message, message.getTarget());
			if (log.isInfoEnabled()) {
			    log.info("Reply " + reply);
			}
			// TODO: return the reply in an ack message
		    } catch (MisdeliveredMessageException e) {
			log.error("Couldn't deliver message to " + message.getTarget(), e);
		    }
		} else {
		    log.warn(domainObject + " is not an AttributedMessage");
		}
	    } catch (JMSException e) {
		log.error("Couldn't extract data from ObjectMessage", e);
	    }	    
	} else {
	    log.warn("Received a JMS message that wasn't an ObjectMessage");
	}
    }

}
