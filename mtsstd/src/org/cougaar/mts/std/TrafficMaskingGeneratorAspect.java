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
  implements TrafficMaskingGeneratorService, StateObject {

  private MaskingQueueDelegate maskingQDelegate;
  public MessageTransportRegistry registry;
  private int requestRate = 0;
  private int replyRate = 333;
  private int replySize = 0;
  boolean timerOn = false;
  private NodeKeeperTimerTask nodeKeeper;
  private MessageAddress myAddress;
  private ReplyTimerTask replyTimerTask;
  private HashMap nodeTimerMap = new HashMap(6);
  private ArrayList statsList = new ArrayList();
  private Timer myTimer = new Timer(true);

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
    if (nodeTimerMap.get(node) != null) {
      cancelTimerTask(node);
    }
    // start up a request masking timer for this node
    MaskingTimerTask mtt = new MaskingTimerTask(node, avgSize, avgPeriod);
    myTimer.scheduleAtFixedRate(mtt, avgPeriod, avgPeriod);
    // register with timer
    nodeTimerMap.put(node, mtt);
    
    // if this is the first timer task we need to turn on the reply 
    // timer task.
    if (replyTimerTask == null) {
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
    // find the reply timer task and start a new one with
    // the new thinktime - note you could miss a reply 
    // but that's probably ok for now
    if (replyTimerTask != null) {
      replyTimerTask.cancel();
    }
    replyRate = thinkTime;
    startReplyTimerTask();
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
    TimerTask theTimerTask = (TimerTask) nodeTimerMap.get(node);
    theTimerTask.cancel();
    nodeTimerMap.remove(node);
  }

  private void startReplyTimerTask() {
    // delay start by replyRate
    replyTimerTask = new ReplyTimerTask();
    myTimer.scheduleAtFixedRate(replyTimerTask, replyRate, replyRate);
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
    AutoMaskingTimerTask amtt = new AutoMaskingTimerTask();
    myTimer.scheduleAtFixedRate(amtt, requestRate, requestRate);

    //start up the reply timer
    startReplyTimerTask();
  }

  //
  // begin inner classes
  //

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
      // If this is a fake message type - wrap it in an envelope
      if (msg instanceof FakeRequestMessage || msg instanceof FakeReplyMessage) {
        MessageAddress dest = msg.getTarget();
        msg = new MaskingMessageEnvelope(msg, dest);
      }
      if (Debug.debug(TRAFFIC_MASKING_GENERATOR))
        System.out.println("\n %%%%% MaskingQueue sending message: "+msg);
      queue.sendMessage(msg);
    }
  }  // end of MaskingQueueDelegate inner class
      

  // Delgate on Deliverer (sees incoming messages)
  public class MaskingDelivererDelegate extends MessageDelivererDelegateImplBase {
    private Random random = new Random();
    
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
            // create random contents to reply with if the size wasn't set
            int contentsSize;
            if (replySize < 1) {
              int randomReplySize = random.nextInt(18000);
              // if its less than 800 do a little hacking 
              //to make sure its not too small.
              if (randomReplySize < 800) {
                randomReplySize = randomReplySize + 800;
              }
              contentsSize = randomReplySize;
            } else {
              contentsSize = replySize;
            }
            byte[] replyContents = new byte[contentsSize];
            random.nextBytes(replyContents);
            FakeReplyMessage reply = new FakeReplyMessage(request.getTarget(),
                                                          request.getOriginator(),
                                                          replyContents);
            replyTimerTask.addMessage(reply);
            if (Debug.debug(TRAFFIC_MASKING_GENERATOR)) {
              System.out.println("\n$$$ Masking Deliverer got Fake Request: "+
                                 request+ " size: "+request.getContents().length+
                                 "\n Queueing Fake Reply: "+reply +
                                 " size: "+replyContents.length);
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
  protected class MaskingTimerTask extends TimerTask {
    private int requestSize;
    private MessageAddress destination;
    private TrafficMaskingStatistics mystats;
    private Random random = new Random();

    public MaskingTimerTask(MessageAddress node, int size, int period) {
      super();
      destination = node;
      requestSize = size;
      mystats = new TrafficMaskingStatistics(node, period, size);
      statsList.add(mystats);
    }

    public void run() {
      byte[] contents = new byte[requestSize];
      random.nextBytes(contents);
      myAddress = registry.getLocalAddress();
      FakeRequestMessage request = 
        new FakeRequestMessage(myAddress, destination, contents);
      maskingQDelegate.sendMessage(request);
      if (Debug.debug(TRAFFIC_MASKING_GENERATOR)) {
        System.out.println("\n$$$ MaskingTimer about to send FakeRequest"+
                           "from: "+myAddress+" to: "+destination+
                           " size of byte array: "+contents.length);
      }
      // add to stats
      mystats.incrementCount();
      mystats.incrementTotalBytes(contents.length);
    }
  }

  // this is the fake 'think' time for reply tasks
  // used in both auto and service directed runs
  public class ReplyTimerTask extends TimerTask {
    private ArrayList replyQueue = new ArrayList();

    public void run() {
      if (!replyQueue.isEmpty()) {
        synchronized(replyQueue) {
          Message replymsg = (Message) replyQueue.get(0);
          // put message on real MTS SendQueue
          maskingQDelegate.sendMessage(replymsg);
          if (Debug.debug(TRAFFIC_MASKING_GENERATOR)) {
            System.out.println("\n $$$ Masking: ReplyTimer sending reply: " +
                               replymsg + " size: "+ 
                               ((FakeReplyMessage)replymsg).getContents().length);
          }
          replyQueue.remove(0);
        }
      }
    }

    public void addMessage(Message msg) {
      synchronized(replyQueue) {
        replyQueue.add(msg);
      }
    }
  }
      

  // creates fake-requests on a timer - requests are a random size
  // used for auto masking setup up by system properties
  public class AutoMaskingTimerTask extends TimerTask {
    private Random contentsGenerator = new Random();
  
    public void run() {
      MessageAddress fakedest = nodeKeeper.getRandomNodeAddress();
      // make sure we have other nodes to send to
      if (fakedest != null) {
        //bound by 18K right now
        int randomSize = contentsGenerator.nextInt(18000);
        // if its less than 800 do a little hacking to make 
        //sure its not too small.
        if (randomSize < 800) {
          randomSize = randomSize + 800;
        }
        byte[] contents = new byte[randomSize];
        contentsGenerator.nextBytes(contents);
        FakeRequestMessage request = 
          new FakeRequestMessage(myAddress, fakedest, contents);
        maskingQDelegate.sendMessage(request);
        if (Debug.debug(TRAFFIC_MASKING_GENERATOR)) {        
          System.out.println("\n&&& AutoMasking About to send "+
                             "FakeRequest from: "+myAddress+
                             " to: "+fakedest+" size of byte array: "+
                             contents.length);
        }
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



    
