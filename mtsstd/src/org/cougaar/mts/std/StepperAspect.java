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

import java.util.ArrayList;
import java.util.Iterator;

public class StepperAspect
    extends StandardAspect
    implements StepManager
{

    private StepFrame frame;
    private ArrayList controllers;

    public Object getDelegate(Object delegatee, Class type) 
    {
	if (type == DestinationQueue.class) {
	    return new DestinationQueueDelegate((DestinationQueue) delegatee);
	} else {
	    return null;
	}
    }

    private StepFrame ensureFrame() {
	synchronized (this) {
	    if (frame == null) {
		frame = new StepFrame(this, getRegistry().getIdentifier());
		if (controllers == null) {
		    controllers = new ArrayList();
		} else {
		    Iterator i = controllers.iterator();
		    while(i.hasNext()) {
			StepController controller = (StepController) i.next();
			frame.addControllerWidget(controller);
		    }
		}
	    }
	}
	frame.setVisible(true);
	return frame;
    }


    // StepManager

    public synchronized void frameClosing() {
	stepAll();
	frame.dispose();
	frame = null;
    }

    public synchronized void addController(StepController controller) {
	controllers.add(controller);
    }


    public synchronized void pauseAll() {
	Iterator i = controllers.iterator();
	while(i.hasNext()) {
	    StepController controller = (StepController) i.next();
	    controller.pause();
	}
    }


    public synchronized void resumeAll() {
	Iterator i = controllers.iterator();
	while(i.hasNext()) {
	    StepController controller = (StepController) i.next();
	    controller.resume();
	}
    }


    public synchronized void stepAll() {
	Iterator i = controllers.iterator();
	while(i.hasNext()) {
	    StepController controller = (StepController) i.next();
	    controller.step();
	}
    }






    private class DestinationQueueDelegate
	extends DestinationQueueDelegateImplBase
	implements StepModel
    {
	
	private StepController widget;
	private boolean stepping;
	private Object lock = new Object();

	public DestinationQueueDelegate (DestinationQueue delegatee) {
	    super(delegatee);
	}
	

	// StepModel
	public boolean isStepping() {
	    return stepping;
	}

	public void setStepping(boolean mode) {
	    stepping = mode;
	    if (!stepping) step();
	}

	public void step() {
	    synchronized (lock) {
		lock.notify();
	    }
	}



	private void ensureWidget(MessageAddress address) {
	    StepFrame frame = ensureFrame();
	    if (widget == null) {
		widget = new StepController(this, address);
		frame.addWidget(widget);
	    }
	}

	public void dispatchNextMessage(Message msg) {
	    ensureWidget(msg.getTarget());
	    if (!stepping) {
		super.dispatchNextMessage(msg);
	    } else {
		synchronized (lock) {
		    widget.messageWait(msg);
		    while (true) {
			try { 
			    lock.wait(); 
			    widget.clearMessage();
			    break;
			}
			catch (InterruptedException ex) {}
		    }
		}
		super.dispatchNextMessage(msg);
	    }
	}



    }


}
