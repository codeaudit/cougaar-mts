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
import java.awt.event.*;
import java.awt.Container;

/**
 * A Swing frame that displays a set of StepControllers.
 */
public class StepFrame
    extends JFrame 
    implements ScrollPaneConstants 
{
    private JComponent contents;
    StepManager manager;

    public StepFrame(StepManager manager, String id)  {
	super("Outgoing messages from " +id);
	this.manager = manager;

	makeComponents();

	addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    StepFrame.this.manager.close();
		}
	    });


	setSize(350, 480);
	setLocation(300, 300);

    }

    private void makeComponents() {
	JPanel buttons = new JPanel();
	buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));

	JButton pauseAll = new JButton("Pause All");
	pauseAll.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    manager.getService().pauseAll();
		}});
	buttons.add(pauseAll);

	JButton resumeAll = new JButton("Resume All");
	resumeAll.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    manager.getService().resumeAll();
		}});
	buttons.add(resumeAll);

	JButton stepAll = new JButton("Step All");
	stepAll.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    manager.getService().stepAll();
		}});
	buttons.add(stepAll);


	contents = new JPanel();
	contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));

	Container cp = getContentPane();
	cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
	cp.add(buttons);
	cp.add(new JScrollPane(contents,
			       VERTICAL_SCROLLBAR_AS_NEEDED,
			       HORIZONTAL_SCROLLBAR_NEVER));


    }

    public void addWidget(final StepController component) {
	SwingUtilities.invokeLater (new Runnable() {
		public void run() {
		    manager.addController(component);
		    addControllerWidget(component);
		    // force a redisplay
		    contents.revalidate();
		}
	    });
    }

    public void addControllerWidget(StepController component) {
	contents.add(Box.createVerticalStrut(10));
	contents.add(component);
    }

}
