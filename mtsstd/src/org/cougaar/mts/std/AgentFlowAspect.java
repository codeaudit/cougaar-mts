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
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.TrafficMatrix;
import org.cougaar.core.mts.TrafficMatrixStatisticsService;
import org.cougaar.core.node.NodeControlService;

import org.cougaar.mts.base.MessageDeliverer;
import org.cougaar.mts.base.MessageDelivererDelegateImplBase;
import org.cougaar.mts.base.MisdeliveredMessageException;
import org.cougaar.mts.base.CommFailureException;
import org.cougaar.mts.base.UnregisteredNameException;
import org.cougaar.mts.base.NameLookupException;
import org.cougaar.mts.base.SendLink;
import org.cougaar.mts.base.DestinationLink;
import org.cougaar.mts.base.DestinationLinkDelegateImplBase;
import org.cougaar.mts.base.SendLinkDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;

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
