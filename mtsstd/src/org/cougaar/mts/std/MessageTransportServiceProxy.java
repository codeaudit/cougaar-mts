/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.mts;

import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.society.MessageStatistics;


/**
 * Currently the only implementation of MessageTransportService.  It
 * does almost nothing by itself - its work is accomplished by
 * redirecting calls to the MessageTransportRegistry and the
 * SendQueue.  */
public class MessageTransportServiceProxy 
    implements MessageTransportService
{
    private MessageTransportRegistry registry;
    private SendQueue sendQ;

    public MessageTransportServiceProxy(MessageTransportRegistry registry,
				       SendQueue queue) 
    {
	this.sendQ = queue;
	this.registry = registry;
    }



    /**
     * Any non-null target passes this check. */
    private boolean checkMessage(Message message) {
	MessageAddress target = message.getTarget();
	// message is ok as long as the target is not empty or null
	return target != null && !target.toString().equals("");
    }




    /**
     * Redirects the sendMessage to the SendQueue. */
    public void sendMessage(Message m) {
	if (checkMessage(m)) {
	    sendQ.sendMessage(m);
	} else {
	    System.err.println("Warning: MessageTransport.sendMessage of malformed message: "+m);
	    Thread.dumpStack();
	    return;
	}
    }

    /**
     * Redirects the request to the MessageTransportRegistry. */
    public void registerClient(MessageTransportClient client) {
	registry.registerClient(client);
    }


    /**
     * Redirects the request to the MessageTransportRegistry. */
    public void unregisterClient(MessageTransportClient client) {
	registry.unregisterClient(client);
    }
    

   
    /**
     * Redirects the request to the MessageTransportRegistry. */
    public String getIdentifier() {
	return registry.getIdentifier();
    }

    /**
     * Redirects the request to the MessageTransportRegistry. */
    public boolean addressKnown(MessageAddress a) {
	return registry.addressKnown(a);
    }

}

