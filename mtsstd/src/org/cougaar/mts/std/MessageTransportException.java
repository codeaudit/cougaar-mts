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

/*
*   Imports
*/

/**
*   This Exception class contains the information for throwing an
*   exception for the unmovable messages by the transport gateway.
*   <p>
*   For the first version of this class the exceptions diffferentiated by the
*   int type passed in during construction.  In the future we will want
*   to break this class out into specifc kinds MessageTransportExceptions of exceptions
**/
public class MessageTransportException extends Exception {

  /**
   *	Constructs a MessageTransportException with a specified detail message. but an Unknown type
   *   @param aType Argument used to set the type of error condition that occurred
   *   return MessageTransportException
   **/
  public MessageTransportException( String aMsg ) {
    super (aMsg);
  }
}
