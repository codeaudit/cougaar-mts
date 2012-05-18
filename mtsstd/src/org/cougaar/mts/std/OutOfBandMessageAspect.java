/*
 *
 * Copyright 2008 by BBN Technologies Corporation
 *
 */

package org.cougaar.mts.std;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.MessageTransportRegistryService;
import org.cougaar.mts.base.OutOfBandMessageService;
import org.cougaar.mts.base.ReceiveLink;
import org.cougaar.mts.base.StandardAspect;

/**
 * Service provider and implementation for {@link OutOfBandMessageService}
 */
public final class OutOfBandMessageAspect
        extends StandardAspect
        implements ServiceProvider {
    
    private OutOfBandMessageService service;
    private MessageTransportRegistryService registry;

    @Override
   public void load() {
        super.load();
        registry = getRegistry();
        service = new OutOfBandMessageServiceImpl();
        ServiceBroker sb = getServiceBroker();
        sb.addService(OutOfBandMessageService.class, this);
    }

    public Object getService(ServiceBroker sb, Object requestor, Class<?> serviceClass) {
        if (serviceClass == OutOfBandMessageService.class) {
            return service;
        }
        return null;
    }

    public void releaseService(ServiceBroker sb,
                               Object requestor,
                               Class<?> serviceClass,
                               Object service) {
    }

    private static final class OutOfBandMessage extends AttributedMessage {
        public OutOfBandMessage(Message msg, MessageAttributes attrs) {
            super(msg, attrs);
        }
    }
    
    private class OutOfBandMessageServiceImpl implements OutOfBandMessageService {
        public boolean isOutOfBandMessage(AttributedMessage message) {
            return message instanceof OutOfBandMessage;
        }
        
        public boolean sendOutOfBandMessage(Message message, MessageAttributes attrs, 
                                            MessageAddress destinaion) {
            synchronized (registry) {
                ReceiveLink link = registry.findLocalReceiveLink(destinaion);
                if (link != null) {
                    link.deliverMessage(new OutOfBandMessage(message, attrs));
                    return true;
                } else {
                    return false;
                }
            }
        }
        
    }
}
