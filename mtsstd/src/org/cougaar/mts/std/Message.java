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

/**
*   imports
**/
import java.io.*;
import java.util.*;

/**
 *   Class Is Basic Structure for Alp Message
 *   <p>
 *   All forms of Messages in the Alp system are derived from this base class
 *   originally Message
 **/
public abstract class Message 
  implements Serializable 
{

  private static final MessageAddress sink = new MessageAddress("Unknown");
  /** This is a Reference target Object **/
  private MessageAddress theOriginator;
  /** This is a Reference target Object **/
  private MessageAddress theTarget;
  /** This is the sequence number **/
  private int theSequenceNumber = 0;

  /**
   *   Default Constructor for factory.
   *   <p>
   *   @param aSource The creator of this message used to consruct the super class
   **/
  public Message() {
    this( sink, sink, 0 );
  }

  /**
   *   Constructor with just the addresses
   *   <p>
   *   @param aSource The creator of this message used to consruct the super class
   **/
  public Message(MessageAddress aSource, MessageAddress aTarget) {
    this(aSource, aTarget, 0);
  }

  /**
   *   Constructor with a full parameter list
   *   <p>
   *   @param source The creator of this message used to consruct the super class
   *   @param aTarget The target for this message
   *   @param aContents  The content of the message
   *	@param anId Primative int value for message id used to create message
   **/
  public Message(MessageAddress aSource, MessageAddress aTarget, int anId) {
    setOriginator(aSource);
    setTarget(aTarget);
    setContentsId(anId);
  }
    
  /**
   *   Constructor for constructing a message form another message.
   *   <p>
   *   @param aMessage The message to use as the data source for construction.
   **/
  public Message(Message aMessage) {
    this(aMessage.getOriginator(),
         aMessage.getTarget(),
	 aMessage.getContentsId());
  }

  /**
   *    Accessor Method for theContentsId Property
   *    @return int the value of the standard message with intrinsics
   **/
  public final int getContentsId() {
    return theSequenceNumber;
  }

  /**
   *   Accessor Method for theOriginator Property
   *   @return Object Returns theOriginator object
   **/
  public final MessageAddress getOriginator() { return theOriginator; }
 
  /**
   *   Accessor Method for theTarget Property
   *   @return Object Returns the target object
   **/
  public final MessageAddress getTarget() { return theTarget; }

  /**
   *   Modify Method for theContentsId Property
   *   @param aContnetsId The modifies theContentsId variable with the int primative
   **/
  public final void setContentsId(int aContentsId) {
    theSequenceNumber = aContentsId;
  }

  /**
   *   Modify Method for theOriginator Property
   *   @param aSource The modifies theOriginator variable with the Object object
   **/
  public final void setOriginator(MessageAddress aSource) { theOriginator = aSource; }

  /**
   *   Modify Method for theTarget Property
   *   @param aTarget The modifies theTarget variable with the Object object
   **/
  public final void setTarget(MessageAddress aTarget) { theTarget = aTarget; }

  /**
   *   Overide the toString implemntation for all message classes
   *   @return String Formatted string for displayying all the internal data of the message.
   **/
  public String toString()
  {
    try {
      return "The source: " + getOriginator().toString() +
        " The Target: " + getTarget().toString() +
        " The Message Id: " + getContentsId();
    } catch (NullPointerException npe) {
      String output = "a Malformed Message: ";
      if ( getOriginator() != null )
        output += " The source: " + getOriginator().toString();
      else
        output += " The source: NULL";
      if ( getTarget() != null )
        output += "The Target: " + getTarget().toString();
      else  
        output += " The Target: NULL";

      return output;
    }
  }

  // externalizable support
  // we don't actually implement the Externalizable interface, so it is
  // up to subclasses to call these methods.
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(theOriginator);
    out.writeObject(theTarget);
    out.writeInt(theSequenceNumber);
  }

  public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException {
    theOriginator=(MessageAddress)in.readObject();
    theTarget=(MessageAddress)in.readObject();
    theSequenceNumber = in.readInt();
  }
}

