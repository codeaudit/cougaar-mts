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

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

import java.util.HashMap;
import java.util.Iterator;

import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;


public final class StepperAspect 
    extends StandardAspect
{

    private StepFrame frame;
    private HashMap controllers;
    private ThreadService threadService;
    private StepService service;


    private ThreadService threadService() {
	if (threadService != null) return threadService;
	ServiceBroker sb = getServiceBroker();
	threadService = 
	    (ThreadService) sb.getService(this, ThreadService.class, null);
	return threadService;
    }

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
		frame = new StepFrame(getRegistry().getIdentifier());
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


    public void load() {
	super.load();
	service = new ServiceImpl();
	ServiceBroker sb = getServiceBroker();
	sb.addService(StepService.class, new StepServiceProvider());
    }

    private synchronized void frameClosing() {
	service.stepAll();
	frame.dispose();
	frame = null;
    }

    private synchronized void addController(StepController controller,
					    MessageAddress address) 
    {
	controllers.put(address, controller);
    }

    


    private class ServiceImpl implements StepService {
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

    private class StepServiceProvider implements ServiceProvider {
	public Object getService(ServiceBroker sb,
				 Object client,
				 Class serviceClass)
	{
	    if (serviceClass == StepService.class) {
		return service;
	    } else {
		return null;
	    }
	}

	public void releaseService(ServiceBroker sb,
				   Object client,
				   Class serviceClass,
				   Object service)
	{
	}

    }



    private  class StepFrame extends JFrame 
	implements ScrollPaneConstants 
    {
	private JComponent contents, controllers, scroller;

	private StepFrame(String id) 
	{
	    super("Outgoing messages from " +id);

	    JPanel buttons = new JPanel();
	    buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));

	    JButton pauseAll = new JButton("Pause All");
	    pauseAll.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			service.pauseAll();
		    }});
	    buttons.add(pauseAll);

	    JButton resumeAll = new JButton("Resume All");
	    resumeAll.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			service.resumeAll();
		    }});
	    buttons.add(resumeAll);

	    JButton stepAll = new JButton("Step All");
	    stepAll.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			service.stepAll();
		    }});
	    buttons.add(stepAll);


  	    contents = new JPanel();
  	    contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));

	    Container cp = getContentPane();
	    cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
	    cp.add(buttons);
	    cp.add(contents);


	    addWindowListener(new WindowAdapter() {
		    public void windowClosing(WindowEvent e) {
			frameClosing();
		    }
		});


	    setSize(350, 480);
	    setLocation(300, 300);
	}

	private void addWidget(final StepController component,
			       final MessageAddress address) 
	{
	    SwingUtilities.invokeLater (new Runnable() {
		    public void run() {
			addController(component, address);
			addControllerWidget(component);
		    }
		});
	}

	private void addControllerWidget(StepController component) {
	    if (controllers != null) {
		controllers =
		    new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				   controllers,
				   component);
		contents.remove(scroller);
	    } else {
		controllers = component;
	    }
	    scroller = new JScrollPane(controllers,
				       VERTICAL_SCROLLBAR_AS_NEEDED,
				       HORIZONTAL_SCROLLBAR_NEVER);
	    contents.add(scroller);
	    contents.revalidate();
	}

    }



    private static class StepController extends JPanel 
	implements ScrollPaneConstants
    {
	private DestinationQueueDelegate delegate;
	private JButton send;
	private JCheckBox pause;
	private JTextArea messageWindow;
	private MessageAddress destination;

	private StepController(DestinationQueueDelegate delegate,
			       MessageAddress destination) 
	{
	    this.delegate = delegate;
	    this.destination = destination;

	    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

	    JPanel buttons = new JPanel();
	    buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));

	    pause = new JCheckBox("Pause");
	    pause.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			boolean mode = pause.isSelected();
			StepController.this.delegate.setStepping(mode);
		    }
		});
	    pause.setSelected(delegate.isStepping());


	    send = new JButton("Send");
	    send.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			StepController.this.delegate.step();
		    }
		});
	    send.setEnabled(false);

	    buttons.add(pause);
	    buttons.add(send);


	    messageWindow = new JTextArea();
	    messageWindow.setEditable(false);
	    messageWindow.setLineWrap(true);
	    
	    this.add(buttons);
	    this.add(new JScrollPane(messageWindow,
				     VERTICAL_SCROLLBAR_ALWAYS,
				     HORIZONTAL_SCROLLBAR_NEVER));

	    setBorder(new TitledBorder("Messages to " +destination));
	    Dimension size = new Dimension(300, 100);
	    // setMaximumSize(size);
//  	    setMinimumSize(size);
	    setPreferredSize(size);

	}

	private void pause() {
	    if (!pause.isSelected()) {
		pause.setSelected(true);
		delegate.setStepping(true);
	    }
	}

	private void resume() {
	    if (pause.isSelected()) {
		pause.setSelected(false);
		delegate.setStepping(false);
	    }
	}

	private void step() {
	    delegate.step();
	}

	// Should these use SwingUtilities,invokeLater?
	private void messageWait(final Message msg) {
	    send.setEnabled(true);
	    messageWindow.setText(msg.toString());
	}

	private void clearMessage() {
	    send.setEnabled(false);
	    messageWindow.setText("");
	}

    }


    private class DestinationQueueDelegate
	extends DestinationQueueDelegateImplBase 
    {
	
	private StepController widget;
	private boolean stepping;
	private Object lock = new Object();
	private Runnable oneStep;

	private DestinationQueueDelegate (DestinationQueue delegatee) {
	    super(delegatee);
	    oneStep = new OneStep();
	}
	

	private boolean isStepping() {
	    return stepping;
	}

	private void setStepping(boolean mode) {
	    stepping = mode;
	    if (!stepping) step();
	}


	private class OneStep implements Runnable {
	    public void run() {
		lockStep();
	    }
	}

	// Should probably happen in its own Thread so it doesn't
	// block Swing.
	private void step() {
	    // lockStep();
	    threadService().getThread(this, oneStep).start();
	}

	private void lockStep() {
	    synchronized (lock) { lock.notify();  }
	}

	private void ensureWidget(MessageAddress address) {
	    StepFrame frame = ensureFrame();
	    if (widget == null) {
		widget = new StepController(this, address);
		frame.addWidget(widget, address);
	    }
	}

	public void dispatchNextMessage(Message msg) {
	    ensureWidget(msg.getTarget());
	    if (!stepping) {
		super.dispatchNextMessage(msg);
	    } else {
		synchronized (lock) {
		    widget.messageWait(msg);
		    getThreadService().blockCurrentThread(lock); 
		}
		widget.clearMessage();
		super.dispatchNextMessage(msg);
	    }
	}



    }


}
