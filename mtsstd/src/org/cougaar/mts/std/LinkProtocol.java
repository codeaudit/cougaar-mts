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

import org.cougaar.core.service.*;

import org.cougaar.core.node.*;

import org.cougaar.core.mts.MessageAddress;


/**
 * The parent class of all LinkProtocols.  Instantiable subclasses
 * are required to do two things: they must be able to say whether or
 * not they can deal with any particular addresss (addressKnown), and
 * they must be able to supply a DestinationLink instance for any
 * address they can deal with (getDestinationLink).  They will also be
 * given the opportunity to "register" clients, if they have any
 * interest in doing so (for instance, an RMI transport might use this
 * as an opportunity to publish an MTImpl for the client on a
 * nameserver). 
 *
 * LinkProtocols are implicitly factories for the creation of
 * DestinationLinks, so the class is declared to extend AspectFactory,
 * in order to allow aspects to be added to the Links.  The aspect
 * attachment is handled in each specific transport class.  */
public abstract class LinkProtocol extends AspectFactory
{
    protected MessageDeliverer deliverer;
    protected MessageTransportRegistry registry;
    protected NameSupport nameSupport;


    // LinkProtocol implementations must supply these!

    /**
     * Create a DestinationLink for the given protocol/destination
     * pair.
     */
    abstract public DestinationLink getDestinationLink(MessageAddress destination);


    /** 
     * Handle any required local and/or nameservice registration for
     * the given client.
     */
    abstract public void registerClient(MessageTransportClient client);

    /** 
     * Handle any required local and/or nameservice de-registration
     * for the given client.
     */
    abstract public void unregisterClient(MessageTransportClient client);


    /**
     * Register an MTS pseudo-agent to handle incoming multicasts.
     */
    abstract public void registerMTS(MessageAddress address);


    /**
     * Determine whether or not the given protocol understands the
     * given address. */
    abstract public boolean addressKnown(MessageAddress address);






    protected LinkProtocol(AspectSupport aspectSupport) {
	super(aspectSupport);
    }

    public void setRegistry(MessageTransportRegistry registry) {
	this.registry = registry;
    }


    public void setNameSupport(NameSupport nameSupport) {
	this.nameSupport = nameSupport;
    }

    public void setDeliverer(MessageDeliverer deliverer) {
	this.deliverer = deliverer;
    }


    

}
