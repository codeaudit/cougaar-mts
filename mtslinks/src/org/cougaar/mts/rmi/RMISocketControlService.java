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
import java.util.List;

import org.cougaar.core.component.Service;
import org.cougaar.core.mts.MessageAddress;

/**
 * This is an MTS-internal service used for simple socket
 * manipulation in RMI communication.  The implementation is in
 * RMISocketControlAspect.  It's used only by the RMILinkProtocol and
 * by OBJS.
 */
public interface RMISocketControlService extends Service {
    /**
     * The SO Timeout is set for ALL sockets that go to the remote RMI
     * reference The side effect of this is that other agents that are
     * on the same node will also have their time out changed.
     */
    boolean setSoTimeout(MessageAddress addr, int timeout);

    /** 
     * The RMILinkProtocol calls this method, Other Aspects should not
     * call this method.
     */
    void setReferenceAddress(Remote reference, MessageAddress addr);


    /**
     * Returns a list of all Sockets used for communication between
     * the running Node and the given remoted address.
     */
    List<Socket> getSocket(MessageAddress addr);
}
