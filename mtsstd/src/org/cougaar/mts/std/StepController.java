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
import javax.swing.border.TitledBorder;
import java.awt.Dimension;

public class StepController 
    extends JPanel 
    implements ScrollPaneConstants
{
    private StepModel model;
    private JButton send;
    private JCheckBox pause;
    private JTextArea messageWindow;

    public StepController(StepModel model,  MessageAddress destination)  {
	this.model = model;
	setBorder(new TitledBorder("Messages to " +destination));
	makeComponents();

	Dimension size = new Dimension(300, 100);
	setMaximumSize(size);
	setMinimumSize(size);
	setPreferredSize(size);

    }

    private void makeComponents() {
	setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

	JPanel buttons = new JPanel();
	buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));

	pause = new JCheckBox("Pause");
	pause.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    boolean mode = pause.isSelected();
		    model.setStepping(mode);
		}
	    });
	pause.setSelected(model.isStepping());


	send = new JButton("Send");
	send.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    model.step();
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

    }

    public void pause() {
	if (!pause.isSelected()) {
	    pause.setSelected(true);
	    model.setStepping(true);
	}
    }

    public void resume() {
	if (pause.isSelected()) {
	    pause.setSelected(false);
	    model.setStepping(false);
	}
    }

    public void step() {
	model.step();
    }

    // Should these use SwingUtilities,invokeLater?
    public void messageWait(Message msg) {
	send.setEnabled(true);
	messageWindow.setText(msg.toString());
    }

    public void clearMessage() {
	send.setEnabled(false);
	messageWindow.setText("");
    }

}
