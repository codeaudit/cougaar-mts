/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.mts.std;
import java.rmi.server.RMISocketFactory;
import java.util.Map;

import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.cougaar.core.component.ComponentSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.SocketFactoryService;

/**
 * Provide SocketFactoryService via the std (aspected) MTS for the rest of the world.
 */

public final class SocketFactorySPC
  extends ComponentSupport
{
  private SFSP _sfsp;
  private SFS _sfs;

  public void load() {
    super.load();
    _sfsp = new SFSP();
    _sfs = new SFS();
    getServiceBroker().addService(SocketFactoryService.class, _sfsp);
  }

  public void unload() {
    getServiceBroker().revokeService(SocketFactoryService.class, _sfsp);
    _sfsp = null;
    _sfs = null;
    super.unload();
  }


  private class SFSP implements ServiceProvider {
    public Object getService(ServiceBroker sb, 
                             Object requestor,
                             Class serviceClass) {
      if (serviceClass == SocketFactoryService.class) {
        return _sfs;
      }
      return null;
    }
    
    public void releaseService(ServiceBroker sb, 
			       Object requestor, 
			       Class serviceClass, 
			       Object service) {
      // no need to do anything
    }
  }

  private class SFS implements SocketFactoryService {
    /** Get an appropriate SocketFactory instance.
     * the return value is typed Object because RMISocketFactory and SSLSocketFactory
     * do not otherwise share a superclass.
     * Implementations will generally support SSLSocketFactory, SSLServerSocketFactory, and RMISocketFactory.
     * RMISocketFactory may be parameterized (via the second argument) with "ssl"=Boolean (default FALSE) and
     * "aspects"=Boolean (default FALSE).
     * <p>
     * Example:<br>
     * <code>
     * Map params = new HashMap(); params.put("ssl", Boolean.TRUE);<br>
     * RMISocketFactory rsf = (RMISocketFactory) socketFactoryService.getSocketFactory(RMISocketFactory.class, params);<br>
     * </code>
     * @param clazz Specifies the class required.  If the class cannot be supported by
     * the service, it will return null.
     * @param m Allows arbitrary preferences and parameters to be specified.
     * @return an object which is instanceof the requested class or null.
     **/
    public Object getSocketFactory(Class clazz, Map m) {
      if (clazz == SSLSocketFactory.class) {
        return SocketFactory.getSSLSocketFactory();
      } else if (clazz == SSLServerSocketFactory.class) {
        return SocketFactory.getSSLServerSocketFactory();
      } else if (clazz == RMISocketFactory.class) {
        boolean sslp = (m.get("ssl") == Boolean.TRUE);
        boolean aspects = (m.get("aspects") == Boolean.TRUE);
        return new SocketFactory(sslp, aspects);
      } else {
        return null;
      }
    }
  }
}
