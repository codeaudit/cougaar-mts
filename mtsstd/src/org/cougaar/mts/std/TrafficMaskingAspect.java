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

import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.society.MessageEnvelope;
import org.cougaar.core.society.MulticastMessageAddress;

import java.util.*;

public class TrafficMaskingAspect extends StandardAspect
{

  private MaskingQueueDelegate maskingQDelegate;
  public MessageTransportRegistry registry;
  private int requestRate = 333;
  boolean timerOn = false;
  private NodeKeeperTimerTask nodeKeeper;
  private MessageAddress myAddress;

  public TrafficMaskingAspect() {
    super();
    registry = MessageTransportRegistry.getRegistry();
    if (Debug.debug(TRAFFIC_MASKING))
      System.out.println("\n $$$ TrafficMaskingAspect constructed");
  }


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
  // begin inner classes
  //

  //new envelope to wrap fake messages
  private static class MaskingMessageEnvelope extends MessageEnvelope {
    private Message message;

    MaskingMessageEnvelope(Message message, MessageAddress destination) {
      super(message, message.getOriginator(), destination);
      this.message = message;
    }

    public String toString() {
      return new String("MaskingMessageEnvelope containing message: "+message.toString());
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
      //The first time we see a message, setup a the timer to send fake messages
      if (!timerOn) {
        myAddress = registry.getLocalAddress();
        setupMaskingTimer();
      }
      // If this is a fake message type - wrap it in an envelope
      if (msg instanceof FakeRequestMessage || msg instanceof FakeReplyMessage) {
        MessageAddress dest = msg.getTarget();
        msg = new MaskingMessageEnvelope(msg, dest);
      }
      if (Debug.debug(TRAFFIC_MASKING))
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
            // create random contents to reply with
            int replySize = random.nextInt(18000);
            // if its less than 800 do a little hacking to make sure its not too small.
            if (replySize < 800) {
              replySize = replySize + 800;
            }
            byte[] replyContents = new byte[replySize];
            random.nextBytes(replyContents);
            // should be put into a think timer hold eventually
            FakeReplyMessage reply = new FakeReplyMessage(request.getTarget(),
                                                          request.getOriginator(),
                                                          replyContents);
            maskingQDelegate.sendMessage(reply);
            if (Debug.debug(TRAFFIC_MASKING)) {
              System.out.println("\n$$$ Deliverer got Fake Request: "+request+
                                 " size: "+request.getContents().length +
                                 "\n Sending Fake Reply: "+reply +
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


  // timer to create fake tasks
  private void setupMaskingTimer() {
    timerOn = true;

    // start up the node keeper
    Timer nodeKeeperTimer = new Timer(true);
    // for now only update every minute and get a new one after 2 seconds
    nodeKeeper = new NodeKeeperTimerTask();
    // Get the list in 10 seconds to allow for other nodes to startup
    // then check every 30 seconds - these probably need tweaking
    // important thing is not to wait too long to get all the initial nodes
    nodeKeeperTimer.schedule(nodeKeeper, 10000, 30000);

    // start up the masking task
    Timer maskingTimer = new Timer(true);
    // delay start by the requestRate 
    maskingTimer.scheduleAtFixedRate(new MaskingTimerTask(), requestRate, requestRate);
  }

  // creates fake-requests on a timer of random size
  public class MaskingTimerTask extends TimerTask {
    private Random contentsGenerator = new Random();
  
    public void run() {
      MessageAddress fakedest = nodeKeeper.getRandomNodeAddress();
      // make sure we have other nodes to send to
      if (fakedest != null) {
        //bound by 18K right now
        int randomSize = contentsGenerator.nextInt(18000);
        // if its less than 800 do a little hacking to make sure its not too small.
        if (randomSize < 800) {
          randomSize = randomSize + 800;
        }
        byte[] contents = new byte[randomSize];
        contentsGenerator.nextBytes(contents);
        FakeRequestMessage request = 
          new FakeRequestMessage(myAddress, fakedest, contents);
        maskingQDelegate.sendMessage(request);
        if (Debug.debug(TRAFFIC_MASKING)) {        
          System.out.println("\n&&&&&&& About to send FakeRequest from: "+myAddress+
                             " to: "+fakedest+" size of byte array: "+
                             contents.length);
        }
      }
    }
  }   // end of MaskingTimerTask inner class

  // keeps list of nodes up to date and generates random node addresses
  // from the current list
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
      Iterator nodeIt = registry.findRemoteMulticastTransports(
                                      (MulticastMessageAddress)MessageAddress.SOCIETY);
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
        if (Debug.debug(TRAFFIC_MASKING))        
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
      if (Debug.debug(TRAFFIC_MASKING))      
        System.out.println("\n$$$ Updating Node List");
      synchronized(nodelist) {
        nodelist.clear();
        nodelist.addAll(newNodes);
      }
    }

  }   // end of NodeKeeperTimerTask inner class



}



    
