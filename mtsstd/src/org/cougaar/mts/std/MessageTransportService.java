/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

package org.cougaar.core.mts;

import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.component.Service;

/**
 * Abstract MessageTransport layer for Society interaction.
 *
 **/

public interface MessageTransportService extends Service
{

    /** Ask MessageTransport to deliver a message (asynchronously).
     * message.getTarget() names the destination.  The client must be
     * registered, otherwise the message will not be sent.
     **/
    void sendMessage(Message m);

    /** 
     * Register a client with MessageTransport.  A client is any
     * object which can receive Messages directed to it as the Target
     * of a message.
     **/
    void registerClient(MessageTransportClient client);


    /** 
     * Unregister a client with MessageTransport.  No further
     * sendMessage calls will be accepted, and any queued messages
     * which aren't successfully delivered will be dropped.
     **/
    void unregisterClient(MessageTransportClient client);


    /**
     * Block until all queued messages have been sent (or dropped).
     * @return The list of dropped messages (could be null). */
    java.util.ArrayList flushMessages();

    /**
     * @return the name of the entity that this MessageTransport
     * represents.  Will usually be the name of a node.
     **/
    String getIdentifier();

    /** @return true IFF the MessageAddress is known to the nameserver **/
    boolean addressKnown(MessageAddress a);
}

