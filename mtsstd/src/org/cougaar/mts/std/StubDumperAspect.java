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

import org.cougaar.core.mts.MessageAddress;

public class StubDumperAspect extends StandardAspect
{

    public StubDumperAspect() {
	super();
    }


    public Object getDelegate(Object delegate, Class type) 
    {
	if (type == NameSupport.class) {
	    return new NameSupportDelegate((NameSupport) delegate);
	} else {
	    return null;
	}
    }




    public class NameSupportDelegate extends NameSupportDelegateImplBase {
	
	public NameSupportDelegate (NameSupport nameSupport) {
	    super(nameSupport);
	}
	

	public void registerAgentInNameServer(Object proxy, 
					      MessageAddress address, 
					      String transportType)
	{
	    super.registerAgentInNameServer(proxy, address, transportType);

	    debugService.info("\nRegistering " + address +
				     " for "+ transportType +
				     " = [" +proxy+ "]");
	}

	public Object lookupAddressInNameServer(MessageAddress address, 
						String transportType)
	{
	    Object result = super.lookupAddressInNameServer(address,
							    transportType);
	    debugService.info("\nLookup " + address +
				     " for "+ transportType +
				     " = [" +result+ "]");
	    return result;
	}


    }




}



    
