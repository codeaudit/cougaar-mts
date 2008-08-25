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

import java.net.URI;
import java.util.Iterator;
import java.util.Set;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MulticastMessageAddress;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.wp.ListAllNodes;

/**
 * This {@link ServiceProvider} provides the {@link NameSupport} service. An
 * inner class implements that service.
 */
public final class NameSupportImpl
        implements ServiceProvider {
    private NameSupport service;

    NameSupportImpl(String id, ServiceBroker sb) {
        service = new ServiceImpl(id, sb);
        AspectSupport aspectSupport = sb.getService(this, AspectSupport.class, null);
        service = aspectSupport.attachAspects(service, NameSupport.class);
    }

    public Object getService(ServiceBroker sb, Object requestor, Class<?> serviceClass) {
        if (serviceClass == NameSupport.class) {
            return service;
        } else {
            return null;
        }
    }

    public void releaseService(ServiceBroker sb,
                               Object requestor,
                               Class<?> serviceClass,
                               Object service) {
    }

    private final static class ServiceImpl
            implements NameSupport {
        private final LoggingService loggingService;
        private final WhitePagesService wpService;
        private final MessageAddress myNodeAddress;

        private ServiceImpl(String id, ServiceBroker sb) {
            wpService = sb.getService(this, WhitePagesService.class, null);
            loggingService = sb.getService(this, LoggingService.class, null);
            myNodeAddress = MessageAddress.getMessageAddress(id);
        }

        public MessageAddress getNodeMessageAddress() {
            return myNodeAddress;
        }

        private class VoidWPCallback
                implements Callback {
            public void execute(Response response) {
                if (response.isSuccess()) {
                    if (loggingService.isInfoEnabled()) {
                        loggingService.info("WP Response: " + response);
                    }
                } else {
                    loggingService.error("WP Error: " + response);
                }
            }
        }

        private final void _register(String agent, URI ref, String protocol) {
            AddressEntry entry = AddressEntry.getAddressEntry(agent, protocol, ref);
            try {
                Callback cb = new VoidWPCallback();
                wpService.rebind(entry, cb);
            } catch (Exception ex) {
                loggingService.error(null, ex);
            }
        }

        private final void _unregister(String agent, URI ref, String protocol) {
            AddressEntry entry = AddressEntry.getAddressEntry(agent, protocol, ref);
            Callback callback = new VoidWPCallback();
            try {
                wpService.unbind(entry, callback);
            } catch (Exception ex) {
                loggingService.error(null, ex);
            }
        }

        public void registerAgentInNameServer(URI reference, MessageAddress addr, String protocol) {
            _register(addr.getAddress(), reference, protocol);
        }

        public void unregisterAgentInNameServer(URI reference, MessageAddress addr, String protocol) {
            _unregister(addr.getAddress(), reference, protocol);
        }

        public void lookupAddressInNameServer(MessageAddress address,
                                              String protocol,
                                              Callback callback) {
            wpService.get(address.getAddress(), protocol, callback);
        }

        public URI lookupAddressInNameServer(MessageAddress address, String protocol) {
            return lookupAddressInNameServer(address, protocol, 0);
        }

        public URI lookupAddressInNameServer(MessageAddress address, String protocol, long timeout) {
            AddressEntry entry;
            try {
                entry = wpService.get(address.getAddress(), protocol, timeout);
            } catch (Exception ex) {
                entry = null;
                loggingService.error(null, ex);
            }
            return entry == null ? null : entry.getURI();
        }

        private static class EmptyIterator<T>
                implements Iterator<T> {
            public boolean hasNext() {
                return false;
            }

            public T next() {
                return null;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        }

        public Iterator<MessageAddress> lookupMulticast(MulticastMessageAddress address) {
            try {
                @SuppressWarnings("unchecked") // wp not genericized
                Set<String> result = ListAllNodes.listAllNodes(wpService, 30000);
                if (result == null) {
                    return new EmptyIterator<MessageAddress>();
                }
                final Iterator<String> iter = result.iterator();
                return new Iterator<MessageAddress>() {
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    public MessageAddress next() {
                        String node = iter.next();
                        return MessageAddress.getMessageAddress(node);
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            } catch (Exception ex) {
                if (loggingService.isWarnEnabled()) {
                    loggingService.warn("Multicast had WP timout");
                }
                return new EmptyIterator<MessageAddress>();
            }
        }

    }

}
