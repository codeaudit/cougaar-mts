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
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject; // not used but needed by ANT and build process -- DO NOT REMOVE


import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.mts.std.AttributedMessage;

/** 
 * RMI remote object providing the implementation of {@link MT}.  Note
 * that this class extends {@link RemoteObject}, not {@link
 * UnicastRemoteObject}, and will therefore not be exported in tne
 * super constructor.  The export has to happen later, and is handled
 * by {@link RMILinkProtocol}.
 * 
 * <p>The transient tags shouldn't really be necessary since this
 * object should always be serialized as an RMI stub.  But leave them
 * in anyway, for documentation if nothing else.
 **/
public class MTImpl extends RemoteObject implements MT 
{
    private MessageAddress address;

    private transient ServiceBroker sb;
    private transient MessageDeliverer deliverer;
    private transient SocketFactory socfac;

    public MTImpl(MessageAddress addr,  
		  ServiceBroker sb,
		  SocketFactory socfac) 
	throws RemoteException 
    {
	// super(0, socfac, socfac);
	super();
	this.socfac = socfac;
	this.sb = sb;
	this.address = addr;
	this.deliverer = (MessageDeliverer) 
	    sb.getService(this,  MessageDeliverer.class, null);
    }

    public SocketFactory getSocketFactory() {
	return socfac;
    }


    public MessageAttributes rerouteMessage(AttributedMessage message) 
	throws MisdeliveredMessageException
    {
	return deliverer.deliverMessage(message, message.getTarget());
    }

    public MessageAddress getMessageAddress() {
	return address;
    }

    public String toString() {
      return "MT for "+getMessageAddress();
    }
}
