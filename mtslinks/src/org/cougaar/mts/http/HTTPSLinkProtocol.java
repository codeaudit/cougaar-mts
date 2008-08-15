/* 
 * <copyright> 
 *  Copyright 1999-2004 Cougaar Software, Inc.
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
package org.cougaar.mts.http;

import org.cougaar.core.service.ServletService;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.LinkProtocol;

/**
 * This {@link LinkProtocol} uses the Cougaar's {@link ServletService} (Tomcat)
 * for communication via https (ssl).
 */
public class HTTPSLinkProtocol
        extends HTTPLinkProtocol {
    public HTTPSLinkProtocol() {
        super();
    }

    public String getProtocolType() {
        return "-HTTPS";
    }

    public String getProtocol() {
        return "https";
    }

    /**
     * Returns TRUE
     */
    protected Boolean usesEncryptedSocket() {
        return Boolean.TRUE;
    }

    // hard-code value. higher than RMI but lower than SSLRMI
    protected int computeCost(AttributedMessage message) {
        return super.computeCost(message) * 3;
    }

    // cost of DestinationLink is hard-coded to 500

    public Class<HTTPSLinkProtocol> getProtocolClass() {
        return HTTPSLinkProtocol.class;
    } // getProtocolClass()

}
