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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;
import java.util.Map;

import org.cougaar.core.qos.metrics.DecayingHistory;

/*
 * Traffic Matrix of AgentFlowRecords for publishing to the metrics service. 
 */
public class TrafficMatrix
{
  
  private HashMap outermatrix; 
  
  public class TrafficRecord extends DecayingHistory.SnapShot 
  {
    public long msgCount;
    public long byteCount;
    
    private TrafficRecord() {
  }

      // deep copy
      private TrafficRecord(TrafficRecord record) {
	  this.msgCount = record.msgCount;
	  this.byteCount = record.byteCount;
      }
    
    // pass through methods
    public long getMsgCount() {
      return msgCount;
    }
    
    public long getByteCount() {
      return byteCount;
    }
    
      public String toString() {
	  return "Traffic Record: msg="+msgCount+" bytes="+byteCount;
      }

  } //TrafficRecord
  
  
  /* construct new traffic matrix -  
   * outer = agent to agent
   * inner = Agent1 to Record1, Record2, Record3,...etc, Agent2 to Record2,...etc.
   *
   A1  A2  A3
   
   A1  R   R   R
   
   A2  R   R   R 
   
   A3  R   R   Rd
   
   */
  
  public TrafficMatrix() {
    // something extraordinarily large - don't know how many agents we're mapping to
    this.outermatrix = new HashMap(89);  
  }    
  

    // Deep copy
    public TrafficMatrix(TrafficMatrix matrix) {
	this.outermatrix = new HashMap(89);  
	synchronized (matrix) {
	    Iterator outer = matrix.outermatrix.entrySet().iterator();
	    while (outer.hasNext()) {
		Map.Entry entry = (Map.Entry) outer.next();
		MessageAddress orig = (MessageAddress) entry.getKey();
		HashMap row = (HashMap) entry.getValue();
		HashMap new_row = new HashMap();
		outermatrix.put(orig, new_row);
		Iterator inner = row.entrySet().iterator();
		while (inner.hasNext()) {
		    Map.Entry sub_entry = (Map.Entry) inner.next();
		    MessageAddress target = (MessageAddress) sub_entry.getKey();
		    TrafficRecord data = (TrafficRecord) sub_entry.getValue();
		    new_row.put(target, new TrafficRecord(data));
		}
	    }
	}
    }

  public void addMsgCount(MessageAddress orig, MessageAddress target, int msgCount) {
    TrafficRecord record = getOrMakeRecord(orig, target);
    record.msgCount+=msgCount;
  }
  
  public void addByteCount(MessageAddress orig, MessageAddress target, int byteCount) {
    TrafficRecord record = getOrMakeRecord(orig, target);
    record.byteCount+=byteCount;
  }
  
  public TrafficRecord getOrMakeRecord(MessageAddress orig_param,
				       MessageAddress target_param) 
  {
    MessageAddress orig = orig_param.getPrimary();
    MessageAddress target = target_param.getPrimary();
    TrafficRecord record = null;
    HashMap inner = null;

    // hash to record
    if(outermatrix.containsKey(orig)) { // has orig
      inner = (HashMap) outermatrix.get(orig); // get the inner
    } else {
      inner = new HashMap();
      outermatrix.put(orig, inner);
    }

    if(inner.containsKey(target)) { // has target
	record = (TrafficRecord) inner.get(target);
    } else { // no inner or record
	record = new TrafficRecord();
	inner.put(target, record);
    }
    
    return record;
  }
  
  public void putRecord(MessageAddress orig_param, 
			MessageAddress target_param, 
			TrafficRecord record)
  {
    
      if( orig_param != null && target_param != null)
	{
	  MessageAddress orig = orig_param.getPrimary();
	  MessageAddress target = target_param.getPrimary();
	  // should check for existing key,  later
	  HashMap new_inner = new HashMap(1);
	  new_inner.put(target, record);
	  outermatrix.put(orig, new_inner);
	} else {
	  throw new NullPointerException("Cannot put in TrafficRecord, either Originator or Target is null");
	}
  }
  
  public TrafficIterator getIterator() {
    return new TrafficIterator();
  }
  




    public class TrafficIterator implements Iterator 
    {
	
	private Iterator inner_i, outer_i;
	private Map.Entry next_inner_entry, next_outer_entry; // look-ahead
	private MessageAddress inner_key, outer_key;
	

	TrafficIterator()
	{
	    outer_i = outermatrix.entrySet().iterator();
	    getNextInnerIterator();
	    getNext();
	}

	private void getNextInnerIterator() 
	{
	    if (outer_i.hasNext()) {
		next_outer_entry = (Map.Entry) outer_i.next();
		HashMap inner = (HashMap) next_outer_entry.getValue();
		inner_i = inner.entrySet().iterator();
	    } else {
		inner_i = null;
	    }
	}

	private void getNext() 
	{
	    if (inner_i == null) {
		next_inner_entry = null;
	    }  else if (inner_i.hasNext()) {
		next_inner_entry = (Map.Entry) inner_i.next();
	    } else {
		getNextInnerIterator();
		getNext();
	    }
	}


	public void remove() 
	{
	    throw new RuntimeException("TrafficIterator does not support remove()");
	}

	public MessageAddress getOrig() {
	    return outer_key;
	}
    
	public MessageAddress getTarget() {
	    return inner_key;
	}

	public boolean hasNext() 
	{
	    return next_inner_entry != null;
	}

	public Object next() 
	{
	    Object result = null;
	    if (next_inner_entry != null) {
		outer_key = (MessageAddress) next_outer_entry.getKey();
		inner_key = (MessageAddress) next_inner_entry.getKey();
		result = next_inner_entry.getValue();
		getNext();
	    } else {
		inner_key = null;
		outer_key = null;
	    }
	    return result;
	}
    }


} // traffic matrix
