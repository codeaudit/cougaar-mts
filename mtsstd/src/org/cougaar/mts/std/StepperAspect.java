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

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;

import java.util.HashMap;
import java.util.Iterator;

/**
 * A StepManager implemented as an Aspect which attaches delegates to
 * the DestinationQueues.  Each delegate is a StepMode that holds an
 * instance of a StepController.  The Aspect itself (one per Node)
 * holds the Swing frame in which the controllers will be displayed
 * (the view).  */
public class StepperAspect
    extends StandardAspect
    implements StepManager
{

    private StepFrame frame;
    private HashMap controllers;
    private ThreadService threadService;
    private StepService service;


    private class StepServiceProvider implements ServiceProvider {
	public Object getService(ServiceBroker sb, 
				 Object requestor, 
				 Class serviceClass) 
	{
	    //  Restrict access?
	    if (serviceClass == StepService.class) {
		return service;
	    }
	    return null;
	}

	public void releaseService(ServiceBroker sb, 
				   Object requestor, 
				   Class serviceClass, 
				   Object service)
	{
	}
    }


    private class StepServiceImpl implements StepService {
	public void pause(MessageAddress destination) {
	    StepController controller =  
		(StepController) controllers.get(destination);
	    if (controller != null) controller.pause();
	}

	public void resume(MessageAddress destination) {
	    StepController controller =  
		(StepController) controllers.get(destination);
	    if (controller != null) controller.resume();
	}

	public void step(MessageAddress destination) {
	    StepController controller =  
		(StepController) controllers.get(destination);
	    if (controller != null) controller.step();
	}


	public synchronized void pauseAll() {
	    Iterator i = controllers.values().iterator();
	    while(i.hasNext()) {
		StepController controller = (StepController) i.next();
		controller.pause();
	    }
	}


	public synchronized void resumeAll() {
	    Iterator i = controllers.values().iterator();
	    while(i.hasNext()) {
		StepController controller = (StepController) i.next();
		controller.resume();
	    }
	}


	public synchronized void stepAll() {
	    Iterator i = controllers.values().iterator();
	    while(i.hasNext()) {
		StepController controller = (StepController) i.next();
		controller.step();
	    }
	}

    }


    private ThreadService threadService() {
	if (threadService != null) return threadService;
	ServiceBroker sb = getServiceBroker();
	threadService = 
	    (ThreadService) sb.getService(this, ThreadService.class, null);
	return threadService;
    }


    private StepFrame ensureFrame() {
	synchronized (this) {
	    if (frame == null) {
		frame = new StepFrame(this, getRegistry().getIdentifier());
		if (controllers == null) {
		    controllers = new HashMap();
		} else {
		    Iterator i = controllers.values().iterator();
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





    // Component
    public void load() {
	super.load();
	service = new StepServiceImpl();
	ServiceProvider provider = new StepServiceProvider();
	getServiceBroker().addService(StepService.class, provider);
    }


    // Aspect
    public Object getDelegate(Object delegatee, Class type) 
    {
	if (type == DestinationQueue.class) {
	    return new DestinationQueueDelegate((DestinationQueue) delegatee);
	} else {
	    return null;
	}
    }


    // StepManager
    public StepService getService() {
	return service;
    }

    public synchronized void close() {
	service.stepAll();
	frame.dispose();
	frame = null;
    }

    public synchronized void addController(StepController controller) {
	controllers.put(controller.getModel().getDestination(), controller);
    }







    private class DestinationQueueDelegate
	extends DestinationQueueDelegateImplBase
	implements StepModel
    {
	
	private StepController widget;
	private boolean stepping;
	private Object lock = new Object();
	private Runnable oneStep;

	public DestinationQueueDelegate (DestinationQueue delegatee) {
	    super(delegatee);
	    oneStep = new OneStep();
	}
	


	// StepModel

	public boolean isStepping() {
	    return stepping;
	}

	public void setStepping(boolean mode) {
	    stepping = mode;
	    if (!stepping) step();
	}

	// Should probably happen in its own Thread so it doesn't
	// block Swing.
	private void lockStep() {
	    synchronized (lock) { lock.notify();  }
	}


	private class OneStep implements Runnable {
	    public void run() {
		lockStep();
	    }
	}


	public void step() {
	    // lockStep();
	    threadService().getThread(oneStep).start();
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
