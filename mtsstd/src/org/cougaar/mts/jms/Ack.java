package org.cougaar.mts.jms;

import java.io.Serializable;

import org.cougaar.core.mts.MessageAttributes;

public class Ack implements Serializable {
    private MessageAttributes attrs;
    private int id;
    
    public Ack() {
	
    }
    
    Ack(MessageAttributes attrs, int id) {
	this.attrs = attrs;
	this.id = id;
    }

    public MessageAttributes getAttrs() {
        return attrs;
    }

    public void setAttrs(MessageAttributes attrs) {
        this.attrs = attrs;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

}
