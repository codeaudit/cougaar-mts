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

package org.cougaar.mts.base;
import java.util.HashMap;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.mts.std.AttributedMessage;

/**
 * This protocol handles all intra-node message traffic.   */
public class LoopbackLinkProtocol 
    extends LinkProtocol
{

    private HashMap links;

    public LoopbackLinkProtocol() {
	super();
	links = new HashMap();
    }

    public synchronized DestinationLink getDestinationLink(MessageAddress address) {
	DestinationLink link = (DestinationLink) links.get(address);
	if (link == null) {
	    link = new Link(address);
	    link = (DestinationLink) attachAspects(link,DestinationLink.class);
	    links.put(address, link);
	}
	return link;
    }

    public void registerClient(MessageTransportClient client) {
	// Does nothing because the Database of local clients is held
	// by MessageTransportServerImpl
    }

    public void unregisterClient(MessageTransportClient client) {
	// Does nothing because the Database of local clients is held
	// by MessageTransportServerImpl
    }

    public boolean addressKnown(MessageAddress address) {
	// true iff the address is local
	return getRegistry().isLocalClient(address);
    }



    private class Link implements DestinationLink {
	MessageAddress address;

	Link(MessageAddress address) {
	    this.address = address;
	}

	public int cost(AttributedMessage msg) {
	    MessageAddress addr = msg.getTarget();
	    if (getRegistry().isLocalClient(addr)) {
		return 0;
	    } else {
		return Integer.MAX_VALUE;
	    }
	}
	

	public boolean isValid() {
	    return getRegistry().isLocalClient(address);
	}

	public MessageAttributes forwardMessage(AttributedMessage message) 
	    throws MisdeliveredMessageException
	{
	    return getDeliverer().deliverMessage(message, message.getTarget());
	}

	public boolean retryFailedMessage(AttributedMessage message,
					  int retryCount) 
	{
	    return true;
	}

    
	public Class getProtocolClass() {
	    return LoopbackLinkProtocol.class;
	}


	public MessageAddress getDestination() {
	    return address;
	}



	public Object getRemoteReference() {
	    return null;
	}

	public void addMessageAttributes(MessageAttributes attrs) {
	    
	}

	

    }

}
