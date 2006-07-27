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
 *
 */
public class JMSLinkProtocol extends RPCLinkProtocol {
    private static final String JMS_URL = "tcp://fig.bbn.com:3035";
    private static final String JMS_IMPL = "org.exolab.jms.jndi.InitialContextFactory";
    private static final String factoryName = "ConnectionFactory";
    
    private Destination destination;
    private Context context;
    private ConnectionFactory factory = null;
    private Connection connection = null;
    private Session session;
    private MessageReceiver receiver;
    
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
		properties.put(Context.INITIAL_CONTEXT_FACTORY, JMS_IMPL);
		properties.put(Context.PROVIDER_URL, JMS_URL);
		context = new InitialContext(properties);
		factory = (ConnectionFactory) context.lookup(factoryName);
		connection = factory.createConnection();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		ServiceBroker sb = getServiceBroker();
		MessageDeliverer deliverer = (MessageDeliverer) 
		    sb.getService(this,  MessageDeliverer.class, null);
		receiver = new MessageReceiver(session, deliverer);
		connection.start();
	    } catch (NamingException e) {
		loggingService.error("Couldn't get JMS session", e);
	    } catch (JMSException e) {
		loggingService.error("Couldn't get JMS session", e);
	    }
	}
    }

    protected void findOrMakeNodeServant() {
	if (destination != null) return;
	ensureSession();
	if (session != null) {
	    String myAddress = 
		getNameSupport().getNodeMessageAddress().getAddress();
	    try {
		destination = session.createQueue(myAddress);
		MessageConsumer consumer = session.createConsumer(destination);
		consumer.setMessageListener(new Listener());
		context.rebind(myAddress, destination);
		URI uri = new URI("jms://" + myAddress);
		setNodeURI(uri);
	    } catch (JMSException e) {
		loggingService.error("Couldn't make JMS queue", e);
	    } catch (URISyntaxException e) {
		loggingService.error("Couldn't make JMS URI", e);
	    } catch (NamingException e) {
		loggingService.error("Couldn't register JMS Destination", e);
	    }
	}
    }

    protected String getProtocolType() {
	return "-JMS";
    }

    protected void releaseNodeServant() {
	// TODO Auto-generated method stub
    }

    protected void remakeNodeServant() {
	// TODO Auto-generated method stub
    }

    protected Boolean usesEncryptedSocket() {
	return Boolean.FALSE;
    }
    
    
    
    private class Listener implements MessageListener {
	public void onMessage(Message msg) {
	    receiver.handleIncomingMessage(msg);
	}
    }
    
    
    
    private class JMSLink extends Link {
	private final MessageSender sender;
	
	JMSLink(MessageAddress addr) {
	    super(addr);
	    this.sender = new MessageSender(session);
	}

	protected Object decodeRemoteRef(URI ref) throws Exception {
	    if (ref == null) {
		loggingService.warn("Got null remote ref for " + getDestination());
		return null;
	    }
	    String node = ref.getHost();
	    if (session != null) {
		return context.lookup(node); // ???
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
