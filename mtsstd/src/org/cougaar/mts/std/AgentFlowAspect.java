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

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.node.NodeControlService;

public class AgentFlowAspect 
  extends StandardAspect
    implements TrafficMatrixStatisticsService, 
	       ServiceProvider,
	       AttributeConstants
{
  
    private TrafficMatrix trafficMatrix;
  
    public AgentFlowAspect() {
	trafficMatrix = new TrafficMatrix();
    }
  
    public void load() {
	super.load();

	ServiceBroker sb = getServiceBroker();

	NodeControlService ncs = (NodeControlService)
	    sb.getService(this, NodeControlService.class, null);

	if (ncs != null) {
	    ServiceBroker rootsb = ncs.getRootServiceBroker();
	    rootsb.releaseService(this, NodeControlService.class, ncs);
	    // We provide TrafficMatrixStatisticsService
	    rootsb.addService(TrafficMatrixStatisticsService.class, this);

	} else {
	    throw new RuntimeException("AgentFlowAspect can only be used in NodeAgents");
	}

    }
  
  
    // ensure there's a TrafficRecord for that map entry
    private TrafficMatrix.TrafficRecord ensureTrafficRecord(MessageAddress src, 
							    MessageAddress dst) 
    {
	TrafficMatrix.TrafficRecord record = null;
	synchronized(trafficMatrix) {
	    record = trafficMatrix.getOrMakeRecord(src, dst);
	}
	return record;
    }
  
    // TrafficMatricStatisticsService Interface
    public TrafficMatrix snapshotMatrix() {
	//FIXME need to copy matrix here!!!
	return new TrafficMatrix(trafficMatrix);
    }
  


    // ServiceProvider Interface
    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	if (serviceClass == TrafficMatrixStatisticsService.class) {
	    return this;
	} else {
	    return null;
	}
    }

    public void releaseService(ServiceBroker sb, 
			       Object requestor, 
			       Class serviceClass, 
			       Object service)
    {
    }


  
    // Helper methods
    boolean delivered(MessageAttributes attributes) {
	return 
	    attributes != null &
	    attributes.getAttribute(DELIVERY_ATTRIBUTE).equals(DELIVERY_STATUS_DELIVERED);
    }

    void countMessages(AttributedMessage message, MessageAttributes meta) {
	if (delivered(meta)) {
	    int msgBytes=0;
	    Object attr= message.getAttribute(MESSAGE_BYTES_ATTRIBUTE);
	    if (attr!=null && (attr instanceof Number) )
		msgBytes=((Number) attr).intValue();
      
	    TrafficMatrix.TrafficRecord theRecord = 
		ensureTrafficRecord(message.getOriginator(), 
				    message.getTarget());
	    synchronized (theRecord) {
		theRecord.msgCount++;
		theRecord.byteCount+=msgBytes;
	    }
	}
    }


    // 
    // Aspect Code to implement TrafficRecord Collection
  
    public Object getDelegate(Object object, Class type) {
	if (type == DestinationLink.class) {
	    return new AgentFlowDestinationLink((DestinationLink) object);
// 	} else 	if (type == MessageDeliverer.class) {
// 	    return new MessageDelivererDelegate((MessageDeliverer) object);
	} else {
	    return null;
	}
    }
  
  
    public class AgentFlowDestinationLink 
	extends DestinationLinkDelegateImplBase
    {
    
	public AgentFlowDestinationLink(DestinationLink link)
	{
	    super(link);
	}
    
    
	public MessageAttributes forwardMessage(AttributedMessage message) 
	    throws UnregisteredNameException, 
		   NameLookupException, 
		   CommFailureException,
		   MisdeliveredMessageException
	{ 
	    // Attempt to Deliver message
	    MessageAttributes meta = super.forwardMessage(message);
	    countMessages(message, meta);
	    return meta;
	}
    }
  
    public class  MessageDelivererDelegate 
	extends MessageDelivererDelegateImplBase 
    {
    
	MessageDelivererDelegate(MessageDeliverer delegatee) {
	    super(delegatee);
	}
    
	public MessageAttributes deliverMessage(AttributedMessage message,
						MessageAddress dest)
	    throws MisdeliveredMessageException
	{  
	    MessageAttributes meta = super.deliverMessage(message, dest);
	    // No counting on the messages in b
	    //countMessages(message, meta);
	    return meta;
	}
    
    }
  
  
    
}
