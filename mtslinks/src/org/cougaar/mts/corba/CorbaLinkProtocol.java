/*
 * <copyright>
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.mts.corba;

import java.net.URI;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.SerializationUtils;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DontRetryException;
import org.cougaar.mts.base.LinkProtocol;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.RPCLinkProtocol;
import org.cougaar.mts.base.UnregisteredNameException;
import org.cougaar.mts.corba.idlj.CorbaDontRetryException;
import org.cougaar.mts.corba.idlj.CorbaMisdeliveredMessage;
import org.cougaar.mts.corba.idlj.MT;
import org.cougaar.mts.corba.idlj.MTHelper;
import org.cougaar.mts.std.AttributedMessage;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

/**
 * This {@link LinkProtocol} uses CORBA for communication.
 */
public class CorbaLinkProtocol
        extends RPCLinkProtocol {
    public static final String PROTOCOL_TYPE = "-CORBA";

    private MT myProxy;
    private final ORB orb;
    private POA poa;

    public CorbaLinkProtocol() {
        super();
        String[] args = null;
        orb = ORB.init(args, null);
        try {
            org.omg.CORBA.Object raw = orb.resolve_initial_references("RootPOA");
            poa = POAHelper.narrow(raw);
            poa.the_POAManager().activate();
        } catch (Exception error) {
            loggingService.error(null, error);
        }

    }

    protected String getProtocolType() {
        return PROTOCOL_TYPE;
    }

    protected Boolean usesEncryptedSocket() {
        return Boolean.FALSE;
    }

    // If this is called, we've already found the remote reference.
    // The cost is currently hardwired at an arbitrary value of 1001
    // (a little more than RMI).
    protected int computeCost(AttributedMessage message) {
        return 1001;
    }

    protected DestinationLink createDestinationLink(MessageAddress address) {
        return new CorbaLink(address);
    }

    protected void ensureNodeServant() {
        if (myProxy != null) {
            return;
        }
        MessageAddress myAddress = getNameSupport().getNodeMessageAddress();
        MTImpl impl = new MTImpl(myAddress, getDeliverer());
        try {
            poa.activate_object(impl);
        } catch (Exception ex) {
            loggingService.error(null, ex);
        }
        myProxy = impl._this();
        setNodeURI(URI.create(orb.object_to_string(myProxy)));
    }

    protected void remakeNodeServant() {
        if (myProxy != null) {
            try {
                byte[] oid = poa.reference_to_id(myProxy);
                poa.deactivate_object(oid);
            } catch (Exception ex) {
                loggingService.error(null, ex);
            }
        }
        myProxy = null;
        ensureNodeServant();
    }

    /**
     * The DestinationLink class for this transport. Forwarding a message with
     * this link means looking up the MT proxy for a remote MTImpl, and calling
     * rerouteMessage on it.
     */
    class CorbaLink
            extends Link {

        CorbaLink(MessageAddress destination) {
            super(destination);
        }

        protected Object decodeRemoteRef(URI ref)
                throws Exception {
            String ior = ref.toString();
            org.omg.CORBA.Object raw = orb.string_to_object(ior);
            MT mt = MTHelper.narrow(raw);
            return mt;
        }

        public Class<CorbaLinkProtocol> getProtocolClass() {
            return CorbaLinkProtocol.class;
        }

        protected MessageAttributes forwardByProtocol(Object remote_ref, AttributedMessage message)
                throws NameLookupException, UnregisteredNameException, CommFailureException,
                MisdeliveredMessageException {
            byte[] bytes = null;
            try {
                bytes = SerializationUtils.toByteArray(message);
            } catch (DontRetryException mex) {
                throw new CommFailureException(mex);
            } catch (java.io.IOException iox) {
                // What would this mean?
            }

            byte[] res = null;
            try {
                res = ((MT) remote_ref).rerouteMessage(bytes);
            } catch (CorbaMisdeliveredMessage mis) {
                // force recache of remote
                decache();
                throw new MisdeliveredMessageException(message);
            } catch (CorbaDontRetryException mex) {
                byte[] ex_bytes = mex.cause;
                try {
                    DontRetryException mse =
                            (DontRetryException) SerializationUtils.fromByteArray(ex_bytes);
                    throw new CommFailureException(mse);
                } catch (Exception ex) {
                    // ???
                }
            } catch (Exception corba_ex) {
                // Some other CORBA failure. Decache and retry.
                decache();
                throw new CommFailureException(corba_ex);
            }

            MessageAttributes attrs = null;
            try {
                attrs = (MessageAttributes) SerializationUtils.fromByteArray(res);
            } catch (DontRetryException mex) {
                throw new CommFailureException(mex);
            } catch (java.io.IOException iox) {
                // What would this mean?
            } catch (ClassNotFoundException cnf) {
            }

            return attrs;
        }

    }

    protected void releaseNodeServant() {
    }

}
