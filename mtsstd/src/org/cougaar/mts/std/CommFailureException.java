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

public class CommFailureException extends Exception 
{
    private Exception nested_exception;

    public CommFailureException(Exception nested) {
	super(nested.toString());
	this.nested_exception = nested;
    }
}

