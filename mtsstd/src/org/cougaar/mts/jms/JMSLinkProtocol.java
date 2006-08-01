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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Hashtable;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.RPCLinkProtocol;
import org.cougaar.mts.base.UnregisteredNameException;
import org.cougaar.mts.std.AttributedMessage;

/**
 *  This class implements a Cougaar LinkProtocol that uses JMS as the
 *  transport.
 */
public class JMSLinkProtocol extends RPCLinkProtocol implements MessageListener {
    private static final String JMS_URL = System.getProperty("org.cougaar.mts.jms.url");
    private static final String JNDI_FACTORY = System.getProperty("org.cougaar.mts.jms.jndi.factory");
    private static final String JMS_FACTORY = System.getProperty("org.cougaar.mts.jms.factory");
    
    // For now use the name server as a unique id of the society
    private static final String SOCIETY_UID = System.getProperty("org.cougaar.name.server");
    
    private Destination destination;
    private Context context;
    private ConnectionFactory factory = null;
    private Connection connection = null;
    private Session session;
    private MessageReceiver receiver;
    private ReplySync sync;
    private MessageConsumer consumer;
    
    protected int computeCost(AttributedMessage message) {
	// TODO Pick a better number
	return 1;
    }

   
    protected DestinationLink createDestinationLink(MessageAddress address) {
	return new JMSLink(address);
    }
    
    private void ensureSession() {
	if (session == null) {
	    try {
		Hashtable properties = new Hashtable();
		properties.put(Context.INITIAL_CONTEXT_FACTORY, JNDI_FACTORY);
		properties.put(Context.PROVIDER_URL, JMS_URL);
		context = new InitialContext(properties);
		factory = (ConnectionFactory) context.lookup(JMS_FACTORY);
		connection = factory.createConnection();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	    } catch (NamingException e) {
		loggingService.error("Couldn't get JMS session", e);
	    } catch (JMSException e) {
		loggingService.error("Couldn't get JMS session", e);
	    }
	}
    }
    
    protected Context getContext() {
	return context;
    }
    
    protected Session getSession() {
	return session;
    }
    
    protected Destination makeQueue(String destinationID) throws JMSException {
	return session.createQueue(destinationID);
    }

    protected void findOrMakeNodeServant() {
	if (destination != null) return;
	ensureSession();
	if (session != null) {
	    String node = getNameSupport().getNodeMessageAddress().getAddress();
	    String destinationID = node + "." + SOCIETY_UID;
	    // Check for leftover queue, flush it manually
	    try {
		Object old = context.lookup(destinationID);
		if (old instanceof Destination) {
		    loggingService.info("Found old Queue");
		    destination = (Destination) old;
		    MessageConsumer flush = session.createConsumer(destination);
		    while (flush.receiveNoWait() != null) {
			loggingService.info("Flushing old message");
		    }
		    flush.close();
		}
	    } catch (NamingException e1) {
		// No old one exists -- ignore
	    } catch (JMSException e) {
		loggingService.error("Error flushing old message", e);
	    }
	    
	    try {
		if (destination == null) {
		    destination = makeQueue(destinationID);
		    context.rebind(destinationID, destination);
		    if (loggingService.isInfoEnabled()) {
			loggingService.info("Made queue " + destinationID);
		    }
		}
		sync = new ReplySync(destination, session);
		consumer = session.createConsumer(destination);
		consumer.setMessageListener(this);
		ServiceBroker sb = getServiceBroker();
		MessageDeliverer deliverer = (MessageDeliverer) 
		    sb.getService(this,  MessageDeliverer.class, null);
		receiver = new MessageReceiver(session, sync, deliverer);
		connection.start();
		URI uri = new URI("jms://" + destinationID);
		setNodeURI(uri);
	    } catch (JMSException e) {
		loggingService.error("Couldn't make JMS queue", e);
	    } catch (URISyntaxException e) {
		loggingService.error("Couldn't make JMS URI", e);
	    } catch (NamingException e) {
		loggingService.error("Couldn't register JMS queue in jndi", e);
	    }
	}
    }

    protected String getProtocolType() {
	return "-JMS";
    }

    protected void releaseNodeServant() {
	// Bogus mechanism added by Sebastian.  Ignore.
    }

    protected void remakeNodeServant() {
	// Only used when a host's address changes.  Ignore for now.
    }

    protected Boolean usesEncryptedSocket() {
	return Boolean.FALSE;
    }
    
    // MessageListener
    public void onMessage(Message msg) {
	receiver.handleIncomingMessage(msg);
    }
    
   
    
    private class JMSLink extends Link {
	private final MessageSender sender;
	
	JMSLink(MessageAddress addr) {
	    super(addr);
	    this.sender = new MessageSender(session, sync);
	}

	protected Object decodeRemoteRef(URI ref) throws Exception {
	    if (ref == null) {
		loggingService.warn("Got null remote ref for " + getDestination());
		return null;
	    }
	    if (session != null) {
		String destination = ref.getSchemeSpecificPart().substring(2); 
		if (loggingService.isInfoEnabled()) {
		    loggingService.info("Looking for Destination queue " + destination+
			    " from reference " + ref);
		}
		try {
		    Object d = context.lookup(destination);
		    if (loggingService.isInfoEnabled()) loggingService.info("Got " + d);
		    return d;
		} catch (Exception e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		    throw e;
		}
	    }
	    return null;
	}

	protected MessageAttributes forwardByProtocol(Object destination, AttributedMessage message)
	throws NameLookupException, UnregisteredNameException, CommFailureException, MisdeliveredMessageException {
	    if (destination instanceof Destination) {
		return sender.handleOutgoingMessage((Destination) destination, message);
	    } else {
		loggingService.error(destination + " is not a javax.jmx.Destination");
		return null;
	    }
	}

	public Class getProtocolClass() {
	    return JMSLinkProtocol.this.getClass();
	}
	
    }

}
