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
    int send_count;
    int rcv_count;

    GossipTrafficRecord() {
    }

    GossipTrafficRecord(GossipTrafficRecord other) {
	this.requests_sent = other.requests_sent;
	this.requests_rcvd =  other.requests_rcvd;
	this.values_sent =  other.values_sent;
	this.values_rcvd =  other.values_rcvd;
	this.send_count = other.send_count;
	this.rcv_count = other.rcv_count;
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
	buf.append(" send_count=");
	buf.append(send_count);
	buf.append(" rcv_count=");
	buf.append(rcv_count);
	buf.append('>');
	return buf.toString();
    }

    public int getRequestSent() {
	return requests_sent;
    }

    public int getRequestReceived() {
	return requests_rcvd;
    }

    public int getValuesSent() {
	return values_sent;
    }

    public int getValuesReceived() {
	return values_rcvd;
    }

    public int getSendCount() {
	return send_count;
    }

    public int getReceiveCount() {
	return rcv_count;
    }

}
