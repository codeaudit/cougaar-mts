/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.mts;

import org.cougaar.core.society.MessageAddress;

public class UnregisteredNameException extends Exception 
{
    private MessageAddress addr;
	
    public UnregisteredNameException(MessageAddress addr) {
	super(addr + " not found");
	this.addr = addr;
    }
}
