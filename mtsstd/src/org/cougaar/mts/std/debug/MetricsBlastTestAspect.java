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

import java.util.Observable;
import java.util.Observer;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.qos.metrics.MetricImpl;
import org.cougaar.core.qos.metrics.MetricsService;
import org.cougaar.core.qos.metrics.MetricsUpdateService;
import org.cougaar.mts.base.StandardAspect;

/**
 * This test Aspect throws lots of data into the metrics service and subscrobes
 * to a formula which uses that data.
 */
public class MetricsBlastTestAspect
        extends StandardAspect
        implements Observer {

    MetricsUpdateService update;
    MetricsService svc;
    Thread blasterThread = null;

    String key, path;

    long callbackCount = 0;
    long blastCount = 0;
    long lastCallbackDelay = 0;
    long lastPrintTime = 0;

    private void dumpCounters(long now) {
        if (1000 < now - lastPrintTime) {
            System.out.println("blast count=" + blastCount + " callback count=" + callbackCount
                    + " Last delay=" + lastCallbackDelay);
            lastPrintTime = now;
        }
    }

    @Override
   public Object getDelegate(Object delegatee, Class<?> type) {
        return null;

    }

    @Override
   public void load() {
        super.load();
        ServiceBroker sb = getServiceBroker();
        update = sb.getService(this, MetricsUpdateService.class, null);
        svc = sb.getService(this, MetricsService.class, null);

        path = SystemProperties.getProperty("org.cougaar.metrics.callback");
        if (path != null) {
            svc.subscribeToValue(path, this);
            System.out.println("Subscribed to " + path);

            key = SystemProperties.getProperty("org.cougaar.metrics.key");
            if (key != null) {
                System.out.println("Blasting to " + key);
                blasterThread = new Thread(new Blaster(), "blaster");
                blasterThread.start();
            }

        }
    }

    public void update(Observable o, Object arg) {
        callbackCount++;
        long now = System.currentTimeMillis();
        long value = ((Metric) arg).longValue();
        lastCallbackDelay = now - value;
    }

    class Blaster
            implements Runnable {
        long now;
        long startTime;

        public void run() {
            // Wait a bit for the Node to initialize
            try {
                Thread.sleep(10000);
            } catch (InterruptedException xxx) {
            }
            // Loop forever turning blaster on and off
            while (true) {
                System.out.println("Starting Blaster");
                startTime = System.currentTimeMillis();
                // Blast for 5 seconds and then stop
                while (5000 > now - startTime) {
                    now = System.currentTimeMillis();
                    Metric m =
                            new MetricImpl(new Long(System.currentTimeMillis()),
                                           0.3,
                                           "",
                                           "MetricsTestAspect");
                    update.updateValue(key, m);
                    blastCount++;
                    dumpCounters(now);
                    try {
                        Thread.sleep(0);
                    } catch (InterruptedException xxx) {
                    }
                }

                System.out.println("Stopped Blaster");
                startTime = System.currentTimeMillis();
                now = System.currentTimeMillis();
                // wait and see how long it takes for the updates to stop
                while (10000 > now - startTime) {
                    dumpCounters(now);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException xxx) {
                    }
                    now = System.currentTimeMillis();
                }
            }
        }
    }
}
