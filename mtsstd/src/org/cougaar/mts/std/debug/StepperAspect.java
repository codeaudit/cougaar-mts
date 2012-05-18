/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.mts.std.debug;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.ThreadService;
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.DestinationQueue;
import org.cougaar.mts.base.DestinationQueueDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;

/**
 * This Aspect includes the ServiceProvider for and implementation of the
 * {@link StepService}, as well as a use of that service in a Swing gui.
 */
public final class StepperAspect
        extends StandardAspect {

    private StepFrame frame;
    private Map<MessageAddress,StepController> controllers;
    private ThreadService threadService;
    private StepService service;

    private ThreadService threadService() {
        if (threadService != null) {
            return threadService;
        }
        ServiceBroker sb = getServiceBroker();
        threadService = sb.getService(this, ThreadService.class, null);
        return threadService;
    }

    @Override
   public Object getDelegate(Object delegatee, Class<?> type) {
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
                    controllers = new HashMap<MessageAddress,StepController>();
                } else {
                    for (StepController controller : controllers.values()) {
                        frame.addControllerWidget(controller);
                    }
                }
            }
        }
        if (!frame.isVisible()) {
            frame.setVisible(true);
        }
        return frame;
    }

    @Override
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

    private synchronized void addController(StepController controller, MessageAddress address) {
        controllers.put(address, controller);
    }

    private class ServiceImpl
            implements StepService {
        public void pause(MessageAddress destination) {
            StepController controller = controllers.get(destination);
            if (controller != null) {
                controller.pause();
            }
        }

        public void resume(MessageAddress destination) {
            StepController controller = controllers.get(destination);
            if (controller != null) {
                controller.resume();
            }
        }

        public void step(MessageAddress destination) {
            StepController controller = controllers.get(destination);
            if (controller != null) {
                controller.step();
            }
        }

        public synchronized void pauseAll() {
            for (StepController controller : controllers.values()) {
                controller.pause();
            }
        }

        public synchronized void resumeAll() {
            for (StepController controller : controllers.values()) {
                controller.resume();
            }
        }

        public synchronized void stepAll() {
            for (StepController controller : controllers.values()) {
                controller.step();
            }
        }
    }

    private class StepServiceProvider
            implements ServiceProvider {
        public Object getService(ServiceBroker sb, Object client, Class<?> serviceClass) {
            if (serviceClass == StepService.class) {
                return service;
            } else {
                return null;
            }
        }

        public void releaseService(ServiceBroker sb,
                                   Object client,
                                   Class<?> serviceClass,
                                   Object service) {
        }

    }

    private class StepFrame
            extends JFrame
            implements ScrollPaneConstants {
        /**
       * 
       */
      private static final long serialVersionUID = 1L;
      private final JComponent contents;
        private JComponent controllers, scroller;

        private StepFrame(String id) {
            super("Outgoing messages from " + id);

            JPanel buttons = new JPanel();
            buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));

            JButton pauseAll = new JButton("Pause All");
            pauseAll.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    service.pauseAll();
                }
            });
            buttons.add(pauseAll);

            JButton resumeAll = new JButton("Resume All");
            resumeAll.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    service.resumeAll();
                }
            });
            buttons.add(resumeAll);

            JButton stepAll = new JButton("Step All");
            stepAll.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    service.stepAll();
                }
            });
            buttons.add(stepAll);

            contents = new JPanel();
            contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));

            Container cp = getContentPane();
            cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
            cp.add(buttons);
            cp.add(contents);

            addWindowListener(new WindowAdapter() {
                @Override
               public void windowClosing(WindowEvent e) {
                    frameClosing();
                }
            });

            setSize(350, 480);
            setLocation(300, 300);
        }

        private void addWidget(final StepController component, final MessageAddress address) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    addController(component, address);
                    addControllerWidget(component);
                }
            });
        }

        private void addControllerWidget(StepController component) {
            if (controllers != null) {
                controllers = new JSplitPane(JSplitPane.VERTICAL_SPLIT, controllers, component);
                contents.remove(scroller);
            } else {
                controllers = component;
            }
            scroller =
                    new JScrollPane(controllers,
                                    VERTICAL_SCROLLBAR_AS_NEEDED,
                                    HORIZONTAL_SCROLLBAR_NEVER);
            contents.add(scroller);
            contents.revalidate();
        }

    }

    private static class StepController
            extends JPanel
            implements ScrollPaneConstants {
        /**
       * 
       */
      private static final long serialVersionUID = 1L;
      private final DestinationQueueDelegate delegate;
        private final JButton send;
        private final JCheckBox pause;
        private final JTextArea messageWindow;
        private final MessageAddress destination;
        private int count;

        private StepController(DestinationQueueDelegate delegate, MessageAddress destination) {
            count = 0;
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

            setBorder(new TitledBorder("Messages to " + destination));
            Dimension size = new Dimension(300, 100);
            // setMaximumSize(size);
            // setMinimumSize(size);
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
        private void messageWait(final AttributedMessage msg) {
            send.setEnabled(true);
            StringBuffer buf = new StringBuffer();
            buf.append(msg.logString());
            buf.append("\nAttributes: ");
            buf.append(msg.getAttributesAsString());
            buf.append("\nBody: ");
            buf.append(msg.getRawMessage().toString());
            messageWindow.setText(buf.toString());
        }

        private void increment() {
            setBorder(new TitledBorder(Integer.toString(++count) + " messages to " + destination));
        }

        private void clearMessage() {
            send.setEnabled(false);
            messageWindow.setText("");
        }

    }

    private class DestinationQueueDelegate
            extends DestinationQueueDelegateImplBase {

        private StepController widget;
        private boolean stepping;
        private final Object lock = new Object();
        private final Runnable oneStep;

        private DestinationQueueDelegate(DestinationQueue delegatee) {
            super(delegatee);
            oneStep = new OneStep();
        }

        private boolean isStepping() {
            return stepping;
        }

        private void setStepping(boolean mode) {
            stepping = mode;
            if (!stepping) {
                step();
            }
        }

        private class OneStep
                implements Runnable {
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
            synchronized (lock) {
                lock.notify();
            }
        }

        private void ensureWidget(MessageAddress address) {
            StepFrame frame = ensureFrame();
            if (widget == null) {
                widget = new StepController(this, address);
                frame.addWidget(widget, address);
            }
        }

        @Override
      public void dispatchNextMessage(AttributedMessage msg) {
            ensureWidget(msg.getTarget());
            widget.increment();
            if (!stepping) {
                super.dispatchNextMessage(msg);
            } else {
                synchronized (lock) {
                    widget.messageWait(msg);
                    // We're now locking up one of the available
                    // Threads!
                    try {
                        lock.wait();
                    } catch (InterruptedException ex) {
                    }
                }
                super.dispatchNextMessage(msg);
                widget.clearMessage();
            }
        }

    }

}
