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

package org.cougaar.mts.std;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.SerializationUtils;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.LinkProtocol; // javadoc
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.MT;
import org.cougaar.mts.base.MTImpl;
import org.cougaar.mts.base.SocketFactory;
import org.cougaar.mts.base.RMILinkProtocol;
import org.cougaar.mts.base.CougaarIOException;


/**
 * This {@link LinkProtocol} is a simple extension of {@link
 * RMILinkProtocol} that uses preserialization to send byte-arrays via
 * the {@link SerializedMT} interface rather than AttributedMessages via the
 * {@link MT} interface.
 */
public class SerializedRMILinkProtocol extends RMILinkProtocol
{

    public SerializedRMILinkProtocol() {
	super();
    }


    protected String getProtocolType() {
	return "-SerializedRMI";
    }

    // Is this different?
    protected int computeCost(AttributedMessage message) {
	return super.computeCost(message);
    }

    protected MTImpl makeMTImpl(MessageAddress myAddress,
				SocketFactory socfac)
	throws java.rmi.RemoteException
    {
	return new SerializedMTImpl(myAddress, getServiceBroker(), socfac);
    }

    protected MessageAttributes doForwarding(MT remote, 
					     AttributedMessage message) 
	throws MisdeliveredMessageException, 
	       java.rmi.RemoteException,
	       CommFailureException
    {
	if (remote instanceof SerializedMT) {
	    byte[] messageBytes = null;
	    try {
		messageBytes = SerializationUtils.toByteArray(message);
	    } catch (CougaarIOException mex) {
		throw new CommFailureException(mex);
	    } catch (java.io.IOException iox) {
		// What would this mean?
	    }

	    byte[] res = null;
	    try {
		res = ((SerializedMT) remote).rerouteMessage(messageBytes);
	    } catch (CougaarIOException mex) {
		throw new CommFailureException(mex);
	    } catch (java.rmi.RemoteException remote_ex) {
		Throwable cause = remote_ex.getCause();
		checkForMisdelivery(cause, message);
		// Not a misdelivery  - rethrow the remote exception
		throw remote_ex;
	    } catch (IllegalArgumentException illegal_arg) {
		checkForMisdelivery(illegal_arg, message);
		// Not a misdelivery  - rethrow the exception
		throw illegal_arg;
	    }

	    MessageAttributes attrs = null;
	    try {
		attrs = (MessageAttributes) 
		    SerializationUtils.fromByteArray(res);
	    } catch (CougaarIOException mex) {
		throw new CommFailureException(mex);
	    } catch (java.io.IOException iox) {
		// What would this mean?
	    } catch (ClassNotFoundException cnf) {
		// What would this mean?
	    }
	    return attrs;
	} else {
	    return super.doForwarding(remote, message);
	}
    }

}
