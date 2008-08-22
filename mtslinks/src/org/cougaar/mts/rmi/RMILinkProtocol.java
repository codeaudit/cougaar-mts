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
package org.cougaar.mts.rmi;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.URI;
import java.rmi.MarshalException;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.rmi.server.UnicastRemoteObject;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.thread.SchedulableStatus;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.CougaarIOException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.LinkProtocol;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.RPCLinkProtocol;
import org.cougaar.mts.base.SocketFactory;
import org.cougaar.mts.base.UnregisteredNameException;
import org.cougaar.util.annotations.Cougaar;
import org.cougaar.util.StateModelException;

/**
 * This {@link LinkProtocol} handles message passing via RMI, one example
 * of RPC communication. The interface is {@link MT}.
 * <p>
 * The cost function of the DestinationLink inner subclass is currently
 * hardwired to an arbitrary value of 1000. This should be made smarter
 * eventually.
 * 
 */
public class RMILinkProtocol
        extends RPCLinkProtocol {

    private MT myProxy;
    private final SocketFactory socfac;
    private RMISocketControlService controlService;
    
    @Cougaar.Arg(name="port", defaultValue="0")
    private int port = 0;

    public RMILinkProtocol() {
        super();
        socfac = getSocketFactory();
    }

    /**
     * If LinkProtocols classes want to define this method, eg in order to
     * provide a service, they should not in general invoke super.load(), since
     * if they do they'll end up clobbering any services defined by super class'
     * service. Instead they should use {@link #super_load}, which runs the
     * standard load() method without running any intervening ones.
     */
    public void load() {
        super.load();
        ServiceBroker sb = getServiceBroker();
        NodeIdentificationService nis = sb.getService(this, NodeIdentificationService.class, null);
        InetAddress address = nis.getInetAddress();
        if (address == null) {
            throw new IllegalStateException("Can't load RMI link protocol: host ip address is not available");
        }
        sb.releaseService(this, NodeIdentificationService.class, nis);
        String host = address.getHostAddress(); // XXX: dotted quad, is that what we want?
        System.setProperty("java.rmi.server.hostname", host);
        controlService = sb.getService(this, RMISocketControlService.class, null);
    }
    
    /**
     * @see org.cougaar.util.GenericStateModelAdapter#unload()
     */
    public synchronized void unload()
            throws StateModelException {
        super.unload();
        if (controlService != null) {
            ServiceBroker sb = getServiceBroker();
            sb.releaseService(this, RMISocketControlService.class, controlService);
        }
        controlService = null;
    }

    protected String getProtocolType() {
        return "-RMI";
    }

    // If this is called, we've already found the remote reference.
    protected int computeCost(AttributedMessage message) {
        return 1000;
    }

    /**
     * Even though {@link MisdeliveredMessageException} extends
     * {@link RemoteException}, nonethless they'll be wrapped. Check for this
     * case here. Also look for {@link IllegalArgumentException}, which can also
     * occur as a side-effect of mobility.
     */
    protected void checkForMisdelivery(Throwable ex, AttributedMessage message)
            throws MisdeliveredMessageException {
        if (ex instanceof MisdeliveredMessageException) {
            throw (MisdeliveredMessageException) ex;
        } else if (ex instanceof IllegalArgumentException) {
            // Probably a misdelivered message that failed during
            // deserialization. Try to check with a string match...
            String msg = ex.getMessage();
            int match = msg.indexOf("is not an Agent on this node");
            if (match > 0) {
                // pretend this is a MisdeliveredMessageException
                throw new MisdeliveredMessageException(message);
            }
        }
        // If we get here, the caller is responsible for rethrowing
        // the exception.
    }

    /**
     * Need to declare CommFailure exception even though it isn't thrown here
     * because overriding methods can throw it.  Keep eclipse happy by adding
     * javadoc for it.
     *
     * @throws CommFailureException
     */
    protected MessageAttributes doForwarding(MT remote, AttributedMessage message)
            throws MisdeliveredMessageException, RemoteException, CommFailureException {
        MessageAttributes result = null;
        try {
            SchedulableStatus.beginNetIO("RMI call");
            result = remote.rerouteMessage(message); // **** RMI-specific
        } catch (RemoteException remote_ex) {
            Throwable cause = remote_ex.getCause();
            checkForMisdelivery(cause, message);
            // Not a misdelivery - rethrow the remote exception
            throw remote_ex;
        } catch (IllegalArgumentException illegal_arg) {
            checkForMisdelivery(illegal_arg, message);
            // Not a misdelivery - rethrow the exception
            throw illegal_arg;
        } finally {
            SchedulableStatus.endBlocking();
        }
        return result;
    }

    protected Boolean usesEncryptedSocket() {
        return Boolean.FALSE;
    }

    protected DestinationLink createDestinationLink(MessageAddress address) {
        return new RMILink(address);
    }

    protected void ensureNodeServant() {
        if (myProxy != null) {
            return;
        }
        try {
            MessageAddress myAddress = getNameSupport().getNodeMessageAddress();
            myProxy = makeMTImpl(myAddress, socfac);
            Remote remote = UnicastRemoteObject.exportObject(myProxy, port, socfac, socfac);
            setNodeURI(RMIRemoteObjectEncoder.encode(remote));
        } catch (RemoteException ex) {
            loggingService.error(null, ex);
        } catch (Exception other) {
            loggingService.error(null, other);
        }
    }

    protected void releaseNodeServant() {
        try {
            UnicastRemoteObject.unexportObject(myProxy, true);
        } catch (NoSuchObjectException ex) {
            // don't care
        }
        myProxy = null;
    }

    protected void remakeNodeServant() {
        try {
            UnicastRemoteObject.unexportObject(myProxy, true);
        } catch (NoSuchObjectException ex) {
            // don't care
        }
        myProxy = null;
        ensureNodeServant();
    }
    
    /**
     * Standard RMI handling of security and other cougaar-specific io
     * exceptions. Subclasses may need to do something different (eg
     * {@link org.cougaar.mts.corba.CorbaLinkProtocol}).
     * <p>
     * If the argument itself is a {@link MarshalException} whose cause is a
     * {@link CougaarIOException}, a local cougaar-specific error has occured.
     * <p>
     * If the argument is some other {@link RemoteException} whose cause is an
     * {@link UnmarshalException} whose cause in turn is a CougaarIOException, a
     * remote cougaar-specific error has occured.
     * <p>
     * Otherwise this is some other kind of remote error.
     */
    private void handleSecurityException(Exception ex)
            throws CommFailureException {
        Throwable cause = ex.getCause();
        if (ex instanceof MarshalException) {
            if (cause instanceof CougaarIOException) {
                throw new CommFailureException((Exception) cause);
            }
            // When a TransientIOException is thrown sometimes it
            // triggers different exception on the socket, which gets
            // through instead of the TransientIOException. For now we
            // will catch these and treat them as if they were
            // transient (though other kinds of SocketExceptions
            // really shouldn't be).
            else if (cause instanceof SocketException) {
                // Throwing a CommFailureException doesn't seem right
                // anymore (as of 1.4.2). So don't do it anymore,
                // but log it.
                if (loggingService.isDebugEnabled()) {
                    loggingService.debug("Got a SocketException as the cause of a MarshallException: "
                                                 + cause.getMessage(),
                                         ex);
                    // cause = new TransientIOException(cause.getMessage());
                    // throw new CommFailureException((Exception) cause);
                }
            }
        } else if (cause instanceof UnmarshalException) {
            Throwable remote_cause = cause.getCause();
            if (remote_cause instanceof CougaarIOException) {
                throw new CommFailureException((Exception) remote_cause);
            }
        }
    }

    private SocketFactory getSocketFactory() {
        return new SocketFactory(false, true);
    }

    private MTImpl makeMTImpl(MessageAddress myAddress, SocketFactory socfac) {
        return new MTImpl(myAddress, getServiceBroker(), socfac);
    }

    protected class RMILink
            extends Link {

        protected RMILink(MessageAddress destination) {
            super(destination);
        }

        protected Object decodeRemoteRef(URI ref)
                throws Exception {
            MessageAddress target = getDestination();
            if (getRegistry().isLocalClient(target)) {
                // myself as an RMI stub
                return myProxy;
            }

            if (ref == null) {
                return null;
            }

            Object object = null;
            try {
                // This call can block in net i/o
                SchedulableStatus.beginNetIO("RMI reference decode");
                object = RMIRemoteObjectDecoder.decode(ref);
            } catch (Throwable ex) {
                loggingService.error("Can't decode URI " + ref, ex);
            } finally {
                SchedulableStatus.endBlocking();
            }

            if (object == null) {
                return null;
            }

            if (controlService != null) {
                controlService.setReferenceAddress((Remote) object, target);
            }

            if (object instanceof MT) {
                return object;
            } else {
                throw new RuntimeException("Object " + object + " is not a MessageTransport!");
            }
        }

        public Class<? extends RMILinkProtocol> getProtocolClass() {
            return RMILinkProtocol.this.getClass();
        }

        protected MessageAttributes forwardByProtocol(Object remote_ref, AttributedMessage message)
                throws NameLookupException, UnregisteredNameException, CommFailureException,
                MisdeliveredMessageException {
            try {
                return doForwarding((MT) remote_ref, message);
            } catch (MisdeliveredMessageException mis) {
                // force recache of remote
                decache();
                throw mis;
            }
            // RMILinkProtocol won't throw this but subclasses might.
            catch (CommFailureException cfe) {
                // force recache of remote
                decache();
                throw cfe;
            } catch (RemoteException ex) {
                if (loggingService.isDebugEnabled()) {
                    loggingService.debug("RemoteException", ex);
                }
                handleSecurityException(ex);
                // If we get here it wasn't a security exception
                decache();
                throw new CommFailureException(ex);
            } catch (Exception ex) {
                // Ordinary comm failure. Force recache of remote
                if (loggingService.isDebugEnabled()) {
                    loggingService.debug("Ordinary comm failure", ex);
                }
                decache();
                // Ordinary comm failure
                throw new CommFailureException(ex);
            }
        }

    }

}
