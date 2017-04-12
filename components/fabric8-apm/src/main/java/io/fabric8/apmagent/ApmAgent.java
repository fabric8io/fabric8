/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.apmagent;

import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.fabric8.apmagent.metrics.ApmAgentContext;
import io.fabric8.apmagent.metrics.ThreadMetrics;
import io.fabric8.apmagent.strategy.sampling.SamplingStrategy;
import io.fabric8.apmagent.strategy.trace.TraceStrategy;
import io.fabric8.apmagent.utils.PropertyUtils;

import org.jolokia.jvmagent.JvmAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Java Agent for APM.
 */
public class ApmAgent implements ApmAgentMBean, ApmConfigurationChangeListener {
    public static final ApmAgent INSTANCE = new ApmAgent();
    private static final Logger LOG = LoggerFactory.getLogger(ApmAgent.class);

    final ApmConfiguration configuration = new ApmConfiguration();
    private AtomicBoolean initialized = new AtomicBoolean();
    private AtomicBoolean started = new AtomicBoolean();
    private final ApmAgentContext apmAgentContext;
    private Instrumentation instrumentation;
    private Strategy strategy;

    // The following is the entry point when loaded dynamically to inject
    // recorders from the target process.

    private ApmAgent() {
        this.apmAgentContext = new ApmAgentContext(this);
    }

    // The following is the entry point when loaded as a java agent along with
    // the target process on the java command line.

    public static void agentmain(final String args,
                                 final Instrumentation instrumentation) throws Exception {
        try {
            ApmAgent agent = ApmAgent.INSTANCE;
            if (agent.initialize(instrumentation, args)) {
                if (agent.getConfiguration().isStartJolokiaAgent()) {
                    JvmAgent.agentmain(args, instrumentation);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed in agentmain due " + e.getMessage(), e);
            throw e;
        }
    }

    public static void premain(String args, Instrumentation instrumentation) throws Exception {
        try {
            ApmAgent agent = ApmAgent.INSTANCE;
            if (agent.initialize(instrumentation, args)) {
                if (agent.getConfiguration().isStartJolokiaAgent()) {
                    JvmAgent.premain(args, instrumentation);
                }
                if (agent.getConfiguration().isAutoStartMetrics()) {
                    agent.startMetrics();
                }
            }
        } catch (Exception e) {
            LOG.error("Failed in premain due " + e.getMessage(), e);
            throw e;
        }
    }

    public static void enterMethod(String methodName) {
        if (INSTANCE.started.get()) {
            INSTANCE.apmAgentContext.enterMethod(Thread.currentThread(), methodName, false);
        }
    }

    public static void exitMethod(String methodName) {
        if (INSTANCE.started.get()) {
            INSTANCE.apmAgentContext.exitMethod(Thread.currentThread(), methodName, false);
        }
    }

    public List<String> getTransformedMethods() {
        if (isInitialized()) {
            return apmAgentContext.getTransformedMethods();
        }
        return Collections.EMPTY_LIST;
    }

    public List<String> getAllMethods() {
        if (isInitialized()) {
            return apmAgentContext.getAllMethods();
        }
        return Collections.EMPTY_LIST;
    }

    public List<ThreadMetrics> getThreadMetrics() {
        if (isInitialized()) {
            return apmAgentContext.getThreadMetrics();
        }
        return Collections.EMPTY_LIST;
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public ApmConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * @return false if already initialized, else true if is actually initialized
     */
    public boolean initialize(final Instrumentation instrumentation, String args) throws Exception {
        boolean result;
        if ((result = initialized.compareAndSet(false, true))) {
            this.instrumentation = instrumentation;
            PropertyUtils.setProperties(configuration, args);
            configuration.addChangeListener(this);
            apmAgentContext.initialize();
            ApmConfiguration.STRATEGY theStrategy = configuration.getStrategyImpl();
            switch (theStrategy) {
                case TRACE:
                    this.strategy = new TraceStrategy(apmAgentContext, instrumentation);
                    LOG.debug("Using Trace strategy");
                    break;
                default:
                    this.strategy = new SamplingStrategy(apmAgentContext);
                    LOG.debug("Using Sampling strategy");

            }
            this.strategy.initialize();

            //add shutdown hook
            Thread cleanup = new Thread() {

                @Override
                public void run() {
                    try {
                        ApmAgent apmAgent = ApmAgent.INSTANCE;
                        apmAgent.shutDown();
                    } catch (Exception e) {
                        LOG.warn("Failed to run shutdown hook due " + e.getMessage(), e);
                    }
                }

            };
            Runtime.getRuntime().addShutdownHook(cleanup);
        }
        return result;
    }

    public void startMetrics() {
        if (isInitialized() && started.compareAndSet(false, true)) {
            apmAgentContext.start();
            try {
                Strategy s = this.strategy;
                if (s != null) {
                    s.start();
                }
            } catch (Throwable e) {
                LOG.warn("Failed to start strategy due " + e.getMessage() + ". This exception is ignored.", e);
            }
        } else {
            LOG.debug("Metrics already started");
        }
    }

    public void stopMetrics() {
        if (started.compareAndSet(true, false)) {
            try {
                Strategy s = this.strategy;
                if (s != null) {
                    s.stop();
                }
            } catch (Throwable e) {
                LOG.warn("Failed to stop strategy due " + e.getMessage() + ". This exception is ignored.", e);
            }
            apmAgentContext.stop();
        }
    }

    // The following is the implementation for resetting the instrumentation.

    public void shutDown() {
        if (initialized.compareAndSet(true, false)) {
            stopMetrics();
            configuration.removeChangeListener(this);
            apmAgentContext.shutDown();
            try {
                //clean up
                Strategy s = this.strategy;
                if (s != null) {
                    s.shutDown();
                }
            } catch (Throwable e) {
                LOG.warn("Failed to shutdown due " + e.getMessage(), e);
            }
        }
    }

    @Override
    public boolean isStarted() {
        return started.get();
    }

    @Override
    public void configurationChanged() {
        if (started.get()) {
            if (configuration.isMethodMetricDepthChanged()) {
                apmAgentContext.methodMetricsDepthChanged();
            }
            if (configuration.isThreadMetricDepthChanged()) {
                apmAgentContext.threadMetricsDepthChanged();
            }
            if (configuration.isStrategyChanged()) {
                boolean hasStarted = this.started.get();
                if (initialized.get()) {
                    shutDown();
                    try {
                        //we need to restart
                        initialize(this.instrumentation, null);
                        if (hasStarted) {
                            startMetrics();
                        }
                    } catch (Exception e) {
                        LOG.warn("Could not re-initialize due " + e.getMessage() + ". This exception is ignored.", e);
                    }
                }
            }
        }
    }

}
