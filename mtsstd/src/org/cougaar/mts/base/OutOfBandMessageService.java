/*
 *
 * Copyright 2008 by BBN Technologies Corporation
 *
 */

package org.cougaar.mts.base;

import org.cougaar.core.component.Service;
import org.cougaar.core.mts.MessageAddress;

/**
 * Within the MTS we sometimes need to send internal messages
 * directly to a given Agent.  Use this service to send such
 * messages, and also to check if a given message is one.
 */
public interface OutOfBandMessageService
        extends Service {
    
    public boolean sendOutOfBandMessage(AttributedMessage message, MessageAddress destinaion);
    
    public boolean isOutOfBandMessage(AttributedMessage message);

}
