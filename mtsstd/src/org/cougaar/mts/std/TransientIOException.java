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
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * Special kind of IOException whose stack trace shouldn't be
 * logged.  See DestinationQueueImpl.
 */

public class TransientIOException
    extends CougaarIOException
    implements java.io.Serializable
{
    private transient Logger logger = 
	Logging.getLogger("org.cougaar.core.mts.TransientIOException");

    public TransientIOException() 
    {
	super();
    }

    public TransientIOException(String message) 
    {
	super(message);
    }


    // Make these very quiet
    public void printStackTrace() {
	if (logger.isDebugEnabled()) super.printStackTrace();
    }


    public void printStackTrace(java.io.PrintStream s) {
	if (logger.isDebugEnabled()) super.printStackTrace(s);
    }


    public void printStackTrace(java.io.PrintWriter w) {
	if (logger.isDebugEnabled()) super.printStackTrace(w);
    }
    
}
