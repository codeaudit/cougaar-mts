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


    public MulticastMessageAddress(MessageAttributes attrs) {
	super(attrs);
    }

    public MulticastMessageAddress(MessageAttributes attrs, String address) {
	super(attrs, address);
    }


    public MulticastMessageAddress(MessageAttributes attrs, Class clientClass){
	super(attrs, CLASS_TAG + clientClass.getName() );
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
		return null;
	    }
	} else {
	    return null;
	}
    }
    

}
