/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

package org.cougaar.core.mts;

import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceBroker;

public class TrafficMaskingGeneratorServiceProvider implements ServiceProvider {
  private TrafficMaskingGeneratorService tmgs = null;

  /** Must provide a TrafficMaskingGeneratorService during construction**/
  public TrafficMaskingGeneratorServiceProvider(TrafficMaskingGeneratorAspect theTMG) {
    super();
    if (theTMG == null) {
      throw new IllegalArgumentException("\n" + this +" Requires a valid TrafficMaskingGeneratorAspect");
    }
    this.tmgs = theTMG;
  }

  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (serviceClass == TrafficMaskingGeneratorService.class) {
      return tmgs;
    } else {
      throw new IllegalArgumentException("TrafficMaskingGeneratorServiceProvider does not provide" +
                                         " service for: " + serviceClass);
    }
  }

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service) {
    // need to somehow release the Observer
  }

}  
