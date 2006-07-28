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

import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.std.AttributedMessage;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 *  This utility class handles outgoing JMS messages
 */
public class MessageSender {
    private final Session session;
    private final Map producers;
    private final AckSync sync;
    private final Logger log;
    
    MessageSender(Session session, AckSync sync) {
	this.session = session;
	this.sync = sync;
	this.producers = new HashMap();
	log = Logging.getLogger(getClass().getName());
    }
    
    MessageAttributes handleOutgoingMessage(Destination dest, AttributedMessage message) 
    throws CommFailureException {
	MessageProducer producer = (MessageProducer) producers.get(dest);
	if (producer == null) {
	    try {
		producer = session.createProducer(dest);
		producers.put(dest, producer);
	    } catch (JMSException e) {
		log.error("Couldn't create MessageProducer", e);
		throw new CommFailureException(e);
	    }
	}
	try {
	    ObjectMessage msg = session.createObjectMessage(message);
	    MessageAttributes metadata = sync.sendMessage(msg, producer);
	    return metadata;
	} catch (JMSException e) {
	    log.error("Couldn't send JMS message", e);
	    throw new CommFailureException(e);
	}
    }
}
