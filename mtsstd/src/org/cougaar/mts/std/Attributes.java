/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

import java.io.Serializable;

public interface Attributes extends Serializable, AttributeConstants
{
 
    Object getAttribute(String attribute);

    void setAttribute(String attribute, Object value);
    void removeAttribute(String attribute);
    void addValue(String attribute, Object value);
    void pushValue(String attribute, Object value);
    void removeValue(String attribute, Object value);

    void setLocalAttribute(String attribute, Object value);
    void removeLocalAttribute(String attribute);
    void addLocalValue(String attribute, Object value);
    void pushLocalValue(String attribute, Object value);
    void removeLocalValue(String attribute, Object value);

    Attributes cloneAttributes();
    void clearAttributes();
    void mergeAttributes(Attributes attributes);

}
