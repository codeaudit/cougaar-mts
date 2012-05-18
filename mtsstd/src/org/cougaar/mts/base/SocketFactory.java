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
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.server.RMISocketFactory;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * This is an RMISocketFactory that can wrap {@link ServerSocket}s and apply
 * Aspect delegates to client {@link Socket}s. It handles SSL as well as
 * straight RMI, using the CSI classes for the former if they're available.
 * 
 * @property org.cougaar.message.transport.server_socket_class
 *           ServerSocketWrapper classname (default is no wrapper).
 */
public class SocketFactory
        extends RMISocketFactory
        implements java.io.Serializable {

    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private static transient Logger logger =
            Logging.getLogger("org.cougaar.mts.base.SocketFactory");
    // This has to be set very early from outside
    private static transient SocketControlProvisionService PolicyProvider;

    static void configureProvider(ServiceBroker sb) {
        PolicyProvider = sb.getService(sb, SocketControlProvisionService.class, null);
    }

    private static final String SSL_SOCFAC_CLASS =
            "org.cougaar.core.security.ssl.KeyRingSSLFactory";
    private static final String SSL_SERVER_SOCFAC_CLASS =
            "org.cougaar.core.security.ssl.KeyRingSSLServerFactory";

    private static final Class<?>[] FORMALS = {};
    private static final Object[] ACTUALS = {};

    static SSLSocketFactory getSSLSocketFactory() {
        Class<?> cls = null;
        try {
            cls = Class.forName(SSL_SOCFAC_CLASS);
        } catch (ClassNotFoundException cnf) {
            // silently use the default class
            return (SSLSocketFactory) SSLSocketFactory.getDefault();
        }

        try {
            Method meth = cls.getMethod("getDefault", FORMALS);
            return (SSLSocketFactory) meth.invoke(cls, ACTUALS);
        } catch (Exception ex) {
            if (logger.isErrorEnabled()) {
                logger.error("Error getting SSL socket factory: " + ex);
            }
            return (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
    }

    static ServerSocketFactory getSSLServerSocketFactory() {
        Class<?> cls = null;
        try {
            cls = Class.forName(SSL_SERVER_SOCFAC_CLASS);
        } catch (ClassNotFoundException cnf) {
            // silently use the default class
            return SSLServerSocketFactory.getDefault();
        }

        try {
            Method meth = cls.getMethod("getDefault", FORMALS);
            return (ServerSocketFactory) meth.invoke(cls, ACTUALS);
        } catch (Exception ex) {
            if (logger.isErrorEnabled()) {
                logger.error("Error getting socket factory: " + ex);
            }
            return SSLServerSocketFactory.getDefault();
        }
    }

    private static final String WRAPPER_CLASS_PROP =
            "org.cougaar.message.transport.server_socket_class";

    private static Class<?> serverWrapperClass;
    static {
        String classname = SystemProperties.getProperty(WRAPPER_CLASS_PROP);
        if (classname != null) {
            try {
                serverWrapperClass = Class.forName(classname);
            } catch (Exception ex) {
                System.err.println(ex);
            }
        }
    }

    // The factory will be serialized along with the MTImpl, and we
    // definitely don't want to include the AspectSupport when that
    // happens. Instead, the aspect delegation will be handled by a
    // special static call.
    boolean use_ssl, use_aspects;
    transient SocketControlPolicy policy;

    public SocketFactory(boolean use_ssl, boolean use_aspects) {
        this.use_ssl = use_ssl;
        this.use_aspects = use_aspects;
        // get the policy from a service
    }

    public boolean isMTS() {
        return use_aspects;
    }

    public boolean usesSSL() {
        return use_ssl;
    }

    @Override
   public Socket createSocket(String host, int port)
            throws IOException, UnknownHostException {
        Socket socket = new Socket();
        InetSocketAddress endpoint = new InetSocketAddress(host, port);
        if (policy == null && PolicyProvider != null) {
            policy = PolicyProvider.getPolicy();
        }

        if (policy != null) {
            int timeout = policy.getConnectTimeout(this, host, port);
            socket.connect(endpoint, timeout);
        } else {
            socket.connect(endpoint);
        }

        if (use_ssl) {
            SSLSocketFactory socfac = getSSLSocketFactory();
            socket = socfac.createSocket(socket, host, port, true);
        }

        return use_aspects ? AspectSupportImpl.attachRMISocketAspects(socket) : socket;
    }

    @Override
   public ServerSocket createServerSocket(int port)
            throws IOException {
        ServerSocket s = null;
        if (use_ssl) {
            ServerSocketFactory factory = getSSLServerSocketFactory();
            s = factory.createServerSocket(port);
        } else {
            s = new ServerSocket(port);
        }
        if (serverWrapperClass != null) {
            try {
                ServerSocketWrapper wrapper =
                        (ServerSocketWrapper) serverWrapperClass.newInstance();
                wrapper.setDelegate(s);
                return wrapper;
            } catch (Exception ex) {
                System.err.println(ex);
                return s;
            }
        } else {
            return s;
        }
    }

    @Override
   public int hashCode() {
        if (use_ssl) {
            if (use_aspects) {
                return 0;
            }
            return 1;
        } else {
            if (use_aspects) {
                return 2;
            }
            return 3;
        }
    }

    @Override
   public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o.getClass() == this.getClass()) {
            SocketFactory that = (SocketFactory) o;
            return this.use_ssl == that.use_ssl && this.use_aspects == that.use_aspects;
        }
        return false;
    }
}
