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

import org.cougaar.core.service.*;

import org.cougaar.core.node.*;

/**
 * A marker class for multicasting messages.
 * Used by constant addresses in MessageAddress.
 **/

public class MulticastMessageAddress extends MessageAddress 
{

    private static final String CLASS_TAG = "CLASS_";

    /** for Externalizable use only **/
    public MulticastMessageAddress() {}

    public MulticastMessageAddress( String address ) {
	super(address);
    }


    public MulticastMessageAddress( Class clientClass ) {
	super( CLASS_TAG + clientClass.getName() );
    }

    public boolean hasReceiverClass() {
	return getAddress().startsWith(CLASS_TAG);
    }

    public Class getReceiverClass() {
	if (hasReceiverClass()) {
	    String class_name = getAddress().substring(CLASS_TAG.length());
	    try {
		return Class.forName(class_name);
	    } catch (ClassNotFoundException cnf) {
		System.err.println("Bad multicast address: " +
				   class_name + " is not a class name");
		return null;
	    }
	} else {
	    return null;
	}
    }
    

}
