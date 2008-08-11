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

import java.net.URI;
import java.rmi.Remote;
import java.rmi.server.UnicastRemoteObject;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.thread.SchedulableStatus;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.CougaarIOException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.LinkProtocol;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.RPCLinkProtocol;
import org.cougaar.mts.base.SocketFactory;
import org.cougaar.mts.base.UnregisteredNameException;
import org.cougaar.mts.std.AttributedMessage;
import org.cougaar.util.StateModelException;

/**
 * This {@link LinkProtocol} handles message passing via RMI, one example
 * RPC-like communication. The interface is {@link MT}.
 * 
 * The cost function of the DestinationLink inner subclass is currently
 * hardwired to an arbitrary value of 1000. This should be made smarter
 * eventually.
 * 
 */
public class RMILinkProtocol
        extends RPCLinkProtocol {

    // private MessageAddress myAddress;
    private MT myProxy;
    private final SocketFactory socfac;
    private RMISocketControlService controlService;

    public RMILinkProtocol() {
        super();
        socfac = getSocketFactory();
    }

    // If LinkProtocols classes want to define this method, eg in
    // order to provide a service, they should not in general invoke
    // super.load(), since if they do they'll end up clobbering any
    // services defined by super classes service. Instead they should
    // use super_load(), defined in LinkProtocol, which runs the
    // standard load() method without running any intervening ones.
    public void load() {
        super.load();
        ServiceBroker sb = getServiceBroker();

        // RMISocketControlService could be null
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

    protected SocketFactory getSocketFactory() {
        return new SocketFactory(false, true);
    }

    // If this is called, we've already found the remote reference.
    protected int computeCost(AttributedMessage message) {
        return 1000;
    }

    protected MTImpl makeMTImpl(MessageAddress myAddress, SocketFactory socfac)
            throws java.rmi.RemoteException {
        return new MTImpl(myAddress, getServiceBroker(), socfac);
    }

    // Even though MisdeliveredMessageExceptions are
    // RemoteExceptions, nonethless they'll be wrapped. Check for
    // this case here. Also look for IllegalArgumentExceptions,
    // which can also occur as a side-effect of mobility.
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

    protected MessageAttributes doForwarding(MT remote, AttributedMessage message)
            throws MisdeliveredMessageException, java.rmi.RemoteException, CommFailureException
    // Declare CommFailureException because the signature needs to
    // match SerializedRMILinkProtocol's doForwarding method. That
    // exception will never be thrown here.
    {
        MessageAttributes result = null;
        try {
            SchedulableStatus.beginNetIO("RMI call");
            result = remote.rerouteMessage(message); // **** RMI-specific
        } catch (java.rmi.RemoteException remote_ex) {
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

    // Standard RMI handling of security and other cougaar-specific io
    // exceptions. Subclasses may need to do something different (see
    // CORBALinkProtocol).
    //
    // If the argument itself is a MarshalException whose cause is a
    // CougaarIOException, a local cougaar-specific error has occured.
    //
    // If the argument is some other RemoteException whose cause is an
    // UnmarshalException whose cause in turn is a CougaarIOException,
    // a remote cougaar-specific error has occured.
    //
    // Otherwise this is some other kind of remote error.
    protected void handleSecurityException(Exception ex)
            throws CommFailureException {
        Throwable cause = ex.getCause();
        if (ex instanceof java.rmi.MarshalException) {
            if (cause instanceof CougaarIOException) {
                throw new CommFailureException((Exception) cause);
            }
            // When a TransientIOException is thrown sometimes it
            // triggers different exception on the socket, which gets
            // through instead of the TransientIOException. For now we
            // will catch these and treat them as if they were
            // transient (though other kinds of SocketExceptions
            // really shouldn't be).
            else if (cause instanceof java.net.SocketException) {
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
        } else if (cause instanceof java.rmi.UnmarshalException) {
            Throwable remote_cause = cause.getCause();
            if (remote_cause instanceof CougaarIOException) {
                throw new CommFailureException((Exception) remote_cause);
            }
        }
    }

    protected void ensureNodeServant() {
        if (myProxy != null) {
            return;
        }
        try {
            MessageAddress myAddress = getNameSupport().getNodeMessageAddress();
            myProxy = makeMTImpl(myAddress, socfac);
            Remote remote = UnicastRemoteObject.exportObject(myProxy, 0, socfac, socfac);
            setNodeURI(RMIRemoteObjectEncoder.encode(remote));
        } catch (java.rmi.RemoteException ex) {
            loggingService.error(null, ex);
        } catch (Exception other) {
            loggingService.error(null, other);
        }
    }

    protected void releaseNodeServant() {
        try {
            UnicastRemoteObject.unexportObject(myProxy, true);
        } catch (java.rmi.NoSuchObjectException ex) {
            // don't care
        }
        myProxy = null;
    }

    protected void remakeNodeServant() {
        try {
            UnicastRemoteObject.unexportObject(myProxy, true);
        } catch (java.rmi.NoSuchObjectException ex) {
            // don't care
        }
        myProxy = null;
        ensureNodeServant();
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

        public Class getProtocolClass() {
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
            } catch (java.rmi.RemoteException ex) {
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
