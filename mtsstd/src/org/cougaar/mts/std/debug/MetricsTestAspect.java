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
import org.cougaar.mts.base.AttributedMessage;
import org.cougaar.mts.base.SendQueue;
import org.cougaar.mts.base.SendQueueDelegateImplBase;
import org.cougaar.mts.base.StandardAspect;

/**
 * This test Aspect queries and updates the metric service on every message
 * send. The query is as given by @property "org.cougaar.metrics.query. The
 * update key is as given by @property org.cougaar.metrics.key. If @property
 * org.cougaar.metrics.callback is supplied it will also subscribe to the
 * specified formula.
 */
public class MetricsTestAspect
        extends StandardAspect
        implements Observer {

    MetricsUpdateService update;
    MetricsService svc;
    long lastUpdate = 0;

    public Object getDelegate(Object delegatee, Class<?> type) {
        if (type == SendQueue.class) {
            return new DummySendQueue((SendQueue) delegatee);
        } else {
            return null;
        }
    }

    public void load() {
        super.load();
        ServiceBroker sb = getServiceBroker();
        update = sb.getService(this, MetricsUpdateService.class, null);
        svc = sb.getService(this, MetricsService.class, null);

        String path = SystemProperties.getProperty("org.cougaar.metrics.callback");
        if (path != null) {
            svc.subscribeToValue(path, this);
            System.out.println("Subscribed to " + path);
        }
    }

    public void update(Observable o, Object arg) {
        long now = System.currentTimeMillis();
        long updateDelta = now - lastUpdate;
        // long value = ((Metric) arg).longValue();

        System.out.println("Update Time=" + updateDelta + " Value =" + arg);
    }

    private class DummySendQueue
            extends SendQueueDelegateImplBase {
        DummySendQueue(SendQueue delegatee) {
            super(delegatee);
        }

        public void sendMessage(AttributedMessage message) {
            runTest();
            super.sendMessage(message);
        }

    }

    public void runTest() {
        String path = SystemProperties.getProperty("org.cougaar.metrics.query");
        if (path != null) {
            Metric val = svc.getValue(path);
            System.out.println(path + "=" + val);
        }

        String key = SystemProperties.getProperty("org.cougaar.metrics.key");
        if (key != null) {
            Metric m =
                    new MetricImpl(new Long(System.currentTimeMillis()),
                                   0.3,
                                   "",
                                   "MetricsTestAspect");
            System.out.println("Published " + key + "=" + m);
            update.updateValue(key, m);
            lastUpdate = System.currentTimeMillis();
        }

    }

}
