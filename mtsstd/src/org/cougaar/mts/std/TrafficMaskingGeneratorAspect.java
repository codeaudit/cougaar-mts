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

import org.cougaar.core.component.StateObject;
import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.society.MessageEnvelope;
import org.cougaar.core.society.MulticastMessageAddress;

import java.util.*;

public class TrafficMaskingGeneratorAspect extends StandardAspect 
  implements TrafficMaskingGeneratorService, StateObject 
{

    private MaskingQueueDelegate maskingQDelegate;
    public MessageTransportRegistry registry;
    private int requestRate = 0;
    private int replyRate = 333;
    private int replySize = 0;
    boolean timerOn = false;
    private NodeKeeperTimerTask nodeKeeper;
    private MessageAddress myAddress;
    private ReplyTimerTaskController replyTimerTaskController;
    private HashMap nodeTimerTaskMap = new HashMap(6);
    private ArrayList statsList = new ArrayList();
    private Timer myTimer = new Timer(true);
    private Random random = new Random();
    private ExpRandom expRandom = new ExpRandom();

    public TrafficMaskingGeneratorAspect() {
	super();
	registry = MessageTransportRegistry.getRegistry();
	//get any properties
	String requestPeriod = 
	    System.getProperty("org.cougaar.message.trafficGenerator.requestPeriod");
	if (requestPeriod != null) {
	    int newPeriod = new Integer(requestPeriod).intValue();
	    if (newPeriod > 0) {
		requestRate = newPeriod;
	    }
	}
	String thinkTime = 
	    System.getProperty("org.cougaar.message.trafficGenerator.replyThinkTime", "333");
	int replyInt = new Integer(thinkTime).intValue();
	if (replyInt > 0) {
	    replyRate = replyInt;
	}
	if (Debug.debug(TRAFFIC_MASKING_GENERATOR))
	    System.out.println("\n $$$ TrafficMaskingGeneratorAspect constructed"+
			       "... request is: "+ requestRate + 
			       " reply is: "+replyRate);
    }

    public void load() {
	super.load();
	//set up the service provider
	TrafficMaskingGeneratorServiceProvider tmgSP = 
	    new TrafficMaskingGeneratorServiceProvider(this);
	// add the service to my parent's service broker so my
	// sibling aspects can access it.
	getBindingSite().getServiceBroker().addService(TrafficMaskingGeneratorService.class, tmgSP);
    }

    // aspect implementation
    public Object getDelegate(Object delegate, Class type) {
	if (type == SendQueue.class) {
	    maskingQDelegate = new MaskingQueueDelegate((SendQueue) delegate);
	    return maskingQDelegate;
	} else if (type == MessageDeliverer.class) {
	    return new MaskingDelivererDelegate((MessageDeliverer) delegate);
	} else {
	    return null;
	}
    }


    //
    // implement TrafficMaskingGeneratorService API
    //

    /** Turn on Traffic Masking for a Node with the following params
     *  @param node  The MessageAddress of the Node to send fake messages to
     *  @param avgPeriod How often to send the fake messages in ms
     *  @param avgSize The size in bytes of the contents of the fake messages 
     **/
    public void setRequestParameters(MessageAddress node, int avgPeriod, int avgSize) {
	//check the avgPeriod
	if (avgPeriod < 1) {
	    if (avgPeriod == -1) {
		cancelTimerTask(node);
		return;
	    } else {
		throw new IllegalArgumentException("TrafficMaskingGeneratorService."+
						   "setRequestParameter() "+
						   "received an illegal avgPeriod "+
						   "argument: "+avgPeriod);
	    }
	}
	// check the avgSize
	if (avgSize < 1) {
	    throw new IllegalArgumentException("TrafficMaskingGeneratorService."+
					       "setRequestParameter() "+
					       "received and illegal avgSize "+
					       "argument: "+avgSize);
	}

	// if we already have a timer going for this node - cancel the current one
	if (nodeTimerTaskMap.get(node) != null) {
	    cancelTimerTask(node);
	}
	// start up a request masking timer for this node
	MaskingTimerTaskController mtt = 
	    new MaskingTimerTaskController(node, avgSize, avgPeriod);
	// register with timer
	nodeTimerTaskMap.put(node, mtt);
	mtt.makeNextTask();
    
	// if this is the first timer task we need to turn on the reply 
	// timer task.
	if (replyTimerTaskController == null) {
	    startReplyTimerTask();
	}
    }

    /** Set the Think Time and size parameters for Fake Replies coming 
     *  from the local Node.
     *  @param thinkTime The time in ms to wait before sending a reply
     *  @param avgSize The size in bytes of the contents of the fake reply message
     **/
    public void setReplyParameters(int thinkTime, int avgSize) {
	if (thinkTime < 1 || avgSize < 1) {
	    throw new IllegalArgumentException("TrafficMaskingGeneratorService."+
					       "setReplyParameters() received an "+
					       "a less than 1 argument");
	}
	// simply reset the size
	replySize = avgSize;
	replyRate = thinkTime;
    }

    /** Get information about the fake messages sent from this Node
     *  @return Collection Collection of TrafficMaskingStatistics objects
     *  @see org.cougaar.core.mts.TrafficMaskingStatistics
     **/
    public Collection getStatistics() {
	//need elements and iterator to be safe... Does this cover both???
	return new ArrayList(statsList);
    }

    //
    // utility methods
    //
    private void cancelTimerTask(MessageAddress node) {
	TimerTaskController controller = 
	    (TimerTaskController) nodeTimerTaskMap.get(node);
	controller.cancelLastTask();
	nodeTimerTaskMap.remove(node);
    }

    private void startReplyTimerTask() {
	replyTimerTaskController = new ReplyTimerTaskController(replyRate);

	// The first task will get made when a message is added.
	// replyTimerTaskController.makeNextTask();
    }


    // TimerTasks to create fake request and reply tasks
    private void setupTimerTasks() {
	timerOn = true;

	// start up the node keeper - this guys gets his own Timer too
	Timer nodeKeeperTimer = new Timer(true);
	// for now only update every minute and get a new one after 2 seconds
	nodeKeeper = new NodeKeeperTimerTask();
	// Get the list in 10 seconds to allow for other nodes to startup
	// then check every 30 seconds - these probably need tweaking
	// important thing is not to wait too long to get all the initial nodes
	nodeKeeperTimer.schedule(nodeKeeper, 10000, 30000);

	// start up the masking task - delay start by the requestRate 
	AutoMaskingTimerTaskController amtt = 
	    new AutoMaskingTimerTaskController(requestRate);
	amtt.makeNextTask();

	//start up the reply timer
	startReplyTimerTask();
    }

    // create random contents for message  
    private byte[] randomContents(int avgSize) {
	int contentSize;
	if (avgSize < 1) {
	    contentSize = expRandom.nextInt(9000);
	} else contentSize = expRandom.nextInt(avgSize);
	// make sure its not too small.
	if (contentSize < 400)   contentSize += 400;
	byte[] contents = new byte[contentSize];
	random.nextBytes(contents);
	return contents;
    }

	
    //
    // begin inner classes
    //

    // Random number Generators with distributions

    // Exponential Distribution 
    // Stateless generator
    private static class ExpRandom extends Random {

	ExpRandom() {
	    //super is uniform distribution
	    super();
	}
	// period is the average period, 
	// the range can go from zero to ten times the period
	public int nextInt(int period) {
	    double raw = - (period * Math.log(super.nextDouble()));
	    // clip upper tail
	    if (raw > 10 * period) {
		return 10 * period;
	    }
	    else return (int) Math.round(raw);
	}
    }

    

    // Bursty Distribution
    // simplest stateful generator
    private static class BurstyRandom extends ExpRandom {
	private int burstCount = 0;
	private int insideBurstPeriod;
	private int burstLength;

	BurstyRandom (int insideBurstPeriod, int burstLength) {
	    super();
	    this.insideBurstPeriod = insideBurstPeriod;
	    this.burstLength = burstLength;
	}

	public int nextInt (int betweenBurstPeriod) {
	    if (burstCount <= 0 ) {
		// wait for next burst
		burstCount=super.nextInt(burstLength);
		return super.nextInt(betweenBurstPeriod);
	    } else {
		// inside burst
		--burstCount;
		return super.nextInt(insideBurstPeriod);
	    }
	}
    }



    //new envelope to wrap fake messages
    public static class MaskingMessageEnvelope extends MessageEnvelope {
	private Message message;

	MaskingMessageEnvelope(Message message, MessageAddress destination) {
	    super(message, message.getOriginator(), destination);
	    this.message = message;
	}

	public String toString() {
	    return new String("MaskingMessageEnvelope containing message: "+
			      message.toString());
	}
    }

    // Delegate on SendQueue (sees outgoing messages)
    public class MaskingQueueDelegate extends SendQueueDelegateImplBase {

	public MaskingQueueDelegate(SendQueue queue) {
	    super(queue);
	}
    
	// synchronized so that this aspect's timer thread that sends fake messages
	// does not interact with a real outgoing message.
	public synchronized void sendMessage(Message msg) {
	    //The first time we see a message, setup the auto 
	    //timer to send fake messages(only if system properties were set)
	    if (requestRate > 0 && !timerOn) {
		myAddress = registry.getLocalAddress();
		setupTimerTasks();
	    }
	    queue.sendMessage(msg);
	}

	private  void sendMessageInEnvelope(Message msg) {
	    // If this is a fake message type - wrap it in an envelope
	    if (msg instanceof FakeRequestMessage || 
		msg instanceof FakeReplyMessage) 
		{
		    MessageAddress dest = msg.getTarget();
		    msg = new MaskingMessageEnvelope(msg, dest);
		}
	    if (Debug.debug(TRAFFIC_MASKING_GENERATOR))
		System.out.println("\n %%%%% MaskingQueue sending message: "+msg);
	    sendMessage(msg);
	}
    }  // end of MaskingQueueDelegate inner class
      

    // Delgate on Deliverer (sees incoming messages)
    public class MaskingDelivererDelegate extends MessageDelivererDelegateImplBase {
    
	public MaskingDelivererDelegate (MessageDeliverer deliverer) {
	    super(deliverer);
	}
    
	public void deliverMessage(Message msg, MessageAddress dest) 
	    throws MisdeliveredMessageException
	{
	    if (msg instanceof MaskingMessageEnvelope) {
		Message internalmsg = ((MaskingMessageEnvelope) msg).getContents();
		if (internalmsg instanceof FakeRequestMessage) {
		    FakeRequestMessage request = (FakeRequestMessage)internalmsg;
		    byte[] contents = randomContents(replySize);
		    FakeReplyMessage reply = 
			new FakeReplyMessage(request.getTarget(),
					     request.getOriginator(),
					     contents);
		    replyTimerTaskController.addMessage(reply);
		    if (Debug.debug(TRAFFIC_MASKING_GENERATOR)) {
			System.out.println("\n$$$ Masking Deliverer got Fake Request: "+
					   request+ " size: "+request.getContents().length+
					   "\n Queueing Fake Reply: "+reply +
					   " size: "+contents.length);
		    }
		}
		//if its a fake reply (the other kind of masking message)
		// drop it on the floor.
	    } else {
		// any other kind of message enveloper just gets passed through
		deliverer.deliverMessage(msg, dest);
	    }
	} 
    }  // end of MaskingDelivererDelegate inner class


    //
    // TimerTask inner classes
    //

    // creates fake-requests on a set timer - requests are a set size
    // used by service method setRequestParameters
    
    abstract class TimerTaskController {
	private int period;
	private TimerTask lastTask;
	private Random generator;

	TimerTaskController(Random generator,int period) {
	    this.period = period;
	    this.generator = generator;
	}

	abstract TimerTask makeTask();

	void makeNextTask() {
	    // randomize period, then run a new task once at that time
	    int delay = generator.nextInt(period); // randomize
	    TimerTask newTask = makeTask();
	    // System.err.println("### Scheduling " +newTask+ " at " +delay);
	    myTimer.schedule(newTask, delay);
	    synchronized (this) {
		lastTask = newTask;
	    }

	}

	void cancelLastTask() {
	    synchronized (this) {
		if (lastTask != null) {
		    try { lastTask.cancel(); } catch (Exception ex) {}
		}
		lastTask = null;
	    }
	}

    }


    private class MaskingTimerTaskController extends TimerTaskController {
	private int requestSize;
	private MessageAddress destination;
	private TrafficMaskingStatistics mystats;

	MaskingTimerTaskController(MessageAddress node, int size, int period) {
	    super(new BurstyRandom(period/10, 3),period);
	    destination = node;
	    requestSize = size;
	    mystats = new TrafficMaskingStatistics(node, period, size);
	    statsList.add(mystats);
	}
      

	TimerTask makeTask() {
	    return new Task();
	}

      
	private class Task extends TimerTask {
	    public void run() {
		byte[] contents = randomContents(requestSize);
		myAddress = registry.getLocalAddress();
		FakeRequestMessage request = 
		    new FakeRequestMessage(myAddress, destination, contents);
		maskingQDelegate.sendMessageInEnvelope(request);
		if (Debug.debug(TRAFFIC_MASKING_GENERATOR)) {
		    System.out.println("\n$$$ MaskingTimer about to send FakeRequest"+
				       "from: "+myAddress+" to: "+destination+
				       " size of byte array: "+contents.length);
		}
		// add to stats
		mystats.incrementCount();
		mystats.incrementTotalBytes(contents.length);

		makeNextTask();
	    }
	}
    }

    // this is the fake 'think' time for reply tasks
    // used in both auto and service directed runs
    public class ReplyTimerTaskController extends TimerTaskController {
	private ArrayList replyQueue = new ArrayList();

	ReplyTimerTaskController(int period) {
	    super(new ExpRandom(), period);
	}

	TimerTask makeTask() {
	    return new Task();
	}

	public void addMessage(Message msg) {
	    synchronized(replyQueue) {
		//only add if queue is not too big
		if (replyQueue.size() < 10) {
		    replyQueue.add(msg);
		} else  if (Debug.debug(TRAFFIC_MASKING_GENERATOR)) {
		    System.out.println("\n $$$ TrafficMaskGen: ReplyQueue overflow");

		}
		if (replyQueue.size() == 1) makeNextTask();
	    }
	}


	class Task extends TimerTask {
	    public void run() {
		if (!replyQueue.isEmpty()) {
		    synchronized(replyQueue) {
			Message replymsg = (Message) replyQueue.get(0);
			// put message on real MTS SendQueue
			maskingQDelegate.sendMessageInEnvelope(replymsg);
			if (Debug.debug(TRAFFIC_MASKING_GENERATOR)) {
			    System.out.println("\n $$$ Masking: ReplyTimer sending reply: " +
					       replymsg + " size: "+ 
					       ((FakeReplyMessage)replymsg).getContents().length);
			}
			replyQueue.remove(0);
		    }
		}
		synchronized (replyQueue) {
		    if (replyQueue.size() > 0) makeNextTask();
		}
	    }
	}

    }
      

    // creates fake-requests on a timer - requests are a random size
    // used for auto masking setup up by system properties
    public class AutoMaskingTimerTaskController extends TimerTaskController {
  
	AutoMaskingTimerTaskController(int delay) {
	    super(new BurstyRandom(delay/10,3),delay);
	}

	// find the stats object for this node destination
	private TrafficMaskingStatistics getStatsObject(MessageAddress dest) {
	    Collection allStats = getStatistics();
	    if (!allStats.isEmpty()) {
		Iterator statsIt = allStats.iterator();
		while (statsIt.hasNext()) {
		    TrafficMaskingStatistics aStat = (TrafficMaskingStatistics)statsIt.next();
		    if (aStat.getNode().equals(dest)) {
			return aStat;
		    }
		}
	    }
	    // create one for this node if we don't have one yet
	    return new TrafficMaskingStatistics(dest, requestRate, 9000);
	}

	TimerTask makeTask() {
	    return new Task();
	}
      
	class Task extends TimerTask {
	    public void run() {
		MessageAddress fakedest = nodeKeeper.getRandomNodeAddress();
		// make sure we have other nodes to send to
		if (fakedest != null) {
		    TrafficMaskingStatistics mystats = getStatsObject(fakedest);
		    byte[] contents = randomContents(-1);
		    FakeRequestMessage request = 
			new FakeRequestMessage(myAddress, fakedest, contents);
		    maskingQDelegate.sendMessageInEnvelope(request);
		    if (Debug.debug(TRAFFIC_MASKING_GENERATOR)) {        
			System.out.println("\n&&& AutoMasking About to send "+
					   "FakeRequest from: "+myAddress+
					   " to: "+fakedest+" size of byte array: "+
					   contents.length);
		    }
		    // add to stats
		    mystats.incrementCount();
		    mystats.incrementTotalBytes(contents.length);
		}

		makeNextTask();
	    }
	}


    }   // end of AutoMaskingTimerTask inner class

    // keeps list of nodes up to date and generates random node addresses
    // from the current list - only used in auto mode when
    // system properties are defined
    public class NodeKeeperTimerTask extends TimerTask {
	private Random generator = new Random();
	private ArrayList nodelist;

	//constructor
	public NodeKeeperTimerTask() {
	    super();
	    // one-time initialize node list so we don't 
	    // get a request for an address before the timer 
	    // has run the first time
	    nodelist = new ArrayList();
	    Iterator iter = registry.findRemoteMulticastTransports(
								   (MulticastMessageAddress)MessageAddress.SOCIETY);
	    while (iter.hasNext()) {
		MessageAddress anAddress = (MessageAddress) iter.next();
		if (! anAddress.equals(myAddress)) {
		    nodelist.add(anAddress);
		}
	    }
	}
          
	public void run() {
	    //update the node list
	    MulticastMessageAddress societynodes = 
		(MulticastMessageAddress)MessageAddress.SOCIETY;
	    Iterator nodeIt = registry.findRemoteMulticastTransports(societynodes);
	    ArrayList tmpnodes = new ArrayList();
	    while (nodeIt.hasNext()) {
		MessageAddress nodeAddress = (MessageAddress) nodeIt.next();
		if (! nodeAddress.equals(myAddress)) {
		    tmpnodes.add(nodeAddress);
		}
	    }
	    updateNodeList(tmpnodes);
	}

	//get random node address from collection
	public MessageAddress getRandomNodeAddress() {
	    MessageAddress winner = null;
	    synchronized(nodelist) {
		if (Debug.debug(TRAFFIC_MASKING_GENERATOR))        
		    System.out.println("\n$$$ nodelist.size is: "+nodelist.size());
		if (nodelist.size() > 0) {
		    int randomIndex = generator.nextInt(nodelist.size());
		    winner = (MessageAddress) nodelist.get(randomIndex);
		}
	    }
	    return winner;
	}

	//safely update the list while no one else is using it
	private void updateNodeList(Collection newNodes) {
	    if (Debug.debug(TRAFFIC_MASKING_GENERATOR))      
		System.out.println("\n$$$ Masking: Updating Node List");
	    synchronized(nodelist) {
		nodelist.clear();
		nodelist.addAll(newNodes);
	    }
	}

    }   // end of NodeKeeperTimerTask inner class

    //  
    // StateObject stuff
    //
    public void setState(Object loadState) {
	// nothing for now
    }
    public Object getState() {
	//nothing for now
	return null;
    }

    }



    