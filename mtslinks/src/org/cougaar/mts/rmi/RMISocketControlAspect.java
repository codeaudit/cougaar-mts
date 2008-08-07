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
package org.cougaar.mts.rmi;
import java.net.Socket;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.mts.base.StandardAspect;

/**
 * This Aspect creates a ServiceProvider for and implementation of the
 * RMISocketControlService.  As currently defined, this  service is
 * mostly for setting socket timeouts.   It can also be used to get a
 * List of open sockets from a given MessageAddress.  
 */
public class RMISocketControlAspect
    extends StandardAspect {
    
    private Impl impl;

    public RMISocketControlAspect() {
    }

    public void load() {
        super.load();

        Provider provider = new Provider();
        impl = new Impl();
        getServiceBroker().addService(RMISocketControlService.class, provider);
    }

    public Object getDelegate(Object object, Class type) {
        if (type == Socket.class) {
            impl.cacheSocket((Socket) object);
        }
        return null;
    }

    private class Provider implements ServiceProvider {
        public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
            if (serviceClass == RMISocketControlService.class) {
                return impl;
            } else {
                return null;
            }
        }

        public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, 
                                   Object service) {
        }
    }

    private class Impl implements RMISocketControlService {
        Map<String, List<Socket>> sockets;      // host:port -> list of sockets
        Map<MessageAddress, Remote> references; 
        Map<Remote, MessageAddress> addresses;
        Map<MessageAddress, Integer> default_timeouts;
        Map<String,Remote> referencesByKey;  // host:port -> Remote stub

        private Impl() {
            sockets = new HashMap<String, List<Socket>>();
            references = new HashMap<MessageAddress,Remote>();
            addresses = new HashMap<Remote,MessageAddress>();
            default_timeouts = new HashMap<MessageAddress, Integer>();
            referencesByKey = new HashMap<String,Remote>();

            Runnable reaper = new Runnable() {
                public void run() {
                    reapClosedSockets();
                }
            };
            ThreadService tsvc = getThreadService();
            Schedulable sched = tsvc.getThread(this, reaper, "Socket Reaper");

            sched.schedule(0, 5000);
        }

        private String getKey(String host, int port) {
            return getKey(host, Integer.toString(port));
        }

        private String getKey(String host, String port) {
            // May need to canonicalize the host
            return host+ ":" +port;
        }

        private String getKey(Remote ref) {
            // Dig out the host and port, then look it up in 'sockets'.
            // form is
            // classname[RemoteStub [ref: [endpoint:[host:port](local),objID:[0]]]]
            String refString = ref.toString();
            int host_start = refString.indexOf("[endpoint:[");
            if (host_start < 0) return null;
            host_start += 11;
            int host_end = refString.indexOf(':', host_start);
            if (host_end < 0) return null;
            String host = refString.substring(host_start, host_end);
            int port_start = 1 + host_end;
            int port_end = port_start;
            int port_end_1 = refString.indexOf(',', host_end);
            int port_end_2 = refString.indexOf(']', host_end);
            if (port_end_1 < 0 && port_end_2 < 0) return null;
            if (port_end_1 < 0) 
                port_end = port_end_2;
            else if (port_end_2 < 0)
                port_end = port_end_1;
            else
                port_end = Math.min(port_end_1, port_end_2);
            String portString = refString.substring(port_start, port_end);

            String key = getKey(host, portString);
            referencesByKey.put(key, ref);

            return key;
        }

        private String getKey(Socket skt) {
            String host = skt.getInetAddress().getHostAddress();
            int port = skt.getPort();
            return getKey(host, port);
        }

        private Integer getDefaultTimeout(String key) {
            Object ref = referencesByKey.get(key);
            Object addr = addresses.get(ref);
            return default_timeouts.get(addr);
        }


        private void cacheSocket (Socket skt) {
            String key = getKey(skt);
            Integer timeout = getDefaultTimeout(key);
            if (timeout != null) {
                try {
                    skt.setSoTimeout(timeout.intValue());
                } catch (java.net.SocketException ex) {
                    // Don't care
                }
            }
            synchronized (this) {
                List<Socket> skt_list = sockets.get(key);
                if (skt_list == null) {
                    skt_list = new ArrayList<Socket>();
                    sockets.put(key, skt_list);
                }
                skt_list.add(skt);
            }
        }

        synchronized void reapClosedSockets() {
            // Prune closed sockets
            for (List<Socket> socketList : sockets.values()) {
                Iterator<Socket> itr = socketList.iterator();
                while (itr.hasNext()) {
                    Socket socket = itr.next();
                    if (socket.isClosed()) {
                        itr.remove();
                    }
                }
            }
        }

        synchronized boolean setSoTimeout(Remote reference, int timeout) {
            String key = getKey(reference);
            List<Socket> skt_list =  sockets.get(key);
            if (skt_list == null) {
                return false;
            }
            boolean success = false;
            Iterator<Socket> itr = skt_list.iterator();
            while (itr.hasNext()) {
                Socket skt = itr.next();
                try { 
                    skt.setSoTimeout(timeout);
                    success = true;
                } catch (java.net.SocketException ex) {
                    itr.remove();
                }
            }
            return success;
        }

        public boolean setSoTimeout(MessageAddress addr, int timeout) {
            // Could use the NameService to lookup the Reference from
            // the address.
            default_timeouts.put(addr, timeout);
            Remote reference = references.get(addr);
            if (reference != null) {
                return setSoTimeout(reference, timeout);
            } else {
                return false;
            }
        }

        public synchronized void setReferenceAddress(Remote reference, 
                                                     MessageAddress addr) {
            references.put(addr, reference);
            addresses.put(reference, addr);
            Integer timeout = default_timeouts.get(addr);
            if (timeout != null) {
                setSoTimeout(reference, timeout.intValue());
            }
        }


        public List<Socket> getSocket(MessageAddress addr) {
            Remote ref = references.get(addr);
            if (ref == null) {
                return null;
            }
            String key = getKey(ref);
            return sockets.get(key);
        }

    }


}
