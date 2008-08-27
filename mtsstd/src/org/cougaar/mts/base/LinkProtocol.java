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

import java.io.IOException;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.GroupMessageAddress;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageTransportClient;

/**
 * The parent class of all Link Protocols. Instantiable subclasses are required
 * to do two things: they must be able to say whether or not they can deal with
 * any particular addresss (addressKnown), and they must be able to supply a
 * DestinationLink instance for any address they can deal with
 * (getDestinationLink). They will also be given the opportunity to "register"
 * clients, if they have any interest in doing so (for instance, an RMI
 * transport might use this as an opportunity to publish an MTImpl for the
 * client on a nameserver).
 * 
 * LinkProtocols are implicitly factories for the creation of DestinationLinks,
 * so the class is declared to extend AspectFactory, in order to allow aspects
 * to be added to the Links. The aspect attachment is handled in each specific
 * transport class.
 * 
 * Finally, LinkProtocols can act as ServiceProviders for protocol-specific
 * services.
 */
abstract public class LinkProtocol
        extends BoundComponent
        implements ServiceProvider {
    private MessageDeliverer deliverer;

    protected class ServiceProxy
            implements LinkProtocolService {
        public boolean addressKnown(MessageAddress address) {
            return LinkProtocol.this.addressKnown(address);
        }
    }

    // LinkProtocol implementations must supply these!

    /**
     * Create a DestinationLink for the given protocol/destination pair.
     */
    abstract public DestinationLink getDestinationLink(MessageAddress destination);

    /**
     * Handle any required local and/or nameservice registration for the given
     * client.
     */
    abstract public void registerClient(MessageTransportClient client);

    /**
     * Handle any required local and/or nameservice de-registration for the
     * given client.
     */
    abstract public void unregisterClient(MessageTransportClient client);

    /**
     * Determine whether or not the given protocol understands the given
     * address.
     */
    abstract public boolean addressKnown(MessageAddress address);

    protected LinkProtocol() {
        // System.out.println("Made LinkProtocol " +this);
    }

    protected MessageDeliverer getDeliverer() {
        if (deliverer == null) {
            ServiceBroker sb = getServiceBroker();
            deliverer = sb.getService(this, MessageDeliverer.class, null);
        }
        return deliverer;
    }

    // Allow subclasses to provide their own load()
    protected void super_load() {
        super.load();
        getRegistry().addLinkProtocol(this);
    }

    public void load() {
        super_load();
    }

    // Default is no-op. Socket based protocols (RMI, HTTP, CORBA,
    // etc) will have work to do.
    public void ipAddressChanged() {
    }

    public Object getService(ServiceBroker sb, Object requestor, Class<?> serviceClass) {
        return null;
    }

    public void releaseService(ServiceBroker sb,
                               Object requestor,
                               Class<?> serviceClass,
                               Object service) {
    }

    public <T> T attachAspects(T delegate, Class<T> type) {
        return getAspectSupport().attachAspects(delegate, type);
    }
    
    /**
     * By default, a protocol can handle everything <em>except</em> true
     * multicast addresses.
     * <p>
     * TODO: If we decide to convert the old-style MulticastAddress to extend
     * GroupMessageAddress, we'll need to pass those here, since in that case the
     * protocol doesn't matter.
     */
    public boolean supportsAddressType(Class<? extends MessageAddress> addressType) {
        return !GroupMessageAddress.class.isAssignableFrom(addressType);
    }
    
    
    // Multicast, not supported by default.
    public void join(GroupMessageAddress multicastAddress) throws IOException {
        throw new IOException(this + " cannot join multicast groups");
    }

    public void leave(GroupMessageAddress multicastAddress) throws IOException {
        throw new IOException(this + " cannot leave multicast groups");
    }
}
