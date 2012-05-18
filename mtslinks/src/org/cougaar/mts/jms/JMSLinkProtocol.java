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
import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.RPCLinkProtocol;
import org.cougaar.mts.base.UnregisteredNameException;

/**
 * This class implements a Cougaar LinkProtocol that uses JMS as the transport.
 */
public class JMSLinkProtocol
        extends RPCLinkProtocol
        implements MessageListener {
    // TODO What is the advantage of using -D over plugin parameters.
    // Plugin parameters would allow multiple JMS protocol instances
    private static final String JMS_URL = SystemProperties.getProperty("org.cougaar.mts.jms.url");
    private static final String JNDI_FACTORY =
            SystemProperties.getProperty("org.cougaar.mts.jms.jndi.factory");
    private static final String JMS_FACTORY =
            SystemProperties.getProperty("org.cougaar.mts.jms.factory");
    // TODO Weblogic specific code should be pulled out
    private static final String WEBLOGIC_SERVERNAME =
            SystemProperties.getProperty("org.cougaar.mts.jms.weblogic.server");

    // For now use the name server as a unique id of the society
    private static final String SOCIETY_UID =
            SystemProperties.getProperty("org.cougaar.name.server");

    // JNDI naming context to get JMS connection factory and destinations
    private Context context;
    // Connection factory for our JMS server
    private ConnectionFactory factory;
    // Connection to our JMS Server
    private Connection connection;
    // Session to our JMS Server
    private Session session;
    // Our JMS destination queue/topic for receiving messages.
    private Destination servantDestination;
    // manager for receiving messages
    private MessageReceiver receiver;
    // manager for sending messages and waiting for replys
    private ReplySync sync;
    // JMS Callback object to receive jms messages
    private MessageConsumer consumer;
    // JMS object for sending messages, not bound to a specific defination.
    private MessageProducer genericProducer; // shared for all outgoing

    // messages

    @Override
   public void load() {
        super.load();
    }

    @Override
   protected int computeCost(AttributedMessage message) {
        // TODO Better cost function for JMS transport
        // TODO JAZ This might be the place to ensure the session and Servent
        // Be careful not to test on each call. if failed only test once per
        // retry period.
        // for non-infinite cost, our Servant up and remote destination
        // available
        return 1500;
    }

    @Override
   protected DestinationLink createDestinationLink(MessageAddress address) {
        return new JMSLink(address);
    }

    protected void fillContextProperties(Map<String, Object> properties) {
        properties.put(Context.INITIAL_CONTEXT_FACTORY, JNDI_FACTORY);
        properties.put(Context.PROVIDER_URL, JMS_URL);
    }

    protected InitialContext makeInitialContext(Hashtable<String, Object> properties)
            throws NamingException {
        return new InitialContext(properties);
    }

    protected Destination lookupDestinationInContext(String destinationName)
            throws NamingException {
        Object raw = context.lookup(destinationName);
        if (raw instanceof Destination) {
            return (Destination) raw;
        } else {
            return null;
        }
    }

    protected void rebindDestinationInContext(String name, Destination destination)
            throws NamingException {
        // Make a delegating Destination with extra fields
        context.rebind(name, destination);
    }

    protected ConnectionFactory makeConnectionFactory()
            throws NamingException {
        return (ConnectionFactory) context.lookup(JMS_FACTORY);
    }

    protected Connection makeConnection()
            throws JMSException {
        return factory.createConnection();
    }

    protected void makeSessionExceptionListener()
            throws JMSException {
        JMSExceptionListener exceptionListener = new JMSExceptionListener();
        connection.setExceptionListener(exceptionListener);
    }

    protected Session makeSession()
            throws JMSException {
        return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    protected String getMyServantId(String node) {
        if (WEBLOGIC_SERVERNAME != null) {
            return /* WEBLOGIC_SERVERNAME + "/" + */node;
        } else {
            return node + "." + SOCIETY_UID;
        }
    }

    protected MessageSender makeMessageSender(ReplySync replySync) {
        return new MessageSender(this, replySync);
    }

    protected MessageReceiver makeMessageReceiver(ReplySync sync, MessageDeliverer deliverer) {
        return new MessageReceiver(sync, deliverer);
    }

    protected final ReplySync findOrMakeReplySync() {
        if (sync == null) {
            sync = makeReplySync();
        }
        return sync;
    }

    protected ReplySync makeReplySync() {
        return new ReplySync(this);
    }

    protected Destination makeServantDestination(String myServantId)
            throws JMSException, NamingException {
        Destination destination = session.createQueue(myServantId);
        rebindDestinationInContext(myServantId, destination);
        if (loggingService.isInfoEnabled()) {
            loggingService.info("Made queue " + myServantId);
        }
        return destination;
    }

    protected Session ensureSession() {
        if (session == null) {
            try {
                Hashtable<String, Object> properties = new Hashtable<String, Object>();
                fillContextProperties(properties);
                context = makeInitialContext(properties);
                factory = makeConnectionFactory();
                connection = makeConnection();
                makeSessionExceptionListener();
                session = makeSession();
                genericProducer = makeProducer(null);
            } catch (NamingException e) {
                if (loggingService.isWarnEnabled()) {
                    loggingService.warn("Couldn't get JMS session: Naming Cause=" + e.getMessage());
                }
                session = null;
            } catch (JMSException e) {
                if (loggingService.isWarnEnabled()) {
                    loggingService.warn("Couldn't get JMS session: JMS Cause=" + e.getMessage());
                }
                session = null;
            }
        }
        return session;
    }

    protected Context getContext() {
        return context;
    }

    protected void closeContext()
            throws NamingException {
        if (context != null) {
            try {
                context.close();
            } catch (NullPointerException e) {
                // Don't care if context got set to null by another thread.
            }
            context = null;
        }
    }

    protected ConnectionFactory getFactory() {
        return factory;
    }

    protected Connection getConnection() {
        return connection;
    }

    protected void closeConnection()
            throws JMSException {
        // Closing a contection also closes sessions, producers and consumers
        if (connection != null) {
            try {
                connection.close();
            } catch (NullPointerException e) {
                // Ignore these, it just means another thread
                // already did the close
            }
            connection = null;
        }
    }

    protected Session getSession() {
        return session;
    }

    protected Destination getServant() {
        return servantDestination;
    }

    @Override
   protected void ensureNodeServant() {
        if (servantDestination != null) {
            return;
        }
        setNodeURI(null);
        ensureSession();
        if (session != null) {
            String node = getNameSupport().getNodeMessageAddress().getAddress();
            String myServantId = getMyServantId(node);
            if (myServantId == null) {
                if (loggingService.isWarnEnabled()) {
                    loggingService.warn("Servant Id not set");
                }
            }

            // Check for leftover queue, flush it manually
            try {
                Destination old = lookupDestinationInContext(myServantId);
                if (old != null) {
                    if (loggingService.isInfoEnabled()) {
                        loggingService.info("Found old Queue");
                    }
                    servantDestination = old;
                    flushObsoleteMessages();
                }
            } catch (NamingException e1) {
                if (loggingService.isInfoEnabled()) {
                    loggingService.info("Queue " + myServantId + " doesn't exist yet");
                }
            } catch (JMSException e) {
                if (loggingService.isWarnEnabled()) {
                    loggingService.warn("Error flushing old message: Cause=" + e.getMessage());
                }
            }

            try {
                if (servantDestination == null) {
                    servantDestination = makeServantDestination(myServantId);
                }
                if (consumer != null) {
                    // Old listener from a previous session
                    try {
                        // unsubscribe out of date consumer.
                        closeConsumer(consumer);
                    } catch (Exception e) {
                        // JMS Errors here should logged but otherwise ignored
                        if (loggingService.isInfoEnabled()) {
                            loggingService.info("Error closing old message listener: "
                                    + e.getMessage());
                        }
                    }
                }
                consumer = makeMessageConsumer(session, servantDestination, myServantId);
                subscribeConsumer(consumer, this);
                if (receiver == null) {
                    ServiceBroker sb = getServiceBroker();
                    MessageDeliverer deliverer = sb.getService(this, MessageDeliverer.class, null);
                    receiver = makeMessageReceiver(findOrMakeReplySync(), deliverer);
                }
                connection.start();
                URI uri = makeURI(myServantId);
                setNodeURI(uri);
            } catch (JMSException e) {
                if (loggingService.isWarnEnabled()) {
                    loggingService.warn("Couldn't make JMS queue " + e.getMessage());
                }
                releaseNodeServant();
            } catch (URISyntaxException e) {
                if (loggingService.isWarnEnabled()) {
                    loggingService.warn("Couldn't make JMS URI " + e.getMessage());
                }
                releaseNodeServant();
            } catch (NamingException e) {
                if (loggingService.isWarnEnabled()) {
                    loggingService.warn("Couldn't register JMS queue in jndi" + e.getMessage());
                }
                releaseNodeServant();
            }
        }
    }

    protected String getSelector(String myServantId) {
        return null;
    }

    protected URI makeURI(String myServantId)
            throws URISyntaxException {
        return new URI("jms", myServantId, null, null, null);
    }

    protected String extractDestinationName(URI ref) {
        return ref.getAuthority();
    }

    protected MessageConsumer getConsumer() {
        return consumer;
    }

    protected MessageConsumer makeMessageConsumer(Session session,
                                                  Destination destination,
                                                  String ServantID)
            throws JMSException {
        MessageConsumer consumer = session.createConsumer(destination);
        return consumer;
    }

    // Utility close method
    protected void closeConsumer(MessageConsumer consumer)
            throws JMSException {
        try {
            consumer.setMessageListener(null);
            consumer.close();
        } catch (NullPointerException e) {
            // Don't care if consumer is set to null
            // during this operation.
        }
    }

    protected void subscribeConsumer(MessageConsumer consumer, JMSLinkProtocol protocol)
            throws JMSException {
        consumer.setMessageListener(this);
    }

    protected MessageProducer makeProducer(Destination destination)
            throws JMSException {
        MessageProducer producer = session.createProducer(destination);
        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        return producer;
    }

    protected MessageProducer getGenericProducer() {
        return genericProducer;
    }

    protected void flushObsoleteMessages()
            throws JMSException {
        int flushCount = 0;
        MessageConsumer flush = makeMessageConsumer(session, servantDestination, null);
        Object flushedMessage = flush.receiveNoWait();
        while (flushedMessage != null) {
            flushCount += 1;
            if (loggingService.isDebugEnabled()) {
                loggingService.debug("Flushing old message " + flushedMessage);
            }
            flushedMessage = flush.receiveNoWait();
        }
        if (loggingService.isInfoEnabled()) {
            loggingService.info("Flushed " + flushCount + " old messages ");
        }
        flush.close();
    }

    @Override
   protected String getProtocolType() {
        return "-JMS";
    }

    @Override
   protected void releaseNodeServant() {
        // Tear down context->factory->connection->session->producers and
        // consumers
        if (loggingService.isInfoEnabled()) {
            loggingService.warn("Releasing Servant");
        }
        // Closing connection closes session, producers, consummers, and
        // exception listener
        try {
            closeConnection();
        } catch (JMSException e) {
            if (loggingService.isWarnEnabled()) {
                loggingService.warn("Problem Closing Connection: " + e);
            }
        }
        try {
            closeContext();
        } catch (NamingException e) {
            if (loggingService.isWarnEnabled()) {
                loggingService.warn("Problem Closing Context: " + e);
            }
        }
        servantDestination = null;
        consumer = null;
        receiver = null;
        session = null;
        connection = null;
        context = null;
    }

    private final Object remakeLock = new Object();
    private boolean remakeInProgress = false;

    // This method should only be runnable
    // in one thread at a time. But the
    // other calls can't block. Instead
    // they return immediately.
    // TODO add a min retry period
    @Override
   protected void remakeNodeServant() {
        synchronized (remakeLock) {
            if (remakeInProgress) {
                return;
            }
            remakeInProgress = true;
        }
        session = null;
        servantDestination = null;
        ensureNodeServant();
        remakeInProgress = false;
    }

    @Override
   protected Boolean usesEncryptedSocket() {
        return Boolean.FALSE;
    }

    // MessageListener
    public void onMessage(Message msg) {
        receiver.handleIncomingMessage(msg);
    }

    @Override
   protected boolean isServantAlive() {
        return super.isServantAlive() && session != null && servantDestination != null
                && consumer != null && receiver != null;
    }

    protected class JMSExceptionListener
            implements ExceptionListener {
        public void onException(JMSException ex) {
            if (loggingService.isWarnEnabled()) {
                loggingService.warn("JMS Connection error: Cause=" + ex.getMessage());
            }
            releaseNodeServant();
        }
    }

    // MTS Station to send a message to a specific remote Agent
    // Even if multiple remote Agents are on the same Node, there will be one
    // instance per Agent
    public class JMSLink
            extends Link {
        private final MessageSender sender;
        protected URI uri;

        protected JMSLink(MessageAddress addr) {
            super(addr);
            this.sender = makeMessageSender(findOrMakeReplySync());
        }

        @Override
      public boolean isValid(AttributedMessage message) {
            return ensureNodeServantIsAlive() && super.isValid(message);
        }

        @Override
      protected Object decodeRemoteRef(URI ref)
                throws Exception {
            if (ref == null) {
                if (loggingService.isWarnEnabled()) {
                    loggingService.warn("Got null remote ref for " + getDestination());
                }
                return null;
            }
            if (session != null) {
                String destinationName = extractDestinationName(ref);
                if (loggingService.isInfoEnabled()) {
                    loggingService.info("Looking for Destination queue " + destinationName
                            + " from reference " + ref);
                }
                try {
                    // TODO if JNDI server is down this will not work
                    // is test for null good enough
                    Destination d = lookupDestinationInContext(destinationName);
                    if (loggingService.isInfoEnabled()) {
                        loggingService.info("Got " + d);
                    }
                    this.uri = ref;
                    return d;
                } catch (Exception e) {
                    if (loggingService.isWarnEnabled()) {
                        loggingService.warn("JNDI error: " + e.getMessage());
                    }
                    throw e;
                }
            }
            return null;
        }

        @Override
      protected MessageAttributes forwardByProtocol(Object destination, AttributedMessage message)
                throws NameLookupException, UnregisteredNameException, CommFailureException,
                MisdeliveredMessageException {
            if (!(destination instanceof Destination)) {
                if (loggingService.isErrorEnabled()) {
                    loggingService.error(destination + " is not a javax.jmx.Destination");
                }
                return null;
            }
            try {
                return sender.handleOutgoingMessage(uri, (Destination) destination, message);
            } catch (CommFailureException e1) {
                decache();
                throw e1;
            } catch (MisdeliveredMessageException e2) {
                decache();
                throw e2;
            } catch (Exception e3) {
                decache();
                throw new CommFailureException(e3);
            }
        }

        public Class<? extends JMSLinkProtocol> getProtocolClass() {
            return JMSLinkProtocol.this.getClass();
        }

    }

}
