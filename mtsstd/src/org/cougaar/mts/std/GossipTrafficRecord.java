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

public class GossipTrafficRecord
{
    int requests_sent;
    int requests_rcvd;
    int values_sent;
    int values_rcvd;
    int msg_with_gossip_sent;
    int msg_with_gossip_rcvd;
    int msg_sent;
    int msg_rcvd;

    GossipTrafficRecord() {
    }

    GossipTrafficRecord(GossipTrafficRecord other) {
	this.requests_sent = other.requests_sent;
	this.requests_rcvd =  other.requests_rcvd;
	this.values_sent =  other.values_sent;
	this.values_rcvd =  other.values_rcvd;
	this.msg_with_gossip_sent = other.msg_with_gossip_sent;
	this.msg_with_gossip_rcvd = other.msg_with_gossip_rcvd;
	this.msg_sent = other.msg_sent;
	this.msg_rcvd = other.msg_rcvd;

    }
    

    public String toString() {
	StringBuffer buf = new StringBuffer();
	buf.append("<requests_sent=");
	buf.append(requests_sent);
	buf.append(" requests_rcvd=");
	buf.append(requests_rcvd);
	buf.append(" values_sent=");
	buf.append(values_sent);
	buf.append(" values_rcvd=");
	buf.append(values_rcvd);
	buf.append(" msg_with_gossip_sent=");
	buf.append(msg_with_gossip_sent);
	buf.append(" msg_with_gossip_rcvd=");
	buf.append(msg_with_gossip_rcvd);
	buf.append(" msg_with_gossip_sent=");
	buf.append(msg_sent);
	buf.append(" msg_rcvd=");
	buf.append(msg_rcvd);
	buf.append('>');
	return buf.toString();
    }

    public int getRequestsSent() {
	return requests_sent;
    }

    public int getRequestsReceived() {
	return requests_rcvd;
    }

    public int getValuesSent() {
	return values_sent;
    }

    public int getValuesReceived() {
	return values_rcvd;
    }

    public int getMessagesWithGossipSent() {
	return msg_with_gossip_sent;
    }

    public int getMessagesWithGossipReceived() {
	return msg_with_gossip_rcvd;
    }

    public int getMessagesSent() {
	return msg_sent;
    }

    public int getMessagesReceived() {
	return msg_rcvd;
    }

}
