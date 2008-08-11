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

package org.cougaar.mts.std;

import java.util.List;

import org.cougaar.core.component.Service;
import org.cougaar.mts.base.MessageTransportAspect;
import org.cougaar.mts.base.StandardAspect;

/**
 * This is an MTS-internal utility service which supports the use of aspects.
 * It's used to add new aspects, typically at load time, and to attach aspect
 * delegates at run time.
 */
public interface AspectSupport
        extends Service {
    /**
     * Return an aspect object whose class is as given. If there's more than
     * one, returns the last one added. If there are none, return null.
     */
    MessageTransportAspect findAspect(String classname);

    /**
     * Add an aspect to the global list.
     */
    void addAspect(MessageTransportAspect aspect);

    /**
     * Add an aspect to the global list. This method is vestigial, since
     * StandardAspects are also MessageTransportAspects.
     */
    void addAspect(StandardAspect aspect);

    /**
     * Allow each global aspect to attach a delegate to given object at the
     * cut-point indicated by the given type.
     * 
     * The generic parameter T enforces that the object provided matches the
     * type provided.
     */
    <T> T attachAspects(T object, Class<T> type);

    /**
     * Allow each aspect in the list of candidates to attach a delegate to given
     * object at the cut-point indicated by the given type.
     */
    <T> T attachAspects(T delegate, Class<T> type, List<String> candidates);
}
