/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.mts.base;
import java.util.ArrayList;
import java.util.Iterator;

import org.cougaar.core.component.Service;
import org.cougaar.core.mts.AgentState;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.mts.std.MulticastMessageAddress;

/**
 * The MessageTransportRegistry singleton is a utility instance that
 * helps certain pieces of the message transport subsystem to find one
 * another. */
public interface MessageTransportRegistryService extends Service
{


    // public void setNameSupport(NameSupport nameSupport);
    // public void setReceiveLinkFactory(ReceiveLinkFactory receiveLinkFactory);

    void addLinkProtocol(LinkProtocol lp);
    boolean hasLinkProtocols(); // only useful for the LinkProtocolFactory
    String getIdentifier();
    boolean isLocalClient(MessageAddress id);
    ReceiveLink findLocalReceiveLink(MessageAddress id);
    Iterator findLocalMulticastReceivers(MulticastMessageAddress addr);
    Iterator findRemoteMulticastTransports(MulticastMessageAddress addr);
    void registerClient(MessageTransportClient client);
    void unregisterClient(MessageTransportClient client);
    boolean addressKnown(MessageAddress address);
    ArrayList getDestinationLinks(MessageAddress destination);
    AgentState getAgentState(MessageAddress agent);
    void removeAgentState(MessageAddress agent);
}